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
import java.time.LocalDate

import scala.collection.immutable
import scala.reflect.classTag

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.indexed
import eu.cdevreeze.yaidom.print.DocumentPrinterUsingDom
import eu.cdevreeze.yaidom.queryapi._
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

  private val year = 2017

  test("testCreateXml") {
    val dates = getDatesReversed(Nil, year).reverse.toIndexedSeq

    val nonWeekendDays = dates.filter(d => !isWeekend(d))

    import resolved.Node._

    val emptyTimesheetElem: resolved.Elem =
      emptyElem(EName("timesheet"))
        .plusChildren(
          nonWeekendDays.map { day =>
            emptyElem(EName("day"))
              .plusAttribute(EName("date"), day.toString)
              .plusAttribute(EName("dayOfWeek"), day.getDayOfWeek.toString)
          })

    val rawTimesheetElem = emptyTimesheetElem transformChildElems {
      case elm if elm.localName == "day" =>
        val day = TimesheetElem(indexed.Elem(simple.Elem.from(elm, Scope.Empty))).asInstanceOf[Day]

        require(!isWeekend(day.date), s"Saturday or Sunday: ${day.date}")

        if (isDutchHoliday(day.date)) {
          addTask(elm, "official-holiday", 8)
        } else if (onVacation(day.date)) {
          addTask(elm, "vacation", 8)
        } else if (onSickLeave(day.date)) {
          addTask(elm, "sick", 8)
        } else if (mayBeWorkingOnValidator(day.date)) {
          addTask(elm, "XBRL-validator", 8)
        } else {
          elm
        }
      case elm =>
        elm
    }

    val timesheetElem =
      removeTasksWithoutHours(
        rawTimesheetElem transformChildElems {
          case elm if elm.localName == "day" =>
            val day = TimesheetElem(indexed.Elem(simple.Elem.from(elm, Scope.Empty))).asInstanceOf[Day]

            require(!isWeekend(day.date), s"Saturday or Sunday: ${day.date}")

            if (isBlockchainTrainingDay(day.date)) {
              tryToAddTaskWithoutChangingTotalHours(elm, "blockchain-training", 8)
            } else if (isBlockchainPreparationDay(day.date)) {
              tryToAddTaskWithoutChangingTotalHours(elm, "blockchain-preparation", 8)
            } else if (day.date == LocalDate.of(year, 2, 8)) {
              tryToAddTaskWithoutChangingTotalHours(elm, "blockchain-meeting", 3)
            } else if (isBlockchainEvent(day.date)) {
              tryToAddTaskWithoutChangingTotalHours(elm, "blockchain-event", 8)
            } else if (teamDataHours.contains(day.date)) {
              tryToAddTaskWithoutChangingTotalHours(elm, "team-data", teamDataHours(day.date))
            } else {
              elm
            }
          case elm =>
            elm
        })

    assertResult(true) {
      timesheetElem.filterElems(_.localName == "day").size >= 250
    }

    val timesheetSimpleElem = simple.Elem.from(timesheetElem, Scope.Empty)

    val timesheet = Timesheet(indexed.Elem(timesheetSimpleElem))

    assertResult(resolved.Elem.from(timesheetElem)) {
      resolved.Elem.from(timesheet)
    }

    assertResult(true) {
      timesheet.days.count(_.dayOfWeek == DayOfWeek.MONDAY) >= 35 &&
        timesheet.days.count(_.dayOfWeek == DayOfWeek.MONDAY) < 70
    }

    val docPrinter = DocumentPrinterUsingDom.newInstance()

    val xmlString = docPrinter.print(simple.Document(timesheetSimpleElem.prettify(2)))
    println(xmlString)
  }

  private def getDatesReversed(accDates: List[LocalDate], year: Int): List[LocalDate] = {
    if (accDates.isEmpty) {
      getDatesReversed(List(LocalDate.of(year, 1, 1)), year)
    } else {
      val nextDay = accDates.head.plusDays(1)

      if (nextDay.getYear == year) getDatesReversed(nextDay :: accDates, year) else accDates
    }
  }

  private def isWeekend(date: LocalDate): Boolean = {
    (date.getDayOfWeek == DayOfWeek.SATURDAY) || (date.getDayOfWeek == DayOfWeek.SUNDAY)
  }

  // Specific dates

  // TODO Check with agenda (e.g. recurring meetings) and commits in Github

  private def onLeave(date: LocalDate): Boolean = {
    !isWeekend(date) && (isDutchHoliday(date) || onVacation(date) || onSickLeave(date))
  }

  private def isDutchHoliday(date: LocalDate): Boolean = {
    Set(
      LocalDate.of(year, 1, 1),
      LocalDate.of(year, 4, 16),
      LocalDate.of(year, 4, 17),
      LocalDate.of(year, 4, 27),
      LocalDate.of(year, 5, 5),
      LocalDate.of(year, 5, 25),
      LocalDate.of(year, 6, 4),
      LocalDate.of(year, 6, 5),
      LocalDate.of(year, 12, 25),
      LocalDate.of(year, 12, 26)).contains(date)
  }

  private def onVacation(date: LocalDate): Boolean = {
    (date == LocalDate.of(year, 3, 3)) ||
      (date == LocalDate.of(year, 5, 26)) ||
      (date == LocalDate.of(year, 6, 2)) ||
      (date.isAfter(LocalDate.of(year, 7, 20)) && date.isBefore(LocalDate.of(year, 8, 12))) ||
      (date.isAfter(LocalDate.of(year, 12, 21)) && date.getYear == year)
  }

  private def onSickLeave(date: LocalDate): Boolean = {
    date == LocalDate.of(year, 8, 21)
  }

  private def mayBeWorkingOnValidator(date: LocalDate): Boolean = {
    date.getYear == year
  }

  private def isBlockchainTrainingDay(date: LocalDate): Boolean = {
    (date == LocalDate.of(year, 1, 12)) || (date == LocalDate.of(year, 1, 13))
  }

  private def isBlockchainPreparationDay(date: LocalDate): Boolean = {
    (date == LocalDate.of(year, 1, 26)) || (date == LocalDate.of(year, 2, 1))
  }

  private def isBlockchainEvent(date: LocalDate): Boolean = {
    date.isAfter(LocalDate.of(year, 2, 8)) && date.isBefore(LocalDate.of(year, 2, 14))
  }

  private val teamDataHours: Map[LocalDate, Int] =
    Map(
      LocalDate.of(year, 4, 14) -> 8,
      LocalDate.of(year, 4, 18) -> 6,
      LocalDate.of(year, 5, 2) -> 8,
      LocalDate.of(year, 5, 10) -> 5,
      LocalDate.of(year, 5, 12) -> 6,
      LocalDate.of(year, 5, 16) -> 8,
      LocalDate.of(year, 5, 17) -> 5,
      LocalDate.of(year, 5, 24) -> 5,
      LocalDate.of(year, 6, 12) -> 3,
      LocalDate.of(year, 6, 14) -> 5,
      LocalDate.of(year, 8, 23) -> 6,
      LocalDate.of(year, 10, 3) -> 8)

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

    require(!onLeave(day.date), s"Cannot add task (on ${day.date} the employee is on leave)")

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
}
