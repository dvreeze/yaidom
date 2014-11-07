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

package eu.cdevreeze.yaidom.queryapitests.docaware

import java.{ util => jutil }

import scala.collection.immutable

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.parse.DocumentParserUsingDom
import eu.cdevreeze.yaidom.queryapitests.AbstractSubtypeAwareElemLikeQueryTest

/**
 * Query test case for an XML dialect using docaware elements.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class SubtypeAwareElemLikeQueryTest extends AbstractSubtypeAwareElemLikeQueryTest {

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.queryapitests.docaware")

  final type E = SubtypeAwareElemLikeQueryTest.DocAwareWrappedElem

  protected val wrappedDocumentContent: E = {
    val docParser = DocumentParserUsingDom.newInstance
    val docUri = classOf[AbstractSubtypeAwareElemLikeQueryTest].getResource("content.xml").toURI
    val doc = docParser.parse(docUri)

    new SubtypeAwareElemLikeQueryTest.DocAwareWrappedElem(eu.cdevreeze.yaidom.docaware.Document(docUri, doc).documentElement)
  }
}

object SubtypeAwareElemLikeQueryTest {

  final class DocAwareWrappedElem(val nativeElem: eu.cdevreeze.yaidom.docaware.Elem) extends AbstractSubtypeAwareElemLikeQueryTest.WrappedElem {

    type NativeElem = eu.cdevreeze.yaidom.docaware.Elem

    type SelfType = DocAwareWrappedElem

    def findAllChildElems: immutable.IndexedSeq[SelfType] =
      nativeElem.findAllChildElems.map(e => new DocAwareWrappedElem(e))

    def resolvedName: EName = nativeElem.resolvedName

    def resolvedAttributes: immutable.Iterable[(EName, String)] = nativeElem.resolvedAttributes

    def qname: QName = nativeElem.qname

    def attributes: immutable.Iterable[(QName, String)] = nativeElem.attributes

    def scope: Scope = nativeElem.scope

    def text: String = nativeElem.text

    def findChildElemByPathEntry(entry: Path.Entry): Option[SelfType] =
      nativeElem.findChildElemByPathEntry(entry).map(e => new DocAwareWrappedElem(e))

    def ancestryOrSelfENames: immutable.IndexedSeq[EName] = nativeElem.ancestryOrSelfENames

    def toElem: eu.cdevreeze.yaidom.simple.Elem = nativeElem.elem
  }
}
