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

import java.{ util => jutil }

import scala.collection.immutable
import scala.math.BigDecimal
import scala.math.BigDecimal.double2bigDecimal
import scala.math.BigDecimal.int2bigDecimal
import scala.math.ceil
import scala.math.floor

import org.joda.time.DateTimeConstants
import org.joda.time.LocalDate
import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.convert.ScalaXmlConversions.convertToElem
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.simple.Document
import eu.cdevreeze.yaidom.simple.Elem
import eu.cdevreeze.yaidom.parse.DocumentParserUsingSax
import eu.cdevreeze.yaidom.print.DocumentPrinterUsingDom
import eu.cdevreeze.yaidom.queryapi.HasENameApi

/**
 * Test case using yaidom for mileage records.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class MileageRecordsTest extends FunSuite {
  import MileageRecordsTest.MileageRecords

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.integrationtest")

  test("testReferentialIntegrity") {
    import HasENameApi._

    val docParser = DocumentParserUsingSax.newInstance

    val doc: Document = {
      val is = classOf[MileageRecordsTest].getResourceAsStream("trips.xml")
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

  test("testSucceedingTrips") {
    import HasENameApi._

    val docParser = DocumentParserUsingSax.newInstance

    val doc: Document = {
      val is = classOf[MileageRecordsTest].getResourceAsStream("trips.xml")
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
      assertResult(true) {
        !trip1.date.isAfter(trip2.date)
      }
    }
  }

  test("testTripLengths") {
    import HasENameApi._

    val docParser = DocumentParserUsingSax.newInstance

    val doc: Document = {
      val is = classOf[MileageRecordsTest].getResourceAsStream("trips.xml")
      docParser.parse(is)
    }
    val mileageRecordsElem = doc.documentElement

    val mileageRecords = MileageRecords.fromElem(mileageRecordsElem)

    // Check trip lengths

    val trips = mileageRecords.trips

    trips foreach { trip =>
      val drivenKm = trip.endKm - trip.startKm

      val knownTrip = mileageRecords.knownTripsByName(trip.tripName)

      assertResult(true, s"Unexpected trip length $drivenKm for trip on ${trip.date} (expected >= ${knownTrip.minKm})") {
        drivenKm >= knownTrip.minKm
      }
      assertResult(true, s"Unexpected trip length $drivenKm for trip on ${trip.date} (expected <= ${knownTrip.maxKm})") {
        drivenKm <= knownTrip.maxKm
      }
    }
  }

  test("testTotalPrivateKms") {
    import HasENameApi._

    val docParser = DocumentParserUsingSax.newInstance

    val doc: Document = {
      val is = classOf[MileageRecordsTest].getResourceAsStream("trips.xml")
      docParser.parse(is)
    }
    val mileageRecordsElem = doc.documentElement

    val mileageRecords = MileageRecords.fromElem(mileageRecordsElem)

    // Check total private kms

    val totalPrivateKms = mileageRecords.totalPrivateKms

    assertResult(true, s"Total private kms $totalPrivateKms larger than expected (>= 350)") {
      totalPrivateKms < 350
    }
  }

  test("testConvertToCsv") {
    import HasENameApi._

    val docParser = DocumentParserUsingSax.newInstance

    val doc: Document = {
      val is = classOf[MileageRecordsTest].getResourceAsStream("trips.xml")
      docParser.parse(is)
    }
    val mileageRecordsElem = doc.documentElement

    val mileageRecords = MileageRecords.fromElem(mileageRecordsElem)

    // Generate CSV

    val csvRecords =
      mileageRecords.trips map { trip =>
        val date = trip.date

        val knownTripOption = mileageRecords.knownTripsByName.get(trip.tripName)
        val from =
          knownTripOption flatMap (knownTrip => mileageRecords.knownAddressesByName.get(knownTrip.fromAddressName)) map (_.name) getOrElse ""
        val to =
          knownTripOption flatMap (knownTrip => mileageRecords.knownAddressesByName.get(knownTrip.toAddressName)) map (_.name) getOrElse ""

        val isPrivate =
          knownTripOption.flatMap(knownTrip => mileageRecords.tripCategoriesByName.get(knownTrip.categoryName)).map(_.isPrivate).getOrElse(true)

        val startKm = trip.startKm
        val endKm = trip.endKm

        val categoryString = if (isPrivate) "P" else ""

        List(date.toString, from, to, startKm.toString, endKm.toString, categoryString).mkString("|")
      }

    assertResult(mileageRecords.trips.size) {
      csvRecords.size
    }
    assertResult(mileageRecords.trips.map(_.date)) {
      csvRecords.map(line => LocalDate.parse(line.split('|')(0)))
    }
  }

  test("testUpdateKms") {
    import HasENameApi._

    val docParser = DocumentParserUsingSax.newInstance
    val docPrinter = DocumentPrinterUsingDom.newInstance

    val doc: Document = {
      val is = classOf[MileageRecordsTest].getResourceAsStream("trips.xml")
      docParser.parse(is)
    }
    val mileageRecordsElem = doc.documentElement

    val mileageRecords = MileageRecords.fromElem(mileageRecordsElem)

    // Update

    val newMileageRecords =
      mileageRecords.
        minus(LocalDate.parse("2013-11-13") -> 0).
        minus(LocalDate.parse("2013-11-13") -> 0).
        plus(LocalDate.parse("2013-11-14") -> 0, "werk-heen", 30).
        plus(LocalDate.parse("2013-11-14") -> 1, "werk-terug", 31).
        updateLengthInKm(LocalDate.parse("2013-11-14") -> 0, 1)

    val dates = List("2013-11-11", "2013-11-15", "2013-12-18").map(s => LocalDate.parse(s))

    dates foreach { date =>
      val trip = mileageRecords.getTripAt(date, 0)
      val newTrip = newMileageRecords.getTripAt(date, 0)

      assertResult(trip.startKm) {
        newTrip.startKm
      }

      assertResult(trip.endKm) {
        newTrip.endKm
      }
    }

    // docPrinter.print(newMileageRecords.toElem, "UTF-8", System.out)
  }

  test("testNoWorkTripsInWeekends") {
    import HasENameApi._

    val docParser = DocumentParserUsingSax.newInstance

    val doc: Document = {
      val is = classOf[MileageRecordsTest].getResourceAsStream("trips.xml")
      docParser.parse(is)
    }
    val mileageRecordsElem = doc.documentElement

    val mileageRecords = MileageRecords.fromElem(mileageRecordsElem)

    // Check there are no work trips in weekends

    val nonPrivateTrips = mileageRecords.trips filter { trip =>
      val knownTripOption = mileageRecords.knownTripsByName.get(trip.tripName)
      val categoryOption =
        knownTripOption flatMap (knownTrip => mileageRecords.tripCategoriesByName.get(knownTrip.categoryName))
      val isPrivate = categoryOption.map(_.isPrivate).getOrElse(true)
      !isPrivate
    }

    assertResult(true) {
      nonPrivateTrips.size >= 100
    }

    nonPrivateTrips foreach { trip =>
      val dayOfWeek = trip.date.getDayOfWeek()

      assertResult(true, s"Work trip found on ${trip.date} in the weekend") {
        (dayOfWeek != DateTimeConstants.SATURDAY) && (dayOfWeek != DateTimeConstants.SUNDAY)
      }
    }
  }
}

object MileageRecordsTest {

  import HasENameApi._

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

    require(km >= 0, s"${km} must be >= 0")

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

    def maxKm: Int = (ceil(km.toDouble) * BigDecimal("1.1")).toInt

    def minKm: Int = (floor(km.toDouble) * BigDecimal("0.95")).toInt
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

    def tripLengthInKm: Int = endKm - startKm

    def +(km: Int): Trip = new Trip(date, tripName, startKm + km, endKm + km)

    def -(km: Int): Trip = new Trip(date, tripName, startKm - km, endKm - km)

    def updateEndKm(deltaLengthInKm: Int): Trip = new Trip(date, tripName, startKm, endKm + deltaLengthInKm)

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

    /**
     * A Trip is identified by its date along with the relative index of the trip w.r.t. all trips on that date.
     */
    type Key = (LocalDate, Int)

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

    def getTripAt(date: LocalDate, relativeIdx: Int): Trip = {
      val tripsAtDate = trips.filter(tr => tr.date == date)
      require(!tripsAtDate.isEmpty)
      require(relativeIdx >= 0 && relativeIdx < tripsAtDate.size)

      tripsAtDate(relativeIdx)
    }

    def totalPrivateKms(): Int = {
      val privateTrips = trips filter { trip =>
        val knownTripOption = knownTripsByName.get(trip.tripName)
        val categoryOption = knownTripOption.flatMap(knownTrip => tripCategoriesByName.get(knownTrip.categoryName))
        categoryOption.map(_.isPrivate).getOrElse(true)
      }
      privateTrips.map(_.tripLengthInKm).sum
    }

    /**
     * Adds a trip, returning a new MileageRecords.
     *
     * Mind consistency of the result (end of previous trip matching start of next trip, etc.), so typically repeated calls
     * for adjacent trips are needed.
     */
    def plus(tripKey: Trip.Key, tripName: String, tripLengthInKm: Int): MileageRecords = {
      require(this.trips.size >= 1)

      val tripsWithIndexOnSameDate = trips.zipWithIndex.filter(_._1.date == tripKey._1)

      val idxOfTrip =
        if (tripsWithIndexOnSameDate.isEmpty) trips.indexWhere(trip => !trip.date.isBefore(tripKey._1)).max(0)
        else {
          require(tripKey._2 <= tripsWithIndexOnSameDate.size)

          if (tripKey._2 == tripsWithIndexOnSameDate.size) tripsWithIndexOnSameDate.last._2 + 1
          else tripsWithIndexOnSameDate.apply(tripKey._2)._2
        }

      assert(idxOfTrip >= 0)
      assert(idxOfTrip <= trips.size)

      val kmToAdd = tripLengthInKm

      val editedTrips =
        if (idxOfTrip == 0) {
          val trip = new Trip(tripKey._1, tripName, trips.head.startKm, trips.head.startKm + kmToAdd)

          trip +: trips.map(tr => tr + kmToAdd)
        } else if (idxOfTrip == trips.size) {
          assert(idxOfTrip >= 1)
          val trip = new Trip(tripKey._1, tripName, trips.last.endKm, trips.last.endKm + kmToAdd)

          trips :+ trip
        } else {
          assert(idxOfTrip >= 1)
          val nextTrip = trips.apply(idxOfTrip)
          val trip = new Trip(tripKey._1, tripName, nextTrip.startKm, nextTrip.startKm + kmToAdd)

          (trips.take(idxOfTrip) :+ trip) ++ (trips.drop(idxOfTrip).map(trip => trip + kmToAdd))
        }

      new MileageRecords(tripCategories, knownAddresses, knownTrips, editedTrips)
    }

    /**
     * Removes a trip, returning a new MileageRecords.
     *
     * Mind consistency of the result (end of previous trip matching start of next trip, etc.), so typically repeated calls
     * for adjacent trips are needed.
     */
    def minus(tripKey: Trip.Key): MileageRecords = {
      val idxOfTrip = trips.zipWithIndex.filter(_._1.date == tripKey._1).apply(tripKey._2)._2
      assert(trips(idxOfTrip).date == tripKey._1)

      val kmToSubtract = trips(idxOfTrip).tripLengthInKm
      val editedTrips = trips.take(idxOfTrip) ++ trips.drop(idxOfTrip + 1).map(trip => trip - kmToSubtract)

      new MileageRecords(tripCategories, knownAddresses, knownTrips, editedTrips)
    }

    /**
     * Updates the end-km of a given trip, by passing a deltaLengthInKm for that trip.
     */
    def updateLengthInKm(tripKey: Trip.Key, deltaLengthInKm: Int): MileageRecords = {
      val idxOfTrip = trips.zipWithIndex.filter(_._1.date == tripKey._1).apply(tripKey._2)._2
      assert(trips(idxOfTrip).date == tripKey._1)

      val oldTrip = trips(idxOfTrip)
      val newTrip = oldTrip.updateEndKm(deltaLengthInKm)

      val editedTrips =
        (trips.take(idxOfTrip) :+ newTrip) ++ trips.drop(idxOfTrip + 1).map(tr => tr + deltaLengthInKm)

      new MileageRecords(tripCategories, knownAddresses, knownTrips, editedTrips)
    }

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
