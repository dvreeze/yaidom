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

  import AirportExampleTest._

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.integrationtest")

  private val nsWebServiceX = "http://www.webserviceX.NET"

  @Test def testDocumentStructure(): Unit = {
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

  @Test def testShortDocumentQueries(): Unit = {
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
      val result = beAirportsDoc.documentElement.findAllElems collect { case e if e.localName == "Country" => e.trimmedText }
      result.toSet
    }
    expectResult(Set("Belgium")) {
      beAirportCountries
    }

    val nlAirportCountryCodes = {
      val result = nlAirportsDoc.documentElement.findAllElems collect { case e if e.localName == "CountryCode" => e.trimmedText }
      result.toSet
    }
    expectResult(Set("461")) {
      nlAirportCountryCodes
    }

    val deAirportCountryAbbrevs = {
      val result = deAirportsDoc.documentElement.findAllElems collect { case e if e.localName == "CountryAbbrviation" => e.trimmedText }
      result.toSet
    }
    expectResult(Set("DE")) {
      deAirportCountryAbbrevs
    }

    // 3. Check elevation query results

    def highestAirport(root: Elem): Elem = {
      val tableElms = root \\ (_.localName == "Table")
      val sorted = tableElms sortBy { (e: Elem) =>
        e findChildElem { _.localName == "RunwayElevationFeet" } map { e => e.trimmedText.toInt } getOrElse (0)
      }
      sorted.last
    }

    val highestNlAirport = highestAirport(nlAirportsDoc.documentElement)
    val highestBeAirport = highestAirport(beAirportsDoc.documentElement)
    val highestDeAirport = highestAirport(deAirportsDoc.documentElement)

    def airportElevationInFeet(e: Elem): Int = {
      require(e.localName == "Table")
      e.getChildElem(_.localName == "RunwayElevationFeet").trimmedText.toInt
    }

    // The highest German airport is Oberpfaffenhofen (Munich)
    expectResult("OBF") {
      airportCode(highestDeAirport)
    }
    assert(airportElevationInFeet(highestDeAirport) > 1800)
    assert(airportElevationInFeet(highestDeAirport) < 2000)

    // The highest Belgian airport is Liege
    expectResult("LGG") {
      airportCode(highestBeAirport)
    }

    // The highest Dutch airport is Maastricht
    expectResult("MST") {
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
      val airportElms = nlAirportsDoc.documentElement.findAllChildElems

      airportElms filter { e =>
        val latLon = LatLon(airportLatitude(e), airportLongitude(e))
        latLon.distance(latLonArnhem) <= maxDistance
      }
    }

    val deAirportsCloseToArnhem = {
      val airportElms = deAirportsDoc.documentElement.findAllChildElems

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

  @Test def testDocumentTransformations(): Unit = {
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

    val scope = Scope.from(Map("" -> "http://www.webserviceX.NET"))

    val airportRootElm =
      Elem(
        qname = QName("NewDataSet"),
        scope = scope,
        children = deAirportsDoc.documentElement.findAllChildElems ++ beAirportsDoc.documentElement.findAllChildElems ++ nlAirportsDoc.documentElement.findAllChildElems)

    expectResult(deAirportsDoc.documentElement.findAllChildElems.size + beAirportsDoc.documentElement.findAllChildElems.size + nlAirportsDoc.documentElement.findAllChildElems.size) {
      airportRootElm.findAllChildElems.size
    }

    // The airport codes we are now interested in
    val airportCodes = List("BRU", "AMS", "HAM", "FRA", "CGN", "MUC", "TXL")

    val airportElms: immutable.Seq[Elem] = {
      // Contains duplicates
      val elms = airportRootElm \ { e => airportCodes.contains(airportCode(e)) }
      val groups = elms groupBy { e => airportCode(e) }
      // No more duplicates
      val result = groups.values map { grp => grp.head }
      result.toList sortBy { e => airportCodes.indexOf(airportCode(e)) }
    }

    val airportLatLons: Map[String, LatLon] = {
      val result = airportCodes map { airport =>
        val airportElm: Elem = {
          val result: Option[Elem] = airportElms find { e => airportCode(e) == airport }
          result.getOrElse(sys.error(s"Airport $airport must exist"))
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
        qname = QName("Distances"),
        children = distances map {
          case (airportCode, dist) =>
            elem(
              qname = QName("Distance"),
              children = Vector(
                textElem(
                  qname = QName("Airport"),
                  attributes = Vector(QName("code") -> airportCode),
                  txt = dist.toString)))
        })
    }

    val airportSummaryElms: immutable.Seq[Elem] =
      for {
        airportElm <- airportElms
      } yield {
        val airportOrCityName = (airportElm getChildElem (_.localName == "CityOrAirportName")).trimmedText
        val country = (airportElm getChildElem (_.localName == "Country")).trimmedText
        val countryAbbreviation = (airportElm getChildElem (_.localName == "CountryAbbrviation")).trimmedText

        val lat = airportLatitude(airportElm)
        val lon = airportLongitude(airportElm)

        val elmBuilder: ElemBuilder =
          elem(
            qname = QName("Airport"),
            children = Vector(
              textElem(QName("AirportCode"), airportCode(airportElm)),
              textElem(QName("AirportOrCityName"), airportOrCityName),
              textElem(QName("Country"), country),
              textElem(QName("CountryAbbreviation"), countryAbbreviation),
              elem(
                qname = QName("Position"),
                children = Vector(
                  textElem(QName("Lat"), lat.toString),
                  textElem(QName("Lon"), lon.toString))),
              distancesElemBuilder(airportElm)))

        elmBuilder.build(scope)
      }

    val airportSummaryRoot = Elem(qname = QName("Airports"), scope = scope, children = airportSummaryElms.toIndexedSeq)

    val domPrinter = {
      val dbf = DocumentBuilderFactory.newInstance
      val tf = TransformerFactory.newInstance

      try {
        tf.getAttribute("indent-number") // Throws an exception if "indent-number" is not supported
        tf.setAttribute("indent-number", java.lang.Integer.valueOf(2))
      } catch {
        case e: Exception => () // Ignore
      }

      DocumentPrinterUsingDom.newInstance(dbf, tf)
    }

    val summaryXmlString = domPrinter.print(Document(airportSummaryRoot))

    val distanceFrankfurtBrussels: Double = {
      val airportElms =
        for {
          airportElm <- airportSummaryRoot.findAllChildElems
          airportCodeElm = airportElm getChildElem (_.localName == "AirportCode")
          if airportCodeElm.trimmedText == "FRA"
        } yield airportElm
      val airportElm = airportElms.headOption.getOrElse(sys.error("Expected airport FRA"))

      val distances =
        for {
          distanceElm <- airportElm \\ (_.localName == "Distance")
          airportElm <- distanceElm \ (_.localName == "Airport")
          if airportElm.attribute(EName("code")) == "BRU"
        } yield airportElm.trimmedText.toDouble
      val distance = distances.headOption.getOrElse(sys.error("Expected distance to BRU"))
      distance
    }

    assert(distanceFrankfurtBrussels > 300 && distanceFrankfurtBrussels < 320)
  }

  private def validateDocumentStructure(root: Elem): Unit = {
    val enameNewDataSet = EName(nsWebServiceX, "NewDataSet")
    val enameTable = EName(nsWebServiceX, "Table")

    // The root element must be named NewDataSet
    expectResult(enameNewDataSet) {
      root.resolvedName
    }

    val tableElms = root.findAllChildElems

    // The root child elements must all be named Table
    expectResult(Set(enameTable)) {
      val elmNames = tableElms map { _.resolvedName }
      elmNames.toSet
    }

    val tablePropertyElms = tableElms flatMap { e => e.findAllChildElems }

    val propertyENames = Set(
      EName(nsWebServiceX, "AirportCode"),
      EName(nsWebServiceX, "CityOrAirportName"),
      EName(nsWebServiceX, "Country"),
      EName(nsWebServiceX, "CountryAbbrviation"),
      EName(nsWebServiceX, "CountryCode"),
      EName(nsWebServiceX, "GMTOffset"),
      EName(nsWebServiceX, "RunwayLengthFeet"),
      EName(nsWebServiceX, "RunwayElevationFeet"),
      EName(nsWebServiceX, "LatitudeDegree"),
      EName(nsWebServiceX, "LatitudeMinute"),
      EName(nsWebServiceX, "LatitudeSecond"),
      EName(nsWebServiceX, "LatitudeNpeerS"),
      EName(nsWebServiceX, "LongitudeDegree"),
      EName(nsWebServiceX, "LongitudeMinute"),
      EName(nsWebServiceX, "LongitudeSeconds"),
      EName(nsWebServiceX, "LongitudeEperW"))

    // The root grandchild elements must all have names mentioned above (in Set propertyENames)
    expectResult(propertyENames) {
      val elmNames = tablePropertyElms map { _.resolvedName }
      elmNames.toSet
    }

    // The root grandchild elements must have no child elements themselves
    expectResult(0) {
      val elms = tablePropertyElms flatMap { _.findAllChildElems }
      elms.size
    }
  }

  private def airportLatitude(e: Elem): Double = {
    require(e.resolvedName == EName(nsWebServiceX, "Table"))

    val degree = e.getChildElem(EName(nsWebServiceX, "LatitudeDegree")).trimmedText.toDouble
    val minute = e.getChildElem(EName(nsWebServiceX, "LatitudeMinute")).trimmedText.toDouble
    val second = e.getChildElem(EName(nsWebServiceX, "LatitudeSecond")).trimmedText.toDouble

    val north = {
      val result = e.getChildElem(EName(nsWebServiceX, "LatitudeNpeerS")).trimmedText
      (result != "S")
    }

    val absoluteValue = degree + (minute / 60) + (second / 3600)
    if (north) absoluteValue else -absoluteValue
  }

  private def airportLongitude(e: Elem): Double = {
    require(e.resolvedName == EName(nsWebServiceX, "Table"))

    val degree = e.getChildElem(EName(nsWebServiceX, "LongitudeDegree")).trimmedText.toDouble
    val minute = e.getChildElem(EName(nsWebServiceX, "LongitudeMinute")).trimmedText.toDouble
    val second = e.getChildElem(EName(nsWebServiceX, "LongitudeSeconds")).trimmedText.toDouble

    val east = {
      val result = e.getChildElem(EName(nsWebServiceX, "LongitudeEperW")).trimmedText
      (result != "W")
    }

    val absoluteValue = degree + (minute / 60) + (second / 3600)
    if (east) absoluteValue else -absoluteValue
  }

  private def airportCode(e: Elem): String = {
    require(e.resolvedName == EName(nsWebServiceX, "Table"))

    e.getChildElem(EName(nsWebServiceX, "AirportCode")).trimmedText
  }
}

object AirportExampleTest {

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
