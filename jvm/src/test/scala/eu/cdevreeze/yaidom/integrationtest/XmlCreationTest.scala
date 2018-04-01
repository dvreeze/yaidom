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
import java.time.format.TextStyle
import java.util.Locale

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.print.DocumentPrinterUsingDom
import eu.cdevreeze.yaidom.resolved
import eu.cdevreeze.yaidom.simple

/**
 * XML creation test, using resolved elements to quickly and easily create some XML snippet.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class XmlCreationTest extends FunSuite with BeforeAndAfterAll {

  private val year = 2017

  test("testCreateXml") {
    val dates = getDatesReversed(Nil, year).reverse.toIndexedSeq

    val workDays = dates.filter(isWorkDay)

    import resolved.Node._

    val emptyTimesheet =
      emptyElem(EName("timesheet"))
        .plusChildren(
          workDays.map { day =>
            emptyElem(EName("day"))
              .plusAttribute(EName("date"), day.toString)
              .plusAttribute(EName("dayOfWeek"), day.getDayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault))
          })

    val timesheet = emptyTimesheet

    assertResult(true) {
      timesheet.filterElems(_.localName == "day").size >= 250
    }

    val timesheetSimpleElem = simple.Elem.from(timesheet, Scope.Empty)

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

  private def isWorkDay(date: LocalDate): Boolean = {
    (date.getDayOfWeek != DayOfWeek.SATURDAY) && (date.getDayOfWeek != DayOfWeek.SUNDAY) &&
      !isDutchHoliday(date)
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
}
