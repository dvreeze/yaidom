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

import eu.cdevreeze.yaidom.saxon.SaxonElem
import eu.cdevreeze.yaidom.saxon.SaxonNode
import javax.xml.xpath.XPathFunction
import net.sf.saxon.om.NodeInfo

/**
 * Function to transform a SaxonElem. Used in XPathTest to show use of custom higher-order functions.
 * Should work without Saxon-EE.
 *
 * @author Chris de Vreeze
 */
class TransformElem extends XPathFunction {

  def evaluate(args: java.util.List[_]): AnyRef = {
    require(args.size == 2, s"Expected 2 arguments but found ${args.size} arguments instead")

    val argSeq = args.asScala.toIndexedSeq
    val nodeInfo = argSeq.head.asInstanceOf[NodeInfo]
    val saxonElem = SaxonNode.wrapElement(nodeInfo)

    val f: SaxonElem => SaxonElem = argSeq.tail.head.asInstanceOf[SaxonElem => SaxonElem]

    val resultElem = f(saxonElem)

    val result: NodeInfo = resultElem.wrappedNode
    result
  }
}
