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

import scala.collection.immutable

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.js.annotation.JSExportTopLevel
import scala.util.Failure
import scala.util.Success

import org.scalajs.dom.console
import org.scalajs.dom.window
import org.scalajs.dom.MouseEvent
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.html.{Option => _, _}

import eu.cdevreeze.yaidom.convert.JsDomConversions
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.jsdom.JsDomDocument
import eu.cdevreeze.yaidom.jsdom.JsDomElem
import eu.cdevreeze.yaidom.simple
import scalatags.JsDom.all._

/**
 * Book list example, from https://www.saxonica.com/saxon-js/documentation/index.html#!samples/booklist,
 * but using yaidom instead of Saxon-JS.
 *
 * TODO Try to make this code more "declarative", like the Saxon-JS original stylesheet.
 *
 * TODO Create yaidom dialect for HTML, and use that.
 *
 * @author Chris de Vreeze
 */
@JSExportTopLevel("BookList")
object BookList {

  private var bookListDoc: JsDomDocument = null

  @JSExport("retrieveBookList")
  def retrieveBookList(bookListUri: String): Unit = {
    Ajax.get(bookListUri) onComplete {
      case Success(xhr) =>
        val responseXml = xhr.responseXML

        val wrapperDoc = JsDomDocument(responseXml)
        bookListDoc = wrapperDoc

        displayTitle()
        displayBooks()
        displayGenres()

        addGenreOnclickHandlers()

        addTableHeaderEventHandlers()
      case Failure(xhr) =>
        console.error(s"Could not retrieve book list at URL '$bookListUri'")
    }
  }

  def displayTitle(): Unit = {
    val today = LocalDate.now

    // We cannot yet use localized formatter patterns!
    val dayOfMonth = today.getDayOfMonth
    val rawMonth = today.getMonth.toString.toLowerCase()
    val month = rawMonth.take(1).toUpperCase() + rawMonth.drop(1)
    val year = today.getYear

    titleHeading.textContent = s"Books available at $dayOfMonth $month $year"
  }

  def displayBooks(): Unit = {
    def detailRow(elem: JsDomElem): TableRow = {
      tr(attr("data-genre") := elem.attribute(EName("CAT")))(
        td(elem.getChildElem(_.localName == "AUTHOR").text),
        td(elem.getChildElem(_.localName == "TITLE").text),
        td(attr("align") := "right")(elem.getChildElem(_.localName == "PRICE").text)).render
    }

    val bookListDocElem: JsDomElem =
      bookListDoc.ensuring(_ != null, s"Document (element) must not be null").documentElement

    val bookTable: Table =
      table(id := "book-table")(
        thead(
          tr(
            th("Author"),
            th("Title"),
            th(attr("data-type") := "number")("Price"))),
        tbody(
          bookListDocElem.filterElems(_.localName == "ITEM").map(e => detailRow(e)))).render

    booksDiv.appendChild(bookTable)
  }

  def displayGenres(): Unit = {
    def detailRow(elem: JsDomElem): TableRow = {
      val code = elem.attribute(EName("CODE"))

      tr(
        td(code),
        td(elem.attribute(EName("DESC"))),
        td(
          input(tpe := "checkbox", name := "genre", value := code, checked := "checked")())).render
    }

    val bookListDocElem: JsDomElem =
      bookListDoc.ensuring(_ != null, s"Document (element) must not be null").documentElement

    val genresTable: Table =
      table(id := "genre-table")(
        thead(
          tr(
            th("Code"),
            th("Description"))),
        tbody(
          bookListDocElem.filterElems(_.localName == "CATEGORY").map(e => detailRow(e)))).render

    val genresForm: Form = form(genresTable).render

    genresDiv.appendChild(genresForm)
  }

  def addGenreOnclickHandlers(): Unit = {
    val genreInputElems: immutable.IndexedSeq[Input] = genresDivAsJsDomElem
      .filterElems { e =>
        e.localName.toLowerCase == "input" &&
          e.attributeOption(EName("type")).contains("checkbox") &&
          e.attributeOption(EName("name")).contains("genre")
      }
      .map(_.wrappedNode.asInstanceOf[Input])

    genreInputElems.foreach { genreInputElem =>
      genreInputElem.onclick = { (event: MouseEvent) =>
        val bookRowsForGenre = getBookRowsForGenre(genreInputElem.value)

        bookRowsForGenre.foreach { bookRow =>
          bookRow.style.display = if (genreInputElem.checked) "table-row" else "none"
        }
      }
    }
  }

  def addTableHeaderEventHandlers(): Unit = {
    val tableHeaderCellElems = getTableHeaderCellsAsJsDomElems

    addTableHeaderOnclickHandlers(tableHeaderCellElems)

    addTableHeaderOnMouseoverHandlers(tableHeaderCellElems)
    addTableHeaderOnMouseoutHandlers(tableHeaderCellElems)
  }

  def addTableHeaderOnclickHandlers(headerCellElems: immutable.IndexedSeq[JsDomElem]): Unit = {
    headerCellElems.foreach { headerCellElem =>
      val headerCell = headerCellElem.wrappedNode.asInstanceOf[TableCell]

      headerCell.onclick = { (event: MouseEvent) =>
        val headerCellJsDomElem =
          htmlAsJsDomDocument.documentElement.findElem(_.path == headerCellElem.path).getOrElse(sys.error(s"Could not find back header cell"))

        val tableId = headerCellJsDomElem.findAncestor(_.localName.toLowerCase == "table").get.attribute(EName("id"))

        val colNrOneBased = headerCellJsDomElem.path.lastEntry.position

        val dataType =
          if (headerCellJsDomElem.attributeOption(EName("data-type")).contains("number")) "number" else "text"

        val ascending =
          !headerCellJsDomElem.parent.parent.attributeOption(EName("data-order")).map(_.toInt).contains(colNrOneBased)

        sortTable(tableId, colNrOneBased, dataType, ascending)
      }
    }
  }

  def addTableHeaderOnMouseoverHandlers(headerCellElems: immutable.IndexedSeq[JsDomElem]): Unit = {
    headerCellElems
      .foreach { headerCellElem =>
        val headerCell = headerCellElem.wrappedNode.asInstanceOf[TableCell]

        headerCell.onmouseover = { (event: MouseEvent) =>
          sortToolTipDiv.style.left = s"${event.clientX + 30}px"
          sortToolTipDiv.style.top = s"${event.clientY - 15}px"
          sortToolTipDiv.style.visibility = "visible"
        }
      }
  }

  def addTableHeaderOnMouseoutHandlers(headerCellElems: immutable.IndexedSeq[JsDomElem]): Unit = {
    headerCellElems
      .foreach { headerCellElem =>
        val headerCell = headerCellElem.wrappedNode.asInstanceOf[TableCell]

        headerCell.onmouseout = { (event: MouseEvent) =>
          sortToolTipDiv.style.visibility = "hidden"
        }
      }
  }

  // Private methods

  private def getBookRowsForGenre(genre: String): immutable.IndexedSeq[TableRow] = {
    booksDivAsJsDomElem
      .filterElems { e =>
        e.localName.toLowerCase == "tr" && e.attributeOption(EName("data-genre")).contains(genre)
      }
      .map(_.wrappedNode.asInstanceOf[TableRow])
  }

  // Generic table sort

  private def sortTable(tableId: String, columnNumberOneBased: Int, dataType: String, ascending: Boolean): Unit = {
    console.log(s"Sorting table. ID: $tableId, col: $columnNumberOneBased, type: $dataType, ascending: $ascending")

    val tableElem: JsDomElem =
      htmlAsJsDomDocument.documentElement
        .findElem(e => e.localName.toLowerCase == "table" && e.attributeOption(EName("id")).contains(tableId))
        .getOrElse(sys.error(s"Missing table with ID '$tableId'"))

    // Convert the table element to a yaidom native simple element, sort the rows, and convert back

    val tableSimpleElem: simple.Elem =
      JsDomConversions.convertToElem(tableElem.wrappedNode, tableElem.parent.scope)

    val editedTableSimpleElem: simple.Elem =
      tableSimpleElem
        .transformElems { e =>
          e.localName.toLowerCase match {
            case "thead" =>
              e.plusAttribute(
                QName("data-order"),
                (if (ascending) columnNumberOneBased else -columnNumberOneBased).toString)
            case "tbody" =>
              val preSortedRows = e.findAllChildElems
                .sortBy { che =>
                  require(che.localName.toLowerCase == "tr", s"Expected 'tr' element but found '${che.localName}'")

                  val tdElem = che.filterElems(_.localName.toLowerCase == "td").apply(columnNumberOneBased - 1)

                  if (dataType == "number") f"${tdElem.text.toDouble}%08.2f" else tdElem.text
                }

              val sortedRows = if (ascending) preSortedRows else preSortedRows.reverse

              e.withChildren(sortedRows)
            case _ =>
              e
          }
        }

    val editedTableElem: JsDomElem =
      JsDomElem(JsDomConversions.convertElem(editedTableSimpleElem, htmlAsJsDomDocument.wrappedDocument, tableElem.parent.scope))

    val parentElem: JsDomElem = tableElem.parent
    parentElem.wrappedNode.replaceChild(editedTableElem.wrappedNode, tableElem.wrappedNode)

    // Note that we have new table content (as far as object identity is concerned), so re-register event handlers

    addTableHeaderEventHandlers()
    addGenreOnclickHandlers()
  }

  // Getting specific parts of the HTML DOM

  private def htmlAsJsDomDocument: JsDomDocument = {
    JsDomDocument(window.document)
  }

  private def getTableHeaderCellsAsJsDomElems: immutable.IndexedSeq[JsDomElem] = {
    htmlAsJsDomDocument
      .documentElement
      .filterElems(_.localName.toLowerCase == "th")
  }

  private def titleHeadingAsJsDomElem: JsDomElem = {
    findElemOrSelfByNameAndId("h1", "title").get
  }

  private def titleHeading: Heading = {
    titleHeadingAsJsDomElem.wrappedNode.asInstanceOf[Heading]
  }

  private def booksDivAsJsDomElem: JsDomElem = {
    findElemOrSelfByNameAndId("div", "books").get
  }

  private def booksDiv: Div = {
    booksDivAsJsDomElem.wrappedNode.asInstanceOf[Div]
  }

  private def genresDivAsJsDomElem: JsDomElem = {
    findElemOrSelfByNameAndId("div", "genres").get
  }

  private def genresDiv: Div = {
    genresDivAsJsDomElem.wrappedNode.asInstanceOf[Div]
  }

  private def sortToolTipDivAsJsDomElem: JsDomElem = {
    findElemOrSelfByNameAndId("div", "sortToolTip").get
  }

  private def sortToolTipDiv: Div = {
    sortToolTipDivAsJsDomElem.wrappedNode.asInstanceOf[Div]
  }

  // Helper method for getting DOM content

  private def findElemOrSelfByNameAndId(localName: String, id: String): Option[JsDomElem] = {
    htmlAsJsDomDocument
      .documentElement
      .findElemOrSelf(e => e.localName.toLowerCase == localName && e.attributeOption(EName("id")).contains(id))
  }
}
