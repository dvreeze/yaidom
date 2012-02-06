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
 * Unique identification of a descendant (or self) ElemLike given a root ElemLike.
 * The ElemPath contains a List of path entries for a child element, grandchild element etc.,
 * but the (root) element itself is referred to by an empty list of path entries.
 */
final case class ElemPath(entries: List[ElemPath.Entry]) extends Immutable { self =>

  require(entries ne null)

  /** Prepends an Entry with the given index to this ElemPath */
  def ::(idx: Int): ElemPath = ElemPath(ElemPath.Entry(idx) :: self.entries)

  /** Returns the ElemPath with the first path entry removed (if any, otherwise throwing an exception). */
  def skipEntry: ElemPath = ElemPath(entries.tail)

  /**
   * Gets the parent path (if any, because the root path has no parent) wrapped in an Option.
   *
   * This method shows much of the reason why class ElemPath exists. If a tree of elements has been indexed on
   * the ElemPaths, then given an element we know its ElemPath, and therefore its parent ElemPath (using this method),
   * from which we can obtain the parent element by following the parent path from the root of the tree.
   */
  def parentPathOption: Option[ElemPath] = entries match {
    case Nil => None
    case _ => Some(ElemPath(entries.dropRight(1)))
  }

  /** Like parentPathOption, but unwrapping the result (or throwing an exception otherwise) */
  def parentPath: ElemPath = parentPathOption.getOrElse(sys.error("The root path has no parent path"))

  /**
   * Returns the corresponding XPath.
   */
  def toXPath: String = {
    val entryXPaths = entries map { entry => entry.toXPath }
    "/" + "*" + entryXPaths.mkString
  }

  override def toString: String = toXPath
}

object ElemPath {

  val Root: ElemPath = ElemPath(Nil)

  def fromIndexes(indexes: List[Int]): ElemPath = ElemPath(indexes map { idx => ElemPath.Entry(idx) })

  /** An entry in an ElemPath, as a zero-based index of the elem as child of the parent. */
  final case class Entry(index: Int) extends Immutable {

    require(index >= 0)

    /** Position (1-based) of the elem as child of the parent. Is 1 + index. */
    def position: Int = 1 + index

    /**
     * Returns the corresponding XPath. Example: <code>*[3]</code>.
     */
    def toXPath: String = {
      "%s*[%d]".format("/", position)
    }

    override def toString: String = toXPath
  }
}
