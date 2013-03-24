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
 * Note: Try to avoid very large String (literal) arguments (in the StringContext). Otherwise the JVM may respond with:
 * "java.lang.ClassFormatError: Unknown constant tag XX in class file", where XX is a number (e.g. 32). This is a 64K
 * String literal limit of the JVM. Fortunately, there is usually a way to avoid very large String literal arguments.
 *
 * @author Chris de Vreeze
 */
private[literal] object XmlLiterals {

  private[literal] final class XmlLiteralHelper(val sc: StringContext) {

    private val placeholderWithoutQuotes = "___par___"
    private val placeholder = "\"" + placeholderWithoutQuotes + "\""

    def xmlDoc(args: Any*): Document = {
      sc.checkLengths(args)

      require(sc.parts forall (part => !part.contains(placeholderWithoutQuotes)),
        "The XML must not contain placeholder %s".format(placeholderWithoutQuotes))

      val xmlWithPlaceholders = sc.standardInterpolator(identity, args.map(arg => placeholder))
      val editedXmlWithPlaceholders = replaceXmlDeclaration(xmlWithPlaceholders)

      val docParser = getDocumentParser

      val docWithPlaceholders = docParser.parse(new ByteArrayInputStream(editedXmlWithPlaceholders.getBytes("UTF-8")))

      val allowedPlaceholderCount = countAllowedPlaceholders(docWithPlaceholders.documentElement, None)
      require(
        allowedPlaceholderCount == args.length,
        "Arguments are only allowed as attribute value or (entire) element content")

      val (rootElems, remainingArgs) = updatePlaceholders(docWithPlaceholders.documentElement, None, args)
      require(remainingArgs.isEmpty, "Problem filling in the arguments")
      require(rootElems.size == 1 && rootElems.head.isInstanceOf[Elem])

      val doc = docWithPlaceholders.withDocumentElement(rootElems.head.asInstanceOf[Elem])
      doc
    }

    private def countAllowedPlaceholders(node: Node, parentOption: Option[Elem]): Int = node match {
      case t: Text =>
        val parentHasOnlyText = parentOption.get.children forall (ch => ch.isInstanceOf[Text])

        if (parentHasOnlyText && (parentOption.get.text.trim == placeholder) && (t.text.trim == placeholder)) 1 else 0
      case e: Elem =>
        val attrValuePlaceholderCount = e.attributes count { case (attrname, attrValue) => attrValue == placeholderWithoutQuotes }

        // Recursive calls
        val childrenPlaceholderCounts = e.children map { ch => countAllowedPlaceholders(ch, Some(e)) }

        attrValuePlaceholderCount + (childrenPlaceholderCounts.sum)
      case _ => 0
    }

    /**
     * Functionally updates the placeholders in a node (tree) with the arguments.
     * It is assumed as precondition that all placeholders are allowed (see countAllowedPlaceholders).
     */
    private def updatePlaceholders(node: Node, parentOption: Option[Elem], args: Seq[Any]): (Seq[Node], Seq[Any]) = node match {
      case t: Text =>
        val parentHasOnlyText = parentOption.get.children forall (ch => ch.isInstanceOf[Text])

        if (parentHasOnlyText && (parentOption.get.text.trim == placeholder) && (t.text.trim == placeholder)) {
          val arg = args.headOption.getOrElse(sys.error("Problem updating placeholders"))
          require(
            hasAllowedArgumentType(arg),
            "All arguments must be of an allowed argument type (String, Node or Seq[Node])")

          val newNodes: Seq[Node] = extractNodes(arg)
          (newNodes, args.tail)
        } else (Vector(t), args)
      case e: Elem =>
        var currentArgs = args

        val newAttributes: immutable.IndexedSeq[(QName, String)] = e.attributes map {
          case (attrName, attrValue) =>
            if (attrValue == placeholderWithoutQuotes) {
              val arg = currentArgs.headOption.getOrElse(sys.error("Problem updating placeholders"))
              require(arg.isInstanceOf[String], "Attribute value arguments must be strings")
              currentArgs = currentArgs.tail

              (attrName -> arg.asInstanceOf[String])
            } else (attrName -> attrValue)
        }

        // Recursive calls
        val newChildren: immutable.IndexedSeq[Node] =
          e.children flatMap { ch =>
            val (newChildren, restArgs) = updatePlaceholders(ch, Some(e), currentArgs)
            currentArgs = restArgs

            // Repairing namespaces in order to prevent namespace undeclarations (for prefixes)

            newChildren map { ch =>
              ch match {
                case che: Elem => NodeBuilder.fromElem(che)(Scope.Empty).build(e.scope)
                case n => n
              }
            }
          }

        val newElem = e.withAttributes(newAttributes).withChildren(newChildren)
        (Vector(newElem) -> currentArgs)
      case n => (Vector(n), args)
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

    private def getDocumentParser: parse.DocumentParser = {
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
}
