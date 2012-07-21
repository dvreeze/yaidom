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
 * Builder for `ElemPath` instances.
 *
 * Example:
 * {{{
 * import ElemPathBuilder.comp
 *
 * val elemPath: ElemPath = ElemPathBuilder.from(comp(QName("parent"), 0), comp(QName("child"), 2)).build(Scope.Empty)
 * }}}
 *
 * Note that the indexes are 0-based. Also note that the Scope passed to the `build` method must be invertible.
 * Otherwise the resolution of QNames can break the indexes of the path builder components.
 */
final class ElemPathBuilder(val entries: immutable.IndexedSeq[ElemPathBuilder.Entry]) extends Immutable { self =>
  require(entries ne null)

  /** Returns true if this is the root `ElemPath`, so if it has no entries */
  def isRoot: Boolean = entries.isEmpty

  /** Prepends a given `Entry` to this `ElemPathBuilder` */
  def prepend(entry: ElemPathBuilder.Entry): ElemPathBuilder = ElemPathBuilder(entry +: self.entries)

  /** Appends a given `Entry` to this `ElemPathBuilder` */
  def append(entry: ElemPathBuilder.Entry): ElemPathBuilder = ElemPathBuilder(self.entries :+ entry)

  /** Builds the `ElemPath`, using the passed `Scope`, which must be invertible */
  def build(scope: Scope): ElemPath = {
    require(scope.isInvertible, "Scope '%s' is not invertible".format(scope))

    val resolvedEntries: immutable.IndexedSeq[ElemPath.Entry] = entries map { _.build(scope) }
    ElemPath(resolvedEntries)
  }

  override def equals(obj: Any): Boolean = obj match {
    case other: ElemPathBuilder =>
      if (hashCode != other.hashCode) false else entries == other.entries
    case _ => false
  }

  override def hashCode: Int = entries.hashCode

  override def toString: String = entries.toString
}

object ElemPathBuilder {

  val Root: ElemPathBuilder = ElemPathBuilder(immutable.IndexedSeq())

  def apply(entries: immutable.IndexedSeq[ElemPathBuilder.Entry]): ElemPathBuilder = new ElemPathBuilder(entries)

  /** Easy to use factory method for `ElemPathBuilder` instances */
  def from(entries: ElemPathBuilder.Entry*): ElemPathBuilder = new ElemPathBuilder(Vector(entries: _*))

  /** An entry in an `ElemPathBuilder`, as an qname plus zero-based index of the elem as child (with that name) of the parent. */
  final case class Entry(qname: QName, index: Int) extends Immutable {
    require(qname ne null)
    require(index >= 0)

    /** Builds the `ElemPath.Entry`, using the passed `Scope`, which must be invertible */
    def build(scope: Scope): ElemPath.Entry = {
      require(scope.isInvertible, "Scope '%s' is not invertible".format(scope))

      val ename: EName = scope.resolveQName(qname).getOrElse(sys.error("Could not resolve qname '%s' in scope %s".format(qname, scope)))
      ElemPath.Entry(ename, index)
    }

    def localName: String = qname.localPart
  }

  /** Constructs an `ElemPathBuilder.Entry` component */
  def comp(qname: QName, index: Int): Entry = Entry(qname, index)
}
