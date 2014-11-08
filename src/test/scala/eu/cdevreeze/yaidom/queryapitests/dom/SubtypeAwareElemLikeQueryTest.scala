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

import scala.collection.immutable

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.convert.DomConversions
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
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

  final type E = SubtypeAwareElemLikeQueryTest.DomBridgeElem

  protected val wrappedDocumentContent: E = {
    val dbf = DocumentBuilderFactory.newInstance()
    val db = dbf.newDocumentBuilder()
    val docUri = classOf[AbstractSubtypeAwareElemLikeQueryTest].getResource("content.xml").toURI
    val doc = db.parse(new File(docUri))

    new SubtypeAwareElemLikeQueryTest.DomBridgeElem(DomDocument(doc).documentElement)
  }
}

object SubtypeAwareElemLikeQueryTest {

  final class DomBridgeElem(val backingElem: eu.cdevreeze.yaidom.dom.DomElem) extends AbstractSubtypeAwareElemLikeQueryTest.BridgeElem {

    type BackingElem = eu.cdevreeze.yaidom.dom.DomElem

    type SelfType = DomBridgeElem

    def findAllChildElems: immutable.IndexedSeq[SelfType] =
      backingElem.findAllChildElems.map(e => new DomBridgeElem(e))

    def resolvedName: EName = backingElem.resolvedName

    def resolvedAttributes: immutable.Iterable[(EName, String)] = backingElem.resolvedAttributes

    def qname: QName = backingElem.qname

    def attributes: immutable.Iterable[(QName, String)] = backingElem.attributes

    def scope: Scope = backingElem.scope

    def text: String = backingElem.text

    def findChildElemByPathEntry(entry: Path.Entry): Option[SelfType] =
      backingElem.findChildElemByPathEntry(entry).map(e => new DomBridgeElem(e))

    def ancestryOrSelfENames: immutable.IndexedSeq[EName] =
      backingElem.ancestorsOrSelf.reverse.map(_.resolvedName)

    def toElem: eu.cdevreeze.yaidom.simple.Elem = {
      DomConversions.convertToElem(backingElem.wrappedNode, Scope.Empty)
    }

    override def equals(other: Any): Boolean = other match {
      case e: DomBridgeElem => backingElem.wrappedNode == e.backingElem.wrappedNode
      case _ => false
    }

    override def hashCode: Int = backingElem.wrappedNode.hashCode
  }
}
