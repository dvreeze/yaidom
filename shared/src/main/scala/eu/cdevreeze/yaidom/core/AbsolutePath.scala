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

package eu.cdevreeze.yaidom.core

import scala.annotation.tailrec
import scala.collection.immutable
import scala.collection.mutable

/**
 * Absolute path of a descendant (or self) `Elem` relative to and starting with a root `Elem`. It represents a unique absolute path to an element, given
 * a root element, independent of other types of nodes, as if the XML tree only consists of elements.
 *
 * Unlike `Path` objects, which are navigation paths and relative to some root element, absolute paths are absolute, containing the root element (as absolute
 * path entry) as well. The absolute paths of elements should never be empty.
 *
 * An [[eu.cdevreeze.yaidom.core.AbsolutePath]] corresponds to one and only one canonical path of the element,
 * which is the corresponding (canonical) XPath expression. See http://ns.inria.org/active-tags/glossary/glossary.html#canonical-path.
 * Note, however, that these XPath expression use URI qualified EQ-names rather than qualified names.
 *
 * The `toString` format of absolute paths is the same as for the equally named class in Saxon.
 *
 * @author Chris de Vreeze
 */
final class AbsolutePath(val entries: immutable.IndexedSeq[AbsolutePath.Entry]) extends Immutable {
  require(entries ne null) // scalastyle:off null

  /** Returns true if this is the empty `AbsolutePath`, so if it has no entries (which is the case for documents as opposed to elements). */
  def isEmpty: Boolean = entries.isEmpty

  /** Returns true if this is not the empty `AbsolutePath`, so if it has at least one entry */
  def nonEmpty: Boolean = !isEmpty

  /** Returns true if this absolute path has precisely one entry */
  def hasSingleEntry: Boolean = {
    entries.size == 1
  }

  /** Returns the element name (as EName) of the last absolute path entry, if any, wrapped in an Option */
  def elementNameOption: Option[EName] = lastEntryOption.map(_.elementName)

  /** Returns the element name (as EName) of the last absolute path entry, if any, and throws an exception otherwise */
  def elementName: EName = elementNameOption.getOrElse(sys.error(s"An empty absolute path has no (last) element name"))

  /** Appends a given `Entry` to this `AbsolutePath` */
  def append(entry: AbsolutePath.Entry): AbsolutePath = AbsolutePath(this.entries :+ entry)

  /**
   * Gets the non-empty parent (if any, because the empty or singleton absolute path has no non-empty parent) wrapped in an `Option`.
   */
  def nonEmptyParentOption: Option[AbsolutePath] = this match {
    case p if p.isEmpty => None
    case p if p.hasSingleEntry => None
    case _ => Some(AbsolutePath(entries.dropRight(1)))
  }

  /** Like `nonEmptyParentOption`, but unwrapping the result (or throwing an exception otherwise) */
  def nonEmptyParent: AbsolutePath = {
    nonEmptyParentOption.getOrElse(sys.error("An empty or singleton absolute path has no non-empty parent"))
  }

  /**
   * Returns the non-empty ancestors-or-self, starting with this absolute path, if non-empty, then the non-empty parent (if any),
   * and ending with a singleton absolute path
   */
  def nonEmptyAncestorsOrSelf: immutable.IndexedSeq[AbsolutePath] = {
    @tailrec
    def accumulate(path: AbsolutePath, acc: mutable.ArrayBuffer[AbsolutePath]): mutable.ArrayBuffer[AbsolutePath] = {
      if (path.nonEmpty) {
        acc += path
      }
      if (path.isEmpty || path.hasSingleEntry) acc else accumulate(path.nonEmptyParent, acc)
    }

    accumulate(this, mutable.ArrayBuffer[AbsolutePath]()).toIndexedSeq
  }

  /** Returns the non-empty ancestors, starting with the non-empty parent (if any), and ending with a singleton absolute path */
  def nonEmptyAncestors: immutable.IndexedSeq[AbsolutePath] = nonEmptyAncestorsOrSelf.drop(1)

  /** Returns `nonEmptyAncestorsOrSelf.find(p)` */
  def findNonEmptyAncestorOrSelf(p: AbsolutePath => Boolean): Option[AbsolutePath] = {
    nonEmptyAncestorsOrSelf.find(p)
  }

  /** Returns `nonEmptyAncestors.find(p)` */
  def findNonEmptyAncestor(p: AbsolutePath => Boolean): Option[AbsolutePath] = {
    nonEmptyAncestors.find(p)
  }

  /** Returns the first entry, if any, wrapped in an `Option` */
  def firstEntryOption: Option[AbsolutePath.Entry] = entries.headOption

  /** Returns the first entry, if any, and throws an exception otherwise */
  def firstEntry: AbsolutePath.Entry = firstEntryOption.getOrElse(sys.error("There are no entries"))

  /** Returns the last entry, if any, wrapped in an `Option` */
  def lastEntryOption: Option[AbsolutePath.Entry] = entries.takeRight(1).headOption

  /** Returns the last entry, if any, and throws an exception otherwise */
  def lastEntry: AbsolutePath.Entry = lastEntryOption.getOrElse(sys.error("There are no entries"))

  /** Convenience method returning true if the first entry (if any) has the given element name */
  def startsWithName(ename: EName): Boolean = firstEntryOption exists { entry => entry.elementName == ename }

  /** Convenience method returning true if the last entry (if any) has the given element name */
  def endsWithName(ename: EName): Boolean = lastEntryOption exists { entry => entry.elementName == ename }

  /** Convenience method returning true if at least one entry has the given element name */
  def containsName(ename: EName): Boolean = entries exists { entry => entry.elementName == ename }

  override def equals(obj: Any): Boolean = obj match {
    case other: AbsolutePath =>
      if (hashCode != other.hashCode) false else entries == other.entries
    case _ => false
  }

  override def hashCode: Int = entries.hashCode

  /** Returns `toCanonicalXPath` */
  override def toString: String = toCanonicalXPath

  /**
   * Returns the corresponding canonical XPath, replacing QNames by URI qualified EQ-names.
   *
   * See http://ns.inria.org/active-tags/glossary/glossary.html#canonical-path for (real) canonical XPath.
   *
   * The XPath expressions returned by this method need no context to convey their semantics,
   * unlike XPath expressions containing QNames (canonical or otherwise). This is an important advantage of these expressions, in spite
   * of their relative verbosity. A good use case is error reporting about parts of XML documents.
   * For example, Saxon-EE uses a similar notation for error reporting (in XML format) in its schema validator.
   */
  def toCanonicalXPath: String = {
    val entryXPaths = entries map { entry => entry.toCanonicalXPath }
    entryXPaths.mkString
  }
}

object AbsolutePath {

  val Empty: AbsolutePath = AbsolutePath(immutable.IndexedSeq())

  def apply(entries: immutable.IndexedSeq[AbsolutePath.Entry]): AbsolutePath = new AbsolutePath(entries)

  /** Easy to use factory method for `AbsolutePath` instances */
  def from(entries: (EName, Int)*): AbsolutePath = {
    val entrySeq: Seq[AbsolutePath.Entry] = entries map { p => Entry(p._1, p._2) }
    new AbsolutePath(entrySeq.toIndexedSeq)
  }

  /**
   * Parses a String, which must be in the `toCanonicalXPath` format, into an `AbsolutePath`.
   */
  def fromCanonicalXPath(s: String): AbsolutePath = {
    if (s.isEmpty) {
      AbsolutePath.Empty
    } else {
      require(s.startsWith("/Q{"), s"The canonical XPath must start with '/Q{', but found '$s'")

      val entryBoundaryIdx = s.indexOf("]/Q{", 3)

      if (entryBoundaryIdx < 0) {
        AbsolutePath(immutable.IndexedSeq(AbsolutePath.Entry.fromCanonicalXPath(s)))
      } else {
        val nextEntry = AbsolutePath.Entry.fromCanonicalXPath(s.substring(0, entryBoundaryIdx + 1))

        // Recursive call
        val nextPath = fromCanonicalXPath(s.substring(entryBoundaryIdx + 1))

        AbsolutePath(nextEntry +: nextPath.entries)
      }
    }
  }

  /**
   * Extractor turning a AbsolutePath into a sequence of entries.
   */
  def unapply(path: AbsolutePath): Option[immutable.IndexedSeq[AbsolutePath.Entry]] = {
    Some(path.entries)
  }

  /** An entry in an `AbsolutePath`, as an expanded element name plus zero-based index of the elem as child element (with that name) of the parent. */
  final case class Entry(elementName: EName, index: Int) extends Immutable {
    require(elementName ne null) // scalastyle:off null
    require(index >= 0)

    /** Position (1-based) of the element as child of the parent. Is 1 + index. */
    def position: Int = 1 + index

    /** Returns the corresponding canonical XPath, replacing QNames by URI qualified EQ-names */
    def toCanonicalXPath: String = {
      s"/${elementName.toUriQualifiedNameString}[${position}]"
    }

    def localName: String = elementName.localPart
  }

  object Entry {

    /** Parses a `String`, which must be in the `toCanonicalXPath` format, into an `AbsolutePath.Entry` */
    def fromCanonicalXPath(s: String): Entry = {
      require(s.startsWith("/Q{"), s"The canonical XPath for the 'entry' must start with '/Q{', but found '$s'")
      require(s.endsWith("]"), s"The canonical XPath for the 'entry' must have a position ending with ']', such as [1], but found '$s'")
      require(s.size >= 5, s"The canonical XPath for the 'entry' must contain at least 5 characters, but found '$s'")

      val positionIndex = s.lastIndexOf("[")
      require(positionIndex >= 2, s"The canonical XPath for the 'entry' must have a position starting with '[', but found '$s'")
      require(positionIndex <= s.size - 3, s"The canonical XPath for the 'entry' must have a position of at least 3 characters, such as [1], but found '$s'")

      val eqnameString = s.substring(1, positionIndex)
      val elementName = EName.fromUriQualifiedNameString(eqnameString)

      val position = s.substring(positionIndex + 1, s.size - 1).toInt

      Entry(elementName, position - 1)
    }
  }
}
