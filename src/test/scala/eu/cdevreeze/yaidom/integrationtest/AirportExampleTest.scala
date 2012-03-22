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
import scala.collection.immutable
import org.junit.{ Test, Before, Ignore }
import org.junit.runner.RunWith
import org.scalatest.{ Suite, BeforeAndAfterAll }
import org.scalatest.junit.JUnitRunner
import parse.DocumentParserUsingSax
import print.DocumentPrinterUsingSax

/**
 * Test case using yaidom on files of airports.
 *
 * Acknowledgments: This test uses output from the web service at http://www.webservicex.net/WS/WSDetails.aspx?WSID=20&CATID=7.
 *
 * To debug the SAX parsers, use JVM option -Djaxp.debug=1.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class AirportExampleTest extends Suite {

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.integrationtest")

  private val nsWebServiceX = "http://www.webserviceX.NET".ns

  @Test def testDocumentStructure() {
    // 1. Parse XML files into Documents

    val saxParser = DocumentParserUsingSax.newInstance

    val deAirportsDoc: Document = {
      val is = classOf[AirportExampleTest].getResourceAsStream("airportsGermany.xml")
      saxParser.parse(is)
    }

    val beAirportsDoc: Document = {
      val is = classOf[AirportExampleTest].getResourceAsStream("airportsBelgium.xml")
      saxParser.parse(is)
    }

    val nlAirportsDoc: Document = {
      val is = classOf[AirportExampleTest].getResourceAsStream("airportsNetherlands.xml")
      saxParser.parse(is)
    }

    // 2. Check document structure

    val rootElms = List(deAirportsDoc, beAirportsDoc, nlAirportsDoc) map { doc => doc.documentElement }

    rootElms foreach { root => validateDocumentStructure(root) }
  }

  private def validateDocumentStructure(root: Elem) {
    val enameNewDataSet = nsWebServiceX.ename("NewDataSet")
    val enameTable = nsWebServiceX.ename("Table")

    // The root element must be named NewDataSet
    expect(enameNewDataSet) {
      root.resolvedName
    }

    val tableElms = root.allChildElems

    // The root child elements must all be named Table
    expect(Set(enameTable)) {
      val elmNames = tableElms map { _.resolvedName }
      elmNames.toSet
    }

    val tablePropertyElms = tableElms flatMap { e => e.allChildElems }

    val propertyENames = Set(
      nsWebServiceX.ename("AirportCode"),
      nsWebServiceX.ename("CityOrAirportName"),
      nsWebServiceX.ename("Country"),
      nsWebServiceX.ename("CountryAbbrviation"),
      nsWebServiceX.ename("CountryCode"),
      nsWebServiceX.ename("GMTOffset"),
      nsWebServiceX.ename("RunwayLengthFeet"),
      nsWebServiceX.ename("RunwayElevationFeet"),
      nsWebServiceX.ename("LatitudeDegree"),
      nsWebServiceX.ename("LatitudeMinute"),
      nsWebServiceX.ename("LatitudeSecond"),
      nsWebServiceX.ename("LatitudeNpeerS"),
      nsWebServiceX.ename("LongitudeDegree"),
      nsWebServiceX.ename("LongitudeMinute"),
      nsWebServiceX.ename("LongitudeSeconds"),
      nsWebServiceX.ename("LongitudeEperW"))

    // The root grandchild elements must all have names mentioned above (in Set propertyENames)
    expect(propertyENames) {
      val elmNames = tablePropertyElms map { _.resolvedName }
      elmNames.toSet
    }

    // The root grandchild elements must have no child elements themselves
    expect(0) {
      val elms = tablePropertyElms flatMap { _.allChildElems }
      elms.size
    }
  }
}
