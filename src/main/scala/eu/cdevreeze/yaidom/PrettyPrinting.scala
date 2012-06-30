package eu.cdevreeze.yaidom

import scala.collection.immutable

/**
 * Pretty printing utility, used in Node and NodeBuilder classes to print the tree representation.
 * The utility is centered around operations on groups of lines, such as shifting.
 */
object PrettyPrinting {

  /** Utility method wrapping a string in a (possibly multi-line) Scala string literal */
  final def toStringLiteral(s: String): String = {
    require(!s.contains("\"\"\""),
      "Sorry, the string contains 3 double quotes in succession. This is not supported. Start of the string: '%s'".format(s.take(30)))

    "\"\"\"" + s + "\"\"\""
  }

  /** Collection of lines, on which operations such as `shift` can be performed */
  final class LineSeq(val lines: immutable.IndexedSeq[String]) {

    /** Shifts each of the lines `spaces` spaces to the right */
    def shift(spaces: Int): LineSeq = {
      require(spaces >= 0, "spaces must be >= 0")

      val result = lines map { line => (" " * spaces) + line }
      new LineSeq(result)
    }

    /** Appends the separator to the last line, if any */
    def appendSeparator(separator: String): LineSeq = {
      val result =
        if (lines.isEmpty) lines else {
          lines.dropRight(1) ++ Vector(lines.last + separator)
        }
      new LineSeq(result)
    }

    /** Returns the LineSeq consisting of these lines followed by the lines of `otherLineSeq` */
    def ++(otherLineSeq: LineSeq): LineSeq = new LineSeq(this.lines ++ otherLineSeq.lines)

    /** Returns the String representation, concatenating all lines, and separating them by newlines */
    def mkString: String = lines.mkString("%n".format())
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
        val nonLastGroups = groups.dropRight(1)
        val lastGroup = groups.last

        val editedNonLastGroups = nonLastGroups map { grp => grp.appendSeparator(separator) }
        val resultGroups = new LineSeqSeq(editedNonLastGroups ++ Vector(lastGroup))
        resultGroups.mkLineSeq
      }
    }
  }

  object LineSeqSeq {

    def apply(groups: LineSeq*): LineSeqSeq = new LineSeqSeq(Vector(groups: _*))
  }
}
