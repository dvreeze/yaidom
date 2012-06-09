/*
 * Copyright 2011 Chris de Vreeze
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

package eu.cdevreeze.yaidom
package integrationtest

import java.{ util => jutil, io => jio }
import javax.xml.parsers._
import javax.xml.transform.TransformerFactory
import scala.collection.immutable
import scala.io.Codec
import org.junit.{ Test, Before, Ignore }
import org.junit.runner.RunWith
import org.scalatest.{ Suite, BeforeAndAfterAll }
import org.scalatest.junit.JUnitRunner
import eu.cdevreeze.yaidom.Predef._
import NodeBuilder._
import parse._
import print._

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

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.integrationtest")

  @Test def testBasicQuerying() {
    // No XML literals for yaidom, but making the structure of the XML explicit, using NodeBuilders
    // Should we make it easy to convert Scala XML literals to yaidom Elems?

    val foo =
      elem(
        qname = QName("foo"),
        children = List(
          elem(
            qname = QName("bar"),
            attributes = Map(QName("type") -> "greet"),
            children = List(text("hi"))),
          elem(
            qname = QName("bar"),
            attributes = Map(QName("type") -> "count"),
            children = List(text("1"))),
          elem(
            qname = QName("bar"),
            attributes = Map(QName("type") -> "color"),
            children = List(text("yellow"))))).build()

    // foo.text returns the empty string in yaidom's case, but it is still easy to get all text inside foo
    expect("hi1yellow") {
      foo.allChildElems.map(_.text).mkString
    }
    expect("hi1yellow") {
      foo.removeAllInterElementWhitespace.findAllElems.map(_.text).mkString
    }
    expect("hi1yellow") {
      foo.removeAllInterElementWhitespace.findAllElemsOrSelf.map(_.text).mkString
    }

    val bars = foo \ "bar"

    expect(List("bar", "bar", "bar")) {
      bars map { _.localName }
    }

    expect("hi 1 yellow") {
      val bars = foo \ "bar"
      bars map { _.text } mkString " "
    }

    expect(List("greet", "count", "color")) {
      (foo \ "bar") map { _.attributeOption("type".ename).getOrElse("") }
    }
    expect(List("greet", "count", "color")) {
      // Knowing that the attribute "type" is always present
      (foo \ "bar") map { _.attribute("type".ename) }
    }

    expect(List("greet" -> "hi", "count" -> "1", "color" -> "yellow")) {
      (foo \ "bar") map { e => (e.attribute("type".ename) -> e.text) }
    }

    // Again verbose but "precise" creation of an Elem
    val baz =
      elem(
        qname = QName("a"),
        children = List(
          elem(
            qname = QName("z"),
            attributes = Map(QName("x") -> "1")),
          elem(
            qname = QName("b"),
            children = List(
              elem(
                qname = QName("z"),
                attributes = Map(QName("x") -> "2")),
              elem(
                qname = QName("c"),
                children = List(
                  elem(
                    qname = QName("z"),
                    attributes = Map(QName("x") -> "3")))),
              elem(
                qname = QName("z"),
                attributes = Map(QName("x") -> "4")))))).build()

    val zs = baz \\ "z"

    expect(List("z", "z", "z", "z")) {
      zs map { _.localName }
    }

    expect(List("1", "2", "3", "4")) {
      (baz \\ "z") map { _.attribute("x".ename) }
    }

    val fooString = """<foo><bar type="greet">hi</bar><bar type="count">1</bar><bar type="color">yellow</bar></foo>"""

    // Using JAXP parsers (with all the possible parser configuration whenever needed) instead of drinking the Kool-Aid
    val docParser = parse.DocumentParserUsingSax.newInstance
    val fooElemFromString: Elem = docParser.parse(new jio.ByteArrayInputStream(fooString.getBytes("utf-8"))).documentElement

    // No, I do not believe in some magic implicit notion of equality for XML, for many reasons
    // (whitespace handling, namespaces/prefixes, dependency on external files, comments, etc. etc.)
    expect(false) {
      foo == fooElemFromString
    }

    // Let's take control of the equality we want here
    expect(true) {
      val fooResolved = resolved.Elem(foo).removeAllInterElementWhitespace
      val fooFromStringResolved = resolved.Elem(fooElemFromString).removeAllInterElementWhitespace
      fooResolved == fooFromStringResolved
    }
  }

  @Test def testConversions() {
    val docParser = DocumentParserUsingSax.newInstance
    val musicElm: Elem = docParser.parse(classOf[BasicXmlProcessingTest].getResourceAsStream("music.xml")).documentElement

    val songs = (musicElm \\ "song") map { songElm =>
      Song(songElm.attribute("title".ename), songElm.attribute("length".ename))
    }

    val totalTime = songs.map(_.timeInSeconds).sum

    expect(11311) {
      totalTime
    }

    val artists = (musicElm \ "artist") map { artistElm =>
      val name = artistElm.attribute("name".ename)

      val albums = (artistElm \ "album") map { albumElm =>
        val title = albumElm.attribute("title".ename)
        val description = albumElm.getChildElemNamed("description".ename).text

        val songs = (albumElm \ "song") map { songElm =>
          Song(songElm.attribute("title".ename), songElm.attribute("length".ename))
        }

        Album(title, songs, description)
      }

      Artist(name, albums)
    }

    val albumLengths = artists flatMap { artist =>
      artist.albums map { album => (artist.name, album.title, album.length) }
    }

    expect(4) {
      albumLengths.size
    }
    expect(("Radiohead", "The King of Limbs", "37:34")) {
      albumLengths(0)
    }
    expect(("Portishead", "Third", "48:50")) {
      albumLengths(3)
    }

    // Marshalling the NodeBuilder way

    val musicElm2 =
      elem(
        qname = QName("music"),
        children = artists map { artist =>
          elem(
            qname = QName("artist"),
            attributes = Map(QName("name") -> artist.name),
            children = artist.albums map { album =>
              elem(
                qname = QName("album"),
                attributes = Map(QName("title") -> album.title),
                children = {
                  val songChildren: immutable.Seq[NodeBuilder] = album.songs map { song =>
                    elem(
                      qname = QName("song"),
                      attributes = Map(QName("title") -> song.title, QName("length") -> song.length))
                  }

                  val descriptionElm = elem(
                    qname = QName("description"),
                    children = List(text(album.description)))

                  songChildren :+ descriptionElm
                })
            })
        }).build()

    val docPrinter = DocumentPrinterUsingSax.newInstance
    val musicXml = docPrinter.print(musicElm2)

    val musicElm3 = docParser.parse(new jio.ByteArrayInputStream(musicXml.getBytes(Codec.UTF8.toString))).documentElement

    val musicElmWithoutLinks: Elem =
      musicElm updated {
        case path: ElemPath if path.lastEntryOption.map(_.elementName.localPart).getOrElse("") == "description" =>
          val e = musicElm.findWithElemPath(path).get
          Elem(e.qname, e.attributes - QName("link"), e.scope, e.children)
      }

    expect(resolved.Elem(musicElmWithoutLinks).removeAllInterElementWhitespace) {
      resolved.Elem(musicElm2).removeAllInterElementWhitespace
    }
    expect(resolved.Elem(musicElmWithoutLinks).removeAllInterElementWhitespace) {
      resolved.Elem(musicElm3).removeAllInterElementWhitespace
    }
  }

  final case class Song(val title: String, val length: String) extends Immutable {
    val timeInSeconds: Int = {
      val arr = length.split(":")
      require(arr.length == 2)
      val minutes = arr(0).toInt
      val seconds = arr(1).toInt
      (minutes * 60) + seconds
    }
  }

  final case class Album(val title: String, val songs: immutable.Seq[Song], val description: String) extends Immutable {
    val timeInSeconds: Int = songs.map(_.timeInSeconds).sum
    val length: String = (timeInSeconds / 60).toString + ":" + (timeInSeconds % 60).toString
  }

  final case class Artist(val name: String, val albums: immutable.Seq[Album]) extends Immutable
}
