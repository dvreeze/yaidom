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

package eu.cdevreeze.yaidom.convert

import java.net.URI

import scala.collection.immutable
import scala.collection.immutable.ArraySeq

import org.scalajs.dom.raw.Attr
import org.scalajs.dom.raw.Element
import org.scalajs.dom.raw.NamedNodeMap
import org.scalajs.dom.raw.NodeList

import eu.cdevreeze.yaidom.core.Declarations
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.ENameProvider
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.QNameProvider
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.simple.Comment
import eu.cdevreeze.yaidom.simple.ConverterToDocument
import eu.cdevreeze.yaidom.simple.Document
import eu.cdevreeze.yaidom.simple.Elem
import eu.cdevreeze.yaidom.simple.Node
import eu.cdevreeze.yaidom.simple.ProcessingInstruction
import eu.cdevreeze.yaidom.simple.Text

/**
 * Converter from JS DOM nodes to yaidom nodes, in particular from `org.scalajs.dom.raw.Element` to `eu.cdevreeze.yaidom.simple.Elem` and
 * from `org.scalajs.dom.raw.Document` to `eu.cdevreeze.yaidom.simple.Document`.
 *
 * This converter regards the input more like an "ElemBuilder" than an "Elem", in that namespace declarations instead of
 * scopes are extracted from input "elements", and in that conversions to yaidom Elems take an additional parent scope
 * parameter (against which namespace declarations are resolved to get the scope of the yaidom element).
 *
 * @author Chris de Vreeze
 */
trait JsDomToYaidomConversions extends ConverterToDocument[org.scalajs.dom.raw.Document] {

  /**
   * Converts an `org.scalajs.dom.raw.Document` to a `eu.cdevreeze.yaidom.simple.Document`.
   */
  final def convertToDocument(v: org.scalajs.dom.raw.Document): Document = {
    // Is the document URI retained after parsing?
    val uriOption: Option[URI] = Option(v.documentURI) map { uriString => new URI(uriString) }

    Document(
      uriOption = uriOption,
      xmlDeclarationOption = None,
      children = nodeListToIndexedSeq(v.childNodes) flatMap {
        case e: org.scalajs.dom.raw.Element => Some(convertToElem(v.documentElement, Scope.Empty))
        case pi: org.scalajs.dom.raw.ProcessingInstruction => Some(convertToProcessingInstruction(pi))
        case c: org.scalajs.dom.raw.Comment => Some(convertToComment(c))
        case _ => None
      })
  }

  /**
   * Given a parent scope, converts an `org.scalajs.dom.raw.Element` to a `eu.cdevreeze.yaidom.simple.Elem`.
   *
   * The result `Elem` gets Scope `parentScope.resolve(extractNamespaceDeclarations(v.getAttributes))`.
   *
   * Be careful: the namespaces inherited by the passed DOM element, if any, are ignored! In other words, the ancestry of
   * the passed DOM element is entirely ignored. This may cause an exception to be thrown, if there are indeed such namespaces,
   * unless they are a subset of the passed parent scope.
   */
  final def convertToElem(v: Element, parentScope: Scope): Elem = {
    val qname: QName = toQName(v)
    val attributes: ArraySeq[(QName, String)] = extractAttributes(v.attributes)

    val namespaceDeclarations: Declarations = extractNamespaceDeclarations(v.attributes)
    val newScope: Scope = parentScope.resolve(namespaceDeclarations)

    // Recursive (not tail-recursive)
    val childSeq = nodeListToIndexedSeq(v.childNodes).flatMap { n => convertToNodeOption(n, newScope) }

    new Elem(
      qname = qname,
      attributes = attributes,
      scope = newScope,
      children = childSeq)
  }

  /**
   * Given a parent scope, converts an `org.scalajs.dom.raw.Node` to an optional `eu.cdevreeze.yaidom.simple.Node`.
   *
   * In case of an element, the result `Elem` (wrapped in an Option) gets Scope
   * `parentScope.resolve(extractNamespaceDeclarations(v.getAttributes))`.
   *
   * Be careful: the namespaces inherited by the passed DOM node, if any, are ignored! In other words, the ancestry of
   * the passed DOM node is entirely ignored. This may cause an exception to be thrown, if there are indeed such namespaces,
   * unless they are a subset of the passed parent scope.
   */
  final def convertToNodeOption(v: org.scalajs.dom.raw.Node, parentScope: Scope): Option[Node] = {
    v match {
      case e: Element =>
        Some(convertToElem(e, parentScope))
      case t: org.scalajs.dom.raw.CDATASection =>
        Some(convertToText(t))
      case t: org.scalajs.dom.raw.Text =>
        Some(convertToText(t))
      case pi: org.scalajs.dom.raw.ProcessingInstruction =>
        Some(convertToProcessingInstruction(pi))
      case c: org.scalajs.dom.raw.Comment =>
        Some(convertToComment(c))
      case _ => None
    }
  }

  /** Converts an `org.scalajs.dom.raw.Text` to a `eu.cdevreeze.yaidom.simple.Text` */
  final def convertToText(v: org.scalajs.dom.raw.Text): Text = v match {
    case cdata: org.scalajs.dom.raw.CDATASection if v.nodeType == org.scalajs.dom.raw.Node.CDATA_SECTION_NODE =>
      Text(text = v.data, isCData = true)
    case _ =>
      Text(text = v.data, isCData = false)
  }

  /** Converts an `org.scalajs.dom.raw.ProcessingInstruction` to a `eu.cdevreeze.yaidom.simple.ProcessingInstruction` */
  final def convertToProcessingInstruction(v: org.scalajs.dom.raw.ProcessingInstruction): ProcessingInstruction =
    ProcessingInstruction(v.target, v.data)

  /** Converts an `org.scalajs.dom.raw.Comment` to a `eu.cdevreeze.yaidom.simple.Comment` */
  final def convertToComment(v: org.scalajs.dom.raw.Comment): Comment = Comment(v.data)

  /** Converts a `NamedNodeMap` to an `immutable.IndexedSeq[(QName, String)]`. Namespace declarations are skipped. */
  final def extractAttributes(domAttributes: NamedNodeMap): ArraySeq[(QName, String)] = {
    (0 until domAttributes.length).flatMap(i => {
      val attr = domAttributes.item(i).asInstanceOf[Attr]

      if (isNamespaceDeclaration(attr)) {
        None
      } else {
        val qname: QName = toQName(attr)
        Some(qname -> attr.value)
      }
    }).to(ArraySeq)
  }

  /** Converts the namespace declarations in a `NamedNodeMap` to a `Declarations` */
  final def extractNamespaceDeclarations(domAttributes: NamedNodeMap): Declarations = {
    val nsMap = {
      val result = (0 until domAttributes.length) flatMap { i =>
        val attr = domAttributes.item(i).asInstanceOf[Attr]

        if (isNamespaceDeclaration(attr)) {
          val result = extractNamespaceDeclaration(attr)
          Some(result) map { pair => (pair._1.getOrElse(""), pair._2) }
        } else {
          None
        }
      }
      result.toMap
    }
    Declarations.from(nsMap)
  }

  /** Helper method that converts a `NodeList` to an `IndexedSeq[org.scalajs.dom.raw.Node]` */
  final def nodeListToIndexedSeq(nodeList: NodeList): ArraySeq[org.scalajs.dom.raw.Node] = {
    val result = (0 until nodeList.length) map { i => nodeList.item(i) }
    result.to(ArraySeq)
  }

  /** Extracts the `QName` of an `org.scalajs.dom.raw.Element` */
  final def toQName(v: org.scalajs.dom.raw.Element)(implicit qnameProvider: QNameProvider): QName = {
    val name: String = v.tagName
    val arr = name.split(':')
    assert(arr.length >= 1 && arr.length <= 2)
    if (arr.length == 1) qnameProvider.getUnprefixedQName(arr(0)) else qnameProvider.getQName(arr(0), arr(1))
  }

  /** Extracts the `QName` of an `org.scalajs.dom.raw.Attr`. If the `Attr` is a namespace declaration, an exception is thrown. */
  final def toQName(v: org.scalajs.dom.raw.Attr)(implicit qnameProvider: QNameProvider): QName = {
    require(!isNamespaceDeclaration(v), "Namespace declaration not allowed")
    val name: String = v.name
    val arr = name.split(':')
    assert(arr.length >= 1 && arr.length <= 2)
    if (arr.length == 1) qnameProvider.getUnprefixedQName(arr(0)) else qnameProvider.getQName(arr(0), arr(1))
  }

  /** Returns true if the `org.scalajs.dom.raw.Attr` is a namespace declaration */
  final def isNamespaceDeclaration(v: org.scalajs.dom.raw.Attr): Boolean = {
    val name: String = v.name
    val arr = name.split(':')
    assert(arr.length >= 1 && arr.length <= 2)
    val result = arr(0) == "xmlns"
    result
  }

  /** Extracts (optional) prefix and namespace. Call only if `isNamespaceDeclaration(v)`, since otherwise an exception is thrown. */
  final def extractNamespaceDeclaration(v: org.scalajs.dom.raw.Attr): (Option[String], String) = {
    val name: String = v.name
    val arr = name.split(':')
    assert(arr.length >= 1 && arr.length <= 2)
    require(arr(0) == "xmlns")
    val prefixOption: Option[String] = if (arr.length == 1) None else Some(arr(1))
    val attrValue: String = v.value
    (prefixOption, attrValue)
  }

  /** Extracts the `EName` of an `org.scalajs.dom.raw.Element` */
  final def toEName(v: org.scalajs.dom.raw.Element)(implicit enameProvider: ENameProvider): EName = {
    val nsOption = Option(v.namespaceURI)
    enameProvider.getEName(nsOption, v.localName)
  }

  /** Extracts the `EName` of an `org.scalajs.dom.raw.Attr`. If the `Attr` is a namespace declaration, an exception is thrown. */
  final def toEName(v: org.scalajs.dom.raw.Attr)(implicit enameProvider: ENameProvider): EName = {
    require(!isNamespaceDeclaration(v), "Namespace declaration not allowed")
    val nsOption = Option(v.namespaceURI)
    enameProvider.getEName(nsOption, v.localName)
  }

  /** Converts a `NamedNodeMap` to an `immutable.IndexedSeq[(EName, String)]`. Namespace declarations are skipped. */
  final def extractResolvedAttributes(domAttributes: NamedNodeMap): immutable.IndexedSeq[(EName, String)] = {
    (0 until domAttributes.length).flatMap(i => {
      val attr = domAttributes.item(i).asInstanceOf[Attr]

      if (isNamespaceDeclaration(attr)) {
        None
      } else {
        val ename: EName = toEName(attr)
        Some(ename -> attr.value)
      }
    }).toIndexedSeq
  }
}
