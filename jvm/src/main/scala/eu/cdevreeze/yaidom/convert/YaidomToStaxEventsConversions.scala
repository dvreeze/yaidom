/*
 * Copyright 2011-2017 Chris de Vreeze
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.cdevreeze.yaidom.convert

import java.{util => jutil}

import scala.collection.immutable
import scala.jdk.CollectionConverters._

import eu.cdevreeze.yaidom.convert.YaidomToStaxEventsConversions.XmlEventsProducer
import eu.cdevreeze.yaidom.core.Declarations
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.core.jvm.JavaQNames
import eu.cdevreeze.yaidom.simple.Comment
import eu.cdevreeze.yaidom.simple.Document
import eu.cdevreeze.yaidom.simple.DocumentConverter
import eu.cdevreeze.yaidom.simple.Elem
import eu.cdevreeze.yaidom.simple.ElemConverter
import eu.cdevreeze.yaidom.simple.EntityRef
import eu.cdevreeze.yaidom.simple.Node
import eu.cdevreeze.yaidom.simple.ProcessingInstruction
import eu.cdevreeze.yaidom.simple.Text
import javax.xml.stream.XMLEventFactory
import javax.xml.stream.events.Attribute
import javax.xml.stream.events.EndElement
import javax.xml.stream.events.Namespace
import javax.xml.stream.events.StartElement
import javax.xml.stream.events.XMLEvent

/**
 * Converter from yaidom nodes to StAX events, in particular from [[eu.cdevreeze.yaidom.simple.Elem]] to `immutable.IndexedSeq[XMLEvent]`,
 * and from  [[eu.cdevreeze.yaidom.simple.Document]] to `immutable.IndexedSeq[XMLEvent]`.
 *
 * @author Chris de Vreeze
 */
trait YaidomToStaxEventsConversions extends ElemConverter[XmlEventsProducer] with DocumentConverter[XmlEventsProducer] {

  /** Returns encoding used in the createStartDocument call. Can be overridden. */
  def encoding: String = "UTF-8"

  /** Converts a yaidom `Document` to a function from `XMLEventFactory`s to sequences of `XMLEvent` instances */
  final def convertDocument(doc: Document): XmlEventsProducer = {
    { (xmlEventFactory: XMLEventFactory) =>
      val startDocument = xmlEventFactory.createStartDocument(encoding)
      // For the line separator, see for example
      // http://xerces.apache.org/xerces-j/apiDocs/org/apache/xml/serialize/OutputFormat.html#setLineSeparator(java.lang.String).
      val newline = xmlEventFactory.createCharacters("\n")
      val piEvents: immutable.IndexedSeq[XMLEvent] =
        doc.processingInstructions flatMap { pi => convertProcessingInstruction(pi, xmlEventFactory) }
      val commentEvents: immutable.IndexedSeq[XMLEvent] =
        doc.comments flatMap { com => convertComment(com, xmlEventFactory) }
      val docElmEvents = convertElem(doc.documentElement, xmlEventFactory, Scope.Empty)
      val endDocument = xmlEventFactory.createEndDocument

      immutable.IndexedSeq(startDocument, newline) ++ piEvents ++ commentEvents ++ docElmEvents ++ immutable.IndexedSeq(endDocument)
    }
  }

  /**
   * Converts a yaidom `Elem` to a function from `XMLEventFactory`s to sequences of `XMLEvent` instances.
   * The assumed parent scope is the empty scope, so the namespace declarations of the outer "start element event" follow from the
   * scope of the passed `Elem`.
   */
  final def convertElem(elm: Elem): XmlEventsProducer = {
    { (xmlEventFactory: XMLEventFactory) =>
      val startDocument = xmlEventFactory.createStartDocument
      // For the line separator, see for example
      // http://xerces.apache.org/xerces-j/apiDocs/org/apache/xml/serialize/OutputFormat.html#setLineSeparator(java.lang.String).
      val newline = xmlEventFactory.createCharacters("\n")
      val nonDocEvents = convertElem(elm, xmlEventFactory, Scope.Empty)
      val endDocument = xmlEventFactory.createEndDocument

      immutable.IndexedSeq(startDocument, newline) ++ nonDocEvents ++ immutable.IndexedSeq(endDocument)
    }
  }

  /**
   * Converts a yaidom node to a sequence of `XMLEvent` instances, given an `XMLEventFactory`.
   * The given parent scope is used, in case the node is an `Elem`.
   */
  final def convertNode(node: Node, xmlEventFactory: XMLEventFactory, parentScope: Scope): immutable.IndexedSeq[XMLEvent] = {
    node match {
      case e: Elem => convertElem(e, xmlEventFactory, parentScope)
      case t: Text => convertText(t, xmlEventFactory)
      case pi: ProcessingInstruction => convertProcessingInstruction(pi, xmlEventFactory)
      // Difficult to convert yaidom EntityRef to StAX EntityReference, because of missing declaration
      case er: EntityRef => immutable.IndexedSeq[XMLEvent]()
      case c: Comment => convertComment(c, xmlEventFactory)
    }
  }

  /**
   * Converts a yaidom `Elem` to a sequence of `XMLEvent` instances, given an `XMLEventFactory`.
   * The given parent scope is used, that is, the namespace declarations of the outer "start element event" is
   * `parentScope.relativize(elm.scope)`.
   */
  final def convertElem(elm: Elem, xmlEventFactory: XMLEventFactory, parentScope: Scope): immutable.IndexedSeq[XMLEvent] = {
    // Not tail-recursive, but the recursion depth should be limited

    val startEvent: XMLEvent = createStartElement(elm, xmlEventFactory, parentScope)
    val childEvents: immutable.IndexedSeq[XMLEvent] = elm.children flatMap { ch => convertNode(ch, xmlEventFactory, elm.scope) }
    val endEvent: XMLEvent = createEndElement(elm, xmlEventFactory, parentScope)

    immutable.IndexedSeq(startEvent) ++ childEvents ++ immutable.IndexedSeq(endEvent)
  }

  /**
   * Converts a yaidom `Text` to a sequence of `XMLEvent` instances, given an `XMLEventFactory`.
   */
  final def convertText(text: Text, xmlEventFactory: XMLEventFactory): immutable.IndexedSeq[XMLEvent] = {
    val cdata = text.isCData
    val event =
      if (cdata) {
        xmlEventFactory.createCData(text.text)
      } else {
        xmlEventFactory.createCharacters(text.text)
      }

    immutable.IndexedSeq(event)
  }

  /**
   * Converts a yaidom `ProcessingInstruction` to a sequence of `XMLEvent` instances, given an `XMLEventFactory`.
   */
  final def convertProcessingInstruction(
    processingInstruction: ProcessingInstruction, xmlEventFactory: XMLEventFactory): immutable.IndexedSeq[XMLEvent] = {

    val event = xmlEventFactory.createProcessingInstruction(processingInstruction.target, processingInstruction.data)
    immutable.IndexedSeq(event)
  }

  /**
   * Converts a yaidom `Comment` to a sequence of `XMLEvent` instances, given an `XMLEventFactory`.
   */
  final def convertComment(comment: Comment, xmlEventFactory: XMLEventFactory): immutable.IndexedSeq[XMLEvent] = {
    val event = xmlEventFactory.createComment(comment.text)
    immutable.IndexedSeq(event)
  }

  private def createStartElement(elm: Elem, xmlEventFactory: XMLEventFactory, parentScope: Scope): StartElement = {
    val namespaceDeclarations: Declarations = parentScope.relativize(elm.scope)

    val javaQName = JavaQNames.enameToJavaQName(elm.resolvedName, elm.qname.prefixOption)

    val attrScope = elm.attributeScope

    val attributeIterable: immutable.IndexedSeq[Attribute] = {
      val result = elm.attributes map { kv =>
        val attrQName = kv._1
        val value = kv._2
        val attrEName = attrScope.resolveQNameOption(attrQName).getOrElse(sys.error(
          s"Attribute name '${attrQName}' should resolve to an EName in scope [${attrScope}]"))
        val attrJavaQName = JavaQNames.enameToJavaQName(attrEName, attrQName.prefixOption)

        xmlEventFactory.createAttribute(attrJavaQName, value)
      }
      result
    }

    val attributes: jutil.Iterator[Attribute] =
      (new jutil.ArrayList[Attribute](attributeIterable.asJava)).iterator

    // scalastyle:off null
    val namespaceIterable: immutable.IndexedSeq[Namespace] = {
      val result = namespaceDeclarations.prefixNamespaceMap map { kv =>
        val prefix = kv._1
        val nsUri = kv._2

        if ((prefix eq null) || (prefix == "")) {
          (prefix -> xmlEventFactory.createNamespace(nsUri))
        } else {
          (prefix -> xmlEventFactory.createNamespace(prefix, nsUri))
        }
      }
      result.values.toIndexedSeq
    }

    val namespaces: jutil.Iterator[Namespace] =
      (new jutil.ArrayList[Namespace](namespaceIterable.asJava)).iterator

    xmlEventFactory.createStartElement(javaQName, attributes, namespaces)
  }

  private def createEndElement(elm: Elem, xmlEventFactory: XMLEventFactory, parentScope: Scope): EndElement = {
    val namespaceDeclarations: Declarations = parentScope.relativize(elm.scope)

    val javaQName = JavaQNames.enameToJavaQName(elm.resolvedName, elm.qname.prefixOption)

    // scalastyle:off null
    val namespaceOutOfScopeIterable: immutable.IndexedSeq[Namespace] = {
      val result = namespaceDeclarations.prefixNamespaceMap map { kv =>
        val prefix = kv._1
        val nsUri = kv._2

        if ((prefix eq null) || (prefix == "")) {
          (prefix -> xmlEventFactory.createNamespace(nsUri))
        } else {
          (prefix -> xmlEventFactory.createNamespace(prefix, nsUri))
        }
      }
      result.values.toIndexedSeq
    }

    val namespacesOutOfScope: jutil.Iterator[Namespace] =
      (new jutil.ArrayList[Namespace](namespaceOutOfScopeIterable.asJava)).iterator

    xmlEventFactory.createEndElement(javaQName, namespacesOutOfScope)
  }
}

object YaidomToStaxEventsConversions {

  /** Producer of an `IndexedSeq[XMLEvent]`, given a `XMLEventFactory` as factory of StAX events */
  type XmlEventsProducer = (XMLEventFactory => immutable.IndexedSeq[XMLEvent])
}
