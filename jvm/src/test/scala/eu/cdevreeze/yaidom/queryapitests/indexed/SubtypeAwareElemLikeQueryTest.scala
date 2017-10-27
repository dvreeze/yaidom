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

package eu.cdevreeze.yaidom.queryapitests.indexed

import scala.collection.immutable

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.parse.DocumentParserUsingDom
import eu.cdevreeze.yaidom.queryapitests.AbstractSubtypeAwareElemLikeQueryTest
import eu.cdevreeze.yaidom.queryapitests.AbstractSubtypeAwareElemLikeQueryTest.IndexedBridgeElem
import SubtypeAwareElemLikeQueryTest.BridgeElemTakingIndexedElem

/**
 * Query test case for an XML dialect using indexed elements.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class SubtypeAwareElemLikeQueryTest extends AbstractSubtypeAwareElemLikeQueryTest {

  final type E = BridgeElemTakingIndexedElem

  protected val wrappedDocumentContent: E = {
    val docParser = DocumentParserUsingDom.newInstance
    val docUri = classOf[AbstractSubtypeAwareElemLikeQueryTest].getResource("content.xml").toURI
    val doc = docParser.parse(docUri)

    new BridgeElemTakingIndexedElem(
      eu.cdevreeze.yaidom.indexed.Document(doc.withUriOption(Some(docUri))).documentElement)
  }
}

object SubtypeAwareElemLikeQueryTest {

  /**
   * Overridable bridge element taking a `indexed.Elem`. This is a value class instance, to prevent object creation.
   */
  class BridgeElemTakingIndexedElem(val backingElem: eu.cdevreeze.yaidom.indexed.Elem) extends AnyVal with IndexedBridgeElem {

    final type BackingElem = eu.cdevreeze.yaidom.indexed.Elem

    final type SelfType = BridgeElemTakingIndexedElem

    final type UnwrappedBackingElem = eu.cdevreeze.yaidom.simple.Elem

    final def findAllChildElems: immutable.IndexedSeq[SelfType] =
      backingElem.findAllChildElems.map(e => new BridgeElemTakingIndexedElem(e))

    final def resolvedName: EName = backingElem.resolvedName

    final def resolvedAttributes: immutable.Iterable[(EName, String)] = backingElem.resolvedAttributes

    final def qname: QName = backingElem.qname

    final def attributes: immutable.Iterable[(QName, String)] = backingElem.attributes

    final def scope: Scope = backingElem.scope

    final def text: String = backingElem.text

    final def findChildElemByPathEntry(entry: Path.Entry): Option[SelfType] =
      backingElem.findChildElemByPathEntry(entry).map(e => new BridgeElemTakingIndexedElem(e))

    final def toElem: eu.cdevreeze.yaidom.simple.Elem = backingElem.underlyingElem

    final def rootElem: UnwrappedBackingElem = backingElem.underlyingRootElem

    final def path: Path = backingElem.path

    final def unwrappedBackingElem: UnwrappedBackingElem = backingElem.underlyingElem
  }
}
