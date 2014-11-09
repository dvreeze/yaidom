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
import eu.cdevreeze.yaidom.queryapitests.AbstractSubtypeAwareElemLikeQueryTest.IndexedBridgeElem
import javax.xml.parsers.DocumentBuilderFactory
import SubtypeAwareElemLikeQueryTest.BridgeElemTakingDomElem

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

object SubtypeAwareElemLikeQueryTest {

  /**
   * Overridable bridge element taking a `dom.DomElem`. Having this bridge implement IndexedBridgeElement
   * is a poor match, performance-wise.
   */
  class BridgeElemTakingDomElem(val backingElem: eu.cdevreeze.yaidom.dom.DomElem) extends IndexedBridgeElem {

    final type BackingElem = eu.cdevreeze.yaidom.dom.DomElem

    final type SelfType = BridgeElemTakingDomElem

    final type UnwrappedBackingElem = eu.cdevreeze.yaidom.dom.DomElem

    final def findAllChildElems: immutable.IndexedSeq[SelfType] =
      backingElem.findAllChildElems.map(e => new BridgeElemTakingDomElem(e))

    final def resolvedName: EName = backingElem.resolvedName

    final def resolvedAttributes: immutable.Iterable[(EName, String)] = backingElem.resolvedAttributes

    final def qname: QName = backingElem.qname

    final def attributes: immutable.Iterable[(QName, String)] = backingElem.attributes

    final def scope: Scope = backingElem.scope

    final def text: String = backingElem.text

    final def findChildElemByPathEntry(entry: Path.Entry): Option[SelfType] =
      backingElem.findChildElemByPathEntry(entry).map(e => new BridgeElemTakingDomElem(e))

    final def toElem: eu.cdevreeze.yaidom.simple.Elem = {
      DomConversions.convertToElem(backingElem.wrappedNode, Scope.Empty)
    }

    final def rootElem: UnwrappedBackingElem = {
      backingElem.ancestorsOrSelf.last
    }

    /**
     * Extremely inefficient function to get the relative Path.
     */
    final def path: Path = {
      val entriesReversed =
        backingElem.ancestorsOrSelf.dropRight(1) map { elem =>
          val cnt =
            findPreviousSiblingElements(elem.wrappedNode).filter(e => DomConversions.toQName(e).localPart == elem.localName).
              filter(e => eu.cdevreeze.yaidom.dom.DomElem(e).resolvedName == elem.resolvedName).size
          Path.Entry(elem.resolvedName, cnt)
        }
      Path(entriesReversed.reverse)
    }

    final def unwrappedBackingElem: UnwrappedBackingElem = backingElem

    final override def equals(other: Any): Boolean = other match {
      case e: BridgeElemTakingDomElem => backingElem.wrappedNode == e.backingElem.wrappedNode
      case _ => false
    }

    final override def hashCode: Int = backingElem.wrappedNode.hashCode

    private def findPreviousSiblingElements(elem: org.w3c.dom.Element): List[org.w3c.dom.Element] = {
      findPreviousSiblings(elem) collect { case e: org.w3c.dom.Element => e }
    }

    private def findPreviousSiblings(n: org.w3c.dom.Node): List[org.w3c.dom.Node] = {
      val prev = n.getPreviousSibling

      if (prev eq null) Nil else {
        // Recursive call
        prev :: findPreviousSiblings(prev)
      }
    }
  }
}
