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
package integrationtest

import java.{ util => jutil, io => jio }
import javax.xml.stream.{ XMLInputFactory, XMLOutputFactory, XMLEventFactory }
import javax.xml.stream.events.XMLEvent
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.{ StreamSource, StreamResult }
import scala.collection.immutable
import org.junit.{ Test, Before, Ignore }
import org.junit.runner.RunWith
import org.scalatest.{ Suite, BeforeAndAfterAll }
import org.scalatest.junit.JUnitRunner
import parse.DocumentParserUsingStax
import print.DocumentPrinterUsingStax

/**
 * Large XML test case.
 *
 * Acknowledgments: The large XML files come from http://javakata6425.appspot.com/#!goToPageIIIarticleIIIOptimally%20parse%20humongous%20XML%20files%20with%20vanilla%20Java.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class LargeXmlTest extends Suite with BeforeAndAfterAll {

  @volatile private var doc: Document = _

  override def beforeAll(configMap: Map[String, Any]) {
    val zipFileUrl = classOf[LargeXmlTest].getResource("veryBigFile.zip")
    val zipFile = new jutil.zip.ZipFile(new jio.File(zipFileUrl.toURI))

    val zipEntries = zipFile.entries()
    require(zipEntries.hasMoreElements())

    val zipEntry: jutil.zip.ZipEntry = zipEntries.nextElement()

    val reader = new jio.BufferedReader(new jio.InputStreamReader(zipFile.getInputStream(zipEntry), "utf-8"))

    val stringWriter = new jio.StringWriter
    var c: Int = -1
    while ({ c = reader.read(); c >= 0 }) {
      stringWriter.write(c)
    }
    reader.close()

    val xmlString = stringWriter.toString

    val parser = DocumentParserUsingStax.newInstance
    doc = parser.parse(new StreamSource(new jio.StringReader(xmlString)))
  }

  @Test def testProcessLargeXml() {
    assert(doc.documentElement.allElemsOrSelf.size >= 100000, "Expected at least 100000 elements in the XML")

    expect(Set("contacts".ename, "contact".ename, "firstName".ename, "lastName".ename, "email".ename, "phone".ename)) {
      doc.documentElement.allElemsOrSelf map { e => e.resolvedName } toSet
    }

    val s = "b" * (2000 + 46)
    val elms1 = doc.documentElement elemsOrSelfWhere { e => e.resolvedName == "phone".ename && e.trimmedText == s }
    assert(elms1.size >= 1, "Expected at least one phone element with text value '%s'".format(s))
  }
}
