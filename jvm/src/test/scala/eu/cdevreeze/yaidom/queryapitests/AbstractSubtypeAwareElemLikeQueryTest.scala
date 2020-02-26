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

package eu.cdevreeze.yaidom.queryapitests

import scala.collection.immutable
import scala.reflect.classTag

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.queryapi.BackingNodes
import eu.cdevreeze.yaidom.queryapi.ScopedElemLike
import eu.cdevreeze.yaidom.queryapi.SubtypeAwareElemLike
import eu.cdevreeze.yaidom.queryapitests.AbstractSubtypeAwareElemLikeQueryTest._
import org.scalatest.funsuite.AnyFunSuite

/**
 * AbstractSubtypeAwareElemLike-based query test case, showing how to support custom XML dialects offering the
 * SubtypeAwareElemApi API, without depending on just one backing element implementation.
 *
 * The backing element implementation is abstracted over by using the `BackingNodes.Elem` element abstraction.
 *
 * This test case shows how yaidom can be used to support specific XML dialects in a reasonably type-safe manner,
 * while allowing for multiple backing element implementations.
 *
 * @author Chris de Vreeze
 */
abstract class AbstractSubtypeAwareElemLikeQueryTest extends AnyFunSuite {

  protected val wrappedDocumentContent: BackingNodes.Elem

  test("testQueryTable") {
    val documentContent = new DocumentContent(wrappedDocumentContent)

    val tables = documentContent.findAllElemsOfType(classTag[Table])

    val expectedAncestryOrSelfENames =
      List(
        EName(OfficeNs, "document-content"),
        EName(OfficeNs, "body"),
        EName(OfficeNs, "spreadsheet"),
        EName(TableNs, "table"))

    assertResult(Set(expectedAncestryOrSelfENames)) {
      tables.map(_.reverseAncestryOrSelfENames).toSet
    }

    val firstTable = tables.head

    assertResult(Set("co1", "co2")) {
      firstTable.columns.map(_.styleName).toSet
    }
    assertResult(Set("ro1")) {
      firstTable.rows.map(_.styleName).toSet
    }

    assertResult(true) {
      firstTable.rows.flatMap(_.cells).exists(_.cellText == "JavaServer Pages")
    }

    assertResult(documentContent.findAllElemsOrSelf.map(_.resolvedName)) {
      eu.cdevreeze.yaidom.resolved.Elem.from(documentContent.backingElem).findAllElemsOrSelf.map(_.resolvedName)
    }
  }

  test("testEqualities") {
    val documentContent = new DocumentContent(wrappedDocumentContent)

    assertResult(documentContent.findAllElemsOfType(classTag[TableCell])) {
      documentContent.findAllElemsOfType(classTag[Table]).flatMap(_.rows).flatMap(_.cells)
    }

    assertResult(documentContent.findAllElemsOfType(classTag[Table]).flatMap(_.rows).flatMap(_.cells).flatMap(_.findAllElemsOfType(classTag[Paragraph]))) {
      documentContent.filterElemsOrSelfOfType(classTag[Paragraph]) { e =>
        val ancestryOrSelfENames = e.reverseAncestryOrSelfENames
        Set("table", "table-row", "table-cell", "p").subsetOf(ancestryOrSelfENames.map(_.localPart).toSet)
      }
    }
  }
}

object AbstractSubtypeAwareElemLikeQueryTest {

  val OfficeNs = "urn:oasis:names:tc:opendocument:xmlns:office:1.0"
  val StyleNs = "urn:oasis:names:tc:opendocument:xmlns:style:1.0"
  val TableNs = "urn:oasis:names:tc:opendocument:xmlns:table:1.0"
  val TextNs = "urn:oasis:names:tc:opendocument:xmlns:text:1.0"

  /**
   * Super-class of elements in an ODS spreadsheet content.xml file. It offers the `SubtypeAwareElemApi` API, among
   * other query API traits.
   */
  sealed class SpreadsheetElem(val backingElem: BackingNodes.Elem) extends ScopedElemLike with SubtypeAwareElemLike {

    type ThisElem = SpreadsheetElem

    def thisElem: ThisElem = this

    final def findAllChildElems: immutable.IndexedSeq[SpreadsheetElem] =
      backingElem.findAllChildElems.map(e => SpreadsheetElem(e))

    final def resolvedName: EName = backingElem.resolvedName

    final def resolvedAttributes: immutable.Iterable[(EName, String)] = backingElem.resolvedAttributes

    final def qname: QName = backingElem.qname

    final def attributes: immutable.Iterable[(QName, String)] = backingElem.attributes

    final def scope: Scope = backingElem.scope

    final def text: String = backingElem.text

    final def reverseAncestryOrSelfENames: immutable.IndexedSeq[EName] = {
      backingElem.rootElem.resolvedName +: backingElem.path.entries.map(_.elementName)
    }

    override def equals(other: Any): Boolean = other match {
      case e: SpreadsheetElem => backingElem == e.backingElem
      case _ => false
    }

    override def hashCode: Int = backingElem.hashCode
  }

  final class DocumentContent(backingElem: BackingNodes.Elem) extends SpreadsheetElem(backingElem) {
    require(resolvedName == EName(Some(OfficeNs), "document-content"))
  }

  final class Body(backingElem: BackingNodes.Elem) extends SpreadsheetElem(backingElem) {
    require(resolvedName == EName(Some(OfficeNs), "body"))
  }

  final class Spreadsheet(backingElem: BackingNodes.Elem) extends SpreadsheetElem(backingElem) {
    require(resolvedName == EName(Some(OfficeNs), "spreadsheet"))
  }

  final class Table(backingElem: BackingNodes.Elem) extends SpreadsheetElem(backingElem) with HasStyle {
    require(resolvedName == EName(Some(TableNs), "table"))

    def columns: immutable.IndexedSeq[TableColumn] = findAllChildElemsOfType(classTag[TableColumn])

    def rows: immutable.IndexedSeq[TableRow] = findAllChildElemsOfType(classTag[TableRow])
  }

  final class TableRow(backingElem: BackingNodes.Elem) extends SpreadsheetElem(backingElem) with HasStyle {
    require(resolvedName == EName(Some(TableNs), "table-row"))

    def cells: immutable.IndexedSeq[TableCell] = findAllChildElemsOfType(classTag[TableCell])
  }

  final class TableColumn(backingElem: BackingNodes.Elem) extends SpreadsheetElem(backingElem) with HasStyle {
    require(resolvedName == EName(Some(TableNs), "table-column"))
  }

  final class TableCell(backingElem: BackingNodes.Elem) extends SpreadsheetElem(backingElem) with HasStyle {
    require(resolvedName == EName(Some(TableNs), "table-cell"))

    def cellText: String = findAllChildElemsOfType(classTag[Paragraph]).map(_.text).mkString
  }

  final class Paragraph(backingElem: BackingNodes.Elem) extends SpreadsheetElem(backingElem) with HasStyle {
    require(resolvedName == EName(Some(TextNs), "p"))
  }

  trait HasStyle extends SpreadsheetElem {

    final def styleName: String = attributeOption(EName(TableNs, "style-name")).getOrElse("")
  }

  object SpreadsheetElem {

    def apply(elem: BackingNodes.Elem): SpreadsheetElem = {
      elem.resolvedName match {
        case EName(Some(OfficeNs), "document-content") =>
          new DocumentContent(elem)
        case EName(Some(OfficeNs), "body") =>
          new Body(elem)
        case EName(Some(OfficeNs), "spreadsheet") =>
          new Spreadsheet(elem)
        case EName(Some(TableNs), "table") =>
          new Table(elem)
        case EName(Some(TableNs), "table-row") =>
          new TableRow(elem)
        case EName(Some(TableNs), "table-column") =>
          new TableColumn(elem)
        case EName(Some(TableNs), "table-cell") =>
          new TableCell(elem)
        case EName(Some(TextNs), "p") =>
          new Paragraph(elem)
        case _ =>
          new SpreadsheetElem(elem)
      }
    }
  }

}
