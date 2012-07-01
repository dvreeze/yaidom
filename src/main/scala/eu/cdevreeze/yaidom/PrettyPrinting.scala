package eu.cdevreeze.yaidom

import scala.collection.{ immutable, mutable }
import org.apache.commons.lang3.StringEscapeUtils

/**
 * Pretty printing utility, used in Node and NodeBuilder classes to print the tree representation.
 * The utility is centered around operations on groups of lines, such as shifting.
 *
 * This API is safe to use, because of the use of "immutability everywhere". On the down-side, this very likely negatively
 * affects performance. Even immutable Strings are concatenated on a large scale when using this API intensively on large inputs.
 * On the other hand, without any profiling it is very hard to make any conclusions about performance of this pretty-printing utility.
 */
object PrettyPrinting {

  private val NewLine = "%n".format()

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
    result.append("\"")

    for (line <- lines) {
      result.append(StringEscapeUtils.escapeJava(line))
    }

    result.append("\"")
    result.toString
  }

  /** Collection of lines, on which operations such as `shift` can be performed */
  final class LineSeq(val lines: immutable.IndexedSeq[String]) {

    /** Shifts each of the lines `spaces` spaces to the right */
    def shift(spaces: Int): LineSeq = {
      require(spaces >= 0, "spaces must be >= 0")

      val indent = (" " * spaces)

      val result = lines map { line => indent + line }
      new LineSeq(result)
    }

    /** Appends the separator to the last line, if any */
    def appendSeparator(separator: String): LineSeq = {
      if (lines.isEmpty) this else {
        val result = lines.dropRight(1) :+ (lines.last + separator)
        new LineSeq(result)
      }
    }

    /** Returns the LineSeq consisting of these lines followed by the lines of `otherLineSeq` */
    def ++(otherLineSeq: LineSeq): LineSeq = new LineSeq(this.lines ++ otherLineSeq.lines)

    /** Returns the String representation, concatenating all lines, and separating them by newlines */
    def mkString: String = lines.mkString(NewLine)
  }

  object LineSeq {

    def apply(lines: String*): LineSeq = new LineSeq(Vector(lines: _*))
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
        val lines = mutable.ArrayBuffer[String]()

        val nonLastGroups = groups.dropRight(1)
        val lastGroup = groups.last

        for (grp <- nonLastGroups) {
          // Same as: lines ++= (grp.appendSeparator(separator).lines)

          if (!grp.lines.isEmpty) {
            lines ++= grp.lines.dropRight(1)
            lines += (grp.lines.last + separator)
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
