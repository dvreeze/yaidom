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

/**
 * Unique identification of a descendant (or self) ElemLike given a root ElemLike. It is used for transformations
 * from one node tree to another collection of nodes.
 *
 * The ElemPath contains a List of path entries for a specific child element, grandchild element etc.,
 * but the (root) element itself is referred to by an empty list of path entries.
 *
 * Strictly speaking, each element in a tree would be uniquely identified by path entries that only contained
 * a child index instead of an element name plus child index (of children with the given name). Yet that would
 * be far less easy to use. Hence ElemPath.Entry instances each contain an element name plus index.
 */
final case class ElemPath(entries: List[ElemPath.Entry]) extends Immutable { self =>

  require(entries ne null)

  /** Prepends a given Entry to this ElemPath */
  def ::(entry: ElemPath.Entry): ElemPath = ElemPath(entry :: self.entries)

  /** Returns the ElemPath with the first path entry removed (if any, otherwise throwing an exception). */
  def skipEntry: ElemPath = ElemPath(entries.tail)

  /**
   * Gets the parent path (if any, because the root path has no parent) wrapped in an Option.
   *
   * This method shows much of the reason why class ElemPath exists. If we know an element's ElemPath, and therefore its
   * parent ElemPath (using this method), then we can obtain the parent element by following the parent path from the
   * root of the tree.
   */
  def parentPathOption: Option[ElemPath] = entries match {
    case Nil => None
    case _ => Some(ElemPath(entries.dropRight(1)))
  }

  /** Like parentPathOption, but unwrapping the result (or throwing an exception otherwise) */
  def parentPath: ElemPath = parentPathOption.getOrElse(sys.error("The root path has no parent path"))

  /**
   * Returns the corresponding pseudo-XPath.
   */
  def toPseudoXPath: String = {
    val entryPseudoXPaths = entries map { entry => entry.toPseudoXPath }
    "/" + "*" + entryPseudoXPaths.mkString
  }

  override def toString: String = toPseudoXPath
}

object ElemPath {

  val Root: ElemPath = ElemPath(Nil)

  /** An entry in an ElemPath, as an expanded element name plus zero-based index of the elem as child (with that name) of the parent. */
  final case class Entry(elementName: ExpandedName, index: Int) extends Immutable {

    require(elementName ne null)
    require(index >= 0)

    /** Position (1-based) of the elem as child of the parent. Is 1 + index. */
    def position: Int = 1 + index

    /**
     * Returns the corresponding pseudo-XPath. Example: <code>*[3]</code>.
     */
    def toPseudoXPath: String = {
      "%s%s[%d]".format("/", elementName.toString, position)
    }

    override def toString: String = toPseudoXPath
  }
}
