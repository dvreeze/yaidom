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

import java.{ lang => jlang }

/**
 * XML string handling utility methods.
 *
 * @author Chris de Vreeze
 */
private[yaidom] object XmlStringUtils {

  /** Returns true if the name is probably a valid XML name (even if reserved or containing a colon) */
  def isProbablyValidXmlName(s: String): Boolean = {
    require(s ne null) // scalastyle:off null
    (s.length > 0) && isProbableXmlNameStart(s(0)) && {
      s.drop(1) forall { c => isProbableXmlNameChar(c) }
    }
  }

  def containsColon(s: String): Boolean = s.indexOf(":") >= 0

  /** Returns true if the name is probably a valid XML name which contains no colon. */
  def isAllowedElementLocalName(s: String): Boolean = {
    require(s ne null) // scalastyle:off null
    (s.length > 0) && !containsColon(s) && isProbablyValidXmlName(s)
  }

  /** Returns true if the name is probably a valid XML name which contains no colon. */
  def isAllowedPrefix(s: String): Boolean = {
    require(s ne null) // scalastyle:off null
    (s.length > 0) && !containsColon(s) && isProbablyValidXmlName(s)
  }

  def isAllowedOnlyInCData(c: Char): Boolean = (c == '<') || (c == '&')

  /** Escapes XML text, in particular ampersands, less-than and greater-than symbols */
  def escapeText(s: String): String = {
    require(s ne null) // scalastyle:off null

    // Taken from Anti-XML, and enhanced (there are 5 predefined entities)
    s flatMap {
      case '&'  => "&amp;"
      case '<'  => "&lt;"
      case '>'  => "&gt;"
      case '\'' => "&apos;"
      case '"'  => "&quot;"
      case c    => List(c)
    }
  }

  /**
   * Normalizes the string, removing surrounding whitespace and normalizing internal whitespace to a single space.
   * Whitespace includes #x20 (space), #x9 (tab), #xD (carriage return), #xA (line feed). If there is only whitespace,
   * the empty string is returned. Inspired by the JDOM library.
   */
  def normalizeString(s: String): String = {
    require(s ne null) // scalastyle:off null

    val separators = Array(' ', '\t', '\r', '\n')
    val words: Seq[String] = s.split(separators).toSeq filterNot { s => s.isEmpty }

    words.mkString(" ") // Returns empty string if words.isEmpty
  }

  private def isProbableXmlNameStart(c: Char): Boolean = c match {
    case '-'                             => false
    case '.'                             => false
    case c if jlang.Character.isDigit(c) => false
    case _                               => isProbableXmlNameChar(c)
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
