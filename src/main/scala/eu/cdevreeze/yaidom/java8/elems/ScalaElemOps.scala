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

package eu.cdevreeze.yaidom.java8.elems

import java.util.Optional
import java.util.function.Predicate
import java.util.stream.Stream

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.java8.Attr
import eu.cdevreeze.yaidom.java8.ElemPathEntryPair
import eu.cdevreeze.yaidom.java8.ResolvedAttr
import eu.cdevreeze.yaidom.scalaxml.ScalaXmlElem

/**
 * Like `ScalaElems`, but supporting static imports in Java. Easy to use in Java 8 code.
 *
 * @author Chris de Vreeze
 */
object ScalaElemOps {

  type E = ScalaXmlElem

  private val delegate = ScalaElems.getInstance

  // ElemApi methods

  def findAllChildElems(elem: E): Stream[E] = {
    delegate.findAllChildElems(elem)
  }

  def findAllElems(elem: E): Stream[E] = {
    delegate.findAllElems(elem)
  }

  def findAllElemsOrSelf(elem: E): Stream[E] = {
    delegate.findAllElemsOrSelf(elem)
  }

  def filterChildElems(elem: E, p: Predicate[E]): Stream[E] = {
    delegate.filterChildElems(elem, p)
  }

  def filterElems(elem: E, p: Predicate[E]): Stream[E] = {
    delegate.filterElems(elem, p)
  }

  def filterElemsOrSelf(elem: E, p: Predicate[E]): Stream[E] = {
    delegate.filterElemsOrSelf(elem, p)
  }

  def findChildElem(elem: E, p: Predicate[E]): Optional[E] = {
    delegate.findChildElem(elem, p)
  }

  def findElem(elem: E, p: Predicate[E]): Optional[E] = {
    delegate.findElem(elem, p)
  }

  def findElemOrSelf(elem: E, p: Predicate[E]): Optional[E] = {
    delegate.findElemOrSelf(elem, p)
  }

  def findTopmostElems(elem: E, p: Predicate[E]): Stream[E] = {
    delegate.findTopmostElems(elem, p)
  }

  def findTopmostElemsOrSelf(elem: E, p: Predicate[E]): Stream[E] = {
    delegate.findTopmostElemsOrSelf(elem, p)
  }

  def getChildElem(elem: E, p: Predicate[E]): E = {
    delegate.getChildElem(elem, p)
  }

  // IsNavigableApi methods

  def findAllChildElemsWithPathEntries(elem: E): Stream[ElemPathEntryPair[E]] = {
    delegate.findAllChildElemsWithPathEntries(elem)
  }

  def findChildElemByPathEntry(elem: E, entry: Path.Entry): Optional[E] = {
    delegate.findChildElemByPathEntry(elem, entry)
  }

  def getChildElemByPathEntry(elem: E, entry: Path.Entry): E = {
    delegate.getChildElemByPathEntry(elem, entry)
  }

  def findElemOrSelfByPath(elem: E, path: Path): Optional[E] = {
    delegate.findElemOrSelfByPath(elem, path)
  }

  def getElemOrSelfByPath(elem: E, path: Path): E = {
    delegate.getElemOrSelfByPath(elem, path)
  }

  def findReverseAncestryOrSelfByPath(elem: E, path: Path): Optional[Stream[E]] = {
    delegate.findReverseAncestryOrSelfByPath(elem, path)
  }

  def getReverseAncestryOrSelfByPath(elem: E, path: Path): Stream[E] = {
    delegate.getReverseAncestryOrSelfByPath(elem, path)
  }

  // HasENameApi methods

  def resolvedName(elem: E): EName = {
    delegate.resolvedName(elem)
  }

  def resolvedAttributes(elem: E): Stream[ResolvedAttr] = {
    delegate.resolvedAttributes(elem)
  }

  def localName(elem: E): String = {
    delegate.localName(elem)
  }

  def attributeOption(elem: E, expandedName: EName): Optional[String] = {
    delegate.attributeOption(elem, expandedName)
  }

  def attribute(elem: E, expandedName: EName): String = {
    delegate.attribute(elem, expandedName)
  }

  def findAttributeByLocalName(elem: E, localName: String): Optional[String] = {
    delegate.findAttributeByLocalName(elem, localName)
  }

  // HasTextApi

  def text(elem: E): String = {
    delegate.text(elem)
  }

  def trimmedText(elem: E): String = {
    delegate.trimmedText(elem)
  }

  def normalizedText(elem: E): String = {
    delegate.normalizedText(elem)
  }

  // HasQNameApi methods

  def qname(elem: E): QName = {
    delegate.qname(elem)
  }

  def attributes(elem: E): Stream[Attr] = {
    delegate.attributes(elem)
  }

  // HasScopeApi

  def scope(elem: E): Scope = {
    delegate.scope(elem)
  }

  // ScopedElemApi own methods

  def attributeAsQNameOption(elem: E, expandedName: EName): Optional[QName] = {
    delegate.attributeAsQNameOption(elem, expandedName)
  }

  def attributeAsQName(elem: E, expandedName: EName): QName = {
    delegate.attributeAsQName(elem, expandedName)
  }

  def attributeAsResolvedQNameOption(elem: E, expandedName: EName): Optional[EName] = {
    delegate.attributeAsResolvedQNameOption(elem, expandedName)
  }

  def attributeAsResolvedQName(elem: E, expandedName: EName): EName = {
    delegate.attributeAsResolvedQName(elem, expandedName)
  }

  def textAsQName(elem: E): QName = {
    delegate.textAsQName(elem)
  }

  def textAsResolvedQName(elem: E): EName = {
    delegate.textAsResolvedQName(elem)
  }
}
