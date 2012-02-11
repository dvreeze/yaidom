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
import java.net.URI
import javax.xml.XMLConstants
import javax.xml.stream._
import javax.xml.stream.events.{ ProcessingInstruction => _, Comment => _, _ }
import scala.collection.JavaConverters._
import scala.collection.{ immutable, mutable }
import StaxEventsToElemConverter._

/**
 * Converter from immutable.Seq[XMLEvent] to [[eu.cdevreeze.yaidom.Elem]], or to [[eu.cdevreeze.yaidom.Document]].
 *
 * @author Chris de Vreeze
 */
trait StaxEventsToElemConverter extends ConverterToElem[immutable.Seq[XMLEvent]] with ConverterToDocument[immutable.Seq[XMLEvent]] {

  def convertToDocument(v: immutable.Seq[XMLEvent]): Document = {
    val events: immutable.Seq[XMLEvent] = v.toList dropWhile { ev => !ev.isStartDocument }

    val eventsWithDepths: immutable.Seq[EventWithDepth] = {
      var depth = 0
      events.map(ev => ev match {
        case start: StartElement => depth += 1; new EventWithDepth(start, depth)
        case end: EndElement => val currDepth = depth; depth -= 1; new EventWithDepth(end, currDepth)
        case _ => new EventWithDepth(ev, depth)
      })
    }

    require(!eventsWithDepths.drop(3).isEmpty) // Cheaper than calling size for Lists

    val result = eventsToDocument(eventsWithDepths)

    require {
      result.remainder forall { ev => !ev.event.isStartElement && !ev.event.isEndElement && !ev.event.isStartDocument && !ev.event.isEndDocument }
    }
    result.doc
  }

  def convertToElem(v: immutable.Seq[XMLEvent]): Elem = {
    val events: immutable.Seq[XMLEvent] = v.toList dropWhile { ev => !ev.isStartElement }

    val eventsWithDepths: immutable.Seq[EventWithDepth] = {
      var depth = 0
      events.map(ev => ev match {
        case start: StartElement => depth += 1; new EventWithDepth(start, depth)
        case end: EndElement => val currDepth = depth; depth -= 1; new EventWithDepth(end, currDepth)
        case _ => new EventWithDepth(ev, depth)
      })
    }

    require(!eventsWithDepths.drop(1).isEmpty) // Cheaper than calling size for Lists

    val result = eventsToElem(eventsWithDepths, Scope.Empty)

    require {
      result.remainder forall { ev => !ev.event.isStartElement && !ev.event.isEndElement }
    }
    result.elem
  }

  private def eventsToDocument(eventsWithDepths: immutable.Seq[EventWithDepth]): DocumentResult = {
    require(!eventsWithDepths.drop(3).isEmpty) // Cheaper than calling size for Lists
    require(eventsWithDepths.head.event.isStartDocument)

    val startDocument: StartDocument = eventsWithDepths.head.event.asInstanceOf[StartDocument]

    def startsWithEndDocument(events: immutable.Seq[EventWithDepth]): Boolean = {
      if (events.isEmpty) false else {
        val head: EventWithDepth = events.head
        head.event.isInstanceOf[EndDocument]
      }
    }

    var remainingEvents = eventsWithDepths.drop(1)
    val pis = mutable.Buffer[ProcessingInstruction]()
    val comments = mutable.Buffer[Comment]()
    var docElement: Elem = null

    // Imperative code
    while (!remainingEvents.isEmpty && !startsWithEndDocument(remainingEvents)) {
      remainingEvents.head match {
        case ev if ev.event.isStartElement => {
          require(docElement eq null, "Only 1 document element allowed and required")
          val result = eventsToElem(remainingEvents, Scope.Empty)
          docElement = result.elem
          remainingEvents = result.remainder
        }
        case ev if ev.event.isProcessingInstruction => {
          val piEv = ev.event.asInstanceOf[javax.xml.stream.events.ProcessingInstruction]
          val pi = eventToProcessingInstruction(piEv)
          pis += pi
          remainingEvents = remainingEvents.drop(1)
        }
        case ev if ev.event.isInstanceOf[javax.xml.stream.events.Comment] => {
          val comEv = ev.event.asInstanceOf[javax.xml.stream.events.Comment]
          val com = eventToComment(comEv)
          comments += com
          remainingEvents = remainingEvents.drop(1)
        }
        case _ => remainingEvents = remainingEvents.drop(1)
      }
    }
    require(docElement ne null, "There must be 1 document element")
    require(startsWithEndDocument(remainingEvents))

    remainingEvents = remainingEvents.drop(1)

    val baseUriOption: Option[URI] =
      if ((startDocument.getSystemId eq null) || (startDocument.getSystemId == "")) None else {
        Some(new URI(startDocument.getSystemId))
      }

    val doc: Document = new Document(
      baseUriOption = baseUriOption,
      documentElement = docElement,
      processingInstructions = pis.toIndexedSeq,
      comments = comments.toIndexedSeq)
    new DocumentResult(doc, remainingEvents)
  }

  private def eventsToElem(eventsWithDepths: immutable.Seq[EventWithDepth], parentScope: Scope): ElemResult = {
    require(!eventsWithDepths.drop(1).isEmpty) // Cheaper than calling size for Lists
    require(eventsWithDepths.head.event.isStartElement)

    val startElement: StartElement = eventsWithDepths.head.event.asStartElement

    val elem: Elem = eventToElem(startElement, parentScope)

    def startsWithMatchingEndElement(events: immutable.Seq[EventWithDepth]): Boolean = {
      if (events.isEmpty) false else {
        val head: EventWithDepth = events.head
        (head.depth == eventsWithDepths.head.depth) && (head.event.isEndElement)
      }
    }

    var remainingEvents = eventsWithDepths.drop(1)
    val children = mutable.Buffer[Node]()

    // Imperative code
    while (!remainingEvents.isEmpty && !startsWithMatchingEndElement(remainingEvents)) {
      remainingEvents.head match {
        case ev if ev.event.isStartElement => {
          // Recursive call (not tail-recursive, but recursion depth is rather limited)
          val result = eventsToElem(remainingEvents, elem.scope)
          val ch = result.elem
          children += ch
          remainingEvents = result.remainder
        }
        case ev if ev.event.isCharacters => {
          val charEv = ev.event.asCharacters
          val ch = if (charEv.isCData) eventToCData(charEv) else eventToText(charEv)
          children += ch
          remainingEvents = remainingEvents.drop(1)
        }
        case ev if ev.event.isEntityReference => {
          val erEv = ev.event.asInstanceOf[EntityReference]
          val ch = eventToEntityRef(erEv)
          children += ch
          remainingEvents = remainingEvents.drop(1)
        }
        case ev if ev.event.isProcessingInstruction => {
          val piEv = ev.event.asInstanceOf[javax.xml.stream.events.ProcessingInstruction]
          val ch = eventToProcessingInstruction(piEv)
          children += ch
          remainingEvents = remainingEvents.drop(1)
        }
        case ev if ev.event.isInstanceOf[javax.xml.stream.events.Comment] => {
          val comEv = ev.event.asInstanceOf[javax.xml.stream.events.Comment]
          val ch = eventToComment(comEv)
          children += ch
          remainingEvents = remainingEvents.drop(1)
        }
        case _ => remainingEvents = remainingEvents.drop(1)
      }
    }
    require(startsWithMatchingEndElement(remainingEvents))
    val endElementEv = remainingEvents.head.event.asEndElement
    require(endElementEv.getName == eventsWithDepths.head.event.asStartElement.getName)

    remainingEvents = remainingEvents.drop(1)

    val elemWithChildren: Elem = elem.withChildren(children.toIndexedSeq)
    new ElemResult(elemWithChildren, remainingEvents)
  }

  private def eventToText(event: Characters): Text = Text(text = event.getData, isCData = false)

  private def eventToCData(event: Characters): Text = Text(text = event.getData, isCData = true)

  private def eventToEntityRef(event: EntityReference): EntityRef = EntityRef(event.getName)

  private def eventToProcessingInstruction(event: javax.xml.stream.events.ProcessingInstruction): ProcessingInstruction =
    ProcessingInstruction(event.getTarget, event.getData)

  private def eventToComment(event: javax.xml.stream.events.Comment): Comment = Comment(event.getText)

  private def eventToElem(startElement: StartElement, parentScope: Scope): Elem = {
    val declarations: Scope.Declarations = {
      val namespaces: List[Namespace] = startElement.getNamespaces.asScala.toList collect { case ns: Namespace => ns }
      // The Namespaces can also hold namespace undeclarations (with null or the empty string as namespace URI)

      val declaredScope: Scope = {
        val defaultNs = namespaces filter { _.isDefaultNamespaceDeclaration } map { ns => Option(ns.getNamespaceURI).getOrElse("") } filter { _ != "" } headOption
        val prefScope = {
          val result = namespaces filterNot { _.isDefaultNamespaceDeclaration } map { ns => (ns.getPrefix -> Option(ns.getNamespaceURI).getOrElse("")) } filter { _._2 != "" }
          result.toMap
        }
        new Scope(defaultNamespace = defaultNs, prefixScope = prefScope)
      }
      val undeclaredOptionalPrefixes: Set[Option[String]] = {
        val defaultNs = namespaces filter { _.isDefaultNamespaceDeclaration } map { ns => Option(ns.getNamespaceURI).getOrElse("") } filter { _ == "" } headOption
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
      new Scope.Declarations(declared = declaredScope, undeclaredOptionalPrefixes = undeclaredOptionalPrefixes)
    }
    val currScope = parentScope.resolve(declarations)

    val elemExpandedName = ExpandedName.fromJavaQName(startElement.getName)
    val elemPrefix: Option[String] = ExpandedName.prefixFromJavaQName(startElement.getName)

    val currAttrs: Map[QName, String] = {
      val attributes: List[Attribute] = startElement.getAttributes.asScala.toList collect { case a: Attribute => a }
      val result = attributes map { a =>
        val prefix: Option[String] = ExpandedName.prefixFromJavaQName(a.getName)
        val name = ExpandedName.fromJavaQName(a.getName).toQName(prefix)
        (name -> a.getValue)
      }
      result.toMap
    }

    // Line and column numbers can be retrieved from startElement.getLocation, but are ignored here

    Elem(elemExpandedName.toQName(elemPrefix), currAttrs, currScope, Nil.toIndexedSeq)
  }
}

object StaxEventsToElemConverter {

  private final class EventWithDepth(val event: XMLEvent, val depth: Int) extends Immutable

  private final class ElemResult(val elem: Elem, val remainder: immutable.Seq[EventWithDepth]) extends Immutable

  private final class DocumentResult(val doc: Document, val remainder: immutable.Seq[EventWithDepth]) extends Immutable
}
