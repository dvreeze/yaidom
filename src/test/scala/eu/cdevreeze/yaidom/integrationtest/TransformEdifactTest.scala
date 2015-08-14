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

import java.{ util => jutil, io => jio }
import scala.collection.immutable
import org.junit.{ Test, Before }
import org.junit.runner.RunWith
import org.scalatest.{ Suite, BeforeAndAfterAll, Ignore }
import org.scalatest.junit.JUnitRunner
import eu.cdevreeze.yaidom.convert.ScalaXmlConversions._
import eu.cdevreeze.yaidom.convert
import eu.cdevreeze.yaidom.dom
import eu.cdevreeze.yaidom.indexed
import eu.cdevreeze.yaidom.resolved
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.simple.Elem
import eu.cdevreeze.yaidom.simple.Node
import eu.cdevreeze.yaidom.parse.DocumentParserUsingSax
import eu.cdevreeze.yaidom.print.DocumentPrinterUsingSax

/**
 * Update test for edifact messages.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class TransformEdifactTest extends Suite {

  private val docParser = DocumentParserUsingSax.newInstance
  private val docPrinter = DocumentPrinterUsingSax.newInstance

  @Test def testTransform(): Unit = {
    val edifactDoc =
      indexed.Document(docParser.parse(classOf[TransformEdifactTest].getResource("sample-edifact.xml").toURI))

    val declarationElem = makeDeclaration(edifactDoc.documentElement)

    val declarationString = docPrinter.print(declarationElem.prettify(2))
    println(declarationString)

    assertResult(true) {
      declarationElem.findAllElemsOrSelf.size >= 3
    }
  }

  private def makeDeclaration(edifactElem: indexed.Elem): Elem = {
    require(edifactElem.resolvedName == EName(EnvNs, "unEdifact"))

    val scope = TargetScope

    val ownReverseAncestryOrSelf = List(EName(EnvNs, "unEdifact"), EName(EnvNs, "interchangeMessage"), EName(GovcbrNs, "GOVCBR"))

    Node.emptyElem(QName("Declaration"), scope).
      plusChild(
        makeTextElem(
          getSingleEdifactElem(
            edifactElem,
            ownReverseAncestryOrSelf ++ List(EName(GovcbrNs, "BGM"), EName(CNs, "e1225"))),
          EName(CNs, "e1225"),
          QName("FunctionCode"))).
        plusChild(
          makeTextElem(
            getSingleEdifactElem(
              edifactElem,
              ownReverseAncestryOrSelf ++ List(EName(GovcbrNs, "RFF"), EName(CNs, "C506"), EName(CNs, "e1154"))),
            EName(CNs, "e1154"),
            QName("FunctionalReferenceID"))).
          plusChild(
            makeTextElem(
              getSingleEdifactElem(
                edifactElem,
                ownReverseAncestryOrSelf ++ List(EName(GovcbrNs, "DTM"), EName(CNs, "C507"), EName(CNs, "e2380"))),
              EName(CNs, "e2380"),
              QName("IssueDateTime"))).
            plusChild(
              makeTextElem(
                getSingleEdifactElem(
                  edifactElem,
                  ownReverseAncestryOrSelf ++ List(EName(GovcbrNs, "BGM"), EName(CNs, "C002"), EName(CNs, "e1001"))),
                EName(CNs, "e1001"),
                QName("TypeCode"))).
              plusChild(
                makeTextElem(
                  getSingleEdifactElem(
                    edifactElem,
                    ownReverseAncestryOrSelf ++ List(EName(GovcbrNs, "BGM"), EName(CNs, "C106"), EName(CNs, "e1056"))),
                  EName(CNs, "e1056"),
                  QName("VersionID"))).
                plusChild(
                  makeBorderTransportMeans(
                    getSingleEdifactElem(
                      edifactElem,
                      ownReverseAncestryOrSelf ++ List(EName(GovcbrNs, "Segment_group_34")))))
  }

  private def getSingleEdifactElem(contextEdifactElem: indexed.Elem, reverseAncestryOrSelfENames: Seq[EName], p: indexed.Elem => Boolean = { _ => true }): indexed.Elem = {
    val elems = contextEdifactElem.filterElemsOrSelf(e => e.reverseAncestryOrSelfENames == reverseAncestryOrSelfENames)

    require(elems.size == 1, s"Expected precisely 1 element with reverse ancestry $reverseAncestryOrSelfENames (within the context element ${contextEdifactElem.resolvedName}) but found ${elems.size}")

    elems.head
  }

  private def makeTextElem(edifactElem: indexed.Elem, sourceEName: EName, targetQName: QName): Elem = {
    require(edifactElem.resolvedName == sourceEName)

    val scope = TargetScope

    Node.textElem(targetQName, scope, edifactElem.text)
  }

  private def makeBorderTransportMeans(edifactElem: indexed.Elem): Elem = {
    require(edifactElem.resolvedName == EName(GovcbrNs, "Segment_group_34"))

    val scope = TargetScope

    val ownReverseAncestryOrSelf = List(EName(EnvNs, "unEdifact"), EName(EnvNs, "interchangeMessage"), EName(GovcbrNs, "GOVCBR"), EName(GovcbrNs, "Segment_group_34"))

    Node.emptyElem(QName("BorderTransportMeans"), scope).
      plusChild(
        makeTextElem(
          getSingleEdifactElem(
            edifactElem,
            ownReverseAncestryOrSelf ++ List(EName(GovcbrNs, "TDT"), EName(CNs, "C222"), EName(CNs, "e8213"))),
          EName(CNs, "e8213"),
          QName("ID"))).
        plusChild(
          makeTextElem(
            getSingleEdifactElem(
              edifactElem,
              ownReverseAncestryOrSelf ++ List(EName(GovcbrNs, "TDT"), EName(CNs, "C222"), EName(CNs, "e1131"))),
            EName(CNs, "e1131"),
            QName("IdentificationTypeCode"))).
          plusChild(
            makeTextElem(
              getSingleEdifactElem(
                edifactElem,
                ownReverseAncestryOrSelf ++ List(EName(GovcbrNs, "TDT"), EName(CNs, "C001"), EName(CNs, "e8179"))),
              EName(CNs, "e8179"),
              QName("TypeCode"))).
            plusChild(
              makeTextElem(
                getSingleEdifactElem(
                  edifactElem,
                  ownReverseAncestryOrSelf ++ List(EName(GovcbrNs, "RFF"), EName(CNs, "C506"), EName(CNs, "e1154"))),
                EName(CNs, "e1154"),
                QName("StayID"))).
              plusChild(
                makeBorderTransportMeansItinerary(
                  getSingleEdifactElem(
                    edifactElem,
                    ownReverseAncestryOrSelf ++ List(EName(GovcbrNs, "Segment_group_36")),
                    { e => e.findElem(_.resolvedName == EName(CNs, "e3227")).map(_.text).getOrElse("") == "153" })))
  }

  private def makeBorderTransportMeansItinerary(edifactElem: indexed.Elem): Elem = {
    require(edifactElem.resolvedName == EName(GovcbrNs, "Segment_group_36"))
    require(edifactElem.findElem(_.resolvedName == EName(CNs, "e3227")).map(_.text).getOrElse("") == "153")

    val scope = TargetScope

    val ownReverseAncestryOrSelf = List(EName(EnvNs, "unEdifact"), EName(EnvNs, "interchangeMessage"), EName(GovcbrNs, "GOVCBR"), EName(GovcbrNs, "Segment_group_34"), EName(GovcbrNs, "Segment_group_36"))

    Node.emptyElem(QName("Itinerary"), scope).
      plusChild(
        makeTextElem(
          getSingleEdifactElem(
            edifactElem,
            ownReverseAncestryOrSelf ++ List(EName(GovcbrNs, "LOC"), EName(CNs, "C517"), EName(CNs, "e3225"))),
          EName(CNs, "e3225"),
          QName("ID")))
  }

  private val EnvNs = "urn:org.milyn.edi.unedifact.v41"
  private val CNs = "urn:org.milyn.edi.unedifact:un:d15a:common"
  private val GovcbrNs = "urn:org.milyn.edi.unedifact:un:d15a:govcbr"
  
  private val Tns = "urn:wco:datamodel:WCO:MAI:01A"
  private val TargetScope = Scope.from("" -> Tns)
}
