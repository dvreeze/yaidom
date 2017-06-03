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
 * Unique identification of a descendant (or self) `Elem` given a root `Elem`. It represents a unique path to an element, given
 * a root element, independent of other types of nodes, as if the XML tree only consists of elements.
 *
 * In other words, a `Path` is a '''sequence of instructions, each of them stating how to get to a specific child element'''.
 * Each such instruction is a `Path.Entry`. So Paths do not contain the root element, and we can talk about Paths
 * in isolation, without referring to any specific DOM-like tree.
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
 * `Path` instances are useful when navigating (see [[eu.cdevreeze.yaidom.queryapi.IsNavigable]]), and in "functional updates"
 * (see [[eu.cdevreeze.yaidom.queryapi.UpdatableElemLike]]).
 *
 * An [[eu.cdevreeze.yaidom.core.Path]] corresponds to one and only one canonical path of the element (modulo prefix names),
 * which is the corresponding (canonical) XPath expression. See http://ns.inria.org/active-tags/glossary/glossary.html#canonical-path.
 * There is one catch, though. The `Path` does not know the root element name, so that is not a part of the corresponding
 * canonical XPath expression. See the documentation of method `toResolvedCanonicalXPath`.
 *
 * The `Path` contains an `IndexedSeq` of path entries for a specific child element, grandchild element etc.,
 * but the (root) element itself is referred to by an empty list of path entries.
 *
 * As an alternative to class `Path`, each element in a tree could be uniquely identified by "path entries" that only contained
 * a child index instead of an element name plus element child index (of element children with the given name). Yet that would
 * be far less easy to use. Hence `Path.Entry` instances each contain an element name plus index.
 *
 * @author Chris de Vreeze
 */
final class Path(val entries: immutable.IndexedSeq[Path.Entry]) extends Immutable { self =>
  require(entries ne null)

  /** Returns true if this is the empty `Path`, so if it has no entries */
  def isEmpty: Boolean = entries.isEmpty

  /** Returns true if this is not the empty `Path`, so if it has at least one entry */
  def nonEmpty: Boolean = !isEmpty

  /** Returns the element name (as EName) of the last path entry, if any, wrapped in an Option */
  def elementNameOption: Option[EName] = lastEntryOption.map(_.elementName)

  /** Prepends a given `Entry` to this `Path` */
  def prepend(entry: Path.Entry): Path = Path(entry +: self.entries)

  /** Prepends a given `Path` to this `Path` */
  def prepend(other: Path): Path = Path(other.entries ++ self.entries)

  /** Returns the `Path` with the first path entry (if any) removed, wrapped in an `Option`. */
  def withoutFirstEntryOption: Option[Path] = entries match {
    case xs if xs.isEmpty => None
    case _                => Some(Path(entries.tail))
  }

  /** Like `withoutFirstEntryOption`, but unwrapping the result (or throwing an exception otherwise) */
  def withoutFirstEntry: Path = withoutFirstEntryOption.getOrElse(sys.error("The root path has no first entry to remove"))

  /** Appends a given `Entry` to this `Path` */
  def append(entry: Path.Entry): Path = Path(self.entries :+ entry)

  /** Appends a given relative `Path` to this `Path` */
  def append(other: Path): Path = Path(self.entries ++ other.entries)

  /** Appends a given relative `Path` to this `Path`. Alias for `append(other)`. */
  def ++(other: Path): Path = append(other)

  /**
   * Gets the parent path (if any, because the empty path has no parent) wrapped in an `Option`.
   *
   * This method shows much of the reason why class `Path` exists. If we know an element's `Path`, and therefore its
   * parent `Path` (using this method), then we can obtain the parent element by following the parent path from the
   * root of the tree.
   */
  def parentPathOption: Option[Path] = entries match {
    case xs if xs.isEmpty => None
    case _                => Some(Path(entries.dropRight(1)))
  }

  /** Like `parentPathOption`, but unwrapping the result (or throwing an exception otherwise) */
  def parentPath: Path = parentPathOption.getOrElse(sys.error("The root path has no parent path"))

  /** Returns the ancestor-or-self paths, starting with this path, then the parent (if any), and ending with the root path */
  def ancestorOrSelfPaths: immutable.IndexedSeq[Path] = {
    @tailrec
    def accumulate(path: Path, acc: mutable.ArrayBuffer[Path]): mutable.ArrayBuffer[Path] = {
      acc += path
      if (path.isEmpty) acc else accumulate(path.parentPath, acc)
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

  override def toString: String = s"Path(${entries.toString})"

  /**
   * Returns the corresponding "resolved" canonical XPath, but modified for the root element (which is unknown in the `Path`).
   * The modification is that the root element is written as a slash followed by an asterisk. Unlike real
   * XPath, QNames are resolved as ENames, and shown in James Clark notation for ENames.
   *
   * See http://ns.inria.org/active-tags/glossary/glossary.html#canonical-path for (real) canonical XPath.
   *
   * The "resolved" XPath-like expressions returned by this method need no context to convey their semantics,
   * unlike real XPath expressions (canonical or otherwise). This is an important advantage of these expressions, in spite
   * of their relative verbosity. A good use case is error reporting about parts of XML documents.
   * For example, Saxon-EE uses a similar notation for error reporting (in XML format) in its schema validator.
   */
  def toResolvedCanonicalXPath: String = {
    val entryXPaths = entries map { entry => entry.toResolvedCanonicalXPath }
    "/" + "*" + entryXPaths.mkString
  }

  /**
   * Given an invertible `Scope`, returns the corresponding canonical XPath, but modified for the root element (which is unknown in the `Path`).
   * The modification is that the root element is written as a slash followed by an asterisk.
   *
   * See http://ns.inria.org/active-tags/glossary/glossary.html#canonical-path.
   */
  @deprecated(message = "Use 'toResolvedCanonicalXPath' instead", since = "1.6.2")
  def toCanonicalXPath(scope: Scope): String = {
    require(scope.isInvertible, s"Scope '${scope}' is not invertible")

    val entryXPaths = entries map { entry => entry.toCanonicalXPath(scope) }
    "/" + "*" + entryXPaths.mkString
  }
}

object Path {

  val Empty: Path = Path(immutable.IndexedSeq())

  def apply(entries: immutable.IndexedSeq[Path.Entry]): Path = new Path(entries)

  /** Returns `fromCanonicalXPath(s)(scope)`. The passed scope must be invertible. */
  @deprecated(message = "Use 'fromResolvedCanonicalXPath' instead", since = "1.6.2")
  def apply(s: String)(scope: Scope): Path = {
    require(scope.isInvertible, s"Scope '${scope}' is not invertible")

    fromCanonicalXPath(s)(scope)
  }

  /** Easy to use factory method for `Path` instances */
  def from(entries: (EName, Int)*): Path = {
    val entrySeq: Seq[Path.Entry] = entries map { p => Entry(p._1, p._2) }
    new Path(entrySeq.toIndexedSeq)
  }

  /**
   * Parses a String, which must be in the `toResolvedCanonicalXPath` format, into a `Path`.
   *
   * Entry boundaries are recognized by finding ']' characters (followed by the slash starting the
   * next entry). In theory this is not robust, but in practice it is. See discussions on valid
   * characters in IRIs and URIs.
   */
  def fromResolvedCanonicalXPath(s: String): Path = {
    require(s.startsWith("/*"), "The 'resolved' canonical XPath must start with '/*'")
    val remainder = s.drop(2)
    require(remainder.headOption.forall(_ == '/'), "The 'resolved' canonical XPath's third character, if any, must be a slash")
    require(
      remainder.isEmpty || remainder.endsWith("]"),
      "The 'resolved' canonical XPath, if non-empty, must have a (last entry) position ending with ']', such as [1]")

    def getEntryStringsReversed(str: String): List[String] = str match {
      case "" => Nil
      case _ =>
        assert(str.startsWith("/"))
        assert(str.endsWith("]"))

        val previousEntryEndIndex = str.lastIndexOf("]/")

        val (entryString, previousEntriesString) =
          if (previousEntryEndIndex < 0) {
            (str, "")
          } else {
            (str.substring(previousEntryEndIndex + 1), str.substring(0, previousEntryEndIndex + 1))
          }

        // Recursive call
        entryString :: getEntryStringsReversed(previousEntriesString)
    }

    val entryStrings = getEntryStringsReversed(remainder).toIndexedSeq.reverse
    val entries = entryStrings map { entryString => Path.Entry.fromResolvedCanonicalXPath(entryString) }
    Path(entries)
  }

  /** Parses a String, which must be in the `toCanonicalXPath` format, into an `Path`. The passed scope must be invertible. */
  @deprecated(message = "Use 'fromResolvedCanonicalXPath' instead", since = "1.6.2")
  def fromCanonicalXPath(s: String)(scope: Scope): Path = {
    require(scope.isInvertible, s"Scope '${scope}' is not invertible")

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

  /**
   * Extractor turning a Path into a sequence of entries.
   */
  def unapply(path: Path): Option[immutable.IndexedSeq[Path.Entry]] = {
    Some(path.entries)
  }

  /** An entry in an `Path`, as an expanded element name plus zero-based index of the elem as child element (with that name) of the parent. */
  final case class Entry(elementName: EName, index: Int) extends Immutable {
    require(elementName ne null)
    require(index >= 0)

    /** Position (1-based) of the element as child of the parent. Is 1 + index. */
    def position: Int = 1 + index

    /** Returns the corresponding "resolved" canonical XPath, replacing QNames by ENames */
    def toResolvedCanonicalXPath: String = {
      s"/${elementName.toString}[${position}]"
    }

    /** Given an invertible `Scope`, returns the corresponding canonical XPath */
    @deprecated(message = "Use 'toResolvedCanonicalXPath' instead", since = "1.6.2")
    def toCanonicalXPath(scope: Scope): String = {
      require(scope.isInvertible, s"Scope '${scope}' is not invertible")

      val prefixOption: Option[String] = {
        if (elementName.namespaceUriOption.isEmpty) None else {
          val nsUri: String = elementName.namespaceUriOption.get
          require(scope.namespaces.contains(nsUri), s"Expected at least one (possibly empty) prefix for namespace URI '${nsUri}'")

          val result = scope.prefixNamespaceMap.toList collectFirst {
            case pair if pair._2 == nsUri =>
              val prefix: String = pair._1
              prefix
          }
          require(result.isDefined)
          if (result.get.isEmpty) None else result
        }
      }

      s"/${elementName.toQName(prefixOption).toString}[${position}]"
    }

    def localName: String = elementName.localPart
  }

  object Entry {

    /** Returns `fromCanonicalXPath(s)(scope)`. The passed scope must be invertible. */
    @deprecated(message = "Use 'fromResolvedCanonicalXPath' instead", since = "1.6.2")
    def apply(s: String)(scope: Scope): Entry = {
      require(scope.isInvertible, s"Scope '${scope}' is not invertible")

      fromCanonicalXPath(s)(scope)
    }

    /** Parses a `String`, which must be in the `toResolvedCanonicalXPath` format, into an `Path.Entry` */
    def fromResolvedCanonicalXPath(s: String): Entry = {
      require(s.startsWith("/"), "The 'resolved' canonical XPath for the 'entry' must start with a slash")
      require(s.endsWith("]"), "The 'resolved' canonical XPath for the 'entry' must have a position ending with ']', such as [1]")
      require(s.size >= 5, "The 'resolved' canonical XPath for the 'entry' must contain at least 5 characters")

      val positionIndex = s.lastIndexOf("[")
      require(positionIndex >= 2, s"The 'resolved' canonical XPath for the 'entry' must have a position starting with '['")
      require(positionIndex <= s.size - 3, "The 'resolved' canonical XPath for the 'entry' must have a position of at least 3 characters, such as [1]")

      val enameString = s.substring(1, positionIndex)
      val elementName = EName.parse(enameString)

      val position = s.substring(positionIndex + 1, s.size - 1).toInt

      Entry(elementName, position - 1)
    }

    /** Parses a `String`, which must be in the `toCanonicalXPath` format, into an `Path.Entry`, given an invertible `Scope` */
    @deprecated(message = "Use 'fromResolvedCanonicalXPath' instead", since = "1.6.2")
    def fromCanonicalXPath(s: String)(scope: Scope): Entry = {
      require(scope.isInvertible, s"Scope '${scope}' is not invertible")

      // We use the fact that "/", "[" and "]" are never part of qualified names!

      require(s.startsWith("/"), "The canonical XPath for the 'entry' must start with a slash")
      val remainder = s.drop(1)
      require(remainder.size > 3, "The canonical XPath for the 'entry' must contain at least 4 characters")
      val (qnameString, positionString) = remainder span { c => c != '[' }
      require(positionString.size >= 3, "The canonical XPath for the 'entry' must have a position of at least 3 characters, such as [1]")
      require(positionString.startsWith("["), "The canonical XPath for the 'entry' must have a position starting with '[', such as [1]")
      require(positionString.endsWith("]"), "The canonical XPath for the 'entry' must have a position ending with ']', such as [1]")

      val qname = QName.parse(qnameString)
      val elementName = scope.resolveQNameOption(qname).getOrElse(sys.error(s"Could not resolve QName '${qname}'"))
      val position = positionString.drop(1).dropRight(1).toInt

      Entry(elementName, position - 1)
    }
  }
}
