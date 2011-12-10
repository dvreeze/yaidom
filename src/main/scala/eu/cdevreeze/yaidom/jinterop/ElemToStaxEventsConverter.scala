/*
 * Copyright 2011 Chris de Vreeze
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

package eu.cdevreeze.yaidom
package jinterop

import java.{ util => jutil }
import javax.xml.XMLConstants
import javax.xml.stream._
import javax.xml.stream.events.{ ProcessingInstruction => _, _ }
import scala.collection.JavaConverters._
import scala.collection.{ immutable, mutable }
import ElemToStaxEventsConverter._

/**
 * Converter from Elem to immutable.Seq[XMLEvent]
 *
 * @author Chris de Vreeze
 */
trait ElemToStaxEventsConverter extends ElemConverter[XmlEventsProducer] {

  def convertElem(elm: Elem): XmlEventsProducer = {
    { (xmlEventFactory: XMLEventFactory) =>
      val startDocument = xmlEventFactory.createStartDocument
      val nonDocEvents = convertElem(elm, xmlEventFactory)
      val endDocument = xmlEventFactory.createEndDocument

      immutable.IndexedSeq(startDocument) ++ nonDocEvents ++ immutable.Seq(endDocument)
    }
  }

  private def convertNode(node: Node, xmlEventFactory: XMLEventFactory): immutable.IndexedSeq[XMLEvent] = {
    node match {
      case e: Elem => convertElem(e, xmlEventFactory)
      case t: Text => convertText(t, xmlEventFactory)
      case pi: ProcessingInstruction => convertProcessingInstruction(pi, xmlEventFactory)
      case t: CData => convertCData(t, xmlEventFactory)
      case er: EntityRef => immutable.IndexedSeq[XMLEvent]() // TODO Implement
    }
  }

  private def convertElem(elm: Elem, xmlEventFactory: XMLEventFactory): immutable.IndexedSeq[XMLEvent] = {
    // Not tail-recursive, but the recursion depth should be limited

    val startEvent: XMLEvent = createStartElement(elm, xmlEventFactory)
    val childEvents: immutable.IndexedSeq[XMLEvent] = elm.children.flatMap(ch => convertNode(ch, xmlEventFactory))
    val endEvent: XMLEvent = createEndElement(elm, xmlEventFactory)

    immutable.IndexedSeq(startEvent) ++ childEvents ++ immutable.IndexedSeq(endEvent)
  }

  private def convertText(text: Text, xmlEventFactory: XMLEventFactory): immutable.IndexedSeq[XMLEvent] = {
    val event = xmlEventFactory.createCharacters(text.text)
    immutable.IndexedSeq(event)
  }

  private def convertProcessingInstruction(
    processingInstruction: ProcessingInstruction, xmlEventFactory: XMLEventFactory): immutable.IndexedSeq[XMLEvent] = {

    val event = xmlEventFactory.createProcessingInstruction(processingInstruction.target, processingInstruction.data)
    immutable.IndexedSeq(event)
  }

  private def convertCData(cdata: CData, xmlEventFactory: XMLEventFactory): immutable.IndexedSeq[XMLEvent] = {
    val event = xmlEventFactory.createCData(cdata.text)
    immutable.IndexedSeq(event)
  }

  private def createStartElement(elm: Elem, xmlEventFactory: XMLEventFactory): StartElement = {
    // TODO Take parent element scope into account
    val javaQName = elm.resolvedName.toJavaQName(elm.qname.prefixOption)

    val attributeList: List[Attribute] = elm.attributes.map(kv => {
      val attrQName = kv._1
      val value = kv._2
      val attrExpandedName = elm.attributeScope.resolveQName(attrQName).getOrElse(sys.error(
        "Attribute name '%s' should resolve to an ExpandedName in scope [%s]".format(attrQName, elm.attributeScope)))
      val attrJavaQName = attrExpandedName.toJavaQName(attrQName.prefixOption)

      (attrQName -> xmlEventFactory.createAttribute(attrJavaQName, value))
    }).values.toList

    val attributes: jutil.Iterator[Attribute] = new jutil.ArrayList[Attribute](attributeList.toBuffer.asJava).iterator

    val namespaceList: List[Namespace] = elm.scope.toMap.map(kv => {
      val prefix = kv._1
      val nsUri = kv._2

      if ((prefix eq null) || (prefix == "")) {
        (prefix -> xmlEventFactory.createNamespace(nsUri))
      } else {
        (prefix -> xmlEventFactory.createNamespace(prefix, nsUri))
      }
    }).values.toList

    val namespaces: jutil.Iterator[Namespace] = new jutil.ArrayList[Namespace](namespaceList.toBuffer.asJava).iterator

    xmlEventFactory.createStartElement(javaQName, attributes, namespaces)
  }

  private def createEndElement(elm: Elem, xmlEventFactory: XMLEventFactory): EndElement = {
    // TODO Take parent element scope into account
    val javaQName = elm.resolvedName.toJavaQName(elm.qname.prefixOption)

    // TODO Namespaces that have gone out of scope are now ignored!
    xmlEventFactory.createEndElement(javaQName, null)
  }
}

object ElemToStaxEventsConverter {

  type XmlEventsProducer = (XMLEventFactory => immutable.Seq[XMLEvent])
}
