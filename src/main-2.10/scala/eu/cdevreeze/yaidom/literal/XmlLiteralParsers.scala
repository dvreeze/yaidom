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
package literal

import scala.collection.immutable
import scala.util.parsing.combinator._
import org.apache.commons.lang3.StringEscapeUtils

/**
 * Generator for parsers of restricted XML literal expressions. The results from successful parses are `NodeBuilder` instances.
 *
 * See http://burak.emir.googlepages.com/scalaxbook.docbk.html, Appendix A, for the official Scala XML literals.
 *
 * Plenty of questions are to be answered, before a mature implementation can be made. For example:
 * <ul>
 * <li>What should the XML literals be? Free to copy and paste, or more rigid and defined by a grammar like below?</li>
 * <li>Which node types to support? Comments? Entity references? Processing instructions?</li>
 * <li>Are XML literals indeed stand-alone, requiring no resolution of other files?</li>
 * <li>How to deal with whitespace?</li>
 * <li>Do we allow both double and single quote pairs for attribute values?</li>
 * <li>Where are Scala expressions allowed? Attribute values, namespace URIs and element content only?</li>
 * <li>Are CDATA sections supported?</li>
 * <li>How do we specify optional attributes? By a Scala expression of Option type?</li>
 * <li>How to use a macro to parse the XML literal at compile-time?</li>
 * </ul>
 *
 * Note: Parsing large "XML literal expressions" is very slow.
 *
 * @author Chris de Vreeze
 */
object XmlLiteralParsers extends JavaTokenParsers {

  // TODO Placeholders!

  // ElemBuilder

  // TODO Empty tags

  def element: Parser[ElemBuilder] =
    "<" ~> qname ~ pseudoAttributes ~ elementRemainder ^^ {
      case (qn ~ attrs ~ remainder) =>
        val (children, endQnOption) = remainder
        if ((endQnOption.isDefined) && (qn != endQnOption.get)) {
          return failure(s"Start and end tag must be the same, but found $qn and ${endQnOption.get}, respectively")
        }

        val namespaces: Declarations = {
          val result = attrs.toIndexedSeq collect {
            case (pref, value) if pref == "xmlns" => ("" -> value)
            case (pref, value) if pref.startsWith("xmlns:") => (pref.drop("xmlns:".length) -> value)
          }
          Declarations.from(result.toMap)
        }

        val attributes: immutable.IndexedSeq[(QName, String)] = {
          attrs.toIndexedSeq collect {
            case (pref, value) if (pref != "xmlns") && (!pref.startsWith("xmlns:")) => (QName(pref) -> value)
          }
        }

        NodeBuilder.elem(
          qname = qn,
          namespaces = namespaces,
          attributes = attributes,
          children = children.toIndexedSeq)
    }

  def elementRemainder: Parser[(Seq[NodeBuilder], Option[QName])] =
    (emptyElementRemainder | normalElementRemainder)

  def emptyElementRemainder: Parser[(Seq[NodeBuilder], Option[QName])] =
    "/>" ^^ { case _ => (Seq(), None) }

  def normalElementRemainder: Parser[(Seq[NodeBuilder], Option[QName])] =
    ">" ~> content ~ endTag ^^ {
      case (children ~ end) => (children, Some(end))
    }

  def endTag: Parser[QName] =
    "</" ~> endTagQName <~ ">"

  def qname: Parser[QName] =
    ident ~ opt(":" ~> ident) ^^ { case (x ~ y) => if (y.isEmpty) QName(x) else QName(x, y.get) }

  def endTagQName: Parser[QName] =
    ident ~ opt(":" ~> ident) ^^ { case (x ~ y) => if (y.isEmpty) QName(x) else QName(x, y.get) }

  def pseudoAttributes: Parser[Seq[(String, String)]] = rep(pseudoAttribute)

  def pseudoAttribute: Parser[(String, String)] = prefix ~ "=" ~ stringLiteral ^^ {
    case (name ~ _ ~ value) => (name, unwrapStringLiteral(value))
  }

  def prefix: Parser[String] =
    ident ~ opt(":" ~> ident) ^^ { case (x ~ y) => if (y.isEmpty) x else x + ":" + y.get }

  def content: Parser[Seq[NodeBuilder]] = rep(node)

  // TODO Other nodes (comments, PIs, entity refs)

  def node: Parser[NodeBuilder] = (element | text)

  // TODO Improve regex for text

  def text: Parser[NodeBuilder] = """[\d\s\w\p{Punct}&&[^<]]+""".r ^^ {
    case s => NodeBuilder.text(s)
  }

  // Helpers

  private def unwrapStringLiteral(literal: String): String = {
    require(literal.startsWith("\"") && literal.endsWith("\""),
      "Expected string literal, enclosed by a pair of double quotes, but found '%s'".format(literal))

    val content = literal.drop(1).dropRight(1)
    StringEscapeUtils.unescapeJava(content)
  }
}
