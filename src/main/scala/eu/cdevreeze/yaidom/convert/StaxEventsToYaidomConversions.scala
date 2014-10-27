/*
 * Copyright 2011-2014 Chris de Vreeze
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

import java.net.URI

import scala.collection.BufferedIterator
import scala.collection.Iterator
import scala.collection.JavaConverters.asScalaIteratorConverter
import scala.collection.immutable
import scala.collection.mutable

import eu.cdevreeze.yaidom.core.Declarations
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.QNameProvider
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.simple.Comment
import eu.cdevreeze.yaidom.simple.ConverterToDocument
import eu.cdevreeze.yaidom.simple.Document
import eu.cdevreeze.yaidom.simple.Elem
import eu.cdevreeze.yaidom.simple.EntityRef
import eu.cdevreeze.yaidom.simple.Node
import eu.cdevreeze.yaidom.simple.ProcessingInstruction
import eu.cdevreeze.yaidom.simple.Text
import javax.xml.XMLConstants
import javax.xml.namespace.{ QName => JQName }
import javax.xml.stream.events.Attribute
import javax.xml.stream.events.Characters
import javax.xml.stream.events.EndElement
import javax.xml.stream.events.EntityReference
import javax.xml.stream.events.Namespace
import javax.xml.stream.events.StartDocument
import javax.xml.stream.events.StartElement
import javax.xml.stream.events.XMLEvent

/**
 * Converter from StAX events to yaidom nodes, in particular from `immutable.IndexedSeq[XMLEvent]` to [[eu.cdevreeze.yaidom.simple.Elem]] and
 * to [[eu.cdevreeze.yaidom.simple.Document]].
 *
 * There are also analogous conversions that take an `Iterator[XMLEvent]` instead. These can be used to reduce memory usage.
 *
 * This converter regards the input event stream more like an "ElemBuilder" than an "Elem", in that namespace declarations instead
 * of scopes are extracted from input "elements", and in that conversions to yaidom Elems take an additional parent scope
 * parameter (against which namespace declarations are resolved to get the scope of the yaidom element).
 *
 * @author Chris de Vreeze
 */
trait StaxEventsToYaidomConversions extends ConverterToDocument[immutable.IndexedSeq[XMLEvent]] {
  import StaxEventsToYaidomConversions.EventWithDepth
  import StaxEventsToYaidomConversions.DocumentResult
  import StaxEventsToYaidomConversions.ElemResult

  /**
   * Converts the given sequence of `XMLEvent` instances to a yaidom `Document`. Invokes `convertToDocument(v.iterator)`.
   */
  final def convertToDocument(v: immutable.IndexedSeq[XMLEvent]): Document = {
    convertToDocument(v.iterator)
  }

  /**
   * Converts the given iterator of `XMLEvent` instances to a yaidom `Document`.
   *
   * First drops events until a "start document" event is found. After conversion, no "start/end document/element" events may be left.
   */
  final def convertToDocument(v: Iterator[XMLEvent]): Document = {
    val eventIterator: Iterator[XMLEvent] = v dropWhile { ev => !ev.isStartDocument }

    val eventWithDepthIterator: BufferedIterator[EventWithDepth] = {
      var depth = 0
      val result = eventIterator map { ev =>
        ev match {
          case start: StartElement =>
            depth += 1; new EventWithDepth(start, depth)
          case end: EndElement =>
            val currDepth = depth; depth -= 1; new EventWithDepth(end, currDepth)
          case _ => new EventWithDepth(ev, depth)
        }
      }
      result.buffered
    }

    val result = eventsToDocument(eventWithDepthIterator)

    require {
      result.remainder forall { ev =>
        !ev.event.isStartElement && !ev.event.isEndElement && !ev.event.isStartDocument && !ev.event.isEndDocument
      }
    }
    result.doc
  }

  /**
   * Converts the given sequence of `XMLEvent` instances to a yaidom `Elem`. Invokes `convertToElem(v.iterator, parentScope)`.
   *
   * Be careful: the namespaces inherited by the "start element event", if any, are ignored! In other words, the ancestry of
   * the passed events is entirely ignored. This may cause an exception to be thrown, if there are indeed such namespaces,
   * unless they are a subset of the passed parent scope.
   */
  final def convertToElem(v: immutable.IndexedSeq[XMLEvent], parentScope: Scope): Elem = {
    convertToElem(v.iterator, parentScope)
  }

  /**
   * Converts the given iterator of `XMLEvent` instances to a yaidom `Elem`.
   *
   * First drops events until a "start element" event is found. After conversion, no "start/end element" events may be left.
   * The given parent scope is used.
   *
   * Be careful: the namespaces inherited by the "start element event", if any, are ignored! In other words, the ancestry of
   * the passed events is entirely ignored. This may cause an exception to be thrown, if there are indeed such namespaces,
   * unless they are a subset of the passed parent scope.
   */
  final def convertToElem(v: Iterator[XMLEvent], parentScope: Scope): Elem = {
    val eventIterator: Iterator[XMLEvent] = v dropWhile { ev => !ev.isStartElement }

    val eventWithDepthIterator: BufferedIterator[EventWithDepth] = {
      var depth = 0
      val result = eventIterator map { ev =>
        ev match {
          case start: StartElement =>
            depth += 1; new EventWithDepth(start, depth)
          case end: EndElement =>
            val currDepth = depth; depth -= 1; new EventWithDepth(end, currDepth)
          case _ => new EventWithDepth(ev, depth)
        }
      }
      result.buffered
    }

    val result = eventsToElem(eventWithDepthIterator, parentScope)

    require {
      result.remainder forall { ev => !ev.event.isStartElement && !ev.event.isEndElement }
    }
    result.elem
  }

  /** Converts a StAX `Characters` event to a yaidom `Text` */
  final def convertToText(event: Characters): Text = {
    val cdata = event.isCData
    Text(text = event.getData, isCData = cdata)
  }

  /** Converts a StAX `EntityReference` event to a yaidom `EntityRef` */
  final def convertToEntityRef(event: EntityReference): EntityRef = EntityRef(event.getName)

  /** Converts a StAX `ProcessingInstruction` event to a yaidom `ProcessingInstruction` */
  final def convertToProcessingInstruction(event: javax.xml.stream.events.ProcessingInstruction): ProcessingInstruction =
    ProcessingInstruction(event.getTarget, event.getData)

  /** Converts a StAX `Comment` event to a yaidom `Comment` */
  final def convertToComment(event: javax.xml.stream.events.Comment): Comment = Comment(event.getText)

  private def eventsToDocument(eventWithDepthIterator: BufferedIterator[EventWithDepth]): DocumentResult = {
    require(eventWithDepthIterator.hasNext)

    var it = eventWithDepthIterator

    val head = it.next()
    require(head.event.isStartDocument)

    val startDocument: StartDocument = head.event.asInstanceOf[StartDocument]

    def startsWithEndDocument(eventIterator: BufferedIterator[EventWithDepth]): Boolean = {
      if (!eventIterator.hasNext) false else {
        val hd: EventWithDepth = eventIterator.head
        hd.event.isEndDocument
      }
    }

    val pis = mutable.Buffer[ProcessingInstruction]()
    val comments = mutable.Buffer[Comment]()
    var docElement: Elem = null

    // Imperative code
    while (it.hasNext && !startsWithEndDocument(it)) {
      val nextHead = it.head

      nextHead.event match {
        case ev if ev.isStartElement => {
          require(docElement eq null, "Only 1 document element allowed and required")
          val result = eventsToElem(it, Scope.Empty)
          docElement = result.elem
          it = result.remainder
        }
        case ev: javax.xml.stream.events.ProcessingInstruction => {
          val pi = convertToProcessingInstruction(ev)
          pis += pi
          it.next()
        }
        case ev: javax.xml.stream.events.Comment => {
          val com = convertToComment(ev)
          comments += com
          it.next()
        }
        case _ => it.next()
      }
    }
    require(docElement ne null, "There must be 1 document element")
    require(startsWithEndDocument(it))

    it.next()

    val uriOption: Option[URI] =
      if ((startDocument.getSystemId eq null) || (startDocument.getSystemId == "")) None else {
        Some(new URI(startDocument.getSystemId))
      }

    val doc: Document = new Document(
      uriOption = uriOption,
      documentElement = docElement,
      processingInstructions = pis.toIndexedSeq,
      comments = comments.toIndexedSeq)
    new DocumentResult(doc, it)
  }

  private def eventsToElem(eventWithDepthIterator: BufferedIterator[EventWithDepth], parentScope: Scope): ElemResult = {
    require(eventWithDepthIterator.hasNext)

    var it = eventWithDepthIterator

    val head = it.next()
    require(head.event.isStartElement)

    val startElement: StartElement = head.event.asStartElement

    val elem: Elem = eventToElem(startElement, parentScope)

    def startsWithMatchingEndElement(eventIterator: BufferedIterator[EventWithDepth]): Boolean = {
      if (!eventIterator.hasNext) false else {
        val hd: EventWithDepth = eventIterator.head
        (hd.depth == head.depth) && (hd.event.isEndElement)
      }
    }

    val children = mutable.Buffer[Node]()

    // Imperative code
    while (it.hasNext && !startsWithMatchingEndElement(it)) {
      val nextHead = it.head

      nextHead.event match {
        case ev: StartElement => {
          // Recursive call (not tail-recursive, but recursion depth is rather limited)
          val result = eventsToElem(it, elem.scope)
          val ch = result.elem
          children += ch
          it = result.remainder
        }
        case ev: Characters => {
          val ch = convertToText(ev)
          children += ch
          it.next()
        }
        case ev: EntityReference => {
          val ch = convertToEntityRef(ev)
          children += ch
          it.next()
        }
        case ev: javax.xml.stream.events.ProcessingInstruction => {
          val ch = convertToProcessingInstruction(ev)
          children += ch
          it.next()
        }
        case ev: javax.xml.stream.events.Comment => {
          val ch = convertToComment(ev)
          children += ch
          it.next()
        }
        case _ => it.next()
      }
    }
    require(startsWithMatchingEndElement(it))
    val endElementEv = it.next.event.asEndElement
    require(endElementEv.getName == head.event.asStartElement.getName)

    val elemWithChildren: Elem = elem.withChildren(children.toIndexedSeq)
    new ElemResult(elemWithChildren, it)
  }

  private def eventToElem(startElement: StartElement, parentScope: Scope)(implicit qnameProvider: QNameProvider): Elem = {
    val declarations: Declarations = {
      val namespaces: List[Namespace] = startElement.getNamespaces.asScala.toList collect { case ns: Namespace => ns }
      // The Namespaces can also hold namespace undeclarations (with null or the empty string as namespace URI)

      val declaredScope: Scope = {
        val defaultNs = {
          val result = namespaces filter { _.isDefaultNamespaceDeclaration } map { ns => Option(ns.getNamespaceURI).getOrElse("") } filter { _ != "" }
          result.headOption
        }
        val prefScope = {
          val result = namespaces filterNot { _.isDefaultNamespaceDeclaration } map { ns => (ns.getPrefix -> Option(ns.getNamespaceURI).getOrElse("")) } filter { _._2 != "" }
          result.toMap
        }
        val defaultNsMap: Map[String, String] = if (defaultNs.isEmpty) Map() else Map("" -> defaultNs.get)
        Scope.from(defaultNsMap ++ prefScope)
      }
      val undeclaredOptionalPrefixes: Set[Option[String]] = {
        val defaultNs = {
          val result = namespaces filter { _.isDefaultNamespaceDeclaration } map { ns => Option(ns.getNamespaceURI).getOrElse("") } filter { _ == "" }
          result.headOption
        }
        val defaultNsUndeclared = defaultNs.isDefined

        val undeclaredPrefixOptions: Set[Option[String]] = {
          val result = namespaces filterNot { _.isDefaultNamespaceDeclaration } map { ns => (ns.getPrefix -> Option(ns.getNamespaceURI).getOrElse("")) } filter { _._2 == "" } map { kv => Some(kv._1) }
          result.toSet
        }

        if (defaultNsUndeclared)
          Set(None) ++ undeclaredPrefixOptions
        else
          undeclaredPrefixOptions
      }
      val undeclaredMap: Map[String, String] =
        (undeclaredOptionalPrefixes map (prefOption => if (prefOption.isEmpty) "" -> "" else prefOption.get -> "")).toMap
      Declarations.from(declaredScope.prefixNamespaceMap ++ undeclaredMap)
    }
    val currScope = parentScope.resolve(declarations)

    val elemPrefixOption: Option[String] = prefixOptionFromJavaQName(startElement.getName)
    val elemQName = qnameProvider.getQName(elemPrefixOption, startElement.getName.getLocalPart)

    val currAttrs: immutable.IndexedSeq[(QName, String)] = {
      val attributes: List[Attribute] = startElement.getAttributes.asScala.toList collect { case a: Attribute => a }
      val result = attributes map { a =>
        val prefixOption: Option[String] = prefixOptionFromJavaQName(a.getName)
        val qname = qnameProvider.getQName(prefixOption, a.getName.getLocalPart)
        (qname -> a.getValue)
      }
      result.toIndexedSeq
    }

    // Line and column numbers can be retrieved from startElement.getLocation, but are ignored here

    Elem(elemQName, currAttrs, currScope, immutable.IndexedSeq())
  }

  /** Gets an optional prefix from a `javax.xml.namespace.QName` */
  private def prefixOptionFromJavaQName(jqname: JQName): Option[String] = {
    val prefix: String = jqname.getPrefix
    if ((prefix eq null) || (prefix == XMLConstants.DEFAULT_NS_PREFIX)) None else Some(prefix)
  }
}

private object StaxEventsToYaidomConversions {

  private class EventWithDepth(val event: XMLEvent, val depth: Int) extends Immutable

  private class ElemResult(val elem: Elem, val remainder: BufferedIterator[EventWithDepth]) extends Immutable

  private class DocumentResult(val doc: Document, val remainder: BufferedIterator[EventWithDepth]) extends Immutable
}
