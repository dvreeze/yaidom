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

package eu.cdevreeze.yaidom.docaware

import java.net.URI

import scala.collection.immutable

import eu.cdevreeze.yaidom.core.Declarations
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.queryapi.IsNavigable
import eu.cdevreeze.yaidom.queryapi.ScopedElemLike
import eu.cdevreeze.yaidom.simple

/**
 * Abstract super-trait of "indexed" Elem, without making any choice between speed of creation or speed of querying.
 * It leaves this choice open, by leaving method `findAllChildElems` abstract.
 *
 * @author Chris de Vreeze
 */
trait AbstractDocawareElem[E <: AbstractDocawareElem[E]] extends ScopedElemLike[E] with IsNavigable[E] with Immutable { self: E =>

  def docUri: URI

  def parentBaseUri: URI

  def rootElem: simple.Elem

  def path: Path

  def elem: simple.Elem

  def findAllChildElems: immutable.IndexedSeq[E]

  final override def resolvedName: EName = elem.resolvedName

  final override def resolvedAttributes: immutable.IndexedSeq[(EName, String)] = elem.resolvedAttributes

  final override def findChildElemByPathEntry(entry: Path.Entry): Option[E] = {
    val filteredChildElems = findAllChildElems.toStream filter { e => e.resolvedName == entry.elementName }

    val childElemOption = filteredChildElems.drop(entry.index).headOption
    assert(childElemOption.forall(_.resolvedName == entry.elementName))
    childElemOption
  }

  final override def qname: QName = elem.qname

  final override def attributes: immutable.IndexedSeq[(QName, String)] = elem.attributes

  /**
   * Returns `this.elem.scope`
   */
  final override def scope: Scope = this.elem.scope

  /**
   * Returns the namespaces declared in this element.
   *
   * If the original parsed XML document contained duplicate namespace declarations (i.e. namespace declarations that are the same
   * as some namespace declarations in their context), these duplicate namespace declarations were lost during parsing of the
   * XML into an `Elem` tree. They therefore do not occur in the namespace declarations returned by this method.
   */
  final def namespaces: Declarations = {
    val parentScope = this.path.parentPathOption map { path => rootElem.getElemOrSelfByPath(path).scope } getOrElse (Scope.Empty)
    parentScope.relativize(this.elem.scope)
  }

  /**
   * Returns the concatenation of the texts of text children, including whitespace. Non-text children are ignored.
   * If there are no text children, the empty string is returned.
   */
  final override def text: String = {
    val textStrings = elem.textChildren map { t => t.text }
    textStrings.mkString
  }

  /**
   * Returns the ENames of the ancestry-or-self, starting with the root element and ending with this element.
   *
   * That is, returns:
   * {{{
   * rootElem.resolvedName +: path.entries.map(_.elementName)
   * }}}
   */
  final def ancestryOrSelfENames: immutable.IndexedSeq[EName] = {
    rootElem.resolvedName +: path.entries.map(_.elementName)
  }

  /**
   * Returns the ENames of the ancestry, starting with the root element and ending with the parent of this element, if any.
   *
   * That is, returns:
   * {{{
   * ancestryOrSelfENames.dropRight(1)
   * }}}
   */
  final def ancestryENames: immutable.IndexedSeq[EName] = {
    ancestryOrSelfENames.dropRight(1)
  }

  /**
   * Returns the base URI of the element. That is, returns:
   * {{{
   * attributeOption(XmlBaseEName).map(s => parentBaseUri.resolve(new URI(s))).getOrElse(parentBaseUri)
   * }}}
   */
  final def baseUri: URI = {
    val explicitBaseUriOption = attributeOption(AbstractDocawareElem.XmlBaseEName).map(s => new URI(s))
    explicitBaseUriOption.map(u => parentBaseUri.resolve(u)).getOrElse(parentBaseUri)
  }
}

object AbstractDocawareElem {

  private val XmlNs = "http://www.w3.org/XML/1998/namespace"

  private val XmlBaseEName = EName(XmlNs, "base")
}
