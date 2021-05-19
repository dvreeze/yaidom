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

package eu.cdevreeze.yaidom.integrationtest

import eu.cdevreeze.yaidom.parse

/**
 * See AbstractOtherNamespaceTest.
 *
 * Acknowledgments: This test uses the examples in http://www.datypic.com/books/defxmlschema/chapter03.html, that are also used
 * in the excellent book Definitive XML Schema.
 *
 * @author Chris de Vreeze
 */

class OtherNamespaceTestUsingStaxParser extends AbstractOtherNamespaceTest {

  val documentParser: parse.DocumentParser = parse.DocumentParserUsingStax.newInstance()

  // The StAX-based parser in the Oracle JDK does not correctly handle XML 1.1, so we use a different one.
  // Having Woodstox on the test classpath, the XMLInputFactory must be com.ctc.wstx.stax.WstxInputFactory.
  // On an IBM JDK, we do not need Woodstox for StAX support for XML 1.1 (nice to know when using WebSphere).
  val documentParserForXml11: parse.DocumentParser = parse.DocumentParserUsingStax.newInstance()
}
