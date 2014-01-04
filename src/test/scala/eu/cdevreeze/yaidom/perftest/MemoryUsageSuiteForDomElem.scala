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
package perftest

import java.io._
import java.net.URI
import javax.xml.parsers._
import scala.util.Try

/**
 * Concrete AbstractMemoryUsageSuite sub-class using DOM wrapper elements.
 *
 * See the documentation of the super-class for the advice to run this suite in isolation only!
 *
 * @author Chris de Vreeze
 */
class MemoryUsageSuiteForDomElem extends AbstractMemoryUsageSuite {

  type E = dom.DomElem

  protected def parseXmlFiles(files: Vector[File]): Vector[Try[dom.DomElem]] = {
    val dbf = DocumentBuilderFactory.newInstance
    val db = dbf.newDocumentBuilder

    files map { f => Try(db.parse(f)).map(_.getDocumentElement).map(e => dom.DomElem(e)) }
  }

  protected def createCommonRootParent(rootElems: Vector[dom.DomElem]): dom.DomElem = {
    val dbf = DocumentBuilderFactory.newInstance
    val db = dbf.newDocumentBuilder
    val d = db.newDocument

    val newRoot = d.createElement("root")

    rootElems foreach { e =>
      val importedE = d.importNode(e.wrappedNode, true)
      newRoot.appendChild(importedE)
    }
    dom.DomElem(newRoot)
  }

  protected def maxMemoryToFileLengthRatio: Int = 6
}
