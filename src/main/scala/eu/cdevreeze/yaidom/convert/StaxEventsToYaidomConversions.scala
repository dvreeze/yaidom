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

import java.net.URI
import java.nio.charset.Charset
import java.{ util => jutil }

import scala.Vector
import scala.collection.BufferedIterator
import scala.collection.Iterator
import scala.collection.JavaConverters.asScalaIteratorConverter
import scala.collection.immutable
import scala.collection.mutable

import eu.cdevreeze.yaidom.core.Declarations
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.QNameProvider
import eu.cdevreeze.yaidom.core.AncestryPath
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.core.XmlDeclaration
import eu.cdevreeze.yaidom.simple.CanBeDocumentChild
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
import javax.xml.stream.XMLEventReader
import javax.xml.stream.events.Attribute
import javax.xml.stream.events.Characters
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
trait StaxEventsToYaidomConversions extends ConverterToDocument[Iterator[XMLEvent]] {

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

    val eventStateIterator: BufferedIterator[EventWithAncestry] = {
      val result = convertToEventWithAncestryIterator(eventIterator)
      result.buffered
    }

    val doc = takeDocument(eventStateIterator)

    require {
      eventStateIterator forall { ev =>
        !ev.event.isStartElement && !ev.event.isEndElement && !ev.event.isStartDocument && !ev.event.isEndDocument
      }
    }
    doc
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

    val eventStateIterator: BufferedIterator[EventWithAncestry] = {
      val result = convertToEventWithAncestryIterator(eventIterator)
      result.buffered
    }

    val elem = takeElem(eventStateIterator)

    require {
      eventStateIterator forall { ev => !ev.event.isStartElement && !ev.event.isEndElement }
    }
    elem
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

  /**
   * Converts the iterator of XMLEvents to an iterator of EventWithAncestry objects.
   * This method can also be used in streaming scenarios, where only chunks of a large XML stream are kept in memory
   * at any moment in time.
   */
  final def convertToEventWithAncestryIterator(events: Iterator[XMLEvent]): Iterator[EventWithAncestry] = {
    var state: Option[AncestryPath] = None

    events map { event =>
      val nextState = EventWithAncestry.nextAncestryPathOption(state, event)
      state = nextState
      new EventWithAncestry(event, nextState)
    }
  }

  /**
   * Given a buffered iterator of EventWithAncestry objects, converts the (initial) events to a Document. The first event must
   * be a StartDocument event, or else an exception is thrown.
   *
   * The input iterator should come from a call to method `convertToEventWithAncestryIterator`.
   */
  final def takeDocument(eventStateIterator: BufferedIterator[EventWithAncestry]): Document = {
    require(eventStateIterator.hasNext)

    var it = eventStateIterator

    val head = it.next()
    require(head.event.isStartDocument, s"Not a StartDocument event: ${head.event}")

    val startDocument: StartDocument = head.event.asInstanceOf[StartDocument]

    def startsWithEndDocument(eventIterator: BufferedIterator[EventWithAncestry]): Boolean = {
      if (!eventIterator.hasNext) false else {
        val hd: EventWithAncestry = eventIterator.head
        hd.event.isEndDocument
      }
    }

    val xmlVersionOption = Option(startDocument.getVersion)
    val xmlDeclOption = xmlVersionOption map { xmlVersion =>
      XmlDeclaration.fromVersion(xmlVersion).
        withEncodingOption(if (startDocument.encodingSet()) Some(Charset.forName(startDocument.getCharacterEncodingScheme)) else None).
        withStandaloneOption(if (startDocument.standaloneSet) Some(startDocument.isStandalone) else None)
    }

    val docChildren = mutable.Buffer[CanBeDocumentChild]()

    // Imperative code
    while (it.hasNext && !startsWithEndDocument(it)) {
      val nextHead = it.head

      nextHead.event match {
        case ev if ev.isStartElement => {
          require(docChildren.collect({ case e: Elem => e }).isEmpty, "Only 1 document element allowed and required")
          val docElem = takeElem(it)
          docChildren += docElem
        }
        case ev: javax.xml.stream.events.ProcessingInstruction => {
          val pi = convertToProcessingInstruction(ev)
          docChildren += pi

          it.next()
        }
        case ev: javax.xml.stream.events.Comment => {
          val com = convertToComment(ev)
          docChildren += com
          it.next()
        }
        case _ => it.next()
      }
    }
    require(docChildren.collect({ case e: Elem => e }).size == 1, "There must be 1 document element")
    require(startsWithEndDocument(it))

    it.next()

    val uriOption: Option[URI] =
      if ((startDocument.getSystemId eq null) || (startDocument.getSystemId == "")) None else {
        Some(new URI(startDocument.getSystemId))
      }

    val doc: Document = new Document(
      uriOption = uriOption,
      xmlDeclarationOption = xmlDeclOption,
      children = docChildren.toVector)
    doc
  }

  /**
   * Given a buffered iterator of EventWithAncestry objects, converts the (initial) events to a Elem. The first event
   * must be a StartElement event, or else an exception is thrown. The iterator can still be used afterwards for the
   * remaining events.
   *
   * The input iterator should come from a call to method `convertToEventWithAncestryIterator`.
   */
  final def takeElem(eventStateIterator: BufferedIterator[EventWithAncestry]): Elem = {
    require(eventStateIterator.hasNext)

    var it = eventStateIterator

    val head = it.next()
    require(head.event.isStartElement, s"Not a StartElement event: ${head.event}")

    val startElement: StartElement = head.event.asStartElement
    require(head.ancestryPathAfterEventOption.nonEmpty)

    val entry: AncestryPath.Entry = head.ancestryPathAfterEventOption.get.firstEntry
    val elem: Elem = Elem(entry.qname, entry.attributes, entry.scope, immutable.IndexedSeq())

    def startsWithMatchingEndElement(eventIterator: BufferedIterator[EventWithAncestry]): Boolean = {
      if (!eventIterator.hasNext) false else {
        val hd: EventWithAncestry = eventIterator.head
        (hd.event.isEndElement) && (hd.qnames == head.qnames.tail)
      }
    }

    val children = mutable.Buffer[Node]()

    // Imperative code
    while (it.hasNext && !startsWithMatchingEndElement(it)) {
      val nextHead = it.head

      nextHead.event match {
        case ev: StartElement => {
          // Recursive call (not tail-recursive, but recursion depth is rather limited)
          val ch = takeElem(it)
          children += ch
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
    require(
      endElementEv.getName == head.event.asStartElement.getName,
      s"Expected end-element name ${head.event.asStartElement.getName} but encountered ${endElementEv.getName}")

    val elemWithChildren: Elem = elem.withChildren(children.toIndexedSeq)
    elemWithChildren
  }

  /**
   * Given a buffered iterator of EventWithAncestry objects, converts the (initial) events to an Elem sequence until
   * the predicate holds. The first event, if any, must be a StartElement event, or else an exception is thrown. The iterator
   * can still be used afterwards for the remaining events.
   *
   * The input iterator should come from a call to method `convertToEventWithAncestryIterator`.
   *
   * Note that comments, processing instructions etc. may get lost in this operation. Be careful with the predicate: if it does
   * not hold for the last elements, they are returned anyway. Moreover, if the predicate does not stop in time, too many elements may be
   * kept in memory.
   *
   * TODO Return a Node collection instead, or create a similar method that returns a Node collection.
   */
  final def takeElemsUntil(
    eventStateIterator: BufferedIterator[EventWithAncestry],
    p: (immutable.IndexedSeq[Elem], EventWithAncestry) => Boolean): immutable.IndexedSeq[Elem] = {

    if (!eventStateIterator.hasNext) {
      Vector()
    } else {
      var it = eventStateIterator

      require(it.head.event.isStartElement, s"Not a StartElement event: ${it.head.event}")

      var elems = Vector[Elem]()

      while (it.hasNext && !p(elems, it.head)) {
        assert(it.head.event.isStartElement, s"Not a StartElement event: ${it.head.event}")

        elems = elems :+ takeElem(it)

        while (it.hasNext && !it.head.event.isStartElement) {
          it.next // May lose comments etc.
        }
      }

      assert(!it.hasNext || p(elems, it.head))
      elems
    }
  }

  /**
   * Convenience method to turn an XMLEventReader into a Scala Iterator of XMLEvent objects.
   */
  final def asIterator(xmlEventReader: XMLEventReader): Iterator[XMLEvent] = {
    val it = xmlEventReader.asInstanceOf[jutil.Iterator[XMLEvent]]
    it.asScala
  }
}
