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

package eu.cdevreeze.yaidom.integrationtest

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import eu.cdevreeze.yaidom.convert.DomConversions
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.parse.DocumentParserUsingStax
import eu.cdevreeze.yaidom.resolved
import eu.cdevreeze.yaidom.simple.Elem
import javax.xml.bind.JAXBContext
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.bind.annotation.adapters.XmlAdapter
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.stream.StreamSource
import javax.xml.bind.annotation.XmlElement

/**
 * JAXB interop test. Just in case yaidom must interop with JAXB, this test case may show how.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class JaxbTest extends FunSuite {

  test("testRoundtripXbrl") {
    val jaxbContext = JAXBContext.newInstance(classOf[JaxbTest.Wrapper])
    val marshaller = jaxbContext.createMarshaller()
    val unmarshaller = jaxbContext.createUnmarshaller()

    val docParser = DocumentParserUsingStax.newInstance

    val doc = docParser.parse(classOf[JaxbTest].getResourceAsStream("sample-xbrl-instance.xml"))

    val elemAdapter = new JaxbTest.ElemAdapter
    val wrapper1 = new JaxbTest.Wrapper
    wrapper1.setData(doc.documentElement)

    val bos = new ByteArrayOutputStream
    marshaller.marshal(wrapper1, bos)
    val bytes = bos.toByteArray

    val src = new StreamSource(new ByteArrayInputStream(bytes))
    val wrapper2 = unmarshaller.unmarshal(src).asInstanceOf[JaxbTest.Wrapper]

    val xbrlInstanceElem2 = wrapper2.getData.asInstanceOf[Elem]

    assertResult(true) {
      xbrlInstanceElem2.findAllElemsOrSelf.size >= 500
    }

    assertResult(resolved.Elem(doc.documentElement).removeAllInterElementWhitespace) {
      resolved.Elem(xbrlInstanceElem2).removeAllInterElementWhitespace
    }
  }
}

object JaxbTest {

  @XmlRootElement(name = "DataEnvelope", namespace = "http://datawrapper")
  @XmlAccessorType(value = XmlAccessType.PUBLIC_MEMBER)
  class Wrapper {

    private[this] var data: Elem = _

    @XmlJavaTypeAdapter(value = classOf[ElemAdapter])
    @XmlElement(namespace = "http://www.xbrl.org/2003/instance", name = "xbrl")
    def getData: Elem = data

    def setData(v: Elem): Unit = (data = v)
  }

  /**
   * A JAXB XmlAdapter that uses org.w3c.dom.Element as value type and simple.Elem as bound type.
   * Note that the return type of the marshal method (and the parameter type of method unmarshal) is AnyRef.
   */
  class ElemAdapter(val dbf: DocumentBuilderFactory) extends XmlAdapter[AnyRef, Elem] {

    def this() = this(DocumentBuilderFactory.newInstance)

    def marshal(v: Elem): AnyRef = {
      val doc = dbf.newDocumentBuilder().newDocument()
      DomConversions.convertElem(v)(doc)
    }

    def unmarshal(v: AnyRef): Elem = {
      DomConversions.convertToElem(v.asInstanceOf[org.w3c.dom.Element], Scope.Empty)
    }
  }
}
