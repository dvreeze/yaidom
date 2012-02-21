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

/** DOM-based Document parser */
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

  /** Returns a new instance */
  def newInstance(): DocumentParserUsingDom = {
    val dbf = DocumentBuilderFactory.newInstance
    new DocumentParserUsingDom(dbf)
  }
}