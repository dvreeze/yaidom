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
package parse

import org.xml.sax.{ ContentHandler, Attributes, Locator }
import org.xml.sax.helpers.DefaultHandler
import org.xml.sax.ext.LexicalHandler
import scala.collection.{ immutable, mutable }
import eu.cdevreeze.yaidom._
import NodeBuilder._
import DefaultElemProducingSaxHandler._

/**
 * Default [[eu.cdevreeze.yaidom.jinterop.ElemProducingSaxHandler]] implementation.
 *
 * This is a trait instead of a class, so it is easy to mix in EntityResolvers, ErrorHandlers, etc.
 */
trait DefaultElemProducingSaxHandler extends ElemProducingSaxHandler with LexicalHandler {

  // This content handler has a relatively simple state space, so is rather easy to reason about.

  // I very much like immutability, but sometimes mutable is better, like in this case.
  // All my attempts to implement this handler using immutable objects resulted in poor performance.
  // Mutability inside the SAX handler implementation fits the role of the class (namely to build an immutable Document).
  // It is also a good fit for the implementation of "parsing state", because we need stable object identities,
  // but rapidly changing state of those objects. Hence old-fashioned mutable objects.

  @volatile private var topLevelProcessingInstructions: immutable.IndexedSeq[MutableProcessingInstruction] = immutable.IndexedSeq()

  @volatile private var topLevelComments: immutable.IndexedSeq[MutableComment] = immutable.IndexedSeq()

  @volatile private var currentRoot: MutableElem = _

  @volatile private var currentElem: MutableElem = _

  @volatile private var currentlyInCData: Boolean = false

  final override def startDocument() = ()

  final override def startElement(uri: String, localName: String, qName: String, atts: Attributes) {
    val parentScope = if (currentElem eq null) Scope.Empty else currentElem.scope
    val elm: MutableElem = startElementToMutableElem(uri, localName, qName, atts, parentScope)

    if (currentRoot eq null) {
      require(currentElem eq null)

      currentRoot = elm
      currentElem = currentRoot
    } else {
      require(currentElem ne null)

      currentElem.children :+= elm
      elm.parentOption = Some(currentElem)
      currentElem = elm
    }
  }

  final override def endElement(uri: String, localName: String, qName: String) {
    require(currentRoot ne null)
    require(currentElem ne null)

    currentElem = currentElem.parentOption collect { case e: MutableElem => e } getOrElse null
  }

  final override def characters(ch: Array[Char], start: Int, length: Int) {
    val isCData = this.currentlyInCData
    val txt: MutableText = MutableText(new String(ch, start, length), isCData)

    if (currentRoot eq null) {
      // Ignore
      require(currentElem eq null)
    } else {
      require(currentElem ne null)

      currentElem.children :+= txt
    }
  }

  final override def processingInstruction(target: String, data: String) {
    val pi = MutableProcessingInstruction(target, data)

    if (currentRoot eq null) {
      require(currentElem eq null)

      val newPis = topLevelProcessingInstructions :+ pi
      topLevelProcessingInstructions = newPis
    } else {
      require(currentElem ne null)

      currentElem.children :+= pi
    }
  }

  final override def endDocument() = ()

  final override def ignorableWhitespace(ch: Array[Char], start: Int, length: Int) {
    // Self call. If ignorable whitespace makes it until here, we store it in the result tree.
    characters(ch, start, length)
  }

  // ContentHandler methods startPrefixMapping, endPrefixMapping, skippedEntity, setDocumentLocator not overridden

  final override def startCDATA() {
    this.currentlyInCData = true
  }

  final override def endCDATA() {
    this.currentlyInCData = false
  }

  final override def comment(ch: Array[Char], start: Int, length: Int) {
    val comment = MutableComment(new String(ch, start, length))

    if (currentRoot eq null) {
      require(currentElem eq null)

      val newComments = topLevelComments :+ comment
      topLevelComments = newComments
    } else {
      require(currentElem ne null)

      currentElem.children :+= comment
    }
  }

  final override def startEntity(name: String) = ()

  final override def endEntity(name: String) = ()

  final override def startDTD(name: String, publicId: String, systemId: String) = ()

  final override def endDTD() = ()

  final def resultingElem: Elem = {
    require(currentRoot ne null, "When parsing is ready, the current root must not be null")
    require(currentElem eq null, "When parsing is ready, the current path must be at the root")

    val root = currentRoot.toNode.asInstanceOf[Elem]
    root
  }

  final def resultingDocument: Document = {
    val docElem = resultingElem
    val pis = topLevelProcessingInstructions map { pi => pi.toNode.asInstanceOf[ProcessingInstruction] }
    val comments = topLevelComments map { comment => comment.toNode.asInstanceOf[Comment] }
    new Document(None, docElem, pis, comments)
  }

  private def startElementToMutableElem(uri: String, localName: String, qName: String, atts: Attributes, parentScope: Scope): MutableElem = {
    require(uri ne null)
    require(localName ne null)
    require(qName ne null)

    val elmQName: QName = if (qName != "") qName.qname else localName.qname

    val nsDecls = extractDeclarations(atts)
    val attrMap = extractAttributeMap(atts)

    val newScope = parentScope.resolve(nsDecls)

    new MutableElem(
      parentOption = None,
      qname = elmQName,
      attributes = attrMap,
      scope = newScope,
      children = mutable.IndexedSeq())
  }

  private def extractDeclarations(atts: Attributes): Scope.Declarations = {
    val result = attributeOrDeclarationMap(atts) filterKeys { qname => isNamespaceDeclaration(qname) } map { kv =>
      val key = kv._1.qname
      val prefix = if (key.prefixOption.isEmpty) "" else key.localPart
      val nsUri = kv._2
      (prefix -> nsUri)
    }
    Scope.Declarations.fromMap(result)
  }

  private def extractAttributeMap(atts: Attributes): Map[QName, String] = {
    val result = attributeOrDeclarationMap(atts) filterKeys { qname => !isNamespaceDeclaration(qname) } map { kv =>
      val qname = kv._1.qname
      val attValue = kv._2
      (qname -> attValue)
    }
    result
  }

  private def attributeOrDeclarationMap(atts: Attributes): Map[String, String] = {
    val result = (0 until atts.getLength).toIndexedSeq map { (idx: Int) => (atts.getQName(idx) -> atts.getValue(idx)) }
    result.toMap
  }

  /** Returns true if the attribute qualified (prefixed) name is a namespace declaration */
  private def isNamespaceDeclaration(attrQName: String): Boolean = {
    val arr = attrQName.split(':')
    require(arr.length >= 1 && arr.length <= 2)
    val result = arr(0) == "xmlns"
    result
  }
}

object DefaultElemProducingSaxHandler {

  trait MutableNode {
    def toNode: Node
  }

  trait MutableParentNode extends MutableNode {

    def children: mutable.IndexedSeq[MutableNode]
  }

  class MutableElem(
    var parentOption: Option[MutableElem],
    val qname: QName,
    val attributes: Map[QName, String],
    val scope: Scope,
    var children: mutable.IndexedSeq[MutableNode]) extends MutableParentNode {

    def toNode: Elem = {
      // Recursive (not tail-recursive)
      Elem(
        qname,
        attributes,
        scope,
        (children map { ch => ch.toNode }).toIndexedSeq)
    }
  }

  case class MutableText(text: String, isCData: Boolean) extends MutableNode {

    def toNode: Text = Text(text, isCData)
  }

  case class MutableProcessingInstruction(target: String, data: String) extends MutableNode {

    def toNode: ProcessingInstruction = ProcessingInstruction(target, data)
  }

  case class MutableEntityRef(entity: String) extends MutableNode {

    def toNode: EntityRef = EntityRef(entity)
  }

  case class MutableComment(text: String) extends MutableNode {

    def toNode: Comment = Comment(text)
  }
}