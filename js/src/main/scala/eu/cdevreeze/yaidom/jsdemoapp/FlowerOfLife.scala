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

    drawCircle(renderer, center, radius, colour)

    val deltaX = math.sqrt(math.pow(radius, 2) - math.pow(radius / 2, 2))
    val firstLeftCenter = center.up(radius / 2).left(deltaX)
    val firstRightCenter = center.up(radius / 2).right(deltaX)

    val centerPoints = Seq(
      center.up(radius),
      firstLeftCenter,
      firstRightCenter,
      center.down(radius),
      firstLeftCenter.down(radius),
      firstRightCenter.down(radius)) ++ Seq(
      center.left(deltaX * 2),
      center.right(deltaX * 2),
      center.up(radius * 2),
      center.down(radius * 2),
      center.up(radius).left(deltaX * 2),
      center.down(radius).left(deltaX * 2),
      center.up(radius).right(deltaX * 2),
      center.down(radius).right(deltaX * 2))

    centerPoints.foreach { centerPoint =>
      drawCircle(renderer, centerPoint, radius, colour)
    }
  }

  private def drawCircle(renderer: dom.CanvasRenderingContext2D, center: Point, radius: Double, colour: String): Unit = {
    renderer.beginPath()
    renderer.strokeStyle = colour
    renderer.arc(center.x, center.y, radius, 0, math.Pi * 2, true)
    renderer.stroke()
  }

  final case class Point(x: Double, y: Double) {

    def up(amount: Double): Point = Point(x, y - amount)

    def down(amount: Double): Point = Point(x, y + amount)

    def left(amount: Double): Point = Point(x - amount, y)

    def right(amount: Double): Point = Point(x + amount, y)
  }
}
