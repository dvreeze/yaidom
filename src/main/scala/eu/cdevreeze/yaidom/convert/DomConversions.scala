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
package convert

/**
 * Conversions between [[eu.cdevreeze.yaidom.Elem]]s of this API and DOM Elements.
 *
 * Example usage for parsing an XML file into an [[eu.cdevreeze.yaidom.Elem]] using DOM:
 * {{{
 * import DomConversions._
 *
 * val dbf = DocumentBuilderFactory.newInstance
 * val db = dbf.newDocumentBuilder
 * val doc = db.parse(inputFile)
 * val root: Elem = convertToElem(doc.getDocumentElement)
 * }}}
 * Class [[eu.cdevreeze.yaidom.parse.DocumentParserUsingDom]] makes this a lot easier, though.
 *
 * A somewhat involved example for writing an [[eu.cdevreeze.yaidom.Elem]] to an XML file using DOM:
 * {{{
 * import DomConversions._
 *
 * val dbf = DocumentBuilderFactory.newInstance
 * val db = dbf.newDocumentBuilder
 * val doc = db.newDocument
 * val domElement = convertElem(root)(doc)
 *
 * val source = new DOMSource(domElement)
 * val result = new StreamResult(outputStream)
 * val tf = TransformerFactory.newInstance
 * val tr = tf.newTransformer
 * tr.transform(source, result)
 *
 * outputStream.close()
 * }}}
 * Class [[eu.cdevreeze.yaidom.print.DocumentPrinterUsingDom]] makes this a lot easier, though.
 *
 * @author Chris de Vreeze
 */
object DomConversions extends ElemToDomConverter with DomToElemConverter