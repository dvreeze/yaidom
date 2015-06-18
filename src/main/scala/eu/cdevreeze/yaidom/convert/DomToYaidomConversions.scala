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

package eu.cdevreeze.yaidom.convert

import java.net.URI
import java.nio.charset.Charset

import scala.collection.immutable

import org.w3c.dom.Attr
import org.w3c.dom.Element
import org.w3c.dom.NamedNodeMap
import org.w3c.dom.NodeList

import eu.cdevreeze.yaidom.core.Declarations
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.QNameProvider
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.simple.Comment
import eu.cdevreeze.yaidom.simple.ConverterToDocument
import eu.cdevreeze.yaidom.simple.Document
import eu.cdevreeze.yaidom.simple.Elem
import eu.cdevreeze.yaidom.simple.EntityRef
import eu.cdevreeze.yaidom.simple.Node
import eu.cdevreeze.yaidom.simple.ProcessingInstruction
import eu.cdevreeze.yaidom.simple.Text
import eu.cdevreeze.yaidom.simple.XmlDeclaration

/**
 * Converter from DOM nodes to yaidom nodes, in particular from `org.w3c.dom.Element` to [[eu.cdevreeze.yaidom.simple.Elem]] and
 * from `org.w3c.dom.Document` to [[eu.cdevreeze.yaidom.simple.Document]].
 *
 * This converter regards the input more like an "ElemBuilder" than an "Elem", in that namespace declarations instead of
 * scopes are extracted from input "elements", and in that conversions to yaidom Elems take an additional parent scope
 * parameter (against which namespace declarations are resolved to get the scope of the yaidom element).
 *
 * @author Chris de Vreeze
 */
trait DomToYaidomConversions extends ConverterToDocument[org.w3c.dom.Document] {

  /**
   * Converts an `org.w3c.dom.Document` to a [[eu.cdevreeze.yaidom.simple.Document]].
   */
  final def convertToDocument(v: org.w3c.dom.Document): Document = {
    // It seems that the DOM Document does not keep the URI from which it was loaded. Related (but not the same) is bug
    // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4916415.
    val uriOption: Option[URI] = Option(v.getDocumentURI) orElse (Option(v.getBaseURI)) map { uriString => new URI(uriString) }

    val xmlVersionOption = Option(v.getXmlVersion)
    val xmlDeclOption = xmlVersionOption map { xmlVersion =>
      XmlDeclaration.fromVersion(xmlVersion).
        withEncodingOption(Option(v.getXmlEncoding).map(cs => Charset.forName(cs))).
        withStandaloneOption(Some(v.getXmlStandalone))
    }

    Document(
      uriOption = uriOption,
      xmlDeclarationOption = xmlDeclOption,
      documentElement = convertToElem(v.getDocumentElement, Scope.Empty),
      processingInstructions =
        nodeListToIndexedSeq(v.getChildNodes) collect { case pi: org.w3c.dom.ProcessingInstruction => convertToProcessingInstruction(pi) },
      comments =
        nodeListToIndexedSeq(v.getChildNodes) collect { case c: org.w3c.dom.Comment => convertToComment(c) })
  }

  /**
   * Given a parent scope, converts an `org.w3c.dom.Element` to a [[eu.cdevreeze.yaidom.simple.Elem]].
   *
   * The result `Elem` gets Scope `parentScope.resolve(extractNamespaceDeclarations(v.getAttributes))`.
   *
   * Be careful: the namespaces inherited by the passed DOM element, if any, are ignored! In other words, the ancestry of
   * the passed DOM element is entirely ignored. This may cause an exception to be thrown, if there are indeed such namespaces,
   * unless they are a subset of the passed parent scope.
   */
  final def convertToElem(v: Element, parentScope: Scope): Elem = {
    val qname: QName = toQName(v)
    val attributes: immutable.IndexedSeq[(QName, String)] = extractAttributes(v.getAttributes)

    val namespaceDeclarations: Declarations = extractNamespaceDeclarations(v.getAttributes)
    val newScope: Scope = parentScope.resolve(namespaceDeclarations)

    // Recursive (not tail-recursive)
    val childSeq = nodeListToIndexedSeq(v.getChildNodes) flatMap { n => convertToNodeOption(n, newScope) }

    new Elem(
      qname = qname,
      attributes = attributes,
      scope = newScope,
      children = childSeq)
  }

  /**
   * Given a parent scope, converts an `org.w3c.dom.Node` to an optional [[eu.cdevreeze.yaidom.simple.Node]].
   *
   * In case of an element, the result `Elem` (wrapped in an Option) gets Scope
   * `parentScope.resolve(extractNamespaceDeclarations(v.getAttributes))`.
   *
   * Be careful: the namespaces inherited by the passed DOM node, if any, are ignored! In other words, the ancestry of
   * the passed DOM node is entirely ignored. This may cause an exception to be thrown, if there are indeed such namespaces,
   * unless they are a subset of the passed parent scope.
   */
  final def convertToNodeOption(v: org.w3c.dom.Node, parentScope: Scope): Option[Node] = {
    v match {
      case e: Element                            => Some(convertToElem(e, parentScope))
      case t: org.w3c.dom.Text                   => Some(convertToText(t))
      case pi: org.w3c.dom.ProcessingInstruction => Some(convertToProcessingInstruction(pi))
      case er: org.w3c.dom.EntityReference       => Some(convertToEntityRef(er))
      case c: org.w3c.dom.Comment                => Some(convertToComment(c))
      case _                                     => None
    }
  }

  /** Converts an `org.w3c.dom.Text` to a [[eu.cdevreeze.yaidom.simple.Text]] */
  final def convertToText(v: org.w3c.dom.Text): Text = v match {
    case cdata: org.w3c.dom.CDATASection => Text(text = v.getData, isCData = true)
    case _                               => Text(text = v.getData, isCData = false)
  }

  /** Converts an `org.w3c.dom.ProcessingInstruction` to a [[eu.cdevreeze.yaidom.simple.ProcessingInstruction]] */
  final def convertToProcessingInstruction(v: org.w3c.dom.ProcessingInstruction): ProcessingInstruction =
    ProcessingInstruction(v.getTarget, v.getData)

  /** Converts an `org.w3c.dom.EntityReference` to a [[eu.cdevreeze.yaidom.simple.EntityRef]] */
  final def convertToEntityRef(v: org.w3c.dom.EntityReference): EntityRef = EntityRef(v.getNodeName)

  /** Converts an `org.w3c.dom.Comment` to a [[eu.cdevreeze.yaidom.simple.Comment]] */
  final def convertToComment(v: org.w3c.dom.Comment): Comment = Comment(v.getData)

  /** Converts a `NamedNodeMap` to an `immutable.IndexedSeq[(QName, String)]`. Namespace declarations are skipped. */
  final def extractAttributes(domAttributes: NamedNodeMap): immutable.IndexedSeq[(QName, String)] = {
    (0 until domAttributes.getLength).flatMap(i => {
      val attr = domAttributes.item(i).asInstanceOf[Attr]

      if (isNamespaceDeclaration(attr)) None else {
        val qname: QName = toQName(attr)
        Some(qname -> attr.getValue)
      }
    }).toIndexedSeq
  }

  /** Converts the namespace declarations in a `NamedNodeMap` to a `Declarations` */
  final def extractNamespaceDeclarations(domAttributes: NamedNodeMap): Declarations = {
    val nsMap = {
      val result = (0 until domAttributes.getLength) flatMap { i =>
        val attr = domAttributes.item(i).asInstanceOf[Attr]

        if (isNamespaceDeclaration(attr)) {
          val result = extractNamespaceDeclaration(attr)
          Some(result) map { pair => (pair._1.getOrElse(""), pair._2) }
        } else None
      }
      result.toMap
    }
    Declarations.from(nsMap)
  }

  /** Helper method that converts a `NodeList` to an `IndexedSeq[org.w3c.dom.Node]` */
  final def nodeListToIndexedSeq(nodeList: NodeList): immutable.IndexedSeq[org.w3c.dom.Node] = {
    val result = (0 until nodeList.getLength) map { i => nodeList.item(i) }
    result.toIndexedSeq
  }

  /** Extracts the `QName` of an `org.w3c.dom.Element` */
  final def toQName(v: org.w3c.dom.Element)(implicit qnameProvider: QNameProvider): QName = {
    val name: String = v.getTagName
    val arr = name.split(':')
    assert(arr.length >= 1 && arr.length <= 2)
    if (arr.length == 1) qnameProvider.getUnprefixedQName(arr(0)) else qnameProvider.getQName(arr(0), arr(1))
  }

  /** Extracts the `QName` of an `org.w3c.dom.Attr`. If the `Attr` is a namespace declaration, an exception is thrown. */
  final def toQName(v: org.w3c.dom.Attr)(implicit qnameProvider: QNameProvider): QName = {
    require(!isNamespaceDeclaration(v), "Namespace declaration not allowed")
    val name: String = v.getName
    val arr = name.split(':')
    assert(arr.length >= 1 && arr.length <= 2)
    if (arr.length == 1) qnameProvider.getUnprefixedQName(arr(0)) else qnameProvider.getQName(arr(0), arr(1))
  }

  /** Returns true if the `org.w3c.dom.Attr` is a namespace declaration */
  final def isNamespaceDeclaration(v: org.w3c.dom.Attr): Boolean = {
    val name: String = v.getName
    val arr = name.split(':')
    assert(arr.length >= 1 && arr.length <= 2)
    val result = arr(0) == "xmlns"
    result
  }

  /** Extracts (optional) prefix and namespace. Call only if `isNamespaceDeclaration(v)`, since otherwise an exception is thrown. */
  final def extractNamespaceDeclaration(v: org.w3c.dom.Attr): (Option[String], String) = {
    val name: String = v.getName
    val arr = name.split(':')
    assert(arr.length >= 1 && arr.length <= 2)
    require(arr(0) == "xmlns")
    val prefixOption: Option[String] = if (arr.length == 1) None else Some(arr(1))
    val attrValue: String = v.getValue
    (prefixOption, attrValue)
  }

  private def empty2Null(s: String): String = if (s == "") null else s
}
