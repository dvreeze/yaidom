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

import scala.collection.immutable

/**
 * Unique identification of a descendant (or self) `Elem` given a root `Elem`. It is used for transformations
 * from one node tree to another collection of nodes.
 *
 * An [[eu.cdevreeze.yaidom.ElemPath]] corresponds to one and only one canonical path of the element (modulo prefix names),
 * which is the corresponding (canonical) XPath expression. See http://ns.inria.org/active-tags/glossary/glossary.html#canonical-path.
 * There is one catch, though. The `ElemPath` does not know the root element name, so that is not a part of the corresponding
 * canonical XPath expression. See the documentation of method `toCanonicalXPath`.
 *
 * The `ElemPath` contains an `IndexedSeq` of path entries for a specific child element, grandchild element etc.,
 * but the (root) element itself is referred to by an empty list of path entries.
 *
 * Strictly speaking, each element in a tree would be uniquely identified by path entries that only contained
 * a child index instead of an element name plus child index (of children with the given name). Yet that would
 * be far less easy to use. Hence `ElemPath.Entry` instances each contain an element name plus index.
 */
final class ElemPath(val entries: immutable.IndexedSeq[ElemPath.Entry]) extends Immutable { self =>

  require(entries ne null)

  /** Returns true if this is the root `ElemPath`, so if it has no entries */
  def isRoot: Boolean = entries.isEmpty

  /** Prepends a given `Entry` to this `ElemPath` */
  def prepend(entry: ElemPath.Entry): ElemPath = ElemPath(entry +: self.entries)

  /** Returns the `ElemPath` with the first path entry (if any) removed, wrapped in an `Option`. */
  def withoutFirstEntryOption: Option[ElemPath] = entries match {
    case xs if xs.isEmpty => None
    case _ => Some(ElemPath(entries.tail))
  }

  /** Like `withoutFirstEntryOption`, but unwrapping the result (or throwing an exception otherwise) */
  def withoutFirstEntry: ElemPath = withoutFirstEntryOption.getOrElse(sys.error("The root path has no first entry to remove"))

  /** Appends a given `Entry` to this `ElemPath` */
  def append(entry: ElemPath.Entry): ElemPath = ElemPath(self.entries :+ entry)

  /**
   * Gets the parent path (if any, because the root path has no parent) wrapped in an `Option`.
   *
   * This method shows much of the reason why class `ElemPath` exists. If we know an element's `ElemPath`, and therefore its
   * parent `ElemPath` (using this method), then we can obtain the parent element by following the parent path from the
   * root of the tree.
   */
  def parentPathOption: Option[ElemPath] = entries match {
    case xs if xs.isEmpty => None
    case _ => Some(ElemPath(entries.dropRight(1)))
  }

  /** Like `parentPathOption`, but unwrapping the result (or throwing an exception otherwise) */
  def parentPath: ElemPath = parentPathOption.getOrElse(sys.error("The root path has no parent path"))

  /** Returns the first entry, if any, wrapped in an `Option` */
  def firstEntryOption: Option[ElemPath.Entry] = entries.headOption

  /** Returns the first entry, if any, and throws an exception otherwise */
  def firstEntry: ElemPath.Entry = firstEntryOption.getOrElse(sys.error("There are no entries"))

  /** Returns the last entry, if any, wrapped in an `Option` */
  def lastEntryOption: Option[ElemPath.Entry] = entries.takeRight(1).headOption

  /** Returns the last entry, if any, and throws an exception otherwise */
  def lastEntry: ElemPath.Entry = lastEntryOption.getOrElse(sys.error("There are no entries"))

  /** Convenience method returning true if the first entry (if any) has the given element name */
  def startsWithName(ename: ExpandedName): Boolean = firstEntryOption exists { entry => entry.elementName == ename }

  /** Convenience method returning true if the last entry (if any) has the given element name */
  def endsWithName(ename: ExpandedName): Boolean = lastEntryOption exists { entry => entry.elementName == ename }

  /** Convenience method returning true if at least one entry has the given element name */
  def containsName(ename: ExpandedName): Boolean = entries exists { entry => entry.elementName == ename }

  override def equals(obj: Any): Boolean = obj match {
    case other: ElemPath =>
      if (hashCode != other.hashCode) false else entries == other.entries
    case _ => false
  }

  override def hashCode: Int = entries.hashCode

  /**
   * Given a `Scope`, returns the corresponding canonical XPath, but modified for the root element (which is unknown in the `ElemPath`).
   * The modification is that the root element is written as a slash followed by an asterisk.
   *
   * See http://ns.inria.org/active-tags/glossary/glossary.html#canonical-path.
   */
  def toCanonicalXPath(scope: Scope): String = {
    val entryXPaths = entries map { entry => entry.toCanonicalXPath(scope) }
    "/" + "*" + entryXPaths.mkString
  }
}

object ElemPath {

  val Root: ElemPath = ElemPath(immutable.IndexedSeq())

  def apply(entries: immutable.IndexedSeq[ElemPath.Entry]): ElemPath = new ElemPath(entries)

  /** Parses a String, which must be in the `toCanonicalXPath` format, into an `ElemPath` */
  def fromCanonicalXPath(s: String)(scope: Scope): ElemPath = {
    // We use the fact that "/", "*", "[" and "]" are never part of qualified names!

    require(s.startsWith("/"))
    require(s.drop(1).startsWith("*"))
    val remainder = s.drop(2)

    def getEntryStrings(str: String): List[String] = str match {
      case "" => Nil
      case _ =>
        val idx = str indexWhere { c => c == ']' }
        require(idx > 0)
        val curr = str.take(idx + 1)
        val rest = str.drop(idx + 1)
        require(rest.size == 0 || rest.startsWith("/"))
        curr :: getEntryStrings(rest)
    }

    val entryStrings = getEntryStrings(remainder).toIndexedSeq
    val entries = entryStrings map { entryString => ElemPath.Entry.fromCanonicalXPath(entryString)(scope) }
    ElemPath(entries)
  }

  /** An entry in an `ElemPath`, as an expanded element name plus zero-based index of the elem as child (with that name) of the parent. */
  final case class Entry(elementName: ExpandedName, index: Int) extends Immutable {

    require(elementName ne null)
    require(index >= 0)

    /** Position (1-based) of the element as child of the parent. Is 1 + index. */
    def position: Int = 1 + index

    /** Given a `Scope`, returns the corresponding canonical XPath */
    def toCanonicalXPath(scope: Scope): String = {
      val prefixOption: Option[String] = {
        if (elementName.namespaceUriOption.isEmpty) None else {
          val nsUri: String = elementName.namespaceUriOption.get
          require(scope.prefixScope.values.toSet.contains(nsUri), "Expected at least one prefix for namespace URI '%s'".format(nsUri))

          val result = scope.prefixScope.toList collectFirst {
            case pair if pair._2 == nsUri =>
              val prefix: String = pair._1
              val ns: String = pair._2
              prefix
          }
          require(result.isDefined)
          result
        }
      }

      "%s%s[%d]".format("/", elementName.toQName(prefixOption).toString, position)
    }
  }

  object Entry {

    /** Parses a `String`, which must be in the `toCanonicalXPath` format, into an `ElemPath.Entry`, given a `Scope` */
    def fromCanonicalXPath(s: String)(scope: Scope): Entry = {
      // We use the fact that "/", "[" and "]" are never part of qualified names!

      require(s.startsWith("/"))
      val remainder = s.drop(1)
      require(remainder.size > 3)
      val (qnameString, positionString) = remainder span { c => c != '[' }
      require(positionString.size >= 3)
      require(positionString.startsWith("["))
      require(positionString.endsWith("]"))

      val qname = QName.parse(qnameString)
      val elementName = scope.resolveQName(qname).getOrElse(sys.error("Could not resolve QName '%s'".format(qname)))
      val position = positionString.drop(1).dropRight(1).toInt

      Entry(elementName, position - 1)
    }
  }
}
