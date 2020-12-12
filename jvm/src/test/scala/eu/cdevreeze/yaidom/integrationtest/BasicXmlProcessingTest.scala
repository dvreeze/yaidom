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

import java.{io => jio}

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.parse.DocumentParserUsingSax
import eu.cdevreeze.yaidom.print.DocumentPrinterUsingSax
import eu.cdevreeze.yaidom.resolved
import eu.cdevreeze.yaidom.simple.Elem
import eu.cdevreeze.yaidom.simple.Node
import eu.cdevreeze.yaidom.simple.Node._
import org.scalatest.funsuite.AnyFunSuite
import org.xml.sax.InputSource

import scala.collection.immutable

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
class BasicXmlProcessingTest extends AnyFunSuite {

  import BasicXmlProcessingTest._

  test("testBasicQuerying") {
    // No XML literals for yaidom, but making the structure of the XML explicit, using NodeBuilders
    // Should we make it easy to convert Scala XML literals to yaidom Elems?

    val foo =
      elem(
        qname = QName("foo"),
        scope = Scope.Empty,
        children = Vector(
          textElem(
            qname = QName("bar"),
            attributes = Vector(QName("type") -> "greet"),
            scope = Scope.Empty,
            txt = "hi"),
          textElem(qname = QName("bar"), attributes = Vector(QName("type") -> "count"), scope = Scope.Empty, txt = "1"),
          textElem(
            qname = QName("bar"),
            attributes = Vector(QName("type") -> "color"),
            scope = Scope.Empty,
            txt = "yellow")
        )
      )

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
      bars.map {
        _.localName
      }
    }

    assertResult("hi 1 yellow") {
      val bars = foo \ (_.localName == "bar")
      bars
        .map {
          _.text
        }
        .mkString(" ")
    }

    assertResult(List("greet", "count", "color")) {
      (foo \ (_.localName == "bar")).map {
        _.attributeOption(EName("type")).getOrElse("")
      }
    }
    assertResult(List("greet", "count", "color")) {
      // Knowing that the attribute "type" is always present
      (foo \ (_.localName == "bar")).map {
        _.attribute(EName("type"))
      }
    }

    assertResult(List("greet" -> "hi", "count" -> "1", "color" -> "yellow")) {
      (foo \ (_.localName == "bar")).map { e =>
        e.attribute(EName("type")) -> e.text
      }
    }

    // Again verbose but "precise" creation of an Elem
    val baz =
      elem(
        qname = QName("a"),
        scope = Scope.Empty,
        children = Vector(
          emptyElem(qname = QName("z"), attributes = Vector(QName("x") -> "1"), scope = Scope.Empty),
          elem(
            qname = QName("b"),
            scope = Scope.Empty,
            children = Vector(
              emptyElem(qname = QName("z"), attributes = Vector(QName("x") -> "2"), scope = Scope.Empty),
              elem(
                qname = QName("c"),
                scope = Scope.Empty,
                children =
                  Vector(emptyElem(qname = QName("z"), attributes = Vector(QName("x") -> "3"), scope = Scope.Empty))),
              emptyElem(qname = QName("z"), attributes = Vector(QName("x") -> "4"), scope = Scope.Empty)
            )
          )
        )
      )

    val zs = baz \\ (_.localName == "z")

    assertResult(List("z", "z", "z", "z")) {
      zs.map {
        _.localName
      }
    }

    assertResult(List("1", "2", "3", "4")) {
      (baz \\ (_.localName == "z")).map {
        _.attribute(EName("x"))
      }
    }

    val fooString = """<foo><bar type="greet">hi</bar><bar type="count">1</bar><bar type="color">yellow</bar></foo>"""

    // Using JAXP parsers (with all the possible parser configuration whenever needed) instead of drinking the Kool-Aid
    val docParser = DocumentParserUsingSax.newInstance()
    val fooElemFromString: Elem = docParser.parse(new InputSource(new jio.StringReader(fooString))).documentElement

    // No, I do not believe in some magic implicit notion of equality for XML, for many reasons
    // (whitespace handling, namespaces/prefixes, dependency on external files, comments, etc. etc.)
    assertResult(false) {
      foo == fooElemFromString
    }

    // Let's take control of the equality we want here
    assertResult(true) {
      val fooResolved = resolved.Elem.from(foo).removeAllInterElementWhitespace
      val fooFromStringResolved = resolved.Elem.from(fooElemFromString).removeAllInterElementWhitespace
      fooResolved == fooFromStringResolved
    }
  }

  test("testConversions") {
    val docParser = DocumentParserUsingSax.newInstance()
    val musicElm: Elem =
      docParser.parse(classOf[BasicXmlProcessingTest].getResourceAsStream("music.xml")).documentElement

    val songs = (musicElm \\ (_.localName == "song")).map { songElm =>
      Song(songElm.attribute(EName("title")), songElm.attribute(EName("length")))
    }

    val totalTime = songs.map(_.timeInSeconds).sum

    assertResult(11311) {
      totalTime
    }

    val artists = (musicElm \ (_.localName == "artist")).map { artistElm =>
      val name = artistElm.attribute(EName("name"))

      val albums = (artistElm \ (_.localName == "album")).map { albumElm =>
        val title = albumElm.attribute(EName("title"))
        val description = albumElm.getChildElem(EName("description")).text

        val songs = (albumElm \ (_.localName == "song")).map { songElm =>
          Song(songElm.attribute(EName("title")), songElm.attribute(EName("length")))
        }

        Album(title, songs, description)
      }

      Artist(name, albums)
    }

    val albumLengths = artists.flatMap { artist =>
      artist.albums.map { album =>
        (artist.name, album.title, album.length)
      }
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

    val musicElm2: Elem =
      elem(
        qname = QName("music"),
        scope = Scope.Empty,
        children = artists.map { artist =>
          elem(
            qname = QName("artist"),
            attributes = Vector(QName("name") -> artist.name),
            scope = Scope.Empty,
            children = artist.albums.map {
              album =>
                elem(
                  qname = QName("album"),
                  attributes = Vector(QName("title") -> album.title),
                  scope = Scope.Empty,
                  children = {
                    val songChildren: immutable.IndexedSeq[Node] = album.songs.map { song =>
                      emptyElem(
                        qname = QName("song"),
                        attributes = Vector(QName("title") -> song.title, QName("length") -> song.length),
                        scope = Scope.Empty)
                    }

                    val descriptionElm = textElem(QName("description"), Scope.Empty, album.description)

                    songChildren :+ descriptionElm
                  }
                )
            }
          )
        }
      )

    val docPrinter = DocumentPrinterUsingSax.newInstance()
    val musicXml = docPrinter.print(musicElm2)

    val musicElm3 = docParser.parse(new InputSource(new jio.StringReader(musicXml))).documentElement

    val musicElmWithoutLinks: Elem =
      musicElm.transformElemsOrSelf {
        case e if e.localName == "description" =>
          val updatedAttrs = e.attributes.filterNot { case (qn, _) => qn == QName("link") }
          Elem(e.qname, updatedAttrs, e.scope, e.children)
        case e => e
      }

    assertResult(resolved.Elem.from(musicElmWithoutLinks).removeAllInterElementWhitespace.findAllElemsOrSelf.size) {
      resolved.Elem.from(musicElm2).removeAllInterElementWhitespace.findAllElemsOrSelf.size
    }
    assertResult(resolved.Elem.from(musicElmWithoutLinks).removeAllInterElementWhitespace.coalesceAndNormalizeAllText) {
      resolved.Elem.from(musicElm2).removeAllInterElementWhitespace.coalesceAndNormalizeAllText
    }

    assertResult(resolved.Elem.from(musicElmWithoutLinks).removeAllInterElementWhitespace.findAllElemsOrSelf.size) {
      resolved.Elem.from(musicElm3).removeAllInterElementWhitespace.findAllElemsOrSelf.size
    }
    assertResult(resolved.Elem.from(musicElmWithoutLinks).removeAllInterElementWhitespace.coalesceAndNormalizeAllText) {
      resolved.Elem.from(musicElm3).removeAllInterElementWhitespace.coalesceAndNormalizeAllText
    }
  }
}

object BasicXmlProcessingTest {

  final case class Song(title: String, length: String) {
    val timeInSeconds: Int = {
      val arr = length.split(":")
      require(arr.length == 2)
      val minutes = arr(0).toInt
      val seconds = arr(1).toInt
      (minutes * 60) + seconds
    }
  }

  final case class Album(title: String, songs: immutable.IndexedSeq[Song], description: String) {
    val timeInSeconds: Int = songs.map(_.timeInSeconds).sum
    val length: String = (timeInSeconds / 60).toString + ":" + (timeInSeconds % 60).toString
  }

  final case class Artist(name: String, albums: immutable.IndexedSeq[Album])

}
