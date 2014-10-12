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

import scala.collection.immutable

import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.QNameProvider
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.defaultelem.Comment
import eu.cdevreeze.yaidom.defaultelem.ConverterToDocument
import eu.cdevreeze.yaidom.defaultelem.ConverterToElem
import eu.cdevreeze.yaidom.defaultelem.Document
import eu.cdevreeze.yaidom.defaultelem.Elem
import eu.cdevreeze.yaidom.defaultelem.EntityRef
import eu.cdevreeze.yaidom.defaultelem.Node
import eu.cdevreeze.yaidom.defaultelem.ProcessingInstruction
import eu.cdevreeze.yaidom.defaultelem.Text

/**
 * Converter from Scala XML nodes to yaidom nodes, in particular from `scala.xml.Elem` to [[eu.cdevreeze.yaidom.defaultelem.Elem]] and
 * from `scala.xml.Document` to [[eu.cdevreeze.yaidom.defaultelem.Document]].
 *
 * This converter is handy when one wants to use XML literals (as offered by standard Scala XML) in combination with yaidom.
 *
 * This converter regards the input more like an "Elem" than an "ElemBuilder", in that scopes instead of namespace
 * declarations are extracted from input "elements", and in that conversions to yaidom Elems do not take any additional parent
 * scope parameter. On the other hand, Scala XML NamespaceBindings try to be a bit of both yaidom Scopes and yaidom Declarations.
 *
 * '''Beware that conversions from Scala XML Elems to yaidom Elems will fail if the Scala XML Elem uses namespaces in element and/or
 * attribute names that have not been declared!'''
 *
 * @author Chris de Vreeze
 */
trait ScalaXmlToYaidomConversions extends ConverterToDocument[scala.xml.Document] with ConverterToElem[scala.xml.Elem] {

  /**
   * Converts an `scala.xml.Document` to a [[eu.cdevreeze.yaidom.Document]]. The resulting yaidom Document has no document URI.
   *
   * If the input Scala XML Document is not namespace-valid, an exception will be thrown.
   */
  final def convertToDocument(v: scala.xml.Document): Document = {
    val docChildren = v.children
    val pis = v.children collect { case pi: scala.xml.ProcInstr => pi }
    val comments = v.children collect { case com: scala.xml.Comment => com }

    Document(
      uriOption = None,
      documentElement = convertToElem(v.docElem.asInstanceOf[scala.xml.Elem]),
      processingInstructions =
        pis.toIndexedSeq map { pi: scala.xml.ProcInstr => convertToProcessingInstruction(pi) },
      comments =
        comments.toIndexedSeq map { c: scala.xml.Comment => convertToComment(c) })
  }

  /**
   * Converts an `scala.xml.Elem` to an [[eu.cdevreeze.yaidom.Elem]].
   *
   * If the input Scala XML Elem is not namespace-valid, an exception will be thrown.
   */
  final def convertToElem(v: scala.xml.Elem): Elem = {
    val qname: QName = toQName(v)
    val attributes: immutable.IndexedSeq[(QName, String)] = extractAttributes(v.attributes)
    val scope: Scope = extractScope(v.scope)

    // Recursive (not tail-recursive)
    val childSeq = v.child.toIndexedSeq flatMap { n: scala.xml.Node => convertToNodeOption(n) }

    new Elem(
      qname = qname,
      attributes = attributes,
      scope = scope,
      children = childSeq)
  }

  /**
   * Converts an `scala.xml.Node` to an optional [[eu.cdevreeze.yaidom.Node]].
   */
  final def convertToNodeOption(v: scala.xml.Node): Option[Node] = {
    v match {
      case e: scala.xml.Elem => Some(convertToElem(e))
      case cdata: scala.xml.PCData => Some(convertToCData(cdata))
      case t: scala.xml.Text => Some(convertToText(t))
      case at: scala.xml.Atom[_] =>
        // Possibly an evaluated "parameter" in an XML literal
        Some(Text(text = at.data.toString, isCData = false))
      case pi: scala.xml.ProcInstr => Some(convertToProcessingInstruction(pi))
      case er: scala.xml.EntityRef => Some(convertToEntityRef(er))
      case c: scala.xml.Comment => Some(convertToComment(c))
      case _ => None
    }
  }

  /** Converts an `scala.xml.Text` to a [[eu.cdevreeze.yaidom.Text]] */
  final def convertToText(v: scala.xml.Text): Text = Text(text = v.data, isCData = false)

  /** Converts an `scala.xml.PCData` to a [[eu.cdevreeze.yaidom.Text]] */
  final def convertToCData(v: scala.xml.PCData): Text = Text(text = v.data, isCData = true)

  /** Converts an `scala.xml.ProcInstr` to a [[eu.cdevreeze.yaidom.ProcessingInstruction]] */
  final def convertToProcessingInstruction(v: scala.xml.ProcInstr): ProcessingInstruction =
    ProcessingInstruction(v.target, v.proctext)

  /** Converts an `scala.xml.EntityRef` to a [[eu.cdevreeze.yaidom.EntityRef]] */
  final def convertToEntityRef(v: scala.xml.EntityRef): EntityRef = EntityRef(v.entityName)

  /** Converts an `scala.xml.Comment` to a [[eu.cdevreeze.yaidom.Comment]] */
  final def convertToComment(v: scala.xml.Comment): Comment = Comment(v.commentText)

  /** Converts attributes, given as `scala.xml.MetaData`, to an `immutable.IndexedSeq[(QName, String)]`. */
  final def extractAttributes(attrs: scala.xml.MetaData): immutable.IndexedSeq[(QName, String)] = {
    attrs.toIndexedSeq map { attr: scala.xml.MetaData =>
      val attrValue = attr.value
      val attrValueText = if (attrValue.size >= 1) attrValue(0).text else ""
      (toQName(attr) -> attrValueText)
    }
  }

  /**
   * Converts the `scala.xml.NamespaceBinding` to a yaidom `Scope`.
   *
   * This implementation is brittle because of bug: SI 6939: Namespace binding (xmlns) is duplicated if a child redefines a prefix.
   * (see https://issues.scala-lang.org/browse/SI-6939 and https://github.com/scala/scala/pull/1858). Still, this implementation
   * tries to work around that bug.
   */
  final def extractScope(scope: scala.xml.NamespaceBinding): Scope = {
    if ((scope eq null) || (scope.uri eq null) || (scope == scala.xml.TopScope)) Scope.Empty
    else {
      val prefix = if (scope.prefix eq null) "" else scope.prefix

      // Recursive call (not tail-recursive), and working around the above-mentioned bug

      val parentScope = extractScope(scope.parent)

      if (scope.uri.isEmpty) {
        // Namespace undeclaration (which, looking at the NamespaceBinding API doc, seems not to exist)
        // Works for the default namespace too (knowing that "edited" prefix is not null but can be empty)
        parentScope -- Set(prefix)
      } else {
        // Works for namespace overrides too
        parentScope ++ Scope.from(prefix -> scope.uri)
      }
    }
  }

  /** Extracts the `QName` of an `scala.xml.Elem` */
  final def toQName(v: scala.xml.Elem)(implicit qnameProvider: QNameProvider): QName = {
    if (v.prefix eq null) qnameProvider.getUnprefixedQName(v.label) else qnameProvider.getQName(v.prefix, v.label)
  }

  /** Extracts the `QName` of an attribute as `scala.xml.MetaData`. */
  final def toQName(v: scala.xml.MetaData)(implicit qnameProvider: QNameProvider): QName = {
    if (v.isPrefixed) qnameProvider.parseQName(v.prefixedKey) else qnameProvider.parseQName(v.key)
  }
}
