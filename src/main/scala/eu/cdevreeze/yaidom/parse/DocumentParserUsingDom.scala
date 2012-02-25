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
package parse

import java.{ io => jio }
import javax.xml.parsers.{ DocumentBuilderFactory, DocumentBuilder }
import org.w3c.dom.Element
import jinterop.DomConversions._

/**
 * DOM-based Document parser
 *
 * Typical non-trivial creation is as follows, assuming class <code>MyEntityHandler</code>, which extends <code>EntityHandler</code>,
 * and class <code>MyErrorHandler</code>, which extends <code>ErrorHandler</code>:
 * {{{
 * val dbf = DocumentBuilderFactory.newInstance
 *
 * def createDocumentBuilder(documentBuilderFactory: DocumentBuilderFactory): DocumentBuilder = {
 *   val db = documentBuilderFactory.newDocumentBuilder()
 *   db.setEntityResolver(new MyEntityResolver)
 *   db.setErrorHandler(new MyErrorHandler)
 *   db
 * }
 *
 * val domParser = new DocumentParserUsingDom(dbf, createDocumentBuilder _)
 * }}}
 *
 * A custom <code>EntityHandler</code> could be used to retrieve DTDs locally, or even to suppress DTD resolution.
 * The latter can be coded as follows (see http://stuartsierra.com/2008/05/08/stop-your-java-sax-parser-from-downloading-dtds):
 * {{{
 * class MyEntityHandler extends EntityHandler {
 *   override def resolveEntity(publicId: String, systemId: String): InputSource = {
 *     new InputSource(new java.io.StringReader(""))
 *   }
 * }
 * }}}
 */
final class DocumentParserUsingDom(
  val documentBuilderFactory: DocumentBuilderFactory,
  val documentBuilderCreator: DocumentBuilderFactory => DocumentBuilder) extends DocumentParser {

  def this(dbf: DocumentBuilderFactory) = this(
    documentBuilderFactory = dbf,
    documentBuilderCreator = { dbf => dbf.newDocumentBuilder() })

  /** Parses the input stream into a yaidom Document. Closes the input stream afterwards. */
  def parse(inputStream: jio.InputStream): Document = {
    try {
      val db: DocumentBuilder = documentBuilderCreator(documentBuilderFactory)
      val domDoc: org.w3c.dom.Document = db.parse(inputStream)

      convertToDocument(domDoc)
    } finally {
      if (inputStream ne null) inputStream.close()
    }
  }
}

object DocumentParserUsingDom {

  /** Returns <code>newInstance(DocumentBuilderFactory.newInstance)</code> */
  def newInstance(): DocumentParserUsingDom = {
    val dbf = DocumentBuilderFactory.newInstance
    newInstance(dbf)
  }

  /** Returns a new instance, using the given <code>DocumentBuilderFactory</code>, without any further configuration */
  def newInstance(dbf: DocumentBuilderFactory): DocumentParserUsingDom = {
    new DocumentParserUsingDom(dbf)
  }
}
