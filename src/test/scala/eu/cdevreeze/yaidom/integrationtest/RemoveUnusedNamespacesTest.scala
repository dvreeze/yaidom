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

package eu.cdevreeze.yaidom.integrationtest

import scala.Vector

import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.Suite
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.convert.ScalaXmlConversions
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.indexed
import eu.cdevreeze.yaidom.queryapi.XmlBaseSupport
import eu.cdevreeze.yaidom.resolved
import eu.cdevreeze.yaidom.simple.Elem
import eu.cdevreeze.yaidom.simple.Node
import eu.cdevreeze.yaidom.utils.DocumentENameExtractor
import eu.cdevreeze.yaidom.utils.NamespaceUtils.findAllNamespaces
import eu.cdevreeze.yaidom.utils.NamespaceUtils.pushUpPrefixedNamespaces
import eu.cdevreeze.yaidom.utils.NamespaceUtils.stripUnusedNamespaces
import eu.cdevreeze.yaidom.utils.SimpleTextENameExtractor
import eu.cdevreeze.yaidom.utils.TextENameExtractor

/**
 * Test case for removing unused namespaces, which is a somewhat shady area of XML. Yaidom makes automatic unused namespace removal quite possible,
 * as shown below.
 *
 * So maybe yaidom does not make simple things extremely simple, but it does make hard things possible, w.r.t. namespace
 * handling.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class RemoveUnusedNamespacesTest extends Suite {

  private val indexedElemBuilder = indexed.IndexedScopedElem.Builder(XmlBaseSupport.JdkUriResolver)

  /**
   * See http://stackoverflow.com/questions/23002655/xquery-how-to-remove-unused-namespace-in-xml-node.
   */
  @Test def testRemoveUnusedNamespaces(): Unit = {
    val xml =
      <otx xmlns="http://iso.org/OTX/1.0.0" xmlns:i18n="http://iso.org/OTX/1.0.0/i18n" xmlns:diag="http://iso.org/OTX/1.0.0/DiagCom" xmlns:measure="http://iso.org/OTX/1.0.0/Measure" xmlns:string="http://iso.org/OTX/1.0.0/StringUtil" xmlns:dmd="http://iso.org/OTX/1.0.0/Auxiliaries/DiagMetaData" xmlns:fileXml="http://vwag.de/OTX/1.0.0/XmlFile" xmlns:log="http://iso.org/OTX/1.0.0/Logging" xmlns:file="http://vwag.de/OTX/1.0.0/File" xmlns:dataPlus="http://iso.org/OTX/1.0.0/DiagDataBrowsingPlus" xmlns:event="http://iso.org/OTX/1.0.0/Event" xmlns:quant="http://iso.org/OTX/1.0.0/Quantities" xmlns:hmi="http://iso.org/OTX/1.0.0/HMI" xmlns:math="http://iso.org/OTX/1.0.0/Math" xmlns:flash="http://iso.org/OTX/1.0.0/Flash" xmlns:data="http://iso.org/OTX/1.0.0/DiagDataBrowsing" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:dt="http://iso.org/OTX/1.0.0/DateTime" xmlns:eventPlus="http://iso.org/OTX/1.0.0/EventPlus" xmlns:corePlus="http://iso.org/OTX/1.0.0/CorePlus" xmlns:xmime="http://www.w3.org/2005/05/xmlmime" xmlns:job="http://iso.org/OTX/1.0.0/Job" id="id_4e5722f2f81a4309860c146fd3c743e5" name="NewDocument1" package="NewOtxProject1Package1" version="1.0.0.0" timestamp="2014-04-11T09:42:50.2091628+07:00">
        <declarations>
          <constant id="id_fdc639e20fbb42a4b6b039f4262a0001" name="GlobalConstant">
            <realisation>
              <dataType xsi:type="Boolean">
                <init value="false"/>
              </dataType>
            </realisation>
          </constant>
        </declarations>
        <metaData>
          <data key="MadeWith">Created by emotive Open Test Framework - www.emotive.de</data>
          <data key="OtfVersion">4.1.0.8044</data>
        </metaData>
        <procedures>
          <procedure id="id_1a80900324c64ee883d9c12e08c8c460" name="main" visibility="PUBLIC">
            <realisation>
              <flow/>
            </realisation>
          </procedure>
        </procedures>
      </otx>

    val rootElem = ScalaXmlConversions.convertToElem(xml)

    assertResult(Set("", "i18n", "diag", "measure", "string", "dmd", "fileXml", "log", "file", "dataPlus", "event",
      "quant", "hmi", "math", "flash", "data", "xsi", "dt", "eventPlus", "corePlus", "xmime", "job")) {

      rootElem.scope.prefixNamespaceMap.keySet
    }
    assertResult(Set(rootElem.scope)) {
      rootElem.findAllElemsOrSelf.map(_.scope).toSet
    }

    val enameExtractor = DocumentENameExtractor.NoOp

    val usedNamespaces = findAllNamespaces(indexedElemBuilder.build(rootElem), enameExtractor)

    assertResult(Set("", "xsi")) {
      rootElem.scope.filter(kv => usedNamespaces.contains(kv._2)).keySet
    }

    // Now remove the unused namespaces

    val editedRootElem = stripUnusedNamespaces(indexedElemBuilder.build(rootElem), enameExtractor)

    assertResult(Set("", "xsi")) {
      editedRootElem.scope.prefixNamespaceMap.keySet
    }
    assertResult(Set(editedRootElem.scope)) {
      editedRootElem.findAllElemsOrSelf.map(_.scope).toSet
    }

    assertResult(resolved.Elem(rootElem)) {
      resolved.Elem(editedRootElem)
    }
  }

  /**
   * See http://stackoverflow.com/questions/9007894/strip-all-foreign-namespace-nodes-with-xquery.
   */
  @Test def testRemoveChildElementsInOtherNamespaces(): Unit = {
    val xml =
      <entry xmlns="http://www.w3.org/2005/Atom">
        <id>urn:uuid:1234</id>
        <updated>2012-01-20T11:30:11-05:00</updated>
        <published>2011-12-29T15:44:11-05:00</published>
        <link href="?id=urn:uuid:1234" rel="edit" type="application/atom+xml"/>
        <title>Title</title>
        <category scheme="http://uri/categories" term="category"/>
        <fake:fake xmlns:fake="http://fake/" attr="val"/>
        <content type="xhtml">
          <div xmlns="http://www.w3.org/1999/xhtml">
            <p>Blah</p>
          </div>
        </content>
      </entry>

    val rootElem = ScalaXmlConversions.convertToElem(xml)

    val editedRootElem =
      rootElem transformChildElemsToNodeSeq {
        case e: Elem if e.resolvedName.namespaceUriOption == Some("http://www.w3.org/2005/Atom") => Vector(e)
        case e: Elem => Vector()
      }

    assertResult(rootElem.findAllElemsOrSelf.size - 1) {
      editedRootElem.findAllElemsOrSelf.size
    }

    assertResult {
      resolved.Elem(rootElem) transformChildElemsToNodeSeq {
        case e: resolved.Elem if e.resolvedName.namespaceUriOption == Some("http://www.w3.org/2005/Atom") => Vector(e)
        case e: resolved.Elem => Vector()
      }
    } {
      resolved.Elem(editedRootElem)
    }
  }

  /**
   * See https://www.oxygenxml.com/archives/xsl-list/201103/msg00174.html.
   */
  @Test def testCleanupNamespaces(): Unit = {
    val xml =
      <t:Test xmlns:t="http://www.test.org" xmlns:unused="http://www.unused.org" xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
        <t:faultcode>soap:client</t:faultcode>
      </t:Test>

    val rootElem = ScalaXmlConversions.convertToElem(xml)

    val enameExtractor = new DocumentENameExtractor {

      def findAttributeValueENameExtractor(elem: indexed.Elem, attributeEName: EName): Option[TextENameExtractor] = None

      def findElemTextENameExtractor(elem: indexed.Elem): Option[TextENameExtractor] = {
        if (elem.resolvedName == EName("http://www.test.org", "faultcode")) Some(SimpleTextENameExtractor)
        else None
      }
    }

    assertResult(Set("http://www.test.org", "http://schemas.xmlsoap.org/soap/envelope/")) {
      findAllNamespaces(indexedElemBuilder.build(rootElem), enameExtractor)
    }

    val newScope = (rootElem.scope -- Set("t")) ++ Scope.from("" -> "http://www.test.org")

    val editedRootElem: Elem = {
      val elem = rootElem transformElemsOrSelf { e =>
        val newQName =
          if (e.qname.prefixOption.getOrElse("") == "t") QName(e.qname.localPart) else e.qname
        e.copy(qname = newQName, scope = newScope)
      }
      stripUnusedNamespaces(indexedElemBuilder.build(elem), enameExtractor)
    }

    assertResult(Scope.from("" -> "http://www.test.org", "soap" -> "http://schemas.xmlsoap.org/soap/envelope/")) {
      editedRootElem.scope
    }
    assertResult(Set(editedRootElem.scope)) {
      editedRootElem.findAllElemsOrSelf.map(_.scope).toSet
    }

    assertResult(resolved.Elem(rootElem)) {
      resolved.Elem(editedRootElem)
    }
  }

  /**
   * See http://sourceforge.net/p/saxon/mailman/message/11206899/.
   */
  @Test def testPushUpNamespaceDeclarations(): Unit = {
    val xml =
      <mydummyframe>
        <myspecialnode xmlns:aaa="blah" xmlns:bbb="foo" xmlns:ccc="bar" myattr="ccc:karl"/>
        <myspecialnode xmlns:aaa="blah" xmlns:bbb="foo" xmlns:ccc="bar" myattr="ccc:paul"/>
        <myspecialnode xmlns:aaa="blah" xmlns:bbb="foo" xmlns:ccc="bar" myattr="ccc:susan"/>
        <myspecialnode xmlns:aaa="blah" xmlns:bbb="foo" xmlns:ccc="bar" myattr="ccc:peter"/>
        <myspecialnode xmlns:aaa="blah" xmlns:bbb="foo" xmlns:ccc="bar" myattr="ccc:thomas"/>
      </mydummyframe>

    val rootElem = ScalaXmlConversions.convertToElem(xml)

    assertResult(Scope.Empty) {
      rootElem.scope
    }
    assertResult(Set(Scope.from("aaa" -> "blah", "bbb" -> "foo", "ccc" -> "bar"))) {
      rootElem.findAllElems.map(_.scope).toSet
    }

    val editedRootElem = pushUpPrefixedNamespaces(rootElem)

    assertResult(Set(Scope.from("aaa" -> "blah", "bbb" -> "foo", "ccc" -> "bar"))) {
      editedRootElem.findAllElemsOrSelf.map(_.scope).toSet
    }

    assertResult(resolved.Elem(rootElem)) {
      resolved.Elem(editedRootElem)
    }
  }

  /**
   * See http://www.lenzconsulting.com/namespaces-in-xslt/, on "copy-namespaces".
   */
  @Test def testExcludeNamespaces(): Unit = {
    val xml =
      <doc xmlns:my="http://example.com" my:id="AAA">
        <p>This is the first paragraph.</p>
        <p>This is the second paragraph.</p>
      </doc>

    val rootElem = ScalaXmlConversions.convertToElem(xml)

    assertResult(Set(Scope.from("my" -> "http://example.com"))) {
      rootElem.findAllElemsOrSelf.map(_.scope).toSet
    }

    import Node._

    val newElem1 =
      elem(QName("new-doc"), Scope.Empty, rootElem.children)

    assertResult(Set(Scope.Empty, Scope.from("my" -> "http://example.com"))) {
      newElem1.findAllElemsOrSelf.map(_.scope).toSet
    }
    assertResult(Scope.Empty) {
      newElem1.scope
    }
    assertResult(Set(Scope.from("my" -> "http://example.com"))) {
      newElem1.findAllElems.map(_.scope).toSet
    }

    val enameExtractor = DocumentENameExtractor.NoOp

    val newElem2 =
      newElem1 transformChildElems {
        e => stripUnusedNamespaces(indexedElemBuilder.build(e), enameExtractor)
      }

    assertResult(Set(Scope.Empty)) {
      newElem2.findAllElemsOrSelf.map(_.scope).toSet
    }

    assertResult(resolved.Elem(newElem1)) {
      resolved.Elem(newElem2)
    }
  }
}
