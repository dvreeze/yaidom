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
import javax.xml.parsers.{ SAXParserFactory, SAXParser }
import org.xml.sax.{ EntityResolver, InputSource, ErrorHandler, SAXParseException, ContentHandler }
import org.xml.sax.helpers.DefaultHandler
import scala.collection.{ immutable, mutable }
import scala.annotation.tailrec

/**
 * Helper object for creating yaidom XML literals.
 *
 * The XML literals can only have parameters for yaidom Nodes and Node sequences and attribute values (but not for namespace URIs).
 *
 * The underlying parser can not yet be configured.
 *
 * Note: Try to avoid very large String arguments (in the StringContext). Otherwise the JVM may respond with:
 * "java.lang.ClassFormatError: Unknown constant tag XX in class file", where XX is a number (e.g. 32). This is a 64K
 * String limit of the JVM. Fortunately, there is usually a way to avoid very large String arguments.
 *
 * @author Chris de Vreeze
 */
object XmlLiterals {

  implicit class XmlHelper(val sc: StringContext) {

    private val placeholderPrefix = "___par_"
    private val childrenPlaceholderPrefix = placeholderPrefix + "children_"
    private val attrValuePlaceholderPrefix = placeholderPrefix + "attrvalue_"

    def xml(args: Any*): Document = {
      assert(sc.parts.size == 1 + args.size)

      require(sc.parts forall (part => !part.contains(placeholderPrefix)),
        "The XML must not contain placeholder prefix %s".format(placeholderPrefix))

      require(
        args forall (arg => hasAllowedArgumentType(arg)),
        "All arguments must be of an allowed argument type (String, Node or Seq[Node])")

      val parts: Seq[String] = sc.parts

      val partsWithoutComments: Seq[String] = parts map { part => removeComments(part) }

      val partsWithoutCommentsAndCData: Seq[String] = partsWithoutComments map { part => removeCData(part) }

      // Now we have a better view on what kinds of nodes the arguments are in the XML tree, without having to parse the tree

      def canBeAttributeValue(i: Int): Boolean =
        argCanBeAttributeValue(args(i), partsWithoutCommentsAndCData(i), partsWithoutCommentsAndCData(i + 1))

      def canBeChildNodes(i: Int): Boolean =
        argCanBeChildNodes(args(i), partsWithoutCommentsAndCData(i), partsWithoutCommentsAndCData(i + 1))

      val saxParser = getSaxParserForPartParsing

      def checkContextOfAttrValue(i: Int) {
        checkContextOfAttributeValue(args(i), partsWithoutCommentsAndCData(i), partsWithoutCommentsAndCData(i + 1))(saxParser, getSaxHandlerForPartParsing)
      }

      def checkContextOfChildren(i: Int) {
        checkContextOfChildNodes(args(i), partsWithoutCommentsAndCData(i), partsWithoutCommentsAndCData(i + 1))(saxParser, getSaxHandlerForPartParsing)
      }

      require(
        (0 until args.size) forall { i => canBeAttributeValue(i) || canBeChildNodes(i) },
        "All arguments must be potential attribute values or child node sequences")
      assert(
        (0 until args.size) forall { i => !(canBeAttributeValue(i) && canBeChildNodes(i)) },
        "All arguments must EITHER be potential attribute values OR child node sequences")

      // Let's create the (hopefully valid) XML with placeholders

      val xmlWithPlaceholders: String = {
        val sb = new StringBuilder
        sb ++= parts.headOption.getOrElse("")

        val partsButFirst = parts.drop(1)

        assert(args.size == partsButFirst.size)

        for (i <- 0 until partsButFirst.size) {
          if (canBeAttributeValue(i)) {
            checkContextOfAttrValue(i)

            sb ++= quotedAttrValuePlaceholder(i)
          } else {
            assert(canBeChildNodes(i))
            checkContextOfChildren(i)

            sb ++= childrenPlaceholderText(i)
          }
          sb ++= partsButFirst(i)
        }

        sb.toString
      }

      val editedXmlWithPlaceholders: String = replaceXmlDeclaration(xmlWithPlaceholders)

      val docParser = getParser

      // Parse the XML with (possibly quoted) placeholders into a Document with those placeholders
      // The parser will treat the XML bytes as UTF-8

      val doc: Document = docParser.parse(new ByteArrayInputStream(editedXmlWithPlaceholders.getBytes("UTF-8")))

      // Replace the placeholders by Strings or Nodes. To that end, first look up the parameter ElemPaths.

      val parameterParentPaths: Seq[ParameterParentElemPath] = findParameterParentElemPaths(indexed.Document(doc), args)

      val unmatchedParIndexes = (0 until args.size).toSet.diff(parameterParentPaths.map(_.parameterIndex).toSet)
      require(
        unmatchedParIndexes.isEmpty,
        "Not all arguments are placed correctly. Offending argument indexes (0-based): %s".format(unmatchedParIndexes.mkString(", ")))

      for (parParentPath <- parameterParentPaths) {
        val parIdx = parParentPath.parameterIndex
        val kind = parParentPath.parameterKind

        if (kind == AttributeValueKind)
          require(canBeAttributeValue(parIdx), "Expected valid attribute value at an attribute value position")
        else
          require(canBeChildNodes(parIdx), "Expected valid child nodes at a child nodes position")
      }

      val resultDoc: Document =
        parameterParentPaths.reverse.foldLeft(doc) { (tmpDoc, parParentPath) =>
          val parIdx = parParentPath.parameterIndex

          val elem = doc.documentElement.getWithElemPath(parParentPath.path)

          if (parParentPath.parameterKind == ChildNodesKind) {
            val nodes = extractNodes(args(parIdx))
            require(!nodes.isEmpty, "Expected Node, Node sequence or String for parameter %d (0-based)".format(parIdx))

            tmpDoc.updated(parParentPath.path) { e =>
              // Repairing namespaces in order to prevent namespace undeclarations (for prefixes)

              val newChildren = nodes.toIndexedSeq map {
                case ch: Elem => NodeBuilder.fromElem(ch)(Scope.Empty).build(e.scope)
                case ch => ch
              }
              val newE = e.withChildren(newChildren)
              newE
            }
          } else {
            assert(parParentPath.parameterKind == AttributeValueKind)

            val attrOption = elem.attributes find { case (attr, value) => value == attrValuePlaceholder(parIdx) }
            require(attrOption.isDefined, "Expected attribute for parameter %d (0-based)".format(parIdx))
            val (attrName, attrValue) = attrOption.get

            tmpDoc.updated(parParentPath.path) { e =>
              val newE = e.plusAttribute(attrName, args(parIdx).toString)
              newE
            }
          }
        }

      resultDoc
    }

    private def childrenPlaceholderText(idx: Int): String = childrenPlaceholderPrefix + idx

    private def attrValuePlaceholder(idx: Int): String = attrValuePlaceholderPrefix + idx

    private def quotedAttrValuePlaceholder(idx: Int): String = """"%s"""".format(attrValuePlaceholder(idx))

    /**
     * Returns true if the given argument, surrounded by the given "parts" (which should be free of comments and CDATA),
     * is possibly an attribute value. The argument type is checked, and the surroundings of the argument are checked somewhat.
     *
     * If true is returned, there is still no guarantee that the argument is indeed parsed as attribute value by the XML parser.
     */
    private def argCanBeAttributeValue(arg: Any, partBefore: String, partAfter: String): Boolean = arg match {
      case s: String if partBefore.endsWith("=") &&
        (partAfter.trim.startsWith(">") || partAfter.headOption.getOrElse('x').isWhitespace) => true
      case _ => false
    }

    /**
     * Returns true if the given argument, surrounded by the given "parts" (which should be free of comments and CDATA),
     * is possibly a sequence of nodes. The argument type is checked, and the surroundings of the argument are checked somewhat.
     *
     * If true is returned, there is still no guarantee that the argument is indeed parsed as node sequence by the XML parser.
     */
    private def argCanBeChildNodes(arg: Any, partBefore: String, partAfter: String): Boolean = arg match {
      case s: String if partBefore.trim.endsWith(">") && partAfter.trim.startsWith("</") => true
      case n: Node if partBefore.trim.endsWith(">") && partAfter.trim.startsWith("</") => true
      case xs if isNodeSeq(xs) && partBefore.trim.endsWith(">") && partAfter.trim.startsWith("</") => true
      case _ => false
    }

    /**
     * Performs a stronger check than `argCanBeAttributeValue`, throwing an exception if the check fails.
     *
     * In this check, a SAX parser tries to parse the surrounding element that may have the given attribute value.
     */
    private def checkContextOfAttributeValue(arg: Any, partBefore: String, partAfter: String)(saxParser: SAXParser, handler: DefaultHandler) {
      require(argCanBeAttributeValue(arg, partBefore, partAfter))

      val startElemIdx = partBefore lastIndexWhere { c => c == '<' }
      require(startElemIdx >= 0, "Expected element having an attribute with attribute value '%s'".format(arg.toString))
      val sameElemPartBefore = partBefore.substring(startElemIdx)

      val endElemIdx = partAfter indexWhere { c => c == '>' }
      require(endElemIdx >= 0, "Expected start tag having attribute with attribute value '%s' to end".format(arg.toString))
      val sameElemPartAfter = partAfter.take(endElemIdx + 1)
      val selfEndingElemPartAfter =
        if (sameElemPartAfter.endsWith("/>")) sameElemPartAfter else sameElemPartAfter.dropRight(1) + "/>"

      val emptyAttrValue = "\"\""
      val xmlString = sameElemPartBefore + emptyAttrValue + selfEndingElemPartAfter

      assert(xmlString.startsWith("<"))
      assert(xmlString.endsWith("/>"))
      require(xmlString.count(_ == '<') == 1,
        "Expected element having an attribute with attribute value '%s'".format(arg.toString))
      require(xmlString.count(_ == '>') == 1,
        "Expected element having an attribute with attribute value '%s'".format(arg.toString))

      val is = new ByteArrayInputStream(xmlString.getBytes("UTF-8"))

      saxParser.parse(is, handler)
    }

    /**
     * Performs a stronger check than `argCanBeChildNodes`, throwing an exception if the check fails.
     *
     * In this check, a SAX parser tries to parse the surrounding element that may have the given children.
     */
    private def checkContextOfChildNodes(arg: Any, partBefore: String, partAfter: String)(saxParser: SAXParser, handler: DefaultHandler) {
      require(argCanBeChildNodes(arg, partBefore, partAfter))

      val startElemIdx = partBefore lastIndexWhere { c => c == '<' }
      require(startElemIdx >= 0, "Expected element having parameter child nodes")
      val sameElemPartBefore = partBefore.substring(startElemIdx)

      val endElemIdx = partAfter indexWhere { c => c == '>' }
      require(endElemIdx >= 0, "Expected element having parameter child nodes")
      val sameElemPartAfter = partAfter.take(endElemIdx + 1)

      val xmlString = sameElemPartBefore + sameElemPartAfter

      assert(xmlString.startsWith("<"))
      assert(xmlString.endsWith(">"))
      require((sameElemPartBefore.trim + sameElemPartAfter.trim).contains("><"),
        "Expected element having parameter child nodes")
      require(xmlString.count(_ == '<') == 2, "Expected element having parameter child nodes")
      require(xmlString.count(_ == '>') == 2, "Expected element having parameter child nodes")

      val is = new ByteArrayInputStream(xmlString.getBytes("UTF-8"))

      saxParser.parse(is, handler)
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

    private def matchesAttributeValuePlaceholder(e: Elem, idx: Int): Boolean =
      e.attributes.map(_._2).contains(attrValuePlaceholder(idx))

    private def matchesChildNodesPlaceholder(e: Elem, idx: Int): Boolean =
      e.text.trim == childrenPlaceholderText(idx)

    private def findParameterParentElemPaths(doc: indexed.Document, args: Seq[Any]): immutable.IndexedSeq[ParameterParentElemPath] = {
      val result: immutable.IndexedSeq[ParameterParentElemPath] = (0 until args.length).toIndexedSeq flatMap { idx =>
        val elemOption = doc.documentElement findElemOrSelf { e =>
          val matchesAttrValue = matchesAttributeValuePlaceholder(e.elem, idx)
          val matchesChildren = matchesChildNodesPlaceholder(e.elem, idx)

          assert(!(matchesAttrValue && matchesChildren))
          matchesAttrValue || matchesChildren
        }

        val kind: ParameterKind = elemOption map { e =>
          if (matchesAttributeValuePlaceholder(e.elem, idx)) AttributeValueKind else ChildNodesKind
        } getOrElse ChildNodesKind

        elemOption map { e => ParameterParentElemPath(idx, e.elemPath, kind) }
      }
      result
    }

    private def getParser: parse.DocumentParser = {
      val spf = SAXParserFactory.newInstance
      spf.setFeature("http://xml.org/sax/features/namespaces", true)
      spf.setFeature("http://xml.org/sax/features/namespace-prefixes", true)

      trait MyEntityResolver extends EntityResolver {
        override def resolveEntity(publicId: String, systemId: String): InputSource = {
          new InputSource(new StringReader(""))
        }
      }

      trait MyErrorHandler extends ErrorHandler {
        override def warning(exc: SAXParseException) { throw exc }
        override def error(exc: SAXParseException) { throw exc }
        override def fatalError(exc: SAXParseException) { throw exc }
      }

      val result = parse.DocumentParserUsingSax.newInstance(
        spf,
        () => new parse.DefaultElemProducingSaxHandler with MyEntityResolver with MyErrorHandler)

      result
    }

    private def getSaxParserForPartParsing: SAXParser = {
      val spf = SAXParserFactory.newInstance
      spf.setFeature("http://xml.org/sax/features/namespaces", false)
      spf.setFeature("http://xml.org/sax/features/namespace-prefixes", false)

      val saxParser = spf.newSAXParser
      saxParser
    }

    private def getSaxHandlerForPartParsing: DefaultHandler = {
      trait MyEntityResolver extends EntityResolver {
        override def resolveEntity(publicId: String, systemId: String): InputSource = {
          new InputSource(new StringReader(""))
        }
      }

      trait MyErrorHandler extends ErrorHandler {
        override def warning(exc: SAXParseException) { throw exc }
        override def error(exc: SAXParseException) { throw exc }
        override def fatalError(exc: SAXParseException) { throw exc }
      }

      val handler = new DefaultHandler with MyEntityResolver with MyErrorHandler
      handler
    }

    @tailrec
    private def removeComments(part: String): String = {
      val startIdx = part.indexOf("<--")

      if (startIdx < 0) part
      else {
        val endIdx = part.indexOf("-->", startIdx + 1)

        require(endIdx >= 0, "A comment starts in one of the string parts but does not end there")

        val patchedPart = part.patch(startIdx, "", (endIdx - startIdx) + 1)

        // Recursive call
        removeComments(patchedPart)
      }
    }

    @tailrec
    private def removeCData(part: String): String = {
      val startIdx = part.indexOf("<![CDATA[")

      if (startIdx < 0) part
      else {
        val endIdx = part.indexOf("]]>", startIdx + 1)

        require(endIdx >= 0, "A CDATA section starts in one of the string parts but does not end there")

        val patchedPart = part.patch(startIdx, "", (endIdx - startIdx) + 1)

        // Recursive call
        removeCData(patchedPart)
      }
    }

    private def replaceXmlDeclaration(xmlString: String): String = {
      val it = xmlString.linesIterator
      require(it.hasNext)

      val firstLine = it.next

      val newXmlDeclaration = """<?xml version="1.0" encoding="utf-8" standalone="yes"?>"""

      if (firstLine.startsWith("<?xml") && firstLine.trim.endsWith("?>")) {
        newXmlDeclaration + xmlString.drop(firstLine.length)
      } else {
        require(!firstLine.trim.startsWith("<?xml"))
        xmlString
      }
    }
  }

  private[literal] trait ParameterKind
  private[literal] object AttributeValueKind extends ParameterKind
  private[literal] object ChildNodesKind extends ParameterKind

  private[literal] final case class ParameterParentElemPath(parameterIndex: Int, path: ElemPath, parameterKind: ParameterKind) extends Immutable
}
