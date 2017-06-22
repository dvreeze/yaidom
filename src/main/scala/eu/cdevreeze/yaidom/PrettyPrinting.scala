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

package eu.cdevreeze.yaidom

import scala.collection.{ immutable, mutable }

/**
 * Pretty printing utility, used in Node (and indirectly NodeBuilder) (sub)classes to print the tree representation.
 *
 * This API is safe to use, because of the use of "immutability everywhere". On the down-side, this very likely negatively
 * affects performance. On the other hand, the design is such that repeated nested indentation (shifting) does not cause
 * many unnecessary string concatenations.
 *
 * @author Chris de Vreeze
 */
private[yaidom] object PrettyPrinting {

  private val NewLine = "%n".format()

  /**
   * Line, consisting of an indent, followed by the "real" (non-empty) content of the line as a sequence of strings.
   *
   * This class is designed to make `LineSeq` operations such as `append` and `prepend` efficient, without creating any
   * unnecessary string literals. It is also designed to possibly contain multiple successive non-concatenated line parts,
   * in order to prevent unnecessary string concatenation.
   *
   * The line may have newlines, but if the line contains any newlines, there will be no indentation at these line breaks.
   * Thus XML attributes and their values can be modeled as part of one Line object, even if they contain line breaks.
   *
   * Appending and prepending, on the other hand, may not introduce any line breaks!
   */
  final class Line(val indent: Int, val lineParts: immutable.IndexedSeq[String]) {
    require(lineParts ne null)
    require(lineParts.nonEmpty, s"Empty line content (ignoring prefixes and suffixes) not allowed")
    require(lineParts.head.nonEmpty, s"Empty line content (ignoring prefixes and suffixes) not allowed")

    def this(lineParts: immutable.IndexedSeq[String]) = this(0, lineParts)

    def this(indent: Int, line: String) = this(indent, immutable.IndexedSeq(line))

    def this(line: String) = this(0, line)

    /** Functionally adds an indent */
    def plusIndent(addedIndent: Int): Line = {
      if (addedIndent == 0) this else new Line(addedIndent + indent, lineParts)
    }

    /** Functionally appends a trailing string (which must contain no newline) to this line */
    def append(s: String): Line = {
      require(s.indexOf('\n') < 0, "The string to append must not have any newlines")

      if (s.isEmpty) this else new Line(indent, lineParts :+ s)
    }

    /** Functionally prepends a string (which must contain no newline) to this line */
    def prepend(s: String): Line = {
      require(s.indexOf('\n') < 0, "The string to prepend must not have any newlines")

      if (s.isEmpty) this else new Line(indent, s +: lineParts)
    }

    /**
     * Adds the string representation of this line to the given StringBuilder, without creating any new string literals.
     */
    def addToStringBuilder(sb: StringBuilder): Unit = {
      for (i <- 0 until indent) sb.append(' ')

      lineParts foreach { s => sb.append(s) }
    }
  }

  object Line {

    def apply(lineParts: String*): Line = new Line(Vector(lineParts: _*))

    def from(lineParts: immutable.IndexedSeq[String]): Line = new Line(lineParts)

    def fromIndexAndPrefixAndPartsAndSuffix(
      indent: Int,
      prefix: String,
      otherLineParts: immutable.IndexedSeq[String],
      suffix: String): Line = {

      new Line(indent, (prefix +: otherLineParts :+ suffix))
    }
  }

  /** Operations on collections of lines */
  object LineSeq {

    def apply(lines: Line*): immutable.IndexedSeq[Line] = lines.toIndexedSeq

    def singletonOrEmptyLineSeq(parts: immutable.IndexedSeq[String]): immutable.IndexedSeq[Line] = {
      if (parts.forall(_.isEmpty)) immutable.IndexedSeq() else immutable.IndexedSeq(new Line(parts))
    }

    /**
     * Adds the string representation of the line collection to the given StringBuilder, without creating any new string literals.
     *
     * That is, equivalent to appending `lines.map(_.toString).mkString(NewLine)`
     */
    def addToStringBuilder(lines: immutable.IndexedSeq[Line], sb: StringBuilder): Unit = {
      if (lines.nonEmpty) {
        lines.head.addToStringBuilder(sb)

        for (line <- lines.drop(1)) {
          sb ++= NewLine
          line.addToStringBuilder(sb)
        }
      }
    }
  }

  /** Collection of Line sequence instances, on which operations such as `mkLineSeq` can be performed */
  final class LineSeqSeq(val groups: immutable.IndexedSeq[immutable.IndexedSeq[Line]]) {

    /** Flattens this LineSeqSeq into a Line sequence */
    def mkLineSeq: immutable.IndexedSeq[Line] = {
      groups.flatten
    }

    /** Flattens this LineSeqSeq into a Line sequence, but first appends the separator to each non-last group */
    def mkLineSeq(separator: String): immutable.IndexedSeq[Line] = {
      if (groups.isEmpty) immutable.IndexedSeq() else {
        val lines = mutable.ArrayBuffer[Line]()

        val nonLastGroups = groups.dropRight(1)
        val lastGroup = groups.last

        for (grp <- nonLastGroups) {
          // Same as: lines ++= (grp.append(separator).lines)

          if (grp.nonEmpty) {
            lines ++= grp.dropRight(1)
            lines += (grp.last.append(separator))
          }
        }
        lines ++= lastGroup

        lines.toIndexedSeq
      }
    }
  }

  object LineSeqSeq {

    def apply(groups: immutable.IndexedSeq[Line]*): LineSeqSeq = new LineSeqSeq(groups.toIndexedSeq)
  }

  /**
   * Utility method wrapping a string in a Java or multiline string literal, as a sequence of Strings.
   * The result as String can be obtained by calling the no-argument `mkString` method on the result.
   *
   * This implementation does no "Java escaping". It uses multiline string literals, assuming that no
   * three subsequent double quotes occur, but uses Java string literals instead, if all characters are
   * letters, digits or some common punctuation characters.
   *
   * If three subsequent double quotes occur, then the resulting multiline string will be broken.
   * Within the context of pretty printing, where the printed output will not be parsed, this is less of a problem.
   *
   * An earlier implementation used the Apache Commons Lang method StringEscapeUtils.escapeJava.
   * That method in turn uses the java.util.regex.Pattern class. Alas, for large strings this could result in
   * a stack overflow error. See the bug report at http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6337993.
   *
   * Currently, however, Apache Commons Lang is not used in yaidom. Hence, this problem does not occur. Moreover,
   * it reduces the dependencies of yaidom on other libraries.
   */
  final def toStringLiteralAsSeq(s: String): immutable.IndexedSeq[String] = {
    if (useJavaStringLiteral(s)) toJavaStringLiteralAsSeq(s)
    else toMultilineStringLiteralAsSeq(s)
  }

  final def toMultilineStringLiteralAsSeq(s: String): immutable.IndexedSeq[String] = {
    immutable.IndexedSeq("\"\"\"", s, "\"\"\"")
  }

  private def useJavaStringLiteral(s: String): Boolean = {
    // Quite defensive, more so than needed
    // Typically fast enough, because immediately returns false on encountering whitespace (or newline)
    s forall { c =>
      java.lang.Character.isLetterOrDigit(c) ||
        (c == ':') || (c == ';') || (c == '.') || (c == ',') ||
        (c == '-') || (c == '_') || (c == '/') || (c == '\\')
    }
  }

  private def toJavaStringLiteralAsSeq(s: String): immutable.IndexedSeq[String] = {
    immutable.IndexedSeq("\"", s, "\"")
  }
}
