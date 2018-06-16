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

import java.time.DayOfWeek
import java.time.Month
import java.time.LocalDate

import scala.collection.immutable
import scala.reflect.classTag

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.indexed
import eu.cdevreeze.yaidom.print.DocumentPrinterUsingDom
import eu.cdevreeze.yaidom.queryapi.BackingNodes
import eu.cdevreeze.yaidom.queryapi.ScopedElemLike
import eu.cdevreeze.yaidom.queryapi.ScopedNodes
import eu.cdevreeze.yaidom.queryapi.SubtypeAwareElemLike
import eu.cdevreeze.yaidom.resolved
import eu.cdevreeze.yaidom.simple

/**
 * XML creation test, using resolved elements to quickly and easily create some XML snippet.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class XmlCreationTest extends FunSuite with BeforeAndAfterAll {

  import XmlCreationTest._

  test("testCreateTimesheetXmlFor2017") {
    val timesheetAfterFirstPass: Timesheet =
      getTimesheetAfterFirstPass(
        LocalDate.of(2017, 1, 1),
        LocalDate.of(2017, 12, 31),
        dutchHolidays2017,
        onVacation2017,
        sickLeaveDays2017,
        defaultTasksPerDay2017)

    val timesheet =
      refineTimesheet(timesheetAfterFirstPass, refinementsPerDay2017)

    assertResult(true) {
      timesheet.days.size >= 250
    }

    assertResult(true) {
      timesheet.days.count(_.dayOfWeek == DayOfWeek.MONDAY) >= 35 &&
        timesheet.days.count(_.dayOfWeek == DayOfWeek.MONDAY) < 70
    }

    assertResult(24) {
      timesheet.allTasks.filter(_.taskName == "blockchain-hackathon").map(_.hours).sum
    }
    assertResult(Set(Month.FEBRUARY)) {
      timesheet.allTasks.filter(_.taskName == "blockchain-hackathon").map(_.date.getMonth).toSet
    }

    assertResult(Set(8)) {
      timesheet.days.map(_.totalHours).toSet
    }

    val docPrinter = DocumentPrinterUsingDom.newInstance()

    val timesheetSimpleElem = simple.Elem.from(timesheet)

    val xmlString = docPrinter.print(simple.Document(timesheetSimpleElem.prettify(2)))
    println(xmlString)
  }
}

object XmlCreationTest {

  // Yaidom dialect for timesheets

  sealed trait TimesheetNode extends ScopedNodes.Node

  final case class TimesheetText(text: String) extends TimesheetNode with ScopedNodes.Text

  sealed class TimesheetElem(val backingElem: BackingNodes.Elem)
    extends TimesheetNode with ScopedNodes.Elem with ScopedElemLike with SubtypeAwareElemLike {

    type ThisElem = TimesheetElem

    type ThisNode = TimesheetNode

    final def thisElem: ThisElem = TimesheetElem.this

    final def children: immutable.IndexedSeq[TimesheetNode] = {
      backingElem.children flatMap {
        case e: BackingNodes.Elem => Some(TimesheetElem(e))
        case t: BackingNodes.Text => Some(TimesheetText(t.text))
        case _                    => None
      }
    }

    final def findAllChildElems: immutable.IndexedSeq[TimesheetElem] = {
      backingElem.findAllChildElems.map(e => TimesheetElem(e))
    }

    final def resolvedName: EName = backingElem.resolvedName

    final def resolvedAttributes: immutable.IndexedSeq[(EName, String)] = backingElem.resolvedAttributes.toIndexedSeq

    final def text: String = backingElem.text

    final def scope: Scope = backingElem.scope

    final def qname: QName = backingElem.qname

    final def attributes: immutable.IndexedSeq[(QName, String)] = backingElem.attributes.toIndexedSeq
  }

  final class Timesheet(backingElem: BackingNodes.Elem) extends TimesheetElem(backingElem) {
    require(localName == "timesheet")

    def days: immutable.IndexedSeq[Day] = findAllChildElemsOfType(classTag[Day])

    def allTasks: immutable.IndexedSeq[Task] = days.flatMap(_.tasks)
  }

  final class Day(backingElem: BackingNodes.Elem) extends TimesheetElem(backingElem) {
    require(localName == "day")

    def date: LocalDate = LocalDate.parse(attribute(EName("date")))

    def dayOfWeek: DayOfWeek = DayOfWeek.valueOf(attribute(EName("dayOfWeek")))

    def tasks: immutable.IndexedSeq[Task] = findAllChildElemsOfType(classTag[Task])

    def totalHours: Int = tasks.map(_.hours).sum
  }

  final class Task(backingElem: BackingNodes.Elem) extends TimesheetElem(backingElem) {
    require(localName == "task")

    def taskName: String = attribute(EName("name"))

    def hours: Int = attribute(EName("hours")).toInt

    def date: LocalDate = LocalDate.parse(backingElem.parent.attribute(EName("date")))

    def dayOfWeek: DayOfWeek = DayOfWeek.valueOf(backingElem.parent.attribute(EName("dayOfWeek")))
  }

  object TimesheetElem {

    def apply(backingElem: BackingNodes.Elem): TimesheetElem = {
      backingElem.resolvedName match {
        case EName(_, "timesheet") => new Timesheet(backingElem)
        case EName(_, "day")       => new Day(backingElem)
        case EName(_, "task")      => new Task(backingElem)
        case _                     => new TimesheetElem(backingElem)
      }
    }
  }

  object Timesheet {

    def apply(backingElem: BackingNodes.Elem): Timesheet = {
      TimesheetElem(backingElem.ensuring(_.localName == "timesheet")).asInstanceOf[Timesheet]
    }
  }

  // Timesheet manipulation

  val OfficialHoliday = "official-holiday"
  val Vacation = "vacation"
  val Sick = "sick"

  // Weekends are considered free time here!

  private def isWeekend(date: LocalDate): Boolean = {
    (date.getDayOfWeek == DayOfWeek.SATURDAY) || (date.getDayOfWeek == DayOfWeek.SUNDAY)
  }

  private def getTimesheetAfterFirstPass(
    startDate:          LocalDate,
    endDate:            LocalDate,
    officialHolidays:   Set[LocalDate],
    vacationDays:       Set[LocalDate],
    sickLeaveDays:      Set[LocalDate],
    defaultTasksPerDay: Map[LocalDate, Map[String, Int]]): Timesheet = {

    require(startDate.isBefore(endDate), s"$startDate not before $endDate")

    val allDates = getPeriodAsLocalDateSeq(startDate, endDate)

    require(
      officialHolidays.union(vacationDays).union(sickLeaveDays).union(defaultTasksPerDay.keySet).subsetOf(allDates.toSet),
      s"Not all used dates (in the first pass) fit in the period from $startDate to $endDate (maybe weekend?)")

    val nonWeekendDays = allDates.filter(d => !isWeekend(d))

    import resolved.Node._

    val emptyTimesheetElem: resolved.Elem =
      emptyElem(EName("timesheet"))
        .plusChildren(
          nonWeekendDays.map { day =>
            emptyElem(EName("day"))
              .plusAttribute(EName("date"), day.toString)
              .plusAttribute(EName("dayOfWeek"), day.getDayOfWeek.toString)
          })

    val timesheetElem = emptyTimesheetElem transformChildElems {
      case elm if elm.localName == "day" =>
        val day = TimesheetElem(indexed.Elem(simple.Elem.from(elm, Scope.Empty))).asInstanceOf[Day]

        require(!isWeekend(day.date), s"Saturday or Sunday: ${day.date}")

        if (officialHolidays.contains(day.date)) {
          addTask(elm, OfficialHoliday, 8)
        } else if (vacationDays.contains(day.date)) {
          addTask(elm, Vacation, 8)
        } else if (sickLeaveDays.contains(day.date)) {
          addTask(elm, Sick, 8)
        } else if (defaultTasksPerDay.contains(day.date)) {
          defaultTasksPerDay(day.date).foldLeft(elm) {
            case (accDayElm, (taskName, hours)) =>
              addTask(accDayElm, taskName, hours)
          }
        } else {
          elm
        }
      case elm =>
        elm
    }

    val timesheet = Timesheet(indexed.Elem(simple.Elem.from(timesheetElem, Scope.Empty)))
    timesheet
      .ensuring(_.days.map(_.date).toSet == nonWeekendDays.toSet, s"Not all days filled from $startDate to $endDate")
  }

  private def refineTimesheet(
    prevTimesheet:      Timesheet,
    refinedTasksPerDay: Map[LocalDate, Map[String, Int]]): Timesheet = {

    require(
      refinedTasksPerDay.keySet.subsetOf(prevTimesheet.days.map(_.date).toSet),
      s"Not all used dates (in the second pass or later) fit in the period of the given timesheet (maybe weekend?)")

    val pathsPerDate: Map[LocalDate, Path] = {
      prevTimesheet.days.map(d => d.date -> d.backingElem.path).toMap
    }

    val paths: Set[Path] = pathsPerDate.filterKeys(refinedTasksPerDay.keySet).values.toSet

    val rawResultTimesheetElem =
      resolved.Elem.from(prevTimesheet).updateElems(paths) {
        case (elm, path) =>
          assert(elm.localName == "day")

          val date = LocalDate.parse(elm.attribute(EName("date")))
          assert(refinedTasksPerDay.contains(date))

          val taskHours: Map[String, Int] = refinedTasksPerDay(date)

          taskHours.foldLeft(elm) {
            case (accDayElm, (taskName, hours)) =>
              tryToAddTaskWithoutChangingTotalHours(accDayElm, taskName, hours)
          }
      }

    val resultTimesheetElem = removeTasksWithoutHours(rawResultTimesheetElem)

    val resultTimesheet = Timesheet(indexed.Elem(simple.Elem.from(resultTimesheetElem, Scope.Empty)))
    resultTimesheet
  }

  private def onLeave(day: Day): Boolean = {
    !isWeekend(day.date) &&
      ((day.tasks.exists(_.taskName == OfficialHoliday)) ||
        (day.tasks.exists(_.taskName == Vacation)) ||
        (day.tasks.exists(_.taskName == Sick)))
  }

  // Adding and updating tasks

  private def addTask(dayElem: resolved.Elem, taskName: String, hours: Int): resolved.Elem = {
    assert(dayElem.localName == "day")

    dayElem
      .plusChild(
        resolved.Node.emptyElem(EName("task"), Map(EName("name") -> taskName, EName("hours") -> hours.toString)))
  }

  private def tryToSubtractHoursFromTask(dayElem: resolved.Elem, taskName: String, hours: Int): Option[resolved.Elem] = {
    assert(dayElem.localName == "day")

    val day = TimesheetElem(indexed.Elem(simple.Elem.from(dayElem, Scope.Empty))).asInstanceOf[Day]

    val taskIndex: Int =
      day.tasks.zipWithIndex.find(kv => kv._1.taskName == taskName && kv._1.hours >= hours).map(_._2).getOrElse(-1)

    if (taskIndex < 0) {
      None
    } else {
      val pathEntry = Path.Entry(EName("task"), taskIndex)
      val prevTaskValue = day.tasks(taskIndex).ensuring(_.taskName == taskName)

      val result =
        dayElem.updateChildElem(pathEntry) { taskElm =>
          taskElm
            .plusAttribute(EName("hours"), (prevTaskValue.hours.ensuring(_ >= hours) - hours).toString)
        }

      Some(result)
    }
  }

  private def tryToAddTaskWithoutChangingTotalHours(
    dayElem:  resolved.Elem,
    taskName: String,
    hours:    Int): resolved.Elem = {

    assert(dayElem.localName == "day")

    val day = TimesheetElem(indexed.Elem(simple.Elem.from(dayElem, Scope.Empty))).asInstanceOf[Day]

    require(!onLeave(day), s"Cannot add task (on ${day.date} the employee is on leave)")

    val longestTaskOption: Option[Task] = day.tasks.sortBy(_.hours).reverse.headOption

    if (longestTaskOption.isEmpty) {
      dayElem
    } else if (longestTaskOption.get.hours < hours) {
      dayElem
    } else {
      val longestTask = longestTaskOption.get

      val tempResultOption: Option[resolved.Elem] =
        tryToSubtractHoursFromTask(dayElem, longestTask.taskName, hours)

      tempResultOption.map(d => addTask(d, taskName, hours)).getOrElse(dayElem)
    } ensuring { newDayElm =>
      val newDay = TimesheetElem(indexed.Elem(simple.Elem.from(newDayElm, Scope.Empty))).asInstanceOf[Day]

      newDay.tasks.map(_.hours).sum == day.tasks.map(_.hours).sum
    }
  }

  private def removeTasksWithoutHours(timesheetElem: resolved.Elem): resolved.Elem = {
    assert(timesheetElem.localName == "timesheet")

    timesheetElem transformElemsToNodeSeq {
      case e if e.localName == "task" && e.attribute(EName("hours")).toInt == 0 =>
        Vector()
      case e =>
        Vector(e)
    }
  }

  def getPeriodAsLocalDateSeq(startDate: LocalDate, endDate: LocalDate): immutable.IndexedSeq[LocalDate] = {
    require(startDate.isBefore(endDate), s"$startDate not before $endDate")

    Iterator.from(0).map(i => startDate.plusDays(i)).takeWhile(d => !d.isAfter(endDate)).toIndexedSeq
  }

  def getYearAsPeriodAsLocalDateSeq(year: Int): immutable.IndexedSeq[LocalDate] = {
    getPeriodAsLocalDateSeq(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31))
  }

  // Timesheet data 2017

  // Pass 1

  private val dutchHolidays2017: Set[LocalDate] = {
    Set(
      LocalDate.of(2017, 1, 1),
      LocalDate.of(2017, 4, 16),
      LocalDate.of(2017, 4, 17),
      LocalDate.of(2017, 4, 27),
      LocalDate.of(2017, 5, 5),
      LocalDate.of(2017, 5, 25),
      LocalDate.of(2017, 6, 4),
      LocalDate.of(2017, 6, 5),
      LocalDate.of(2017, 12, 25),
      LocalDate.of(2017, 12, 26))
  }

  private val onVacation2017: Set[LocalDate] = {
    Set(LocalDate.of(2017, 3, 3), LocalDate.of(2017, 5, 26), LocalDate.of(2017, 6, 2))
      .union(getPeriodAsLocalDateSeq(LocalDate.of(2017, 7, 21), LocalDate.of(2017, 8, 11)).toSet)
      .union(getPeriodAsLocalDateSeq(LocalDate.of(2017, 12, 22), LocalDate.of(2017, 12, 31)).toSet)
  }

  private val sickLeaveDays2017: Set[LocalDate] = {
    Set(LocalDate.of(2017, 8, 21))
  }

  private val defaultTasksPerDay2017: Map[LocalDate, Map[String, Int]] = {
    getYearAsPeriodAsLocalDateSeq(2017)
      .map(d => d -> Map("XBRL-validator" -> 8))
      .toMap
  }

  // Pass 2

  // TODO Check with agenda (e.g. recurring meetings) and commits in Github

  private val refinementsPerDay2017: Map[LocalDate, Map[String, Int]] = {
    Map(
      LocalDate.of(2017, 1, 3) -> Map("sprint-demo" -> 4),
      LocalDate.of(2017, 1, 4) -> Map("blockchain-preparation" -> 1),
      LocalDate.of(2017, 1, 12) -> Map("blockchain-training" -> 8),
      LocalDate.of(2017, 1, 13) -> Map("blockchain-training" -> 8),
      LocalDate.of(2017, 1, 17) -> Map("sprint-demo" -> 4),
      LocalDate.of(2017, 1, 20) -> Map("performance-review" -> 1),
      LocalDate.of(2017, 1, 26) -> Map("blockchain-preparation" -> 8),
      LocalDate.of(2017, 1, 30) -> Map("training-sharepoint" -> 1),
      LocalDate.of(2017, 1, 31) -> Map("sprint-demo" -> 4),
      LocalDate.of(2017, 2, 1) -> Map("blockchain-preparation" -> 8),
      LocalDate.of(2017, 2, 3) -> Map("blockchain-preparation" -> 1),
      LocalDate.of(2017, 2, 8) -> Map("blockchain-preparation" -> 4),
      LocalDate.of(2017, 2, 9) -> Map("blockchain-hackathon" -> 8),
      LocalDate.of(2017, 2, 10) -> Map("blockchain-hackathon" -> 8),
      LocalDate.of(2017, 2, 13) -> Map("blockchain-hackathon" -> 8),
      LocalDate.of(2017, 2, 14) -> Map("sprint-demo" -> 4),
      LocalDate.of(2017, 2, 20) -> Map("HR" -> 1, "new-year" -> 3),
      LocalDate.of(2017, 2, 24) -> Map("brainstorm-session" -> 1),
      LocalDate.of(2017, 2, 28) -> Map("sprint-demo" -> 4),
      LocalDate.of(2017, 3, 14) -> Map("sprint-demo" -> 4),
      LocalDate.of(2017, 3, 17) -> Map("interview-applicant" -> 2),
      LocalDate.of(2017, 3, 24) -> Map("bila" -> 1),
      LocalDate.of(2017, 3, 28) -> Map("sprint-demo" -> 4, "interview-applicant" -> 2),
      LocalDate.of(2017, 4, 11) -> Map("sprint-demo" -> 4),
      LocalDate.of(2017, 4, 14) -> Map("team-data" -> 8),
      LocalDate.of(2017, 4, 18) -> Map("team-data" -> 6),
      LocalDate.of(2017, 4, 25) -> Map("sprint-demo" -> 4),
      LocalDate.of(2017, 5, 2) -> Map("team-data" -> 8),
      LocalDate.of(2017, 5, 9) -> Map("sprint-demo" -> 4),
      LocalDate.of(2017, 5, 10) -> Map("team-data" -> 5, "knowledge-session" -> 2),
      LocalDate.of(2017, 5, 12) -> Map("team-data" -> 6),
      LocalDate.of(2017, 5, 15) -> Map("bila" -> 1),
      LocalDate.of(2017, 5, 16) -> Map("team-data" -> 8),
      LocalDate.of(2017, 5, 17) -> Map("team-data" -> 5),
      LocalDate.of(2017, 5, 18) -> Map("interview-applicant" -> 2),
      LocalDate.of(2017, 5, 19) -> Map("bila" -> 1),
      LocalDate.of(2017, 5, 23) -> Map("sprint-demo" -> 4),
      LocalDate.of(2017, 5, 24) -> Map("team-data" -> 5),
      LocalDate.of(2017, 6, 6) -> Map("sprint-demo" -> 4),
      LocalDate.of(2017, 6, 12) -> Map("team-data" -> 3, "strategy-team-orange" -> 2),
      LocalDate.of(2017, 6, 14) -> Map("team-data" -> 5),
      LocalDate.of(2017, 6, 20) -> Map("sprint-demo" -> 4),
      LocalDate.of(2017, 7, 4) -> Map("sprint-demo" -> 4),
      LocalDate.of(2017, 8, 15) -> Map("sprint-demo" -> 4),
      LocalDate.of(2017, 8, 23) -> Map("team-data" -> 6, "bila" -> 1),
      LocalDate.of(2017, 8, 29) -> Map("sprint-demo" -> 4),
      LocalDate.of(2017, 8, 31) -> Map("farewell" -> 2, "interview-applicant" -> 2),
      LocalDate.of(2017, 9, 1) -> Map("meeting-development-process" -> 2),
      LocalDate.of(2017, 9, 12) -> Map("sprint-demo" -> 4),
      LocalDate.of(2017, 9, 18) -> Map("bila" -> 1),
      LocalDate.of(2017, 9, 26) -> Map("organisation-change" -> 2),
      LocalDate.of(2017, 10, 3) -> Map("team-data" -> 8),
      LocalDate.of(2017, 10, 10) -> Map("sprint-demo" -> 4),
      LocalDate.of(2017, 10, 16) -> Map("team-data" -> 3, "bila" -> 1),
      LocalDate.of(2017, 10, 24) -> Map("sprint-demo" -> 4),
      LocalDate.of(2017, 11, 7) -> Map("sprint-demo" -> 4),
      LocalDate.of(2017, 11, 16) -> Map("organisation-change" -> 2),
      LocalDate.of(2017, 11, 17) -> Map("farewell" -> 2),
      LocalDate.of(2017, 11, 21) -> Map("sprint-demo" -> 4),
      LocalDate.of(2017, 11, 22) -> Map("bila" -> 1, "townhall" -> 2),
      LocalDate.of(2017, 11, 24) -> Map("chapter-meeting" -> 1),
      LocalDate.of(2017, 11, 28) -> Map("meeting-about-conferences" -> 2),
      LocalDate.of(2017, 11, 30) -> Map("organisation-change" -> 2),
      LocalDate.of(2017, 12, 5) -> Map("sprint-demo" -> 4),
      LocalDate.of(2017, 12, 14) -> Map("meeting-performance-reviews" -> 2, "chapter-meeeting" -> 1),
      LocalDate.of(2017, 12, 18) -> Map("bila" -> 1),
      LocalDate.of(2017, 12, 19) -> Map("organisation-change" -> 2))
  } ensuring (_.keySet.forall(d => !isWeekend(d)), s"No weekend days allowed")
}
