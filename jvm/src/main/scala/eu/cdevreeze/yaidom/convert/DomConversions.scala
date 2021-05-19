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

package eu.cdevreeze.yaidom.convert

/**
 * Conversions between yaidom nodes and DOM nodes.
 *
 * These conversions are used in implementations of yaidom XML parsers and printers. They are also useful
 * in application code. One scenario in which these conversions are useful is as follows:
 * {{{
 * val dbf = DocumentBuilderFactory.newInstance()
 * val db = dbf.newDocumentBuilder
 * val domDoc = db.parse(inputFile)
 *
 * editDomTreeInPlace(domDoc)
 *
 * val doc = DomConversions.convertToDocument(domDoc)
 *
 * useImmutableDoc(doc)
 * }}}
 *
 * @author Chris de Vreeze
 */
object DomConversions extends YaidomToDomConversions with DomToYaidomConversions
