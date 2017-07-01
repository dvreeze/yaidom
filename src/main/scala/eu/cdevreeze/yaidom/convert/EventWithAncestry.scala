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

import scala.collection.JavaConverters.asScalaIteratorConverter
import scala.collection.immutable

import eu.cdevreeze.yaidom.core.AncestryPath
import eu.cdevreeze.yaidom.core.Declarations
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.QNameProvider
import eu.cdevreeze.yaidom.core.Scope
import javax.xml.XMLConstants
import javax.xml.namespace.{ QName => JQName }
import javax.xml.stream.events.Attribute
import javax.xml.stream.events.Namespace
import javax.xml.stream.events.StartElement
import javax.xml.stream.events.XMLEvent

/**
 * A StAX event combined with an optional ancestry path. After turning a stream of StAX events into a stream of EventWithAncestry
 * objects, it is easy to create the entire element tree, or to query "an event" in the context of its ancestry.
 *
 * The ancestry path is optional because it is absent before the first StartElement event and after the last EndElement event.
 * Otherwise it must be present.
 *
 * @author Chris de Vreeze
 */
final case class EventWithAncestry(val event: XMLEvent, val ancestryPathAfterEventOption: Option[AncestryPath]) {

  /**
   * Returns the current Scope after the event.
   */
  def currentScope: Scope = {
    ancestryPathAfterEventOption.map(_.firstEntry.scope).getOrElse(Scope.Empty)
  }

  /**
   * Returns the EventWithAncestry resulting from the given next event.
   */
  def next(nextEvent: XMLEvent): EventWithAncestry = {
    EventWithAncestry(nextEvent, EventWithAncestry.nextAncestryPathOption(ancestryPathAfterEventOption, nextEvent))
  }

  /**
   * Returns the QNames from bottom to top.
   */
  def qnames: List[QName] = {
    ancestryPathAfterEventOption.map(_.qnames).getOrElse(Nil)
  }

  /**
   * Returns the ENames from bottom to top.
   */
  def enames: List[EName] = {
    ancestryPathAfterEventOption.map(_.enames).getOrElse(Nil)
  }
}

object EventWithAncestry {

  /**
   * Returns the optional AncestryPath resulting from the given next event.
   */
  def nextAncestryPathOption(ancestryPathAfterPreviousEventOption: Option[AncestryPath], event: XMLEvent): Option[AncestryPath] = {
    if (event.isStartElement) {
      val startElemEvent = event.asStartElement
      val currentScope = ancestryPathAfterPreviousEventOption.map(_.firstEntry.scope).getOrElse(Scope.Empty)
      val entry =
        EventWithAncestry.convertStartElemEventToAncestryPathEntry(startElemEvent, currentScope)

      val nextAncestryPathOption =
        if (ancestryPathAfterPreviousEventOption.isEmpty) {
          Some(AncestryPath.fromEntry(entry))
        } else {
          ancestryPathAfterPreviousEventOption.map(_.prepend(entry))
        }

      nextAncestryPathOption
    } else if (event.isEndElement) {
      val endElemEvent = event.asEndElement

      require(ancestryPathAfterPreviousEventOption.isDefined, s"Corrupt StAX stream. Cannot process EndElement event if there is no ancestry-or-self")
      require(
        ancestryPathAfterPreviousEventOption.get.firstEntry.qname.localPart == endElemEvent.getName.getLocalPart,
        s"Corrupt StAX stream. The local name ${endElemEvent.getName.getLocalPart} does not match that of the corresponding StartElement event")

      val nextAncestryPathOption =
        ancestryPathAfterPreviousEventOption.flatMap(_.parentOption)
      nextAncestryPathOption
    } else {
      ancestryPathAfterPreviousEventOption
    }
  }

  def convertStartElemEventToAncestryPathEntry(
    startElement: StartElement,
    parentScope: Scope)(implicit qnameProvider: QNameProvider): AncestryPath.Entry = {

    val declarations: Declarations = {
      val namespaces: List[Namespace] = startElement.getNamespaces.asScala.toList collect { case ns: Namespace => ns }
      // The Namespaces can also hold namespace undeclarations (with null or the empty string as namespace URI)

      val declaredScope: Scope = getDeclaredScope(namespaces)
      val undeclaredOptionalPrefixes: Set[Option[String]] = getUndeclaredOptionalPrefixes(namespaces)

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

    AncestryPath.Entry(elemQName, currAttrs, currScope)
  }

  /**
   * Helper method dropping events from the buffered iterator while the given predicate holds.
   * The same buffered iterator can still be used after calling this function.
   */
  def dropWhile(it: BufferedIterator[EventWithAncestry], p: EventWithAncestry => Boolean): Unit = {
    while (it.hasNext && p(it.head)) {
      it.next()
    }
  }

  /**
   * Helper method dropping events from the buffered iterator while the given predicate does not hold.
   * The same buffered iterator can still be used after calling this function.
   */
  def dropWhileNot(it: BufferedIterator[EventWithAncestry], p: EventWithAncestry => Boolean): Unit = {
    dropWhile(it, (e => !p(e)))
  }

  /** Gets an optional prefix from a `javax.xml.namespace.QName` */
  // scalastyle:off null
  private def prefixOptionFromJavaQName(jqname: JQName): Option[String] = {
    val prefix: String = jqname.getPrefix
    if ((prefix eq null) || (prefix == XMLConstants.DEFAULT_NS_PREFIX)) None else Some(prefix)
  }

  private def getDeclaredScope(namespaces: List[Namespace]): Scope = {
    val defaultNs = {
      val result =
        namespaces filter { _.isDefaultNamespaceDeclaration } map { ns => Option(ns.getNamespaceURI).getOrElse("") } filter { _ != "" }

      result.headOption
    }
    val prefScope = {
      val result =
        namespaces filterNot { _.isDefaultNamespaceDeclaration } map { ns =>
          (ns.getPrefix -> Option(ns.getNamespaceURI).getOrElse(""))
        } filter { _._2 != "" }

      result.toMap
    }
    val defaultNsMap: Map[String, String] = if (defaultNs.isEmpty) Map() else Map("" -> defaultNs.get)

    Scope.from(defaultNsMap ++ prefScope)
  }

  private def getUndeclaredOptionalPrefixes(namespaces: List[Namespace]): Set[Option[String]] = {
    val defaultNs = {
      val result = namespaces filter { _.isDefaultNamespaceDeclaration } map { ns => Option(ns.getNamespaceURI).getOrElse("") } filter { _ == "" }
      result.headOption
    }
    val defaultNsUndeclared = defaultNs.isDefined

    val undeclaredPrefixOptions: Set[Option[String]] = {
      val result =
        namespaces filterNot { _.isDefaultNamespaceDeclaration } map { ns =>
          (ns.getPrefix -> Option(ns.getNamespaceURI).getOrElse(""))
        } filter { _._2 == "" } map { kv => Some(kv._1) }

      result.toSet
    }

    if (defaultNsUndeclared) {
      Set(None) ++ undeclaredPrefixOptions
    } else {
      undeclaredPrefixOptions
    }
  }
}
