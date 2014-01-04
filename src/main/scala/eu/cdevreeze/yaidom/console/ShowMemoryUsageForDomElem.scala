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
package console

import java.io._
import java.net.URI
import javax.xml.parsers._
import scala.util.Try
import eu.cdevreeze.yaidom._

/**
 * ShowMemoryUsage "script" using DOM wrapper elements.
 */
private[yaidom] final class ShowMemoryUsageForDomElem(val rootDir: File) extends ShowMemoryUsage[dom.DomElem] {

  def parseXmlFiles(files: Vector[File]): Vector[Try[dom.DomElem]] = {
    val dbf = DocumentBuilderFactory.newInstance
    val db = dbf.newDocumentBuilder

    files map { f => Try(db.parse(f)).map(_.getDocumentElement).map(e => dom.DomElem(e)) }
  }

  def createCommonRootParent(rootElems: Vector[dom.DomElem]): dom.DomElem = {
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
}

private[yaidom] object ShowMemoryUsageForDomElem {

  def main(args: Array[String]): Unit = {
    require(args.size == 1, "Usage: ShowMemoryUsageForDomElem <root dir>")

    val rootDir = new File(args(0))
    val script = new ShowMemoryUsageForDomElem(rootDir)

    script.run()
  }
}
