package eu.cdevreeze.yaidom

import java.{ lang => jlang }

object XmlStringUtils {

  /** Returns true if the name is probably a valid XML name (even if reserved or containing a colon) */
  def isProbableXmlName(s: String): Boolean = {
    require(s ne null)
    (s.length > 0) && isProbableXmlNameStart(s(0)) && {
      s.drop(1) forall { c => isProbableXmlNameChar(c) }
    }
  }

  /** Returns true if the string starts with "xml" (case-insensitive) */
  def isReserved(s: String): Boolean = s.take(3).equalsIgnoreCase("xml")

  def containsColon(s: String): Boolean = s.indexOf(":") >= 0

  /** Returns true if the name is probably a valid XML name which is not reserved and contains no colon. */
  def isAllowedElementLocalName(s: String): Boolean = {
    require(s ne null)
    (s.length > 0) && !isReserved(s) && !containsColon(s) && isProbableXmlName(s)
  }

  /** Returns true if the name is probably a valid XML name which contains no colon. */
  def isAllowedPrefix(s: String): Boolean = {
    require(s ne null)
    (s.length > 0) && !containsColon(s) && isProbableXmlName(s)
  }

  private def isProbableXmlNameStart(c: Char): Boolean = c match {
    case '-' => false
    case '.' => false
    case c if jlang.Character.isDigit(c) => false
    case _ => isProbableXmlNameChar(c)
  }

  private def isProbableXmlNameChar(c: Char): Boolean = c match {
    case '_' => true
    case '-' => true
    case '.' => true
    case '$' => false
    case ':' => true
    case c if jlang.Character.isWhitespace(c) => false
    case c if jlang.Character.isJavaIdentifierPart(c) => true
    case _ => false
  }
}
