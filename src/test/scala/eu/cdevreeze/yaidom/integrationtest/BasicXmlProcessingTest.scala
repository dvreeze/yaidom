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
import javax.xml.parsers._
import javax.xml.transform.TransformerFactory
import scala.collection.immutable
import scala.io.Codec
import org.junit.{ Test, Before, Ignore }
import org.junit.runner.RunWith
import org.scalatest.{ Suite, BeforeAndAfterAll }
import org.scalatest.junit.JUnitRunner
import eu.cdevreeze.yaidom.simple.NodeBuilder
import eu.cdevreeze.yaidom.simple.NodeBuilder._
import eu.cdevreeze.yaidom.parse._
import eu.cdevreeze.yaidom.print._
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.simple.Elem
import eu.cdevreeze.yaidom.parse
import eu.cdevreeze.yaidom.resolved

/**
 * Example http://www.javacodegeeks.com/2012/05/scala-basic-xml-processing.html (for Scala's XML library) "ported" to
 * yaidom, as test case.
 *
 * Yaidom definitely is more verbose than Scala's XML API, but also easier to reason about. It is more verbose, but more
 * precise, especially when it comes to names (qualified names and expanded names).
 *
 * The precision of yaidom becomes more apparent when using namespaces, but they are not used in this test case.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class BasicXmlProcessingTest extends Suite {

  import BasicXmlProcessingTest._

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.integrationtest")

  @Test def testBasicQuerying(): Unit = {
    // No XML literals for yaidom, but making the structure of the XML explicit, using NodeBuilders
    // Should we make it easy to convert Scala XML literals to yaidom Elems?

    val foo =
      elem(
        qname = QName("foo"),
        children = Vector(
          textElem(
            qname = QName("bar"),
            attributes = Vector(QName("type") -> "greet"),
            txt = "hi"),
          textElem(
            qname = QName("bar"),
            attributes = Vector(QName("type") -> "count"),
            txt = "1"),
          textElem(
            qname = QName("bar"),
            attributes = Vector(QName("type") -> "color"),
            txt = "yellow"))).build()

    // foo.text returns the empty string in yaidom's case, but it is still easy to get all text inside foo
    assertResult("hi1yellow") {
      foo.findAllChildElems.map(_.text).mkString
    }
    assertResult("hi1yellow") {
      foo.removeAllInterElementWhitespace.findAllElems.map(_.text).mkString
    }
    assertResult("hi1yellow") {
      foo.removeAllInterElementWhitespace.findAllElemsOrSelf.map(_.text).mkString
    }

    val bars = foo \ (_.localName == "bar")

    assertResult(List("bar", "bar", "bar")) {
      bars map { _.localName }
    }

    assertResult("hi 1 yellow") {
      val bars = foo \ (_.localName == "bar")
      bars map { _.text } mkString " "
    }

    assertResult(List("greet", "count", "color")) {
      (foo \ (_.localName == "bar")) map { _.attributeOption(EName("type")).getOrElse("") }
    }
    assertResult(List("greet", "count", "color")) {
      // Knowing that the attribute "type" is always present
      (foo \ (_.localName == "bar")) map { _.attribute(EName("type")) }
    }

    assertResult(List("greet" -> "hi", "count" -> "1", "color" -> "yellow")) {
      (foo \ (_.localName == "bar")) map { e => (e.attribute(EName("type")) -> e.text) }
    }

    // Again verbose but "precise" creation of an Elem
    val baz =
      elem(
        qname = QName("a"),
        children = Vector(
          elem(
            qname = QName("z"),
            attributes = Vector(QName("x") -> "1")),
          elem(
            qname = QName("b"),
            children = Vector(
              elem(
                qname = QName("z"),
                attributes = Vector(QName("x") -> "2")),
              elem(
                qname = QName("c"),
                children = Vector(
                  elem(
                    qname = QName("z"),
                    attributes = Vector(QName("x") -> "3")))),
              elem(
                qname = QName("z"),
                attributes = Vector(QName("x") -> "4")))))).build()

    val zs = baz \\ (_.localName == "z")

    assertResult(List("z", "z", "z", "z")) {
      zs map { _.localName }
    }

    assertResult(List("1", "2", "3", "4")) {
      (baz \\ (_.localName == "z")) map { _.attribute(EName("x")) }
    }

    val fooString = """<foo><bar type="greet">hi</bar><bar type="count">1</bar><bar type="color">yellow</bar></foo>"""

    // Using JAXP parsers (with all the possible parser configuration whenever needed) instead of drinking the Kool-Aid
    val docParser = parse.DocumentParserUsingSax.newInstance
    val fooElemFromString: Elem = docParser.parse(new jio.ByteArrayInputStream(fooString.getBytes("utf-8"))).documentElement

    // No, I do not believe in some magic implicit notion of equality for XML, for many reasons
    // (whitespace handling, namespaces/prefixes, dependency on external files, comments, etc. etc.)
    assertResult(false) {
      foo == fooElemFromString
    }

    // Let's take control of the equality we want here
    assertResult(true) {
      val fooResolved = resolved.Elem(foo).removeAllInterElementWhitespace
      val fooFromStringResolved = resolved.Elem(fooElemFromString).removeAllInterElementWhitespace
      fooResolved == fooFromStringResolved
    }
  }

  @Test def testConversions(): Unit = {
    val docParser = DocumentParserUsingSax.newInstance
    val musicElm: Elem = docParser.parse(classOf[BasicXmlProcessingTest].getResourceAsStream("music.xml")).documentElement

    val songs = (musicElm \\ (_.localName == "song")) map { songElm =>
      Song(songElm.attribute(EName("title")), songElm.attribute(EName("length")))
    }

    val totalTime = songs.map(_.timeInSeconds).sum

    assertResult(11311) {
      totalTime
    }

    val artists = (musicElm \ (_.localName == "artist")) map { artistElm =>
      val name = artistElm.attribute(EName("name"))

      val albums = (artistElm \ (_.localName == "album")) map { albumElm =>
        val title = albumElm.attribute(EName("title"))
        val description = albumElm.getChildElem(EName("description")).text

        val songs = (albumElm \ (_.localName == "song")) map { songElm =>
          Song(songElm.attribute(EName("title")), songElm.attribute(EName("length")))
        }

        Album(title, songs, description)
      }

      Artist(name, albums)
    }

    val albumLengths = artists flatMap { artist =>
      artist.albums map { album => (artist.name, album.title, album.length) }
    }

    assertResult(4) {
      albumLengths.size
    }
    assertResult(("Radiohead", "The King of Limbs", "37:34")) {
      albumLengths(0)
    }
    assertResult(("Portishead", "Third", "48:50")) {
      albumLengths(3)
    }

    // Marshalling the NodeBuilder way

    val musicElm2 =
      elem(
        qname = QName("music"),
        children = artists map { artist =>
          elem(
            qname = QName("artist"),
            attributes = Vector(QName("name") -> artist.name),
            children = artist.albums map { album =>
              elem(
                qname = QName("album"),
                attributes = Vector(QName("title") -> album.title),
                children = {
                  val songChildren: immutable.IndexedSeq[NodeBuilder] = album.songs map { song =>
                    elem(
                      qname = QName("song"),
                      attributes = Vector(QName("title") -> song.title, QName("length") -> song.length))
                  }

                  val descriptionElm = textElem(QName("description"), album.description)

                  songChildren :+ descriptionElm
                })
            })
        }).build()

    val docPrinter = DocumentPrinterUsingSax.newInstance
    val musicXml = docPrinter.print(musicElm2)

    val musicElm3 = docParser.parse(new jio.ByteArrayInputStream(musicXml.getBytes(Codec.UTF8.toString))).documentElement

    val musicElmWithoutLinks: Elem =
      musicElm transformElemsOrSelf {
        case e if e.localName == "description" =>
          val updatedAttrs = e.attributes filterNot { case (qn, v) => qn == QName("link") }
          Elem(e.qname, updatedAttrs, e.scope, e.children)
        case e => e
      }

    assertResult(resolved.Elem(musicElmWithoutLinks).removeAllInterElementWhitespace.findAllElemsOrSelf.size) {
      resolved.Elem(musicElm2).removeAllInterElementWhitespace.findAllElemsOrSelf.size
    }
    assertResult(resolved.Elem(musicElmWithoutLinks).removeAllInterElementWhitespace.coalesceAndNormalizeAllText) {
      resolved.Elem(musicElm2).removeAllInterElementWhitespace.coalesceAndNormalizeAllText
    }

    assertResult(resolved.Elem(musicElmWithoutLinks).removeAllInterElementWhitespace.findAllElemsOrSelf.size) {
      resolved.Elem(musicElm3).removeAllInterElementWhitespace.findAllElemsOrSelf.size
    }
    assertResult(resolved.Elem(musicElmWithoutLinks).removeAllInterElementWhitespace.coalesceAndNormalizeAllText) {
      resolved.Elem(musicElm3).removeAllInterElementWhitespace.coalesceAndNormalizeAllText
    }
  }
}

object BasicXmlProcessingTest {

  final case class Song(val title: String, val length: String) extends Immutable {
    val timeInSeconds: Int = {
      val arr = length.split(":")
      require(arr.length == 2)
      val minutes = arr(0).toInt
      val seconds = arr(1).toInt
      (minutes * 60) + seconds
    }
  }

  final case class Album(val title: String, val songs: immutable.IndexedSeq[Song], val description: String) extends Immutable {
    val timeInSeconds: Int = songs.map(_.timeInSeconds).sum
    val length: String = (timeInSeconds / 60).toString + ":" + (timeInSeconds % 60).toString
  }

  final case class Artist(val name: String, val albums: immutable.IndexedSeq[Album]) extends Immutable
}
