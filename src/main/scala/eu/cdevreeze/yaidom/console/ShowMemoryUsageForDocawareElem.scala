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
import scala.util.Try
import eu.cdevreeze.yaidom._
import eu.cdevreeze.yaidom.parse._

/**
 * ShowMemoryUsage "script" using docaware.Elem instances.
 */
private[yaidom] final class ShowMemoryUsageForDocawareElem(val rootDir: File) extends ShowMemoryUsage[docaware.Elem] {

  def parseXmlFiles(files: Vector[File]): Vector[Try[docaware.Elem]] = {
    val docParser = DocumentParserUsingSax.newInstance
    files map { f => Try(docParser.parse(f)).map(_.documentElement).map(e => docaware.Elem(f.toURI, e)) }
  }

  def createCommonRootParent(rootElems: Vector[docaware.Elem]): docaware.Elem = {
    val result = Node.elem(qname = QName("root"), scope = Scope.Empty, children = rootElems.map(_.elem))
    docaware.Elem(new URI("file:///newRoot"), result)
  }
}

private[yaidom] object ShowMemoryUsageForDocawareElem {

  def main(args: Array[String]): Unit = {
    require(args.size == 1, "Usage: ShowMemoryUsageForDocawareElem <root dir>")

    val rootDir = new File(args(0))
    val script = new ShowMemoryUsageForDocawareElem(rootDir)

    script.run()
  }
}
