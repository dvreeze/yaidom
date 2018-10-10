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
 * path entry) as well. The absolute paths can never be empty (unlike navigation paths).
 *
 * An [[eu.cdevreeze.yaidom.core.AbsolutePath]] corresponds to one and only one canonical path of the element,
 * which is the corresponding (canonical) XPath expression. See http://ns.inria.org/active-tags/glossary/glossary.html#canonical-path.
 * Note, however, that these XPath expression use URI qualified EQ-names rather than qualified names.
 *
 * The `toString` format of absolute paths is the same as for the equally named class in Saxon. Note the Saxon absolute paths may be empty,
 * whereas yaidom absolute paths can never be empty. After all, yaidom absolute paths can only be used for elements, not for documents.
 *
 * @author Chris de Vreeze
 */
final class AbsolutePath(val entries: immutable.IndexedSeq[AbsolutePath.Entry]) {
  require(entries ne null) // scalastyle:off null
  require(entries.nonEmpty, s"Empty absolute paths not allowed")
  require(entries(0).index == 0, s"The first absolute path entry must have index 0 (position 1)")

  /** Returns true if this absolute path has precisely one entry */
  def isRoot: Boolean = {
    entries.size == 1
  }

  /** Returns the element name (as EName) of the last absolute path entry */
  def elementName: EName = lastEntry.elementName

  /** Appends a given `Entry` to this `AbsolutePath` */
  def append(entry: AbsolutePath.Entry): AbsolutePath = AbsolutePath(this.entries :+ entry)

  /**
   * Gets the parent (if any, because a root path has no parent) wrapped in an `Option`.
   */
  def parentOption: Option[AbsolutePath] = this match {
    case p if p.isRoot => None
    case _ => Some(AbsolutePath(entries.dropRight(1)))
  }

  /** Like `parentOption`, but unwrapping the result (or throwing an exception otherwise) */
  def parent: AbsolutePath = {
    parentOption.getOrElse(sys.error("A root absolute path has no parent"))
  }

  /**
   * Returns the ancestors-or-self, starting with this absolute path, then the parent (if any),
   * and ending with a root absolute path.
   */
  def ancestorsOrSelf: immutable.IndexedSeq[AbsolutePath] = {
    @tailrec
    def accumulate(path: AbsolutePath, acc: mutable.ArrayBuffer[AbsolutePath]): mutable.ArrayBuffer[AbsolutePath] = {
      acc += path
      if (path.isRoot) acc else accumulate(path.parent, acc)
    }

    accumulate(this, mutable.ArrayBuffer[AbsolutePath]()).toIndexedSeq
  }

  /** Returns the ancestors, starting with the parent (if any), and ending with a root absolute path */
  def ancestors: immutable.IndexedSeq[AbsolutePath] = ancestorsOrSelf.drop(1)

  /** Returns `ancestorsOrSelf.find(p)` */
  def findAncestorOrSelf(p: AbsolutePath => Boolean): Option[AbsolutePath] = {
    ancestorsOrSelf.find(p)
  }

  /** Returns `ancestors.find(p)` */
  def findAncestor(p: AbsolutePath => Boolean): Option[AbsolutePath] = {
    ancestors.find(p)
  }

  /** Returns the first entry */
  def firstEntry: AbsolutePath.Entry = entries.head

  /** Returns the last entry */
  def lastEntry: AbsolutePath.Entry = entries.takeRight(1).head

  /** Convenience method returning true if at least one entry has the given element name */
  def containsName(ename: EName): Boolean = entries.exists(_.elementName == ename)

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
    val entryXPaths = entries.map(_.toCanonicalXPath)
    entryXPaths.mkString
  }
}

object AbsolutePath {

  def createRoot(elementName: EName): AbsolutePath = {
    AbsolutePath(immutable.IndexedSeq(AbsolutePath.Entry(elementName, 0)))
  }

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
    val entries = parseCanonicalXPathIntoEntries(s)

    AbsolutePath(entries)
  }

  private def parseCanonicalXPathIntoEntries(s: String): immutable.IndexedSeq[AbsolutePath.Entry] = {
    require(s.nonEmpty, s"An empty canonical XPath is not allowed")
    require(s.startsWith("/Q{"), s"The canonical XPath must start with '/Q{', but found '$s'")

    val entryBoundaryIdx = s.indexOf("]/Q{", 3)

    if (entryBoundaryIdx < 0) {
      immutable.IndexedSeq(AbsolutePath.Entry.fromCanonicalXPath(s))
    } else {
      val nextEntry = AbsolutePath.Entry.fromCanonicalXPath(s.substring(0, entryBoundaryIdx + 1))

      // Recursive call
      val remainingEntries = parseCanonicalXPathIntoEntries(s.substring(entryBoundaryIdx + 1))

      nextEntry +: remainingEntries
    }
  }

  /**
   * Extractor turning a AbsolutePath into a sequence of entries.
   */
  def unapply(path: AbsolutePath): Option[immutable.IndexedSeq[AbsolutePath.Entry]] = {
    Some(path.entries)
  }

  /** An entry in an `AbsolutePath`, as an expanded element name plus zero-based index of the elem as child element (with that name) of the parent. */
  final case class Entry(elementName: EName, index: Int) {
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
