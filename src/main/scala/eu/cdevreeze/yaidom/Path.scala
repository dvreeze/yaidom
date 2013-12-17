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
 * Unique identification of a descendant (or self) `Elem` given a root `Elem`. It represents a unique path to an element, given
 * a root element, independent of other types of nodes, as if the XML tree only consists of elements.
 *
 * For example, consider the following XML:
 * {{{
 * <book:Bookstore xmlns:book="http://bookstore/book" xmlns:auth="http://bookstore/author">
 *   <book:Book ISBN="978-0321356680" Price="35" Edition="2">
 *     <book:Title>Effective Java (2nd Edition)</book:Title>
 *     <book:Authors>
 *       <auth:Author>
 *         <auth:First_Name>Joshua</auth:First_Name>
 *         <auth:Last_Name>Bloch</auth:Last_Name>
 *       </auth:Author>
 *     </book:Authors>
 *   </book:Book>
 *   <book:Book ISBN="978-0981531649" Price="35" Edition="2">
 *     <book:Title>Programming in Scala: A Comprehensive Step-by-Step Guide, 2nd Edition</book:Title>
 *     <book:Authors>
 *       <auth:Author>
 *         <auth:First_Name>Martin</auth:First_Name>
 *         <auth:Last_Name>Odersky</auth:Last_Name>
 *       </auth:Author>
 *       <auth:Author>
 *         <auth:First_Name>Lex</auth:First_Name>
 *         <auth:Last_Name>Spoon</auth:Last_Name>
 *       </auth:Author>
 *       <auth:Author>
 *         <auth:First_Name>Bill</auth:First_Name>
 *         <auth:Last_Name>Venners</auth:Last_Name>
 *       </auth:Author>
 *     </book:Authors>
 *   </book:Book>
 * </book:Bookstore>
 * }}}
 *
 * Then the last name of the first author of the Scala book (viz. Odersky) has the following path:
 * {{{
 * Path.from(
 *   EName("{http://bookstore/book}Book") -> 1,
 *   EName("{http://bookstore/book}Authors") -> 0,
 *   EName("{http://bookstore/author}Author") -> 0,
 *   EName("{http://bookstore/author}Last_Name") -> 0
 * )
 * }}}
 * or:
 * {{{
 * PathBuilder.from(
 *   QName("book:Book") -> 1,
 *   QName("book:Authors") -> 0,
 *   QName("auth:Author") -> 0,
 *   QName("auth:Last_Name") -> 0).build(Scope.from("book" -> "http://bookstore/book", "auth" -> "http://bookstore/author"))
 * }}}
 *
 * `Path` instances are useful in certain queries (see [[eu.cdevreeze.yaidom.PathAwareElemLike]]), and in "functional updates"
 * (see [[eu.cdevreeze.yaidom.UpdatableElemLike]]).
 *
 * An [[eu.cdevreeze.yaidom.Path]] corresponds to one and only one canonical path of the element (modulo prefix names),
 * which is the corresponding (canonical) XPath expression. See http://ns.inria.org/active-tags/glossary/glossary.html#canonical-path.
 * There is one catch, though. The `Path` does not know the root element name, so that is not a part of the corresponding
 * canonical XPath expression. See the documentation of method `toCanonicalXPath`.
 *
 * The `Path` contains an `IndexedSeq` of path entries for a specific child element, grandchild element etc.,
 * but the (root) element itself is referred to by an empty list of path entries.
 *
 * As an alternative to class `Path`, each element in a tree could be uniquely identified by "path entries" that only contained
 * a child index instead of an element name plus element child index (of element children with the given name). Yet that would
 * be far less easy to use. Hence `Path.Entry` instances each contain an element name plus index.
 *
 * '''Warning: indexing using Paths can be slow, especially in large XML trees.''' Hence, it is advisable to use class `Path`
 * wisely in queries and "functional updates". Most queries for elements can be written without them (using the methods in trait
 * `ParentElemLike`, instead of those added by subtrait `PathAwareElemLike`).
 *
 * @author Chris de Vreeze
 */
final class Path(val entries: immutable.IndexedSeq[Path.Entry]) extends Immutable { self =>

  require(entries ne null)

  /** Returns true if this is the root `Path`, so if it has no entries */
  def isRoot: Boolean = entries.isEmpty

  /** Returns the element name (as EName) of the last path entry, if any, wrapped in an Option */
  def elementNameOption: Option[EName] = lastEntryOption.map(_.elementName)

  /** Prepends a given `Entry` to this `Path` */
  def prepend(entry: Path.Entry): Path = Path(entry +: self.entries)

  /** Returns the `Path` with the first path entry (if any) removed, wrapped in an `Option`. */
  def withoutFirstEntryOption: Option[Path] = entries match {
    case xs if xs.isEmpty => None
    case _ => Some(Path(entries.tail))
  }

  /** Like `withoutFirstEntryOption`, but unwrapping the result (or throwing an exception otherwise) */
  def withoutFirstEntry: Path = withoutFirstEntryOption.getOrElse(sys.error("The root path has no first entry to remove"))

  /** Appends a given `Entry` to this `Path` */
  def append(entry: Path.Entry): Path = Path(self.entries :+ entry)

  /** Appends a given relative `Path` to this `Path` */
  def ++(other: Path): Path = Path(this.entries ++ other.entries)

  /**
   * Gets the parent path (if any, because the root path has no parent) wrapped in an `Option`.
   *
   * This method shows much of the reason why class `Path` exists. If we know an element's `Path`, and therefore its
   * parent `Path` (using this method), then we can obtain the parent element by following the parent path from the
   * root of the tree.
   */
  def parentPathOption: Option[Path] = entries match {
    case xs if xs.isEmpty => None
    case _ => Some(Path(entries.dropRight(1)))
  }

  /** Like `parentPathOption`, but unwrapping the result (or throwing an exception otherwise) */
  def parentPath: Path = parentPathOption.getOrElse(sys.error("The root path has no parent path"))

  /** Returns the ancestor-or-self paths, starting with this path, then the parent (if any), and ending with the root path */
  def ancestorOrSelfPaths: immutable.IndexedSeq[Path] = {
    @tailrec
    def accumulate(path: Path, acc: mutable.ArrayBuffer[Path]): mutable.ArrayBuffer[Path] = {
      acc += path
      if (path.isRoot) acc else accumulate(path.parentPath, acc)
    }

    accumulate(self, mutable.ArrayBuffer[Path]()).toIndexedSeq
  }

  /** Returns the ancestor paths, starting with the parent path (if any), and ending with the root path */
  def ancestorPaths: immutable.IndexedSeq[Path] = ancestorOrSelfPaths.drop(1)

  /** Returns `ancestorOrSelfPaths find { path => p(path) }` */
  def findAncestorOrSelfPath(p: Path => Boolean): Option[Path] = {
    ancestorOrSelfPaths find { path => p(path) }
  }

  /** Returns `ancestorPaths find { path => p(path) }` */
  def findAncestorPath(p: Path => Boolean): Option[Path] = {
    ancestorPaths find { path => p(path) }
  }

  /** Returns the first entry, if any, wrapped in an `Option` */
  def firstEntryOption: Option[Path.Entry] = entries.headOption

  /** Returns the first entry, if any, and throws an exception otherwise */
  def firstEntry: Path.Entry = firstEntryOption.getOrElse(sys.error("There are no entries"))

  /** Returns the last entry, if any, wrapped in an `Option` */
  def lastEntryOption: Option[Path.Entry] = entries.takeRight(1).headOption

  /** Returns the last entry, if any, and throws an exception otherwise */
  def lastEntry: Path.Entry = lastEntryOption.getOrElse(sys.error("There are no entries"))

  /** Convenience method returning true if the first entry (if any) has the given element name */
  def startsWithName(ename: EName): Boolean = firstEntryOption exists { entry => entry.elementName == ename }

  /** Convenience method returning true if the last entry (if any) has the given element name */
  def endsWithName(ename: EName): Boolean = lastEntryOption exists { entry => entry.elementName == ename }

  /** Convenience method returning true if at least one entry has the given element name */
  def containsName(ename: EName): Boolean = entries exists { entry => entry.elementName == ename }

  override def equals(obj: Any): Boolean = obj match {
    case other: Path =>
      if (hashCode != other.hashCode) false else entries == other.entries
    case _ => false
  }

  override def hashCode: Int = entries.hashCode

  override def toString: String = "Path(%s)".format(entries.toString)

  /**
   * Given an invertible `Scope`, returns the corresponding canonical XPath, but modified for the root element (which is unknown in the `Path`).
   * The modification is that the root element is written as a slash followed by an asterisk.
   *
   * See http://ns.inria.org/active-tags/glossary/glossary.html#canonical-path.
   */
  def toCanonicalXPath(scope: Scope): String = {
    require(scope.isInvertible, "Scope '%s' is not invertible".format(scope))

    val entryXPaths = entries map { entry => entry.toCanonicalXPath(scope) }
    "/" + "*" + entryXPaths.mkString
  }
}

object Path {

  val Root: Path = Path(immutable.IndexedSeq())

  def apply(entries: immutable.IndexedSeq[Path.Entry]): Path = new Path(entries)

  /** Returns `fromCanonicalXPath(s)(scope)`. The passed scope must be invertible. */
  def apply(s: String)(scope: Scope): Path = {
    require(scope.isInvertible, "Scope '%s' is not invertible".format(scope))

    fromCanonicalXPath(s)(scope)
  }

  /** Easy to use factory method for `Path` instances */
  def from(entries: (EName, Int)*): Path = {
    val entrySeq: Seq[Path.Entry] = entries map { p => Entry(p._1, p._2) }
    new Path(entrySeq.toIndexedSeq)
  }

  /** Parses a String, which must be in the `toCanonicalXPath` format, into an `Path`. The passed scope must be invertible. */
  def fromCanonicalXPath(s: String)(scope: Scope): Path = {
    require(scope.isInvertible, "Scope '%s' is not invertible".format(scope))

    // We use the fact that "/", "*", "[" and "]" are never part of qualified names!

    require(s.startsWith("/"), "The canonical XPath must start with a slash")
    require(s.drop(1).startsWith("*"), "The canonical XPath must have an asterisk after the starting slash")
    val remainder = s.drop(2)
    require(remainder.headOption.forall(_ == '/'), "The canonical XPath's third character, if any, must be a slash")

    def getEntryStrings(str: String): List[String] = str match {
      case "" => Nil
      case _ =>
        val idx = str indexWhere { c => c == ']' }
        require(idx > 0, "The canonical XPath must have positions for each 'entry', such as [1]")
        val curr = str.take(idx + 1)
        val rest = str.drop(idx + 1)
        require(rest.size == 0 || rest.startsWith("/"), "In the canonical XPath, after a position, either nothing or a slash follows")
        curr :: getEntryStrings(rest)
    }

    val entryStrings = getEntryStrings(remainder).toIndexedSeq
    val entries = entryStrings map { entryString => Path.Entry.fromCanonicalXPath(entryString)(scope) }
    Path(entries)
  }

  /** An entry in an `Path`, as an expanded element name plus zero-based index of the elem as child element (with that name) of the parent. */
  final case class Entry(elementName: EName, index: Int) extends Immutable {
    require(elementName ne null)
    require(index >= 0)

    /** Position (1-based) of the element as child of the parent. Is 1 + index. */
    def position: Int = 1 + index

    /** Given an invertible `Scope`, returns the corresponding canonical XPath */
    def toCanonicalXPath(scope: Scope): String = {
      require(scope.isInvertible, "Scope '%s' is not invertible".format(scope))

      val prefixOption: Option[String] = {
        if (elementName.namespaceUriOption.isEmpty) None else {
          val nsUri: String = elementName.namespaceUriOption.get
          require((scope.prefixNamespaceMap - "").values.toSet.contains(nsUri), "Expected at least one prefix for namespace URI '%s'".format(nsUri))

          val result = (scope.prefixNamespaceMap - "").toList collectFirst {
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

    def localName: String = elementName.localPart
  }

  object Entry {

    /** Returns `fromCanonicalXPath(s)(scope)`. The passed scope must be invertible. */
    def apply(s: String)(scope: Scope): Entry = {
      require(scope.isInvertible, "Scope '%s' is not invertible".format(scope))

      fromCanonicalXPath(s)(scope)
    }

    /** Parses a `String`, which must be in the `toCanonicalXPath` format, into an `Path.Entry`, given an invertible `Scope` */
    def fromCanonicalXPath(s: String)(scope: Scope): Entry = {
      require(scope.isInvertible, "Scope '%s' is not invertible".format(scope))

      // We use the fact that "/", "[" and "]" are never part of qualified names!

      require(s.startsWith("/"), "The canonical XPath for the 'entry' must start with a slash")
      val remainder = s.drop(1)
      require(remainder.size > 3, "The canonical XPath for the 'entry' must contain at least 4 characters")
      val (qnameString, positionString) = remainder span { c => c != '[' }
      require(positionString.size >= 3, "The canonical XPath for the 'entry' must have a position of at least 3 characters, such as [1]")
      require(positionString.startsWith("["), "The canonical XPath for the 'entry' must have a position starting with '[', such as [1]")
      require(positionString.endsWith("]"), "The canonical XPath for the 'entry' must have a position ending with ']', such as [1]")

      val qname = QName.parse(qnameString)
      val elementName = scope.resolveQNameOption(qname).getOrElse(sys.error("Could not resolve QName '%s'".format(qname)))
      val position = positionString.drop(1).dropRight(1).toInt

      Entry(elementName, position - 1)
    }
  }
}
