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

package eu.cdevreeze.yaidom.queryapitests.nodeinfo

import javax.xml.transform.stream.StreamSource

import scala.collection.immutable

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.convert
import eu.cdevreeze.yaidom.parse.DocumentParserUsingSax
import eu.cdevreeze.yaidom.queryapi.ElemWithPath
import eu.cdevreeze.yaidom.queryapi.HasParent
import eu.cdevreeze.yaidom.queryapi.ScopedElemLike
import eu.cdevreeze.yaidom.queryapitests.AbstractXbrlInstanceTest
import net.sf.saxon.lib.ParseOptions
import net.sf.saxon.om.AxisInfo
import net.sf.saxon.om.DocumentInfo
import net.sf.saxon.om.NodeInfo
import net.sf.saxon.`type`.Type
import net.sf.saxon.s9api.Processor

/**
 * XBRL instance test case for Saxon-backed elements.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class XbrlInstanceTest extends AbstractXbrlInstanceTest with SaxonTestSupport {

  type MyElem = NodeInfo

  object MyElemFunctionApi extends ElemFunctionApi[MyElem] with DomElem.FunctionApi {

    def path(thisElem: NodeInfo): Path = {
      val parentOpt = parentOption(thisElem)

      if (parentOpt.isEmpty) {
        Path.Empty
      } else {
        val ename = DomNode.nodeInfo2EName(thisElem)
        val idx = precedingSiblings(thisElem).count(e => DomNode.nodeInfo2EName(e) == ename)
        val entry = Path.Entry(ename, idx)

        // Recursive call
        path(parentOpt.get).append(entry)
      }
    }

    def toSimpleElem(thisElem: NodeInfo): yaidom.simple.Elem = {
      require(thisElem.getNodeKind == Type.ELEMENT)

      val qn = DomNode.nodeInfo2QName(thisElem)
      val attrs = attributes(thisElem)
      val sc = scope(thisElem)
      val childNodes = children(thisElem).flatMap(n => toSimpleNodeOption(n))

      yaidom.simple.Elem(qn, attrs, sc, childNodes)
    }

    private def precedingSiblings(thisElem: NodeInfo): immutable.IndexedSeq[NodeInfo] = {
      val it = thisElem.iterateAxis(AxisInfo.PRECEDING_SIBLING)

      val nodes = Stream.continually(it.next()).takeWhile(_ ne null).toVector
      nodes
    }

    private def toSimpleNodeOption(thisNode: NodeInfo): Option[yaidom.simple.Node] = thisNode.getNodeKind match {
      case Type.ELEMENT         => Some(toSimpleElem(thisNode))
      case Type.TEXT            => Some(toSimpleText(thisNode))
      case Type.WHITESPACE_TEXT => Some(toSimpleText(thisNode))
      case Type.COMMENT         => Some(toSimpleComment(thisNode))
      case _                    => /* Ignoring entity references etc. */ None
    }

    private def toSimpleText(thisNode: NodeInfo): yaidom.simple.Text = {
      require(thisNode.getNodeKind == Type.TEXT || thisNode.getNodeKind == Type.WHITESPACE_TEXT)

      yaidom.simple.Text(thisNode.getStringValue, false)
    }

    private def toSimpleComment(thisNode: NodeInfo): yaidom.simple.Comment = {
      require(thisNode.getNodeKind == Type.COMMENT)

      yaidom.simple.Comment(thisNode.getStringValue)
    }
  }

  final class MyElemWithApi(val elem: MyElem) extends ElemWithApi {

    type E = MyElem

    val functionApi = MyElemFunctionApi

    def withElem(newElem: MyElem): ElemWithApi.Aux[MyElem] = {
      new MyElemWithApi(newElem)
    }
  }

  protected final def elemWithApi: ElemWithApi = {
    val parseOptions = new ParseOptions

    val is = new java.io.FileInputStream(sampleXbrlInstanceFile)

    val doc: DocumentInfo =
      processor.getUnderlyingConfiguration.buildDocument(new StreamSource(is), parseOptions)

    val docElem: MyElem = (new DomDocument(doc)).documentElement.wrappedNode

    new MyElemWithApi(docElem)
  }
}
