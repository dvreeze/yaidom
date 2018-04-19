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

package eu.cdevreeze.yaidom.jsdemoapp

import java.time.LocalDate

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.js.annotation.JSExportTopLevel
import scala.util.Failure
import scala.util.Success

import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.raw._

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.jsdom.JsDomDocument
import eu.cdevreeze.yaidom.jsdom.JsDomElem
import scalatags.JsDom.all._

/**
 * Book list example, from https://www.saxonica.com/saxon-js/documentation/index.html#!samples/booklist,
 * but using yaidom instead of Saxon-JS.
 *
 * @author Chris de Vreeze
 */
@JSExportTopLevel("BookList")
object BookList {

  private var doc: JsDomDocument = null

  @JSExport("retrieveBookList")
  def retrieveBookList(
    bookListUri: String,
    titleHeading: HTMLHeadingElement,
    booksDiv: HTMLDivElement,
    genresDiv: HTMLDivElement): Unit = {

    Ajax.get(bookListUri) onComplete {
      case Success(xhr) =>
        val responseXml = xhr.responseXML

        val wrapperDoc = JsDomDocument(responseXml)
        doc = wrapperDoc

        println(s"Received response XML (${wrapperDoc.documentElement.findAllElemsOrSelf.size} elements)")

        displayTitle(titleHeading)
        displayBooks(booksDiv)
        displayGenres(genresDiv)
      case Failure(xhr) =>
        println(s"Could not retrieve book list at URL '$bookListUri'")
    }
  }

  def displayTitle(titleHeading: HTMLHeadingElement): Unit = {
    val today = LocalDate.now

    // We cannot yet use localized formatter patterns!
    val dayOfMonth = today.getDayOfMonth
    val rawMonth = today.getMonth.toString.toLowerCase()
    val month = rawMonth.take(1).toUpperCase() + rawMonth.drop(1)
    val year = today.getYear

    titleHeading.textContent = s"Books available at $dayOfMonth $month $year"
  }

  def displayBooks(booksDiv: HTMLDivElement): Unit = {
    def detailRow(elem: JsDomElem): HTMLTableRowElement = {
      tr(attr("data-genre") := elem.attribute(EName("CAT")))(
        td(elem.getChildElem(_.localName == "AUTHOR").text),
        td(elem.getChildElem(_.localName == "TITLE").text),
        td(attr("align") := "right")(elem.getChildElem(_.localName == "PRICE").text)).render
    }

    val docElem: JsDomElem =
      doc.ensuring(_ != null, s"Document (element) must not be null").documentElement

    val bookTable: HTMLTableElement =
      table(id := "book-table")(
        thead(
          tr(
            th("Author"),
            th("Title"),
            th(attr("data-type") := "number")("Price"))),
        tbody(
          docElem.filterElems(_.localName == "ITEM").map(e => detailRow(e)))).render

    booksDiv.appendChild(bookTable)
  }

  def displayGenres(genresDiv: HTMLDivElement): Unit = {
    def detailRow(elem: JsDomElem): HTMLTableRowElement = {
      val code = elem.attribute(EName("CODE"))

      tr(
        td(code),
        td(elem.attribute(EName("DESC"))),
        td(
          input(tpe := "checkbox", name := "genre", value := code, checked := "checked")())).render
    }

    val docElem: JsDomElem =
      doc.ensuring(_ != null, s"Document (element) must not be null").documentElement

    val genresTable: HTMLTableElement =
      table(id := "genre-table")(
        thead(
          tr(
            th("Code"),
            th("Description"))),
        tbody(
          docElem.filterElems(_.localName == "CATEGORY").map(e => detailRow(e)))).render

    val genresForm: HTMLFormElement = form(genresTable).render

    genresDiv.appendChild(genresForm)
  }
}
