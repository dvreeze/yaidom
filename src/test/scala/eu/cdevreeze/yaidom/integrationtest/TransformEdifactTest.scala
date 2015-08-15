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

import scala.Vector
import scala.collection.immutable

import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.Suite
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.indexed
import eu.cdevreeze.yaidom.parse.DocumentParserUsingSax
import eu.cdevreeze.yaidom.print.DocumentPrinterUsingSax
import eu.cdevreeze.yaidom.resolved
import eu.cdevreeze.yaidom.simple.Elem
import eu.cdevreeze.yaidom.simple.Node.elem
import eu.cdevreeze.yaidom.simple.Node.emptyElem
import eu.cdevreeze.yaidom.simple.Node.textElem
import TransformEdifactTest._

/**
 * Transformation test for EDIFACT messages.
 *
 * The source of the transformation is a Smooks XML representation of a GOVCBR EDIFACT message, for declaration type
 * MAI. The target of the transformation conforms to the WCO Data Model.
 *
 * Note that we could have written the entire transformation, regardless of the declaration type. Per declaration type,
 * a simple check specific to that declaration type would suffice. On the other hand, this test case is just an XML
 * transformation test case using an EDIFACT example.
 *
 * See MIG-NL_Single_Window_Section_2_B2SW_Data_Model_01A01.doc, and for declaration type MAI, see
 * MIG-NL_Single_Window_Section_2_B2SW-MAI_01A01.doc.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class TransformEdifactTest extends Suite {

  private val docParser = DocumentParserUsingSax.newInstance
  private val docPrinter = DocumentPrinterUsingSax.newInstance

  @Test def testTransform(): Unit = {
    val edifactDoc =
      indexed.Document(docParser.parse(classOf[TransformEdifactTest].getResource("edifact/sample-edifact.xml").toURI))

    val declarationElem = makeDeclaration(edifactDoc.documentElement)

    val declarationString = docPrinter.print(declarationElem.prettify(2))
    println(declarationString)

    val expectedDeclarationElem =
      docParser.parse(classOf[TransformEdifactTest].getResource("edifact/sample-declaration.xml").toURI).documentElement

    assertResult(expectedDeclarationElem.findAllElemsOrSelf.map(_.resolvedName)) {
      declarationElem.findAllElemsOrSelf.map(_.resolvedName)
    }

    assertResult(resolved.Elem(removeAllEmptyText(expectedDeclarationElem)).removeAllInterElementWhitespace.coalesceAndNormalizeAllText) {
      resolved.Elem(removeAllEmptyText(declarationElem)).removeAllInterElementWhitespace.coalesceAndNormalizeAllText
    }
  }

  private def makeDeclaration(edifactElem: indexed.Elem): Elem = {
    require(edifactElem.resolvedName == EName(EnvNs, "unEdifact"))

    val govcbrRelativeReverseAncestryOrSelf = List(EName(EnvNs, "interchangeMessage"), EName(GovcbrNs, "GOVCBR"))

    emptyElem(QName("Declaration"), TargetScope) withChildSeqs {
      Vector(
        Vector(
          textElem(
            QName("FunctionCode"),
            TargetScope,
            edifactElem.findElem(
              nestedIn(edifactElem).as(govcbrRelativeReverseAncestryOrSelf ++ List(EName(GovcbrNs, "BGM"), EName(CNs, "e1225")))).get.text)),
        Vector(
          textElem(
            QName("FunctionalReferenceID"),
            TargetScope,
            edifactElem.findElem(
              nestedIn(edifactElem).as(govcbrRelativeReverseAncestryOrSelf ++ List(EName(GovcbrNs, "RFF"), EName(CNs, "C506"), EName(CNs, "e1154")))).get.text)),
        Vector(
          textElem(
            QName("IssueDateTime"),
            TargetScope,
            edifactElem.findElem(
              nestedIn(edifactElem).as(govcbrRelativeReverseAncestryOrSelf ++ List(EName(GovcbrNs, "DTM"), EName(CNs, "C507"), EName(CNs, "e2380")))).get.text)),
        Vector(
          textElem(
            QName("TypeCode"),
            TargetScope,
            edifactElem.findElem(
              nestedIn(edifactElem).as(govcbrRelativeReverseAncestryOrSelf ++ List(EName(GovcbrNs, "BGM"), EName(CNs, "C002"), EName(CNs, "e1001")))).get.text)),
        Vector(
          textElem(
            QName("VersionID"),
            TargetScope,
            edifactElem.findElem(
              nestedIn(edifactElem).as(govcbrRelativeReverseAncestryOrSelf ++ List(EName(GovcbrNs, "BGM"), EName(CNs, "C106"), EName(CNs, "e1056")))).get.text)),
        {
          val elemOption =
            edifactElem.findElem(
              nestedIn(edifactElem).as(govcbrRelativeReverseAncestryOrSelf ++ List(EName(GovcbrNs, "Segment_group_34"))))

          elemOption.toVector map { elm =>
            makeBorderTransportMeans(elm)
          }
        },
        Vector(
          makeDeclarant(
            edifactElem.filterElems(
              nestedIn(edifactElem).as(govcbrRelativeReverseAncestryOrSelf ++ List(EName(GovcbrNs, "Segment_group_7")))).filter(e =>
                e.findElem(_.resolvedName == EName(CNs, "e3035")).map(_.text).getOrElse("") == "DT").head)),
        makeOptPreviousDocument(edifactElem.findElem(nestedIn(edifactElem).as(govcbrRelativeReverseAncestryOrSelf)).get).toVector)
    }
  }

  private def makeBorderTransportMeans(edifactElem: indexed.Elem): Elem = {
    require(edifactElem.resolvedName == EName(GovcbrNs, "Segment_group_34"))

    emptyElem(QName("BorderTransportMeans"), TargetScope) withChildSeqs {
      Vector(
        Vector(
          textElem(
            QName("ID"),
            TargetScope,
            edifactElem.findElem(
              nestedIn(edifactElem).as(List(EName(GovcbrNs, "TDT"), EName(CNs, "C222"), EName(CNs, "e8213")))).get.text)),
        Vector(
          textElem(
            QName("IdentificationTypeCode"),
            TargetScope,
            edifactElem.findElem(
              nestedIn(edifactElem).as(List(EName(GovcbrNs, "TDT"), EName(CNs, "C222"), EName(CNs, "e1131")))).get.text)),
        Vector(
          textElem(
            QName("TypeCode"),
            TargetScope,
            edifactElem.findElem(
              nestedIn(edifactElem).as(List(EName(GovcbrNs, "TDT"), EName(CNs, "C001"), EName(CNs, "e8179")))).get.text)),
        {
          val elemOption =
            edifactElem.findElem(
              nestedIn(edifactElem).as(List(EName(GovcbrNs, "RFF"), EName(CNs, "C506"), EName(CNs, "e1154"))))

          elemOption.toVector map { elm =>
            textElem(QName("StayID"), TargetScope, elm.text)
          }
        },
        Vector(
          makeBorderTransportMeansItinerary(
            edifactElem.filterElems(
              nestedIn(edifactElem).as(List(EName(GovcbrNs, "Segment_group_36")))).filter(e =>
                e.findElem(_.resolvedName == EName(CNs, "e3227")).map(_.text).getOrElse("") == "153").head)))
    }
  }

  private def makeBorderTransportMeansItinerary(edifactElem: indexed.Elem): Elem = {
    require(edifactElem.resolvedName == EName(GovcbrNs, "Segment_group_36"))
    require(edifactElem.findElem(_.resolvedName == EName(CNs, "e3227")).map(_.text).getOrElse("") == "153")

    emptyElem(QName("Itinerary"), TargetScope) withChildSeqs {
      Vector(
        Vector(
          textElem(
            QName("ID"),
            TargetScope,
            edifactElem.findElem(
              nestedIn(edifactElem).as(List(EName(GovcbrNs, "LOC"), EName(CNs, "C517"), EName(CNs, "e3225")))).get.text)))
    }
  }

  private def makeDeclarant(edifactElem: indexed.Elem): Elem = {
    require(edifactElem.resolvedName == EName(GovcbrNs, "Segment_group_7"))
    require(edifactElem.findElem(_.resolvedName == EName(CNs, "e3035")).map(_.text).getOrElse("") == "DT")

    emptyElem(QName("Declarant"), TargetScope) withChildSeqs {
      Vector(
        Vector(
          textElem(
            QName("Name"),
            TargetScope,
            edifactElem.findElem(nestedIn(edifactElem).as(List(EName(GovcbrNs, "NAD"), EName(CNs, "C080"), EName(CNs, "e3036")))).map(_.text).getOrElse(""))),
        Vector(
          textElem(
            QName("ID"),
            TargetScope,
            edifactElem.findElem(
              nestedIn(edifactElem).as(List(EName(GovcbrNs, "NAD"), EName(CNs, "C082"), EName(CNs, "e3039")))).get.text)),
        Vector(
          textElem(
            QName("RoleCode"),
            TargetScope,
            edifactElem.findElem(
              nestedIn(edifactElem).as(List(EName(GovcbrNs, "NAD"), EName(CNs, "e3035")))).get.text)),
        {
          val sgElemOption =
            edifactElem.filterElems(
              nestedIn(edifactElem).as(List(EName(GovcbrNs, "Segment_group_8")))).filter(e =>
                e.findElem(_.resolvedName == EName(CNs, "e3139")).map(_.text).getOrElse("") == "IC").headOption

          val elemOption = sgElemOption flatMap { sgElem =>
            sgElem.findElem(nestedIn(sgElem).as(List(EName(GovcbrNs, "CTA"), EName(CNs, "C056"), EName(CNs, "e3412"))))
          }

          Vector(
            elem(
              QName("Contact"),
              TargetScope,
              Vector(textElem(
                QName("Name"),
                TargetScope,
                elemOption.map(_.text).getOrElse("")))))
        },
        {
          val sgElemOption =
            edifactElem.filterElems(
              nestedIn(edifactElem).as(List(EName(GovcbrNs, "Segment_group_8")))).filter(e =>
                e.findElem(_.resolvedName == EName(CNs, "e3139")).map(_.text).getOrElse("") == "AH").headOption

          val elems = sgElemOption.toVector flatMap { sgElem =>
            sgElem.filterElems(nestedIn(sgElem).as(List(EName(GovcbrNs, "COM"))))
          }

          elems map { elem => makeDeclarantCommunication(elem) }
        })
    }
  }

  private def makeDeclarantCommunication(edifactElem: indexed.Elem): Elem = {
    require(edifactElem.resolvedName == EName(GovcbrNs, "COM"))

    emptyElem(QName("Communication"), TargetScope) withChildSeqs {
      Vector(
        Vector(
          textElem(
            QName("ID"),
            TargetScope,
            edifactElem.findElem(
              nestedIn(edifactElem).as(List(EName(CNs, "C076"), EName(CNs, "e3148")))).get.text)),
        Vector(
          textElem(
            QName("TypeCode"),
            TargetScope,
            edifactElem.findElem(
              nestedIn(edifactElem).as(List(EName(CNs, "C076"), EName(CNs, "e3155")))).get.text)))
    }
  }

  private def makeOptPreviousDocument(edifactElem: indexed.Elem): Option[Elem] = {
    require(edifactElem.resolvedName == EName(GovcbrNs, "GOVCBR"))

    val sgElemOption =
      edifactElem.filterElems(
        nestedIn(edifactElem).as(List(EName(GovcbrNs, "Segment_group_9")))).filter(e =>
          e.findElem(_.resolvedName == EName(CNs, "e1001")).map(_.text).getOrElse("") == "998").headOption

    val elemOption = sgElemOption flatMap { sgElem =>
      sgElem.findElem(nestedIn(sgElem).as(List(EName(GovcbrNs, "DOC"), EName(CNs, "C503"), EName(CNs, "e1004"))))
    }

    elemOption map { elm =>
      emptyElem(QName("PreviousDocument"), TargetScope) withChildSeqs {
        Vector(
          Vector(
            textElem(
              QName("ID"),
              TargetScope,
              elm.text)))
      }
    }
  }

  private def removeAllEmptyText(elem: Elem): Elem = {
    elem.transformElemsOrSelf(e => removeEmptyText(e))
  }

  private def removeEmptyText(elem: Elem): Elem = {
    if (elem.findAllChildElems.isEmpty && elem.text.isEmpty) elem.copy(children = Vector()) else elem
  }
}

object TransformEdifactTest {

  final class NestedIn(val contextElem: indexed.Elem) {

    def as(relativeReverseAncestryOrSelf: immutable.Seq[EName]): (indexed.Elem) => Boolean = {
      elem => elem.reverseAncestryOrSelfENames == (contextElem.reverseAncestryOrSelfENames ++ relativeReverseAncestryOrSelf)
    }
  }

  def nestedIn(contextElem: indexed.Elem): NestedIn = new NestedIn(contextElem)

  private val EnvNs = "urn:org.milyn.edi.unedifact.v41"
  private val CNs = "urn:org.milyn.edi.unedifact:un:d15a:common"
  private val GovcbrNs = "urn:org.milyn.edi.unedifact:un:d15a:govcbr"

  private val Tns = "urn:wco:datamodel:WCO:MAI:01A"
  private val TargetScope = Scope.from("" -> Tns)
}
