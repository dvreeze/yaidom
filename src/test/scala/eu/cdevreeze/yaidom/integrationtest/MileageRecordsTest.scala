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
import javax.xml.transform.{ TransformerFactory, Transformer }
import scala.collection.immutable
import org.junit.{ Test, Before, Ignore }
import org.junit.runner.RunWith
import org.scalatest.{ Suite, BeforeAndAfterAll }
import org.scalatest.junit.JUnitRunner
import org.joda.time.LocalDate
import parse._
import print._
import convert.ScalaXmlConversions._
import MileageRecordsTest._

/**
 * Test case using yaidom for mileage records.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class MileageRecordsTest extends Suite {

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.integrationtest")

  @Test def testReferentialIntegrity(): Unit = {
    import ElemApi._

    val docParser = DocumentParserUsingSax.newInstance

    val doc: Document = {
      val is = classOf[MileageRecordsTest].getResourceAsStream("trips-2013.xml")
      docParser.parse(is)
    }
    val mileageRecordsElem = doc.documentElement

    val mileageRecords = MileageRecords.fromElem(mileageRecordsElem)

    // Check referential integrity

    val knownTripCategoryNames = mileageRecords.knownTrips.map(_.categoryName).toSet
    val tripCategoryNames = mileageRecords.tripCategories.map(_.name).toSet

    assertResult(Set()) {
      knownTripCategoryNames diff tripCategoryNames
    }

    val fromAddressNames = mileageRecords.knownTrips.map(_.fromAddressName).toSet
    val toAddressNames = mileageRecords.knownTrips.map(_.toAddressName).toSet
    val addressNames = mileageRecords.knownAddresses.map(_.name).toSet

    assertResult(Set()) {
      fromAddressNames diff addressNames
    }
    assertResult(Set()) {
      toAddressNames diff addressNames
    }

    val tripNames = mileageRecords.trips.map(_.tripName).toSet
    val knownTripNames = mileageRecords.knownTrips.map(_.name).toSet

    assertResult(Set()) {
      tripNames diff knownTripNames
    }
  }

  @Test def testSucceedingTrips(): Unit = {
    import ElemApi._

    val docParser = DocumentParserUsingSax.newInstance

    val doc: Document = {
      val is = classOf[MileageRecordsTest].getResourceAsStream("trips-2013.xml")
      docParser.parse(is)
    }
    val mileageRecordsElem = doc.documentElement

    val mileageRecords = MileageRecords.fromElem(mileageRecordsElem)

    // Check succeeding trips

    val trips = mileageRecords.trips

    trips.sliding(2) foreach { tripPair =>
      assert(tripPair.size == 2)
      val trip1 = tripPair.head
      val trip2 = tripPair.tail.head

      assertResult(trip1.endKm) {
        trip2.startKm
      }
      assertResult(mileageRecords.knownTripsByName(trip1.tripName).toAddressName) {
        mileageRecords.knownTripsByName(trip2.tripName).fromAddressName
      }
    }
  }
}

object MileageRecordsTest {

  import ElemApi._

  final class TripCategory(val name: String, val isPrivate: Boolean) {

    def toElem: Elem = {
      val xml = <tripCategory isPrivate={ isPrivate.toString }>{ name }</tripCategory>
      convertToElem(xml)
    }
  }

  object TripCategory {

    def fromElem(elem: Elem): TripCategory = {
      require(elem.resolvedName == EName("tripCategory"))
      val name = elem.text
      val isPrivate = elem.attribute(EName("isPrivate")).toBoolean

      new TripCategory(name, isPrivate)
    }
  }

  final class Address(
    val name: String,
    val street: String,
    val houseNumber: String,
    val city: String,
    val zipCode: String,
    val country: String) {

    def toElem(qname: QName): Elem = {
      val xml =
        <address name={ name }>
          <street>{ street }</street>
          <houseNumber>{ houseNumber }</houseNumber>
          <city>{ city }</city>
          <zipCode>{ zipCode }</zipCode>
          <country>{ country }</country>
        </address>
      convertToElem(xml).copy(qname = qname)
    }
  }

  object Address {

    def fromElem(elem: Elem): Address = {
      val name = (elem \@ EName("name")).getOrElse(sys.error("Missing @name"))
      val street = elem.getChildElem(withLocalName("street")).text
      val houseNumber = elem.getChildElem(withLocalName("houseNumber")).text
      val city = elem.getChildElem(withLocalName("city")).text
      val zipCode = elem.getChildElem(withLocalName("zipCode")).text
      val country = elem.getChildElem(withLocalName("country")).text

      new Address(name, street, houseNumber, city, zipCode, country)
    }
  }

  final class KnownTrip(
    val name: String,
    val categoryName: String,
    val fromAddressName: String,
    val toAddressName: String,
    val km: BigDecimal) {

    def toElem: Elem = {
      val xml =
        <knownTrip name={ name }>
          <category>{ categoryName }</category>
          <fromAddress>{ fromAddressName }</fromAddress>
          <toAddress>{ toAddressName }</toAddress>
          <km>{ km.toString }</km>
        </knownTrip>

      convertToElem(xml)
    }
  }

  object KnownTrip {

    def fromElem(elem: Elem): KnownTrip = {
      require(elem.resolvedName == EName("knownTrip"))
      val name = (elem \@ EName("name")).getOrElse(sys.error("Missing @name"))
      val categoryName = elem.getChildElem(withLocalName("category")).text
      val fromAddressName = elem.getChildElem(withLocalName("fromAddress")).text
      val toAddressName = elem.getChildElem(withLocalName("toAddress")).text
      val km = BigDecimal(elem.getChildElem(withLocalName("km")).text)

      new KnownTrip(name, categoryName, fromAddressName, toAddressName, km)
    }
  }

  final class Trip(
    val date: LocalDate,
    val tripName: String,
    val startKm: Int,
    val endKm: Int) {

    require(startKm <= endKm, s"${startKm} must be <= ${endKm} on date ${date}")

    def toElem: Elem = {
      val xml =
        <trip date={ date.toString() }>
          <tripName>{ tripName }</tripName>
          <start>{ startKm.toString }</start>
          <end>{ endKm.toString }</end>
        </trip>

      convertToElem(xml)
    }
  }

  object Trip {

    def fromElem(elem: Elem): Trip = {
      require(elem.resolvedName == EName("trip"))
      val dateString = (elem \@ EName("date")).getOrElse(sys.error("Missing @name"))
      val date = LocalDate.parse(dateString)
      val tripName = elem.getChildElem(withLocalName("tripName")).text
      val startKm = elem.getChildElem(withLocalName("start")).text.toInt
      val endKm = elem.getChildElem(withLocalName("end")).text.toInt

      new Trip(date, tripName, startKm, endKm)
    }
  }

  final class MileageRecords(
    val tripCategories: immutable.IndexedSeq[TripCategory],
    val knownAddresses: immutable.IndexedSeq[Address],
    val knownTrips: immutable.IndexedSeq[KnownTrip],
    val trips: immutable.IndexedSeq[Trip]) {

    val tripCategoriesByName: Map[String, TripCategory] =
      tripCategories.map(tripCat => (tripCat.name -> tripCat)).toMap

    val knownAddressesByName: Map[String, Address] =
      knownAddresses.map(addr => (addr.name -> addr)).toMap

    val knownTripsByName: Map[String, KnownTrip] =
      knownTrips.map(tr => (tr.name -> tr)).toMap

    def toElem: Elem = {
      val xml =
        <mileageRecords>
          <tripCategories>
          </tripCategories>
          <knownAddresses>
          </knownAddresses>
          <knownTrips>
          </knownTrips>
          <trips>
          </trips>
        </mileageRecords>

      val result =
        convertToElem(xml) transformChildElems {
          case elem: Elem if elem.localName == "tripCategories" =>
            elem.copy(children = tripCategories.map(_.toElem))
          case elem: Elem if elem.localName == "knownAddresses" =>
            elem.copy(children = knownAddresses.map(_.toElem(QName("knownAddress"))))
          case elem: Elem if elem.localName == "knownTrips" =>
            elem.copy(children = knownTrips.map(_.toElem))
          case elem: Elem if elem.localName == "trips" =>
            elem.copy(children = trips.map(_.toElem))
          case elem: Elem => elem
        }

      result.prettify(2)
    }
  }

  object MileageRecords {

    def fromElem(elem: Elem): MileageRecords = {
      require(elem.resolvedName == EName("mileageRecords"))
      val tripCategories = elem.filterElems(withLocalName("tripCategory")).map(e => TripCategory.fromElem(e))
      val knownAddresses = elem.filterElems(withLocalName("knownAddress")).map(e => Address.fromElem(e))
      val knownTrips = elem.filterElems(withLocalName("knownTrip")).map(e => KnownTrip.fromElem(e))
      val trips = elem.filterElems(withLocalName("trip")).map(e => Trip.fromElem(e))

      new MileageRecords(tripCategories, knownAddresses, knownTrips, trips)
    }
  }
}
