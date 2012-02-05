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

/** Unique identification of a descendant ElemLike given a root ElemLike. */
final case class ElemPath(entries: List[ElemPath.Entry]) extends Immutable { self =>

  require(entries ne null)

  /** Prepends an Entry with the given index to this ElemPath */
  def ::(idx: Int): ElemPath = ElemPath(ElemPath.Entry(idx) :: self.entries)

  /** Returns the ElemPath with the first path entry removed (if any, otherwise throwing an exception). */
  def tail: ElemPath = ElemPath(entries.tail)

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
