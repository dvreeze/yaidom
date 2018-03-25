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

package eu.cdevreeze.yaidom.xpath.saxon

import scala.collection.JavaConverters.asScalaBufferConverter
import scala.collection.JavaConverters.seqAsJavaListConverter

import eu.cdevreeze.yaidom.saxon.SaxonNode
import eu.cdevreeze.yaidom.core.EName
import javax.xml.xpath.XPathFunction
import net.sf.saxon.om.NodeInfo

/**
 * Function to find all xbrli:context elements in an XBRL instance. Used in XPathTest to show use of custom functions.
 * Should work without Saxon-EE.
 *
 * @author Chris de Vreeze
 */
class FindAllXbrliContexts extends XPathFunction {

  def evaluate(args: java.util.List[_]): AnyRef = {
    require(args.size == 1, s"Expected 1 argument but found ${args.size} arguments instead")

    val argSeq = args.asScala.toIndexedSeq
    val rootElemAsNodeInfo = argSeq.head.asInstanceOf[NodeInfo]
    val xbrlInstanceElem = SaxonNode.wrapElement(rootElemAsNodeInfo)

    val xbrlContexts = xbrlInstanceElem.filterElems(_.resolvedName == XbrliContextEName)

    val result = new java.util.ArrayList(xbrlContexts.map(_.wrappedNode).asJava)
    result
  }

  private val XbrliNamespace = "http://www.xbrl.org/2003/instance"
  private val XbrliContextEName = EName(XbrliNamespace, "context")
}
