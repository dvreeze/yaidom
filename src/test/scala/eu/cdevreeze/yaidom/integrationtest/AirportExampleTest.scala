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
import javax.xml.parsers._
import javax.xml.transform.TransformerFactory
import scala.collection.immutable
import org.junit.{ Test, Before, Ignore }
import org.junit.runner.RunWith
import org.scalatest.{ Suite, BeforeAndAfterAll }
import org.scalatest.junit.JUnitRunner
import parse._
import print._

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

  @Test def testShortDocumentQueries() {
    // 1. Parse XML files into Documents

    val domParser = DocumentParserUsingDom.newInstance

    val deAirportsDoc: Document = {
      val is = classOf[AirportExampleTest].getResourceAsStream("airportsGermany.xml")
      domParser.parse(is)
    }

    val beAirportsDoc: Document = {
      val is = classOf[AirportExampleTest].getResourceAsStream("airportsBelgium.xml")
      domParser.parse(is)
    }

    val nlAirportsDoc: Document = {
      val is = classOf[AirportExampleTest].getResourceAsStream("airportsNetherlands.xml")
      domParser.parse(is)
    }

    // 2. Sanity checks on the documents

    val beAirportCountries = {
      val result = beAirportsDoc.documentElement collectFromElems { case e if e.localName == "Country" => e.trimmedText }
      result.toSet
    }
    expect(Set("Belgium")) {
      beAirportCountries
    }

    val nlAirportCountryCodes = {
      val result = nlAirportsDoc.documentElement collectFromElems { case e if e.localName == "CountryCode" => e.trimmedText }
      result.toSet
    }
    expect(Set("461")) {
      nlAirportCountryCodes
    }

    val deAirportCountryAbbrevs = {
      val result = deAirportsDoc.documentElement collectFromElems { case e if e.localName == "CountryAbbrviation" => e.trimmedText }
      result.toSet
    }
    expect(Set("DE")) {
      deAirportCountryAbbrevs
    }

    // 3. Check elevation query results

    val enameTable = nsWebServiceX.ename("Table")
    val enameRunwayElevationFeet = nsWebServiceX.ename("RunwayElevationFeet")

    def highestAirport(root: Elem): Elem = {
      val sorted = root.filterElemsOrSelfNamed(enameTable) sortBy { e =>
        e.findChildElemNamed(enameRunwayElevationFeet) map { e => e.trimmedText.toInt } getOrElse (0)
      }
      sorted.last
    }

    val highestNlAirport = highestAirport(nlAirportsDoc.documentElement)
    val highestBeAirport = highestAirport(beAirportsDoc.documentElement)
    val highestDeAirport = highestAirport(deAirportsDoc.documentElement)

    def airportElevationInFeet(e: Elem): Int = {
      require(e.resolvedName == enameTable)
      e.getChildElemNamed(enameRunwayElevationFeet).trimmedText.toInt
    }

    // The highest German airport is Oberpfaffenhofen (Munich)
    expect("OBF") {
      airportCode(highestDeAirport)
    }
    assert(airportElevationInFeet(highestDeAirport) > 1800)
    assert(airportElevationInFeet(highestDeAirport) < 2000)

    // The highest Belgian airport is Liege
    expect("LGG") {
      airportCode(highestBeAirport)
    }

    // The highest Dutch airport is Maastricht
    expect("MST") {
      airportCode(highestNlAirport)
    }

    // Checking some obvious facts:
    // The highest airport in Germany is higher than the highest in Belgium, which is higher than the highest in the Netherlands.
    // The highest of these 3 is also the one located south of the next highest, and so on.

    assert(airportElevationInFeet(highestDeAirport) > airportElevationInFeet(highestBeAirport))
    assert(airportElevationInFeet(highestBeAirport) > airportElevationInFeet(highestNlAirport))

    assert(airportLatitude(highestDeAirport) < airportLatitude(highestBeAirport))
    assert(airportLatitude(highestBeAirport) < airportLatitude(highestNlAirport))

    // 4. Check distance query results

    // Airports close enough to Arnhem (not taking the route into account)

    val latLonArnhem = LatLon(52.0, 5.9)

    val maxDistance = 160.0

    val nlAirportsCloseToArnhem = {
      val airportElms = nlAirportsDoc.documentElement.allChildElems

      airportElms filter { e =>
        val latLon = LatLon(airportLatitude(e), airportLongitude(e))
        latLon.distance(latLonArnhem) <= maxDistance
      }
    }

    val deAirportsCloseToArnhem = {
      val airportElms = deAirportsDoc.documentElement.allChildElems

      airportElms filter { e =>
        val latLon = LatLon(airportLatitude(e), airportLongitude(e))
        latLon.distance(latLonArnhem) <= maxDistance
      }
    }

    val nlAirportCodesCloseToArnhem = {
      val result = nlAirportsCloseToArnhem map { e => airportCode(e) }
      result.toSet
    }
    val deAirportCodesCloseToArnhem = {
      val result = deAirportsCloseToArnhem map { e => airportCode(e) }
      result.toSet
    }

    // Schiphol, Rotterdam, Eindhoven and Maastricht are close enough
    assert(Set("AMS", "RTM", "EIN", "MST").subsetOf(nlAirportCodesCloseToArnhem))

    // Duesseldorf and Koeln-Bonn are close enough
    assert(Set("DUS", "CGN").subsetOf(deAirportCodesCloseToArnhem))
    // Frankfurt is quite a bit further away
    assert(!deAirportCodesCloseToArnhem.contains("FRA"))
  }

  @Test def testDocumentTransformations() {
    // 1. Parse XML files into Documents

    val staxParser = DocumentParserUsingStax.newInstance

    val deAirportsDoc: Document = {
      val is = classOf[AirportExampleTest].getResourceAsStream("airportsGermany.xml")
      staxParser.parse(is)
    }

    val beAirportsDoc: Document = {
      val is = classOf[AirportExampleTest].getResourceAsStream("airportsBelgium.xml")
      staxParser.parse(is)
    }

    val nlAirportsDoc: Document = {
      val is = classOf[AirportExampleTest].getResourceAsStream("airportsNetherlands.xml")
      staxParser.parse(is)
    }

    // 2. Transform the XML. We do it in relatively small steps, but still using a for-comprehension.

    val scope = Scope.fromMap(Map("" -> "http://www.webserviceX.NET"))

    val airportRootElm =
      Elem(
        qname = "NewDataSet".qname,
        scope = scope,
        children = deAirportsDoc.documentElement.allChildElems ++ beAirportsDoc.documentElement.allChildElems ++ nlAirportsDoc.documentElement.allChildElems)

    expect(deAirportsDoc.documentElement.allChildElems.size + beAirportsDoc.documentElement.allChildElems.size + nlAirportsDoc.documentElement.allChildElems.size) {
      airportRootElm.allChildElems.size
    }

    // The airport codes we are now interested in
    val airportCodes = List("BRU", "AMS", "HAM", "FRA", "CGN", "MUC", "TXL")

    val airportElms: immutable.Seq[Elem] = {
      // Contains duplicates
      val elms = airportRootElm filterChildElems { e => airportCodes.contains(airportCode(e)) }
      val groups = elms groupBy { e => airportCode(e) }
      // No more duplicates
      val result = groups.values map { grp => grp.head }
      result.toList sortBy { e => airportCodes.indexOf(airportCode(e)) }
    }

    val airportLatLons: Map[String, LatLon] = {
      val result = airportCodes map { airport =>
        val airportElm: Elem = {
          val result: Option[Elem] = airportElms find { e => airportCode(e) == airport }
          result.getOrElse(sys.error("Airport %s must exist".format(airport)))
        }

        val lat = airportLatitude(airportElm)
        val lon = airportLongitude(airportElm)

        (airport -> LatLon(lat, lon))
      }
      result.toMap
    }

    import NodeBuilder._

    def distancesElemBuilder(airportElm: Elem): ElemBuilder = {
      val lat = airportLatitude(airportElm)
      val lon = airportLongitude(airportElm)
      val latLon = LatLon(lat, lon)

      val distances: immutable.IndexedSeq[(String, Double)] =
        airportCodes.toIndexedSeq map { (airportCode: String) =>
          val otherLatLon = airportLatLons(airportCode)
          val dist = latLon.distance(otherLatLon)

          (airportCode -> dist)
        }

      elem(
        qname = "Distances".qname,
        children = distances map {
          case (airportCode, dist) =>
            elem(
              qname = "Distance".qname,
              children = immutable.IndexedSeq(
                elem(
                  qname = "Airport".qname,
                  attributes = Map("code".qname -> airportCode),
                  children = immutable.IndexedSeq(text(dist.toString)))))
        })
    }

    val airportSummaryElms: immutable.Seq[Elem] =
      for {
        airportElm <- airportElms
      } yield {
        val airportOrCityName = airportElm.getChildElemNamed(nsWebServiceX.ename("CityOrAirportName")).trimmedText
        val country = airportElm.getChildElemNamed(nsWebServiceX.ename("Country")).trimmedText
        val countryAbbreviation = airportElm.getChildElemNamed(nsWebServiceX.ename("CountryAbbrviation")).trimmedText

        val lat = airportLatitude(airportElm)
        val lon = airportLongitude(airportElm)

        val elmBuilder: ElemBuilder =
          elem(
            qname = "Airport".qname,
            children = immutable.IndexedSeq(
              elem(
                qname = "AirportCode".qname,
                children = immutable.IndexedSeq(text(airportCode(airportElm)))),
              elem(
                qname = "AirportOrCityName".qname,
                children = immutable.IndexedSeq(text(airportOrCityName))),
              elem(
                qname = "Country".qname,
                children = immutable.IndexedSeq(text(country))),
              elem(
                qname = "CountryAbbreviation".qname,
                children = immutable.IndexedSeq(text(countryAbbreviation))),
              elem(
                qname = "Position".qname,
                children = immutable.IndexedSeq(
                  elem(
                    qname = "Lat".qname, children = immutable.IndexedSeq(text(lat.toString))),
                  elem(
                    qname = "Lon".qname, children = immutable.IndexedSeq(text(lon.toString))))),
              distancesElemBuilder(airportElm)))

        elmBuilder.build(scope)
      }

    val airportSummaryRoot = Elem(qname = "Airports".qname, scope = scope, children = airportSummaryElms.toIndexedSeq)

    val domPrinter = {
      val dbf = DocumentBuilderFactory.newInstance
      val tf = TransformerFactory.newInstance
      tf.setAttribute("indent-number", java.lang.Integer.valueOf(2))
      DocumentPrinterUsingDom.newInstance(dbf, tf)
    }

    val summaryXmlString = domPrinter.print(Document(airportSummaryRoot))
    println(summaryXmlString)

    val distanceFrankfurtBrussels: Double = {
      val airportElms =
        for {
          airportElm <- airportSummaryRoot.allChildElems
          val airportCodeElm = airportElm.getChildElemNamed(nsWebServiceX.ename("AirportCode"))
          if airportCodeElm.trimmedText == "FRA"
        } yield airportElm
      val airportElm = airportElms.headOption.getOrElse(sys.error("Expected airport FRA"))

      val distances =
        for {
          distanceElm <- airportElm.filterElemsNamed(nsWebServiceX.ename("Distance"))
          airportElm <- distanceElm.filterChildElemsNamed(nsWebServiceX.ename("Airport"))
          if airportElm.attribute("code".ename) == "BRU"
        } yield airportElm.trimmedText.toDouble
      val distance = distances.headOption.getOrElse(sys.error("Expected distance to BRU"))
      distance
    }

    assert(distanceFrankfurtBrussels > 300 && distanceFrankfurtBrussels < 320)
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

  private def airportLatitude(e: Elem): Double = {
    require(e.resolvedName == nsWebServiceX.ename("Table"))

    val degree = e.getChildElemNamed(nsWebServiceX.ename("LatitudeDegree")).trimmedText.toDouble
    val minute = e.getChildElemNamed(nsWebServiceX.ename("LatitudeMinute")).trimmedText.toDouble
    val second = e.getChildElemNamed(nsWebServiceX.ename("LatitudeSecond")).trimmedText.toDouble

    val north = {
      val result = e.getChildElemNamed(nsWebServiceX.ename("LatitudeNpeerS")).trimmedText
      (result != "S")
    }

    val absoluteValue = degree + (minute / 60) + (second / 3600)
    if (north) absoluteValue else -absoluteValue
  }

  private def airportLongitude(e: Elem): Double = {
    require(e.resolvedName == nsWebServiceX.ename("Table"))

    val degree = e.getChildElemNamed(nsWebServiceX.ename("LongitudeDegree")).trimmedText.toDouble
    val minute = e.getChildElemNamed(nsWebServiceX.ename("LongitudeMinute")).trimmedText.toDouble
    val second = e.getChildElemNamed(nsWebServiceX.ename("LongitudeSeconds")).trimmedText.toDouble

    val east = {
      val result = e.getChildElemNamed(nsWebServiceX.ename("LongitudeEperW")).trimmedText
      (result != "W")
    }

    val absoluteValue = degree + (minute / 60) + (second / 3600)
    if (east) absoluteValue else -absoluteValue
  }

  private def airportCode(e: Elem): String = {
    require(e.resolvedName == nsWebServiceX.ename("Table"))

    e.getChildElemNamed(nsWebServiceX.ename("AirportCode")).trimmedText
  }

  final case class LatLon(val lat: Double, val lon: Double) {
    import scala.math._

    /**
     * Returns the distance to another LatLon. See http://en.wikipedia.org/wiki/Haversine_formula,
     * and http://www.movable-type.co.uk/scripts/latlong.html.
     */
    def distance(other: LatLon): Double = {
      val radiusEarth = 6371.0

      val dLat = toRadians(other.lat - lat)
      val dLon = toRadians(other.lon - lon)

      val lat1 = toRadians(lat)
      val lat2 = toRadians(other.lat)

      val a = pow((dLat / 2), 2) + (cos(lat1) * cos(lat2) * pow(sin(dLon / 2), 2))

      val c = 2 * atan2(sqrt(a), sqrt(1 - a))

      val d = radiusEarth * c
      d
    }
  }
}
