/*
 * Copyright 2011-2014 Chris de Vreeze
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

package eu.cdevreeze.yaidom.queryapitests.dom

import java.io.File
import java.{ util => jutil }

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.bridge.BridgeElemTakingDomElem
import eu.cdevreeze.yaidom.dom.DomDocument
import eu.cdevreeze.yaidom.queryapitests.AbstractSubtypeAwareElemLikeQueryTest
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Query test case for an XML dialect using DOM wrapper elements.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class SubtypeAwareElemLikeQueryTest extends AbstractSubtypeAwareElemLikeQueryTest {

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.queryapitests.dom")

  final type E = BridgeElemTakingDomElem

  protected val wrappedDocumentContent: E = {
    val dbf = DocumentBuilderFactory.newInstance()
    val db = dbf.newDocumentBuilder()
    val docUri = classOf[AbstractSubtypeAwareElemLikeQueryTest].getResource("content.xml").toURI
    val doc = db.parse(new File(docUri))

    new BridgeElemTakingDomElem(DomDocument(doc).documentElement)
  }
}
