/*
 * Copyright 2011-2014 Chris de Vreeze
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
import org.apache.commons.lang3.StringEscapeUtils

/**
 * Pretty printing utility, used in Node (and indirectly NodeBuilder) (sub)classes to print the tree representation.
 * The utility is centered around operations on groups of lines, such as shifting.
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
   * Line, consisting of an indent, followed by 0 or more prefixes, followed by the initial content of the line, followed
   * by 0 or more suffixes (which are stored in reverse order).
   *
   * This class is designed to make `LineSeq` operations such as `append` and `prepend` efficient, without creating any
   * unnecessary string literals.
   */
  final class Line(val indent: Int, val initialLine: String, val prefixes: List[String], val suffixesReversed: List[String]) {
    require(initialLine ne null)
    require(initialLine.indexOf('\n') < 0)
    require(prefixes forall (s => s.indexOf('\n') < 0))
    require(suffixesReversed forall (s => s.indexOf('\n') < 0))

    def this(indent: Int, line: String) = this(indent, line, Nil, Nil)

    def this(line: String) = this(0, line)

    /** Functionally adds an indent */
    def plusIndent(addedIndent: Int): Line = {
      if (addedIndent == 0) this else new Line(addedIndent + indent, initialLine, prefixes, suffixesReversed)
    }

    /** Functionally appends a trailing string (which must contain no newline) to this line */
    def append(s: String): Line = {
      if (s.isEmpty) this else new Line(indent, initialLine, prefixes, (s :: suffixesReversed))
    }

    /** Functionally prepends a string (which must contain no newline) to this line */
    def prepend(s: String): Line = {
      if (s.isEmpty) this else new Line(indent, initialLine, s :: prefixes, suffixesReversed)
    }

    /**
     * Adds the string representation of this line to the given StringBuilder, without creating any new string literals.
     */
    def addToStringBuilder(sb: StringBuilder): Unit = {
      for (i <- 0 until indent) sb.append(' ')

      prefixes foreach { s => sb.append(s) }

      sb.append(initialLine)

      suffixesReversed.reverse foreach { s => sb.append(s) }
    }
  }

  /**
   * Utility method wrapping a string in a Java string literal.
   *
   * The implementation uses the Apache Commons Lang method StringEscapeUtils.escapeJava.
   * This in turn uses the java.util.regex.Pattern class. Alas, for large strings we may get a stack overflow error.
   * See the bug report at http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6337993.
   * There is not much we can do except for increasing the stack size, with the -Xss JVM option. For example: -Xss20M.
   */
  final def toStringLiteral(s: String): String = {
    val lines = s.linesWithSeparators

    val result = new StringBuilder
    result.append('"')

    for (line <- lines) {
      appendEscapedJava(result, line)
    }

    result.append('"')
    result.toString
  }

  private val commonWhitespace = Set('\r', '\n', ' ', '\t')

  private def appendEscapedJava(sb: StringBuilder, s: String): Unit = {
    if (s forall (c => java.lang.Character.isLetterOrDigit(c) || commonWhitespace.contains(c))) appendNaivelyEscapedJava(sb, s)
    else sb.append(StringEscapeUtils.escapeJava(s))
  }

  private def appendNaivelyEscapedJava(sb: StringBuilder, s: String): Unit = {
    // Expecting only letters, digits or "common" whitespace
    for (c <- s) {
      if (c == '\r') sb ++= "\\r"
      else if (c == '\n') sb ++= "\\n"
      else if (c == '\t') sb ++= "\\t"
      else sb += c
    }
  }

  /**
   * Returns the parameter String as String literals with the concatenation operator ("+") in between, just like it
   * would occur in Java code. The input String is first split into lines, and then each line is turned into a String literal
   * (followed by "+", except for the last line).
   */
  final def toConcatenatedStringLiterals(s: String): LineSeq = {
    val lines = s.linesWithSeparators.toIndexedSeq

    val result = mutable.ArrayBuffer[Line]()

    if (lines.isEmpty) LineSeq() else {
      val linesButLast = lines.dropRight(1)
      val lastLine = lines.last

      val sb = new StringBuilder

      for (line <- linesButLast) {
        sb.clear()
        sb.append("\"")
        appendEscapedJava(sb, line)
        sb.append("\" +")
        result += new Line(sb.toString)
      }

      sb.clear()
      sb.append("\"")
      appendEscapedJava(sb, lastLine)
      sb.append("\"")
      result += new Line(sb.toString)
    }

    new LineSeq(result.toIndexedSeq)
  }

  /** Collection of lines, on which operations such as `shift` can be performed */
  final class LineSeq(val lines: immutable.IndexedSeq[Line]) {

    /** Shifts each of the lines `spaces` spaces to the right */
    def shift(spaces: Int): LineSeq = {
      require(spaces >= 0, "spaces must be >= 0")

      val result = lines map { line => line.plusIndent(spaces) }
      new LineSeq(result)
    }

    /**
     * Appends the given String (which is typically a separator) to the last line, if any.
     * The parameter String must not contain any newlines.
     */
    def append(s: String): LineSeq = {
      require(s.indexOf('\n') < 0, "The string to append must not have any newlines")

      if (lines.isEmpty) this else {
        val result = lines.dropRight(1) :+ (lines.last.append(s))
        new LineSeq(result)
      }
    }

    /**
     * Prepends the given String to the first line, if any, and indenting the other lines with the size of the parameter String.
     * The parameter String must not contain any newlines.
     */
    def prepend(s: String): LineSeq = {
      require(s.indexOf('\n') < 0, "The string to prepend must not have any newlines")

      if (lines.isEmpty) this else {
        val indent = s.length

        val firstLine = lines(0).prepend(s)
        val linesButFirstOne = lines.drop(1) map { line => line.plusIndent(indent) }
        new LineSeq(firstLine +: linesButFirstOne)
      }
    }

    /** Returns the LineSeq consisting of these lines followed by the lines of `otherLineSeq` */
    def ++(otherLineSeq: LineSeq): LineSeq = new LineSeq(this.lines ++ otherLineSeq.lines)

    /**
     * Adds the string representation of this line to the given StringBuilder, without creating any new string literals.
     *
     * That is, equivalent to appending `lines.map(_.toString).mkString(NewLine)`
     */
    def addToStringBuilder(sb: StringBuilder): Unit = {
      if (!lines.isEmpty) {
        lines.head.addToStringBuilder(sb)

        for (line <- lines.drop(1)) {
          sb ++= NewLine
          line.addToStringBuilder(sb)
        }
      }
    }
  }

  object LineSeq {

    def apply(lines: Line*): LineSeq = new LineSeq(Vector(lines: _*))

    def apply(s: String): LineSeq = {
      val lines = s.lines.toIndexedSeq map { (ln: String) => new Line(ln) }
      new LineSeq(lines)
    }
  }

  /** Collection of LineSeq instances, on which operations such as `mkLineSeq` can be performed */
  final class LineSeqSeq(val groups: immutable.IndexedSeq[LineSeq]) {

    /** Flattens this LineSeqSeq into a LineSeq */
    def mkLineSeq: LineSeq = {
      val result = groups flatMap { grp => grp.lines }
      new LineSeq(result)
    }

    /** Flattens this LineSeqSeq into a LineSeq, but first appends the separator to each non-last group */
    def mkLineSeq(separator: String): LineSeq = {
      if (groups.isEmpty) LineSeq() else {
        val lines = mutable.ArrayBuffer[Line]()

        val nonLastGroups = groups.dropRight(1)
        val lastGroup = groups.last

        for (grp <- nonLastGroups) {
          // Same as: lines ++= (grp.append(separator).lines)

          if (!grp.lines.isEmpty) {
            lines ++= grp.lines.dropRight(1)
            lines += (grp.lines.last.append(separator))
          }
        }
        lines ++= lastGroup.lines

        new LineSeq(lines.toIndexedSeq)
      }
    }
  }

  object LineSeqSeq {

    def apply(groups: LineSeq*): LineSeqSeq = new LineSeqSeq(Vector(groups: _*))
  }
}
