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
  val path: Path,
  val elem: eu.cdevreeze.yaidom.Elem) extends ElemLike[Elem] with HasQName with HasText with Immutable {

  @deprecated(message = "Use path instead", since = "0.7.1")
  def elemPath: Path = path

  // The elem must be the same as rootElem.findElemOrSelfByPath(path).get, which is not checked here

  assert(childElems.map(_.elem) == elem.findAllChildElems, "Corrupt element!")
  assert(childElems.forall(_.docUri eq this.docUri), "Corrupt element!")

  /**
   * Returns all child elements, in the correct order.
   *
   * These child elements share the same rootElem with this element, but differ in the paths, which have one more
   * "path entry".
   */
  override def findAllChildElems: immutable.IndexedSeq[Elem] = childElems

  override def resolvedName: EName = elem.resolvedName

  override def resolvedAttributes: immutable.IndexedSeq[(EName, String)] = elem.resolvedAttributes

  override def qname: QName = elem.qname

  override def attributes: immutable.IndexedSeq[(QName, String)] = elem.attributes

  override def equals(obj: Any): Boolean = obj match {
    case other: Elem =>
      (other.docUri == this.docUri) && (other.rootElem == this.rootElem) && (other.path == this.path)
    case _ => false
  }

  override def hashCode: Int = (docUri, rootElem, path).hashCode

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
    val parentScope = this.path.parentPathOption map { path => rootElem.getElemOrSelfByPath(path).scope } getOrElse (Scope.Empty)
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
   * Calls `apply(docUri, rootElem, Path.Root)`
   */
  def apply(docUri: URI, rootElem: eu.cdevreeze.yaidom.Elem): Elem = {
    apply(docUri, rootElem, Path.Root)
  }

  /**
   * Expensive recursive factory method for "docaware elements".
   */
  def apply(docUri: URI, rootElem: eu.cdevreeze.yaidom.Elem, path: Path): Elem = {
    val elem = rootElem.getElemOrSelfByPath(path)

    // Recursive calls
    val childElems = elem.findAllChildElemPathEntries.map(entry => apply(docUri, rootElem, path.append(entry)))

    new Elem(docUri, rootElem, childElems, path, elem)
  }
}
