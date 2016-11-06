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

import scala.Vector
import scala.collection.immutable

import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.FunSuite
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
import TransformEdifactTest.ElemSeqTransformers._

/**
 * Transformation test for EDIFACT messages.
 *
 * The source of the transformation is a Smooks XML representation of a GOVCBR EDIFACT message, for declaration type
 * MAI. The target of the transformation conforms to the WCO Data Model. For the source EDIFACT GOVCBR message in release
 * 15A, see http://www.unece.org/fileadmin/DAM/trade/untdid/d15a/trmd/govcbr_c.htm, especially the message structure.
 *
 * Note that we could have written the entire transformation, regardless of the declaration type. Per declaration type,
 * a simple check specific to that declaration type would then suffice. On the other hand, this test case is just an XML
 * transformation test case using an EDIFACT example.
 *
 * See MIG-NL_Single_Window_Section_2_B2SW_Data_Model_01A01.doc, and for declaration type MAI, see
 * MIG-NL_Single_Window_Section_2_B2SW-MAI_01A01.doc.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class TransformEdifactTest extends FunSuite {

  private val docParser = DocumentParserUsingSax.newInstance
  private val docPrinter = DocumentPrinterUsingSax.newInstance

  test("testTransform") {
    val edifactDoc =
      indexed.Document(
        docParser.parse(classOf[TransformEdifactTest].getResource("edifact/sample-edifact.xml").toURI))

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
        { contextElem: indexed.Elem => contextElem.findElem(nestedIn(contextElem).as(govcbrRelativeReverseAncestryOrSelf ++ List(EName(GovcbrNs, "BGM"), EName(CNs, "e1225")))).toVector } andThen
          mapEachElem({ sourceElem => textElem(QName("FunctionCode"), TargetScope, sourceElem.text) }) apply edifactElem,
        { contextElem: indexed.Elem => contextElem.findElem(nestedIn(contextElem).as(govcbrRelativeReverseAncestryOrSelf ++ List(EName(GovcbrNs, "RFF"), EName(CNs, "C506"), EName(CNs, "e1154")))).toVector } andThen
          mapEachElem({ sourceElem => textElem(QName("FunctionalReferenceID"), TargetScope, sourceElem.text) }) apply edifactElem,
        { contextElem: indexed.Elem => contextElem.findElem(nestedIn(contextElem).as(govcbrRelativeReverseAncestryOrSelf ++ List(EName(GovcbrNs, "DTM"), EName(CNs, "C507"), EName(CNs, "e2380")))).toVector } andThen
          mapEachElem({ sourceElem => textElem(QName("IssueDateTime"), TargetScope, sourceElem.text) }) apply edifactElem,
        { contextElem: indexed.Elem => contextElem.findElem(nestedIn(contextElem).as(govcbrRelativeReverseAncestryOrSelf ++ List(EName(GovcbrNs, "BGM"), EName(CNs, "C002"), EName(CNs, "e1001")))).toVector } andThen
          mapEachElem({ sourceElem => textElem(QName("TypeCode"), TargetScope, sourceElem.text) }) apply edifactElem,
        { contextElem: indexed.Elem => contextElem.findElem(nestedIn(contextElem).as(govcbrRelativeReverseAncestryOrSelf ++ List(EName(GovcbrNs, "BGM"), EName(CNs, "C106"), EName(CNs, "e1056")))).toVector } andThen
          mapEachElem({ sourceElem => textElem(QName("VersionID"), TargetScope, sourceElem.text) }) apply edifactElem,
        { contextElem: indexed.Elem => contextElem.findElem(nestedIn(contextElem).as(govcbrRelativeReverseAncestryOrSelf ++ List(EName(GovcbrNs, "Segment_group_34")))).toVector } andThen
          mapEachElem({ sourceElem => makeBorderTransportMeans(sourceElem) }) apply edifactElem,
        { contextElem: indexed.Elem =>
          contextElem.findElem(
            nestedIn(contextElem).as(
              RichPath(
                Vector(
                  RichPath.Entry(EName(EnvNs, "interchangeMessage")),
                  RichPath.Entry(EName(GovcbrNs, "GOVCBR")),
                  RichPath.Entry(
                    EName(GovcbrNs, "Segment_group_7"),
                    { e => e.findElem(_.resolvedName == EName(CNs, "e3035")).map(_.text).getOrElse("") == "DT" }))))).toVector
        } andThen mapEachElem({ sourceElem => makeDeclarant(sourceElem) }) apply edifactElem,
        { contextElem: indexed.Elem =>
          contextElem.findElem(
            nestedIn(contextElem).as(
              RichPath(
                Vector(
                  RichPath.Entry(EName(EnvNs, "interchangeMessage")),
                  RichPath.Entry(EName(GovcbrNs, "GOVCBR")),
                  RichPath.Entry(
                    EName(GovcbrNs, "Segment_group_9"),
                    { e => e.findElem(_.resolvedName == EName(CNs, "e1001")).map(_.text).getOrElse("") == "998" }),
                  RichPath.Entry(EName(GovcbrNs, "DOC")),
                  RichPath.Entry(EName(CNs, "C503")),
                  RichPath.Entry(EName(CNs, "e1004")))))).toVector
        } andThen mapEachElem({ sourceElem => makePreviousDocument(sourceElem) }) apply edifactElem)
    }
  }

  private def makeBorderTransportMeans(edifactElem: indexed.Elem): Elem = {
    require(edifactElem.resolvedName == EName(GovcbrNs, "Segment_group_34"))

    emptyElem(QName("BorderTransportMeans"), TargetScope) withChildSeqs {
      Vector(
        { contextElem: indexed.Elem => contextElem.findElem(nestedIn(contextElem).as(List(EName(GovcbrNs, "TDT"), EName(CNs, "C222"), EName(CNs, "e8213")))).toVector } andThen
          mapEachElem({ sourceElem => textElem(QName("ID"), TargetScope, sourceElem.text) }) apply edifactElem,
        { contextElem: indexed.Elem => contextElem.findElem(nestedIn(contextElem).as(List(EName(GovcbrNs, "TDT"), EName(CNs, "C222"), EName(CNs, "e1131")))).toVector } andThen
          mapEachElem({ sourceElem => textElem(QName("IdentificationTypeCode"), TargetScope, sourceElem.text) }) apply edifactElem,
        { contextElem: indexed.Elem => contextElem.findElem(nestedIn(contextElem).as(List(EName(GovcbrNs, "TDT"), EName(CNs, "C001"), EName(CNs, "e8179")))).toVector } andThen
          mapEachElem({ sourceElem => textElem(QName("TypeCode"), TargetScope, sourceElem.text) }) apply edifactElem,
        { contextElem: indexed.Elem => contextElem.findElem(nestedIn(contextElem).as(List(EName(GovcbrNs, "RFF"), EName(CNs, "C506"), EName(CNs, "e1154")))).toVector } andThen
          mapEachElem({ sourceElem => textElem(QName("StayID"), TargetScope, sourceElem.text) }) apply edifactElem,
        { contextElem: indexed.Elem =>
          contextElem.findElem(
            nestedIn(contextElem).as(
              RichPath(
                Vector(
                  RichPath.Entry(
                    EName(GovcbrNs, "Segment_group_36"),
                    { e => e.findElem(_.resolvedName == EName(CNs, "e3227")).map(_.text).getOrElse("") == "153" }))))).toVector
        } andThen
          mapEachElem({ sourceElem => makeBorderTransportMeansItinerary(sourceElem) }) apply edifactElem)
    }
  }

  private def makeBorderTransportMeansItinerary(edifactElem: indexed.Elem): Elem = {
    require(edifactElem.resolvedName == EName(GovcbrNs, "Segment_group_36"))
    require(edifactElem.findElem(_.resolvedName == EName(CNs, "e3227")).map(_.text).getOrElse("") == "153")

    emptyElem(QName("Itinerary"), TargetScope) withChildSeqs {
      Vector(
        { contextElem: indexed.Elem => contextElem.findElem(nestedIn(contextElem).as(List(EName(GovcbrNs, "LOC"), EName(CNs, "C517"), EName(CNs, "e3225")))).toVector } andThen
          mapEachElem({ sourceElem => textElem(QName("ID"), TargetScope, sourceElem.text) }) apply edifactElem)
    }
  }

  private def makeDeclarant(edifactElem: indexed.Elem): Elem = {
    require(edifactElem.resolvedName == EName(GovcbrNs, "Segment_group_7"))
    require(edifactElem.findElem(_.resolvedName == EName(CNs, "e3035")).map(_.text).getOrElse("") == "DT")

    emptyElem(QName("Declarant"), TargetScope) withChildSeqs {
      Vector(
        { contextElem: indexed.Elem => contextElem.findElem(nestedIn(contextElem).as(List(EName(GovcbrNs, "NAD"), EName(CNs, "C080"), EName(CNs, "e3036")))).toVector } andThen
          mapOneOptionalElem({ sourceElemOption => textElem(QName("Name"), TargetScope, sourceElemOption.map(_.text).getOrElse("")) }) apply edifactElem,
        { contextElem: indexed.Elem => contextElem.findElem(nestedIn(contextElem).as(List(EName(GovcbrNs, "NAD"), EName(CNs, "C082"), EName(CNs, "e3039")))).toVector } andThen
          mapEachElem({ sourceElem => textElem(QName("ID"), TargetScope, sourceElem.text) }) apply edifactElem,
        { contextElem: indexed.Elem => contextElem.findElem(nestedIn(contextElem).as(List(EName(GovcbrNs, "NAD"), EName(CNs, "e3035")))).toVector } andThen
          mapEachElem({ sourceElem => textElem(QName("RoleCode"), TargetScope, sourceElem.text) }) apply edifactElem,
        { contextElem: indexed.Elem =>
          contextElem.findElem(
            nestedIn(contextElem).as(
              RichPath(
                Vector(
                  RichPath.Entry(
                    EName(GovcbrNs, "Segment_group_8"),
                    { e => e.findElem(_.resolvedName == EName(CNs, "e3139")).map(_.text).getOrElse("") == "IC" }),
                  RichPath.Entry(EName(GovcbrNs, "CTA")),
                  RichPath.Entry(EName(CNs, "C056")),
                  RichPath.Entry(EName(CNs, "e3412")))))).toVector
        } andThen
          mapOneOptionalElem({ sourceElemOption =>
            elem(
              QName("Contact"),
              TargetScope,
              Vector(textElem(
                QName("Name"),
                TargetScope,
                sourceElemOption.map(_.text).getOrElse(""))))
          }) apply edifactElem,
        { contextElem: indexed.Elem =>
          contextElem.filterElems(
            nestedIn(contextElem).as(
              RichPath(
                Vector(
                  RichPath.Entry(
                    EName(GovcbrNs, "Segment_group_8"),
                    { e => e.findElem(_.resolvedName == EName(CNs, "e3139")).map(_.text).getOrElse("") == "AH" }),
                  RichPath.Entry(EName(GovcbrNs, "COM")))))).toVector
        } andThen
          mapEachElem({ sourceElem => makeDeclarantCommunication(sourceElem) }) apply edifactElem)
    }
  }

  private def makeDeclarantCommunication(edifactElem: indexed.Elem): Elem = {
    require(edifactElem.resolvedName == EName(GovcbrNs, "COM"))

    emptyElem(QName("Communication"), TargetScope) withChildSeqs {
      Vector(
        { contextElem: indexed.Elem => contextElem.findElem(nestedIn(contextElem).as(List(EName(CNs, "C076"), EName(CNs, "e3148")))).toVector } andThen
          mapEachElem({ sourceElem => textElem(QName("ID"), TargetScope, sourceElem.text) }) apply edifactElem,
        { contextElem: indexed.Elem => contextElem.findElem(nestedIn(contextElem).as(List(EName(CNs, "C076"), EName(CNs, "e3155")))).toVector } andThen
          mapEachElem({ sourceElem => textElem(QName("TypeCode"), TargetScope, sourceElem.text) }) apply edifactElem)
    }
  }

  private def makePreviousDocument(edifactElem: indexed.Elem): Elem = {
    require(edifactElem.resolvedName == EName(CNs, "e1004"))

    emptyElem(QName("PreviousDocument"), TargetScope) withChildSeqs {
      Vector(
        { contextElem: indexed.Elem => Vector(contextElem) } andThen
          mapEachElem({ sourceElem => textElem(QName("ID"), TargetScope, sourceElem.text) }) apply edifactElem)
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

  /**
   * Transformer, mapping a sequence of indexed elements to a sequence of simple elements.
   */
  type ElemSeqTransformer = ((immutable.IndexedSeq[indexed.Elem]) => immutable.IndexedSeq[Elem])

  object ElemSeqTransformers {

    def mapEachElem(mapElem: indexed.Elem => Elem): ElemSeqTransformer = {
      val mapElems = { (elems: immutable.IndexedSeq[indexed.Elem]) => elems.map(mapElem) }
      mapElems
    }

    def mapAtMostOneElem(mapElem: indexed.Elem => Elem): ElemSeqTransformer = {
      val mapElems = { (elems: immutable.IndexedSeq[indexed.Elem]) => elems.headOption.map(mapElem).toVector }
      mapElems
    }

    def mapOneElem(mapElem: indexed.Elem => Elem): ElemSeqTransformer = {
      val mapElems = { (elems: immutable.IndexedSeq[indexed.Elem]) => Vector(mapElem(elems.head)) }
      mapElems
    }

    def mapOneOptionalElem(mapOptionalElem: Option[indexed.Elem] => Elem): ElemSeqTransformer = {
      val mapElems = { (elems: immutable.IndexedSeq[indexed.Elem]) => Vector(mapOptionalElem(elems.headOption)) }
      mapElems
    }
  }

  /**
   * Path containing path entries that each contain an element EName as well as an element predicate.
   * Note that such paths do not uniquely identify elements in a tree.
   */
  final case class RichPath(val entries: immutable.IndexedSeq[RichPath.Entry]) {

    def enames: immutable.IndexedSeq[EName] = entries.map(_.ename)
  }

  object RichPath {

    final case class Entry(val ename: EName, val p: Elem => Boolean)

    object Entry {

      def apply(ename: EName): Entry = Entry(ename, { elm => true })
    }

    def apply(enames: immutable.Seq[EName]): RichPath = {
      RichPath(enames.map(en => RichPath.Entry(en)).toVector)
    }
  }

  /**
   * Builder of indexed element predicates that use a RichPath in the predicate.
   */
  final class NestedIn(val contextElem: indexed.Elem) {

    def as(path: immutable.Seq[EName]): (indexed.Elem) => Boolean = {
      as(RichPath(path))
    }

    def as(path: RichPath): (indexed.Elem) => Boolean = {
      elem =>
        elem.reverseAncestryOrSelfENames.dropRight(path.entries.size) == contextElem.reverseAncestryOrSelfENames && {
          val partReverseAncestryOrSelf = elem.reverseAncestryOrSelf.takeRight(path.entries.size)

          partReverseAncestryOrSelf.zip(path.entries) forall {
            case (elem, entry) =>
              elem.resolvedName == entry.ename && entry.p(elem.underlyingElem)
          }
        }
    }
  }

  def nestedIn(contextElem: indexed.Elem): NestedIn = new NestedIn(contextElem)

  private val EnvNs = "urn:org.milyn.edi.unedifact.v41"
  private val CNs = "urn:org.milyn.edi.unedifact:un:d15a:common"
  private val GovcbrNs = "urn:org.milyn.edi.unedifact:un:d15a:govcbr"

  private val Tns = "urn:wco:datamodel:WCO:MAI:01A"
  private val TargetScope = Scope.from("" -> Tns)
}
