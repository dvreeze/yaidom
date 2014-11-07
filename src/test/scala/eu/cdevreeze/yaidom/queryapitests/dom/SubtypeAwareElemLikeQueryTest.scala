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

  final type E = SubtypeAwareElemLikeQueryTest.DomWrappedElem

  protected val wrappedDocumentContent: E = {
    val dbf = DocumentBuilderFactory.newInstance()
    val db = dbf.newDocumentBuilder()
    val docUri = classOf[AbstractSubtypeAwareElemLikeQueryTest].getResource("content.xml").toURI
    val doc = db.parse(new File(docUri))

    new SubtypeAwareElemLikeQueryTest.DomWrappedElem(DomDocument(doc).documentElement)
  }
}

object SubtypeAwareElemLikeQueryTest {

  final class DomWrappedElem(val nativeElem: eu.cdevreeze.yaidom.dom.DomElem) extends AbstractSubtypeAwareElemLikeQueryTest.WrappedElem {

    type NativeElem = eu.cdevreeze.yaidom.dom.DomElem

    type SelfType = DomWrappedElem

    def findAllChildElems: immutable.IndexedSeq[SelfType] =
      nativeElem.findAllChildElems.map(e => new DomWrappedElem(e))

    def resolvedName: EName = nativeElem.resolvedName

    def resolvedAttributes: immutable.Iterable[(EName, String)] = nativeElem.resolvedAttributes

    def qname: QName = nativeElem.qname

    def attributes: immutable.Iterable[(QName, String)] = nativeElem.attributes

    def scope: Scope = nativeElem.scope

    def text: String = nativeElem.text

    def findChildElemByPathEntry(entry: Path.Entry): Option[SelfType] =
      nativeElem.findChildElemByPathEntry(entry).map(e => new DomWrappedElem(e))

    def ancestryOrSelfENames: immutable.IndexedSeq[EName] =
      nativeElem.ancestorsOrSelf.reverse.map(_.resolvedName)

    def toElem: eu.cdevreeze.yaidom.simple.Elem = {
      DomConversions.convertToElem(nativeElem.wrappedNode, Scope.Empty)
    }

    override def equals(other: Any): Boolean = other match {
      case e: DomWrappedElem => nativeElem.wrappedNode == e.nativeElem.wrappedNode
      case _ => false
    }

    override def hashCode: Int = nativeElem.wrappedNode.hashCode
  }
}
