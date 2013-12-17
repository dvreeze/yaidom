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
package docaware

import java.net.URI
import scala.collection.immutable

/**
 * An element just like `indexed.Elem`, but storing the URI of the containing document as well. See [[eu.cdevreeze.yaidom.indexed.Elem]]
 * for more details. These details apply to [[eu.cdevreeze.yaidom.docaware.Elem]] too, except that additionally the document URI
 * is stored.
 *
 * @author Chris de Vreeze
 */
final class Elem private[docaware] (
  val docUri: URI,
  val rootElem: eu.cdevreeze.yaidom.Elem,
  childElems: immutable.IndexedSeq[Elem],
  val elemPath: ElemPath) extends ElemLike[Elem] with HasText with Immutable {

  /**
   * The yaidom Elem itself, stored as a val
   */
  val elem: eu.cdevreeze.yaidom.Elem =
    rootElem.findElemOrSelfByPath(elemPath).getOrElse(sys.error("Path %s must exist".format(elemPath)))

  assert(childElems.map(_.elem) == elem.findAllChildElems, "Corrupt element!")
  assert(childElems.forall(_.docUri eq this.docUri), "Corrupt element!")

  /**
   * Returns all child elements, in the correct order.
   *
   * These child elements share the same rootElem with this element, but differ in the element paths, which have one more
   * "path entry".
   */
  override def findAllChildElems: immutable.IndexedSeq[Elem] = childElems

  override def resolvedName: EName = elem.resolvedName

  override def resolvedAttributes: immutable.IndexedSeq[(EName, String)] = elem.resolvedAttributes

  override def equals(obj: Any): Boolean = obj match {
    case other: Elem =>
      (other.docUri == this.docUri) && (other.rootElem == this.rootElem) && (other.elemPath == this.elemPath)
    case _ => false
  }

  override def hashCode: Int = (docUri, rootElem, elemPath).hashCode

  /**
   * Returns `this.elem.scope`
   */
  final def scope: Scope = this.elem.scope

  /**
   * Returns the namespaces declared in this element.
   *
   * If the original parsed XML document contained duplicate namespace declarations (i.e. namespace declarations that are the same
   * as some namespace declarations in their context), these duplicate namespace declarations were lost during parsing of the
   * XML into an `Elem` tree. They therefore do not occur in the namespace declarations returned by this method.
   */
  final def namespaces: Declarations = {
    val parentScope = this.elemPath.parentPathOption map { path => rootElem.getElemOrSelfByPath(path).scope } getOrElse (Scope.Empty)
    parentScope.relativize(this.elem.scope)
  }

  /**
   * Returns the concatenation of the texts of text children, including whitespace. Non-text children are ignored.
   * If there are no text children, the empty string is returned.
   */
  override def text: String = {
    val textStrings = elem.textChildren map { t => t.text }
    textStrings.mkString
  }
}

object Elem {

  /**
   * Calls `apply(docUri, rootElem, ElemPath.Root)`
   */
  def apply(docUri: URI, rootElem: eu.cdevreeze.yaidom.Elem): Elem = {
    apply(docUri, rootElem, ElemPath.Root)
  }

  /**
   * Expensive recursive factory method for "docaware elements".
   */
  def apply(docUri: URI, rootElem: eu.cdevreeze.yaidom.Elem, elemPath: ElemPath): Elem = {
    val elem = rootElem.getElemOrSelfByPath(elemPath)

    // Recursive calls
    val childElems = elem.findAllChildElemPathEntries.map(entry => apply(docUri, rootElem, elemPath.append(entry)))

    new Elem(docUri, rootElem, childElems, elemPath)
  }
}
