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

import scala.math._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.js.annotation.JSExportTopLevel
import scala.util.Failure
import scala.util.Success

import eu.cdevreeze.yaidom.jsdom.JsDomDocument

import org.scalajs.dom
import org.scalajs.dom.html
import org.scalajs.dom.console
import org.scalajs.dom.ext.Ajax

/**
 * Flower of life, "configured" from a small XML file.
 *
 * @author Chris de Vreeze
 */
@JSExportTopLevel("FlowerOfLife")
object FlowerOfLife {

  private var configurationDoc: JsDomDocument = null

  @JSExport("retrieveConfigurationAndDisplayFlower")
  def retrieveConfigurationAndDisplayFlower(configurationUri: String, canvas: html.Canvas): Unit = {
    Ajax.get(configurationUri).onComplete {
      case Success(xhr) =>
        val responseXml = xhr.responseXML

        val wrapperDoc = JsDomDocument(responseXml)
        configurationDoc = wrapperDoc

        val radius = configurationDoc.documentElement.getChildElem(_.localName == "radius").text.toDouble

        val colour = configurationDoc.documentElement.getChildElem(_.localName == "colour").text

        drawFlowerOfLife(canvas, radius, colour)
      case Failure(xhr) =>
        console.error(s"Could not retrieve configuration at URL '$configurationUri'")
    }
  }

  def drawFlowerOfLife(canvas: html.Canvas, radius: Double, colour: String): Unit = {
    console.info(s"Drawing on the canvas")

    val renderer = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]

    canvas.width = canvas.parentElement.clientWidth
    canvas.height = canvas.parentElement.clientHeight

    renderer.fillStyle = "#FAFAFA"
    renderer.fillRect(0, 0, canvas.width, canvas.height)

    val center = Point(canvas.width.toDouble / 2, canvas.height.toDouble / 2)

    val arcs: Seq[Arc] = findAllFlowerOfLifeArcs(center, radius)

    arcs.foreach { arc =>
      drawArc(renderer, arc, colour)
    }
  }

  def drawArc(renderer: dom.CanvasRenderingContext2D, arc: Arc, colour: String): Unit = {
    renderer.beginPath()
    renderer.strokeStyle = colour
    renderer.arc(arc.center.x, arc.center.y, arc.radius, arc.startAngle, arc.endAngle, arc.anticlockwise)
    renderer.stroke()
  }

  def findAllFlowerOfLifeArcs(center: Point, radius: Double): Seq[Arc] = {
    val circleResult: Seq[Circle] = findAllFlowerOfLifeCircles(center, radius)

    val thirtyDegreesInRadians = asin(0.5)
    val (deltaX, deltaY) = (cos(thirtyDegreesInRadians) * radius, radius / 2)

    val fullyFilledCircle: Seq[Arc] = findFullyFilledCircle(center, radius).map(_.right(deltaX * 2))

    val translations: Seq[Arc => Arc] = Seq(
      _.down(radius),
      _.left(deltaX).down(radius / 2),
      _.left(deltaX).down(radius / 2),
      _.left(deltaX).up(radius / 2),
      _.left(deltaX).up(radius / 2),
      _.up(radius),
      _.up(radius),
      _.right(deltaX).up(radius / 2),
      _.right(deltaX).up(radius / 2),
      _.right(deltaX).down(radius / 2),
      _.right(deltaX).down(radius / 2),
    )

    val fullyFilledCircles: Seq[Seq[Arc]] = translations.scanLeft(fullyFilledCircle) { case (prevFilledCircle, translation) =>
      prevFilledCircle.map(translation)
    }

    val circleSet: Set[Circle] = circleResult.toSet
    circleResult ++ (fullyFilledCircles.flatten.distinct.filter(arc => !circleSet.contains(arc.toCircle)))
  }

  private def findAllFlowerOfLifeCircles(center: Point, radius: Double): Seq[Circle] = {
    val middleArcColumn: Seq[Circle] = middleColumnOfCircles(center, radius)

    val leftResult: IndexedSeq[Circle] =
      ((0 until 2)
        .scanLeft(middleArcColumn) {
          case (arcColumn, _) =>
            nextLeftColumnOfCircles(arcColumn)
        })
        .flatten

    val rightResult: IndexedSeq[Circle] =
      ((0 until 2)
        .scanLeft(middleArcColumn) {
          case (arcColumn, _) =>
            nextRightColumnOfCircles(arcColumn)
        })
        .flatten

    (leftResult ++ rightResult).distinct
  }

  private def nextLeftColumnOfCircles(arcColumn: Seq[Circle]): Seq[Circle] = {
    nextColumnOfCircles(arcColumn, true)
  }

  private def nextRightColumnOfCircles(arcColumn: Seq[Circle]): Seq[Circle] = {
    nextColumnOfCircles(arcColumn, false)
  }

  private def nextColumnOfCircles(circleColumn: Seq[Circle], left: Boolean): Seq[Circle] = {
    require(circleColumn.nonEmpty)
    require(circleColumn.map(_.center.x).distinct.size == 1)
    require(circleColumn.map(_.radius.toInt).distinct.size == 1)

    val radius = circleColumn.head.radius
    val deltaX = cos(asin(0.5)) * radius
    val effectiveDeltaX = if (left) -deltaX else deltaX

    circleColumn.init.map(arc => arc.translate(effectiveDeltaX, radius / 2))
  }

  private def middleColumnOfCircles(center: Point, radius: Double): Seq[Circle] = {
    val topmostCircle = Circle(center, radius).up(radius * 2)

    (0 until 5).map(i => topmostCircle.down(radius * i))
  }

  private def findFullyFilledCircle(center: Point, radius: Double): Seq[Arc] = {
    val thirtyDegreesInRadians = asin(0.5)
    val sixtyDegreesInRadians = thirtyDegreesInRadians * 2
    val (deltaX, deltaY) = (cos(thirtyDegreesInRadians) * radius, radius / 2)

    val circle: Circle = Circle(center, radius)

    val spoke1 = Seq(
      PartialCircleArc(center.down(radius), radius, -Pi / 2, -Pi / 2 + sixtyDegreesInRadians, false),
      PartialCircleArc(center.right(deltaX).up(radius / 2), radius, Pi / 2, Pi / 2 + sixtyDegreesInRadians, false)
    )

    val spoke2 = Seq(
      PartialCircleArc(center.left(deltaX).down(radius / 2), radius, -thirtyDegreesInRadians, thirtyDegreesInRadians, false),
      PartialCircleArc(center.right(deltaX).down(radius / 2), radius, Pi - thirtyDegreesInRadians, Pi + thirtyDegreesInRadians, false)
    )

    val spoke3 = Seq(
      PartialCircleArc(center.down(radius), radius, -Pi / 2 - sixtyDegreesInRadians, -Pi / 2, false),
      PartialCircleArc(center.left(deltaX).up(radius / 2), radius, Pi / 2 - sixtyDegreesInRadians, Pi / 2, false)
    )

    val spoke4 = Seq(
      PartialCircleArc(center.up(radius), radius, Pi / 2, Pi / 2 + sixtyDegreesInRadians, false),
      PartialCircleArc(center.left(deltaX).down(radius / 2), radius, -Pi / 2, -Pi / 2 + sixtyDegreesInRadians, false)
    )

    val spoke5 = Seq(
      PartialCircleArc(center.right(deltaX).up(radius / 2), radius, Pi - thirtyDegreesInRadians, Pi + thirtyDegreesInRadians, false),
      PartialCircleArc(center.left(deltaX).up(radius / 2), radius, -thirtyDegreesInRadians, thirtyDegreesInRadians, false)
    )

    val spoke6 = Seq(
      PartialCircleArc(center.right(deltaX).down(radius / 2), radius, -Pi / 2 - sixtyDegreesInRadians, -Pi / 2, false),
      PartialCircleArc(center.up(radius), radius, Pi / 2 - sixtyDegreesInRadians, Pi / 2, false)
    )

    val outside1 = PartialCircleArc(center.right(deltaX * 2), radius, Pi - thirtyDegreesInRadians, Pi + thirtyDegreesInRadians, false)
    val outside2 = PartialCircleArc(center.right(deltaX).down(radius * 1.5), radius, -Pi / 2 - sixtyDegreesInRadians, -Pi / 2, false)
    val outside3 = PartialCircleArc(center.left(deltaX).down(radius * 1.5), radius, -Pi / 2, -Pi / 2 + sixtyDegreesInRadians, false)
    val outside4 = PartialCircleArc(center.left(deltaX * 2), radius, -thirtyDegreesInRadians, thirtyDegreesInRadians, false)
    val outside5 = PartialCircleArc(center.left(deltaX).up(radius * 1.5), radius, Pi / 2 - sixtyDegreesInRadians, Pi / 2, false)
    val outside6 = PartialCircleArc(center.right(deltaX).up(radius * 1.5), radius, Pi / 2, Pi / 2 + sixtyDegreesInRadians, false)

    val outside = Seq[PartialCircleArc](outside1, outside2, outside3, outside4, outside5, outside6)

    val partialCircles = Seq[Seq[PartialCircleArc]](spoke1, spoke2, spoke3, spoke4, spoke5, spoke6).flatten ++ outside

    circle +: partialCircles.distinct
  }

  // Data structures

  final case class Point(x: Double, y: Double) {

    def up(amount: Double): Point = Point(x, y - amount)

    def down(amount: Double): Point = Point(x, y + amount)

    def left(amount: Double): Point = Point(x - amount, y)

    def right(amount: Double): Point = Point(x + amount, y)
  }

  sealed trait Arc {

    type ThisArc <: Arc

    def center: Point

    def radius: Double

    def startAngle: Double

    def endAngle: Double

    def anticlockwise: Boolean

    def translate(deltaX: Double, deltaY: Double): ThisArc

    def moveTo(newCenter: Point): ThisArc

    def downgrade: PartialCircleArc

    final def toCircle: Circle = Circle(center, radius)

    final def up(amount: Double): ThisArc = moveTo(center.up(amount))

    final def down(amount: Double): ThisArc = moveTo(center.down(amount))

    final def left(amount: Double): ThisArc = moveTo(center.left(amount))

    final def right(amount: Double): ThisArc = moveTo(center.right(amount))
  }

  final case class PartialCircleArc(
      center: Point,
      radius: Double,
      startAngle: Double,
      endAngle: Double,
      anticlockwise: Boolean)
      extends Arc {

    type ThisArc = PartialCircleArc

    def translate(deltaX: Double, deltaY: Double): ThisArc = right(deltaX).down(deltaY)

    def moveTo(newCenter: Point): ThisArc = copy(center = newCenter)

    def downgrade: PartialCircleArc = this

    def withStartAngle(start: Double): PartialCircleArc = copy(startAngle = start)

    def withEndAngle(end: Double): PartialCircleArc = copy(endAngle = end)
  }

  final case class Circle(center: Point, radius: Double) extends Arc {

    type ThisArc = Circle

    def startAngle: Double = 0

    def endAngle: Double = math.Pi * 2

    def anticlockwise: Boolean = false

    def translate(deltaX: Double, deltaY: Double): ThisArc = right(deltaX).down(deltaY)

    def moveTo(newCenter: Point): ThisArc = copy(center = newCenter)

    def downgrade: PartialCircleArc = {
      new PartialCircleArc(center, radius, startAngle, endAngle, anticlockwise)
    }
  }
}
