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

/**
 * Yaidom XML literals, implemented with Scala 2.10 String Interpolation.
 *
 * Example usage:
 * {{{
 * def transformBook(book: Elem): Elem = {
 *   val title = (book \ (_.localName == "Title")).map(_.text).mkString
 *   val isbn = (book \@ EName("ISBN")).getOrElse("")
 *
 *   xmlElem"""&lt;book title=${ title } isbn=${ isbn }&gt;
 *   &lt;/book&gt;"""
 * }
 *
 * xmlDoc"""&lt;bookstore bookCount=${ books.size.toString }&gt;
 * ${
 *   for (book <- bookstore \ (_.localName == "Book"))
 *   yield transformBook(book)
 * }
 * &lt;/bookstore&gt;"""
 * }}}
 *
 * '''The current implementation is very primitive and unforgiving.''' The literal is parsed at runtime by an XML parser
 * instead of at compile-time (using a macro). This also means that the example above is inefficient, because the XML parser
 * kicks in for each book (and once for the bookstore)!
 *
 * Currently arguments are only allowed as attribute values and as element content (only surrounded by whitespace).
 * Attribute values must be strings, and element content must be strings, nodes or node sequences.
 *
 * This package depends on the [[eu.cdevreeze.yaidom.parse]] package.
 *
 * @author Chris de Vreeze
 */
package object literal {

  implicit class XmlLiteralHelper(val sc: StringContext) {

    private val helper = new XmlLiterals.XmlLiteralHelper(sc)

    /**
     * Creates a `Document` using string interpolation
     */
    def xmlDoc(args: Any*): Document = {
      helper.xmlDoc(args: _*)
    }

    /**
     * Creates an `Elem` using string interpolation
     */
    def xmlElem(args: Any*): Elem = {
      helper.xmlDoc(args: _*).documentElement
    }
  }
}
