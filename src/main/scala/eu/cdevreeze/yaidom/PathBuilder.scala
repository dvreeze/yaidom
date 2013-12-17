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

import scala.collection.{ immutable, mutable }
import scala.annotation.tailrec

/**
 * Builder for `Path` instances.
 *
 * For example:
 * {{{
 * val path: Path = PathBuilder.from(QName("parent") -> 0, QName("child") -> 2).build(Scope.Empty)
 * }}}
 *
 * Note that the indexes are 0-based. Also note that the Scope passed to the `build` method must be invertible.
 * Otherwise the resolution of QNames can break the indexes of the path builder components.
 *
 * @author Chris de Vreeze
 */
final class PathBuilder(val entries: immutable.IndexedSeq[PathBuilder.Entry]) extends Immutable { self =>
  require(entries ne null)

  /** Returns true if this is the root `Path`, so if it has no entries */
  def isRoot: Boolean = entries.isEmpty

  /** Prepends a given `Entry` to this `PathBuilder` */
  def prepend(entry: PathBuilder.Entry): PathBuilder = PathBuilder(entry +: self.entries)

  /** Appends a given `Entry` to this `PathBuilder` */
  def append(entry: PathBuilder.Entry): PathBuilder = PathBuilder(self.entries :+ entry)

  /** Builds the `Path`, using the passed `Scope`, which must be invertible */
  def build(scope: Scope): Path = {
    require(scope.isInvertible, "Scope '%s' is not invertible".format(scope))

    val resolvedEntries: immutable.IndexedSeq[Path.Entry] = entries map { _.build(scope) }
    Path(resolvedEntries)
  }

  override def equals(obj: Any): Boolean = obj match {
    case other: PathBuilder =>
      if (hashCode != other.hashCode) false else entries == other.entries
    case _ => false
  }

  override def hashCode: Int = entries.hashCode

  override def toString: String = entries.toString
}

object PathBuilder {

  val Root: PathBuilder = PathBuilder(immutable.IndexedSeq())

  def apply(entries: immutable.IndexedSeq[PathBuilder.Entry]): PathBuilder = new PathBuilder(entries)

  /** Easy to use factory method for `PathBuilder` instances */
  def from(entries: (QName, Int)*): PathBuilder = {
    val entrySeq: Seq[PathBuilder.Entry] = entries map { p => Entry(p._1, p._2) }
    new PathBuilder(entrySeq.toIndexedSeq)
  }

  /** An entry in an `PathBuilder`, as an qname plus zero-based index of the elem as child (with that name) of the parent. */
  final case class Entry(qname: QName, index: Int) extends Immutable {
    require(qname ne null)
    require(index >= 0)

    /** Builds the `Path.Entry`, using the passed `Scope`, which must be invertible */
    def build(scope: Scope): Path.Entry = {
      require(scope.isInvertible, "Scope '%s' is not invertible".format(scope))

      val ename: EName = scope.resolveQNameOption(qname).getOrElse(sys.error("Could not resolve qname '%s' in scope %s".format(qname, scope)))
      Path.Entry(ename, index)
    }

    def localName: String = qname.localPart
  }
}
