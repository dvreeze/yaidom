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
package literal

import java.io._

/**
 * Helper object for creating yaidom XML literals.
 *
 * The XML literals can only have parameters for yaidom Nodes and Node sequences and attribute values (but not for namespace URIs).
 *
 * The underlying parser can not yet be configured.
 *
 * @author Chris de Vreeze
 */
object XmlLiterals {

  implicit class XmlHelper(val sc: StringContext) {

    val placeholderPrefix = "___par_"

    def xml(args: Any*): Document = {
      require(sc.parts forall (part => !part.contains(placeholderPrefix)),
        "The XML must not contain placeholder prefix %s".format(placeholderPrefix))

      require(
        args forall (arg => hasAllowedArgumentType(arg)),
        "All arguments must be of an allowed argument type (String, Node or Seq[Node])")

      val strings: Seq[String] = sc.parts

      def canBeAttributeValue(i: Int): Boolean = argCanBeAttributeValue(args(i), sc.parts(i), sc.parts(i + 1))

      def canBeChildNodes(i: Int): Boolean = argCanBeChildNodes(args(i), sc.parts(i), sc.parts(i + 1))

      require(
        (0 until args.size) forall { i => canBeAttributeValue(i) || canBeChildNodes(i) },
        "All arguments must be potential attribute values or child node sequences")
      assert(
        (0 until args.size) forall { i => !(canBeAttributeValue(i) && canBeChildNodes(i)) })

      val xmlWithPlaceholders: String = {
        val sb = new StringBuilder
        sb ++= strings.headOption.getOrElse("")

        val tail = strings.drop(1)
        for (i <- 0 until tail.size) {
          if (canBeAttributeValue(i)) {
            sb ++= quotedPlaceholder(i)
          } else {
            assert(canBeChildNodes(i))
            sb ++= placeholderName(i)
          }
          sb ++= tail(i)
        }

        sb.toString
      }

      val docParser = getParser

      // The Document with (possibly quoted) placeholders
      // TODO UTF-8?
      val doc: Document = docParser.parse(new ByteArrayInputStream(xmlWithPlaceholders.getBytes("UTF-8")))

      val parentPaths: Map[Int, ElemPath] = findArgumentParentElemPaths(doc, args)

      val unmatchedArgIndexes = (0 until args.size).toSet.diff(parentPaths.keySet)
      require(
        unmatchedArgIndexes.isEmpty,
        "Not all arguments are placed correctly. Offending argument indexes (0-based): %s".format(unmatchedArgIndexes.mkString(", ")))

      val resultDoc: Document =
        (0 until args.length).reverse.foldLeft(doc) { (tmpDoc, idx) =>
          val placeholder = placeholderName(idx)

          val path = parentPaths(idx)
          val e = doc.documentElement.getWithElemPath(path)

          if (canBeChildNodes(idx) && (e.text.trim == placeholder)) {
            val nodes = extractNodes(args(idx))
            require(!nodes.isEmpty, "Expected Node, Node sequence or String for parameter %d (0-based)".format(idx))

            tmpDoc.updated(path) { e =>
              val newChildren = nodes.toIndexedSeq map {
                case ch: Elem => NodeBuilder.fromElem(ch)(Scope.Empty).build(e.scope)
                case ch => ch
              }
              val newE = e.withChildren(newChildren)
              newE
            }
          } else if (canBeAttributeValue(idx) && e.attributes.map(_._2).contains(placeholder)) {
            val attrOption = e.attributes find { case (attr, value) => value == placeholder }
            require(attrOption.isDefined, "Expected attribute for parameter %d (0-based)".format(idx))
            val (attrName, attrValue) = attrOption.get

            tmpDoc.updated(path) { e =>
              val newE = e.plusAttribute(attrName, args(idx).toString)
              newE
            }
          } else sys.error("Argument %d (0-based) is not at an allowed position in the XML".format(idx))
        }
      resultDoc
    }

    private def placeholderName(idx: Int): String = placeholderPrefix + idx

    private def quotedPlaceholder(idx: Int): String = """"%s"""".format(placeholderName(idx))

    private def argCanBeAttributeValue(arg: Any, partBefore: String, partAfter: String): Boolean = arg match {
      case s: String if partBefore.endsWith("=") => true
      case _ => false
    }

    private def argCanBeChildNodes(arg: Any, partBefore: String, partAfter: String): Boolean = arg match {
      case s: String if partBefore.trim.endsWith(">") && partAfter.trim.startsWith("</") => true
      case n: Node if partBefore.trim.endsWith(">") && partAfter.trim.startsWith("</") => true
      case xs if isNodeSeq(xs) && partBefore.trim.endsWith(">") && partAfter.trim.startsWith("</") => true
      case _ => false
    }

    private def hasAllowedArgumentType(arg: Any): Boolean = arg match {
      case s: String => true
      case n: Node => true
      case xs: Seq[_] if isNodeSeq(xs) => true
      case _ => false
    }

    private def isNodeSeq(arg: Any): Boolean = arg match {
      case xs: Seq[_] if xs.forall(x => x.isInstanceOf[Node]) => true
      case _ => false
    }

    private def extractNodes(arg: Any): Seq[Node] = arg match {
      case s: String => Seq(Text(s, false))
      case n: Node => Seq(n)
      case xs: Seq[_] if xs.forall(x => x.isInstanceOf[Node]) => xs.asInstanceOf[Seq[Node]]
      case _ => Seq()
    }

    private def findArgumentParentElemPaths(doc: Document, args: Seq[Any]): Map[Int, ElemPath] = {
      def canBeAttributeValue(i: Int): Boolean = argCanBeAttributeValue(args(i), sc.parts(i), sc.parts(i + 1))

      def canBeChildNodes(i: Int): Boolean = argCanBeChildNodes(args(i), sc.parts(i), sc.parts(i + 1))

      val result: Seq[(Int, ElemPath)] = (0 until args.length) flatMap { idx =>
        val elemOption = doc.documentElement findElemOrSelf { e =>
          val matchesAttributeValue =
            canBeAttributeValue(idx) && (e.attributes.map(_._2).contains(placeholderName(idx)))
          val matchesChildNodes =
            canBeChildNodes(idx) && (e.text.trim == placeholderName(idx))
          matchesAttributeValue || matchesChildNodes
        }
        val elemPathOption = elemOption flatMap { e => doc.documentElement findElemOrSelfPath (_ == e) }
        elemPathOption map { path => (idx -> path) }
      }
      result.toMap
    }

    private def getParser: parse.DocumentParser = {
      val result = parse.DocumentParserUsingSax.newInstance
      // TODO Configure
      result
    }
  }
}
