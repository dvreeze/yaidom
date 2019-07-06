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

  def findAllFlowerOfLifeArcs(center: Point, radius: Double): Seq[Arc] = {
    val circleResult = findAllFlowerOfLifeCircles(center, radius)

    val (deltaX, deltaY) = (cos(asin(0.5)) * radius, radius / 2)

    val centralCircle: Circle = Circle(center, radius)

    val topLeftPartialCircleCenter: Point = center.up(radius * 2).left(deltaX).up(deltaY)
    val topLeftPartialCircle: Arc =
      PartialCircleArc(topLeftPartialCircleCenter, radius, -asin(0.5), -asin(0.5) + Pi, false)

    val topRightPartialCircleCenter: Point = center.up(radius * 2).right(deltaX).up(deltaY)
    val topRightPartialCircle: Arc =
      PartialCircleArc(topRightPartialCircleCenter, radius, asin(0.5), asin(0.5) + Pi, false)

    val bottomLeftPartialCircleCenter: Point = center.down(radius * 2).left(deltaX).down(deltaY)
    val bottomLeftPartialCircle: Arc =
      PartialCircleArc(bottomLeftPartialCircleCenter, radius, asin(0.5) - Pi, asin(0.5), false)

    val bottomRightPartialCircleCenter: Point = center.down(radius * 2).right(deltaX).down(deltaY)
    val bottomRightPartialCircle: Arc =
      PartialCircleArc(bottomRightPartialCircleCenter, radius, -asin(0.5) - Pi, -asin(0.5), false)

    val leftArc =
      centralCircle.left(deltaX * 3).up(deltaY).downgrade.withStartAngle(-Pi / 2).withEndAngle(Pi / 2)

    val rightArc =
      centralCircle.right(deltaX * 3).down(deltaY).downgrade.withStartAngle(Pi / 2).withEndAngle(Pi / 2 + Pi)

    val partialCircleResultWithoutOuterLayer: Seq[Arc] =
      (0 until 2).map(i => topLeftPartialCircle.move(-deltaX * i, deltaY * i)) ++
        Seq(topLeftPartialCircle.move(-deltaX * 2, deltaY * 2).downgrade.withEndAngle(Pi / 2)) ++
        (0 until 2).map(i => topRightPartialCircle.move(deltaX * i, deltaY * i)) ++
        Seq(topRightPartialCircle.move(deltaX * 2, deltaY * 2).downgrade.withStartAngle(Pi / 2)) ++
        (0 until 2).map(i => bottomLeftPartialCircle.move(-deltaX * i, -deltaY * i)) ++
        Seq(bottomLeftPartialCircle.move(-deltaX * 2, -deltaY * 2).downgrade.withStartAngle(-Pi / 2)) ++
        (0 until 2).map(i => bottomRightPartialCircle.move(deltaX * i, -deltaY * i)) ++
        Seq(bottomRightPartialCircle.move(deltaX * 2, -deltaY * 2).downgrade.withEndAngle(-Pi / 2)) ++
        Seq(leftArc, leftArc.down(radius)) ++
        Seq(rightArc, rightArc.up(radius)) ++
        Seq.empty

    val outerLeftArc = centralCircle.left(deltaX * 4).downgrade.withStartAngle(-asin(0.5)).withEndAngle(asin(0.5))

    val outerRightArc =
      centralCircle.right(deltaX * 4).downgrade.withStartAngle(Pi - asin(0.5)).withEndAngle(Pi + asin(0.5))

    val outerUpperLeftArc =
      centralCircle.left(deltaX).up(3.5 * radius).downgrade.withStartAngle(asin(0.5)).withEndAngle(Pi / 2)

    val outerLowerLeftArc = centralCircle
      .left(deltaX)
      .down(3.5 * radius)
      .downgrade
      .withStartAngle(-Pi / 2)
      .withEndAngle(-Pi / 2 + 2 * asin(0.5))

    val outerUpperRightArc =
      centralCircle.right(deltaX).up(3.5 * radius).downgrade.withStartAngle(Pi / 2).withEndAngle(Pi / 2 + 2 * asin(0.5))

    val outerLowerRightArc = centralCircle
      .right(deltaX)
      .down(3.5 * radius)
      .downgrade
      .withStartAngle(-Pi / 2 - 2 * asin(0.5))
      .withEndAngle(-Pi / 2)

    val outerLayer: Seq[Arc] =
      Seq(outerLeftArc, outerLeftArc.up(radius), outerLeftArc.down(radius)) ++
        Seq(outerRightArc, outerRightArc.up(radius), outerRightArc.down(radius)) ++
        Seq(outerUpperLeftArc, outerUpperLeftArc.move(-deltaX, deltaY), outerUpperLeftArc.move(-deltaX * 2, deltaY * 2)) ++
        Seq(
          outerLowerLeftArc,
          outerLowerLeftArc.move(-deltaX, -deltaY),
          outerLowerLeftArc.move(-deltaX * 2, -deltaY * 2)) ++
        Seq(
          outerUpperRightArc,
          outerUpperRightArc.move(deltaX, deltaY),
          outerUpperRightArc.move(deltaX * 2, deltaY * 2)) ++
        Seq(
          outerLowerRightArc,
          outerLowerRightArc.move(deltaX, -deltaY),
          outerLowerRightArc.move(deltaX * 2, -deltaY * 2)) ++
        Seq(
          centralCircle.up(radius * 3).downgrade.withStartAngle(asin(0.5)).withEndAngle(Pi - asin(0.5)),
          centralCircle.down(radius * 3).downgrade.withStartAngle(asin(0.5) - Pi).withEndAngle(-asin(0.5)))

    circleResult ++ partialCircleResultWithoutOuterLayer ++ outerLayer
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

  private def drawArc(renderer: dom.CanvasRenderingContext2D, arc: Arc, colour: String): Unit = {
    renderer.beginPath()
    renderer.strokeStyle = colour
    renderer.arc(arc.center.x, arc.center.y, arc.radius, arc.startAngle, arc.endAngle, arc.anticlockwise)
    renderer.stroke()
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

    circleColumn.init.map(arc => arc.move(effectiveDeltaX, radius / 2))
  }

  private def middleColumnOfCircles(center: Point, radius: Double): Seq[Circle] = {
    val topmostCircle = Circle(center, radius).up(radius * 2)

    (0 until 5).map(i => topmostCircle.down(radius * i))
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

    def move(deltaX: Double, deltaY: Double): ThisArc

    def moveTo(newCenter: Point): ThisArc

    def downgrade: PartialCircleArc

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

    def move(deltaX: Double, deltaY: Double): ThisArc = right(deltaX).down(deltaY)

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

    def move(deltaX: Double, deltaY: Double): ThisArc = right(deltaX).down(deltaY)

    def moveTo(newCenter: Point): ThisArc = copy(center = newCenter)

    def downgrade: PartialCircleArc = {
      new PartialCircleArc(center, radius, startAngle, endAngle, anticlockwise)
    }

    def clip(startAngle: Double, endAngle: Double, anticlockwise: Boolean): PartialCircleArc = {
      new PartialCircleArc(center, radius, startAngle, endAngle, anticlockwise)
    }
  }
}
