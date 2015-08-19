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

package eu.cdevreeze.yaidom.parse

import java.nio.charset.Charset

import scala.collection.JavaConverters.enumerationAsScalaIteratorConverter
import scala.collection.immutable
import scala.collection.mutable

import org.xml.sax.Attributes
import org.xml.sax.ext.LexicalHandler
import org.xml.sax.ext.Locator2
import org.xml.sax.helpers.NamespaceSupport

import eu.cdevreeze.yaidom.core.Declarations
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.QNameProvider
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.simple.CanBeDocumentChild
import eu.cdevreeze.yaidom.simple.Comment
import eu.cdevreeze.yaidom.simple.Document
import eu.cdevreeze.yaidom.simple.Elem
import eu.cdevreeze.yaidom.simple.EntityRef
import eu.cdevreeze.yaidom.simple.Node
import eu.cdevreeze.yaidom.simple.ProcessingInstruction
import eu.cdevreeze.yaidom.simple.Text
import eu.cdevreeze.yaidom.simple.XmlDeclaration
import net.jcip.annotations.NotThreadSafe

/**
 * Default [[eu.cdevreeze.yaidom.parse.ElemProducingSaxHandler]] implementation.
 *
 * This is a trait instead of a class, so it is easy to mix in `EntityResolver`s, `ErrorHandler`s, etc.
 *
 * @author Chris de Vreeze
 */
@NotThreadSafe
trait DefaultElemProducingSaxHandler extends ElemProducingSaxHandler with LexicalHandler with SaxHandlerWithLocator {

  // This content handler has a relatively simple state space, so is rather easy to reason about.

  // I very much like immutability, but sometimes mutable is better, like in this case.
  // All my attempts to implement this handler using immutable objects resulted in poor performance.
  // Mutability inside the SAX handler implementation fits the role of the class (namely to build an immutable Document).
  // It is also a good fit for the implementation of "parsing state", because we need stable object identities,
  // but rapidly changing state of those objects. Hence old-fashioned mutable objects.

  private var docChildren: immutable.IndexedSeq[CanBeInternalDocumentChild] = immutable.IndexedSeq()

  private var currentRoot: InternalElemNode = _

  private var currentElem: InternalElemNode = _

  private var currentlyInCData: Boolean = false

  private val namespaceSupport = new NamespaceSupport()

  private var namespaceContextStarted = false

  private var xmlDeclarationOption: Option[XmlDeclaration] = None

  final override def startDocument(): Unit = ()

  final override def startElement(uri: String, localName: String, qName: String, atts: Attributes): Unit = {
    pushContextIfNeeded()
    namespaceContextStarted = false

    val parentScope = if (currentElem eq null) Scope.Empty else currentElem.scope
    val elm: InternalElemNode = startElementToInternalElemNode(uri, localName, qName, atts, parentScope)

    if (currentRoot eq null) {
      require(currentElem eq null)

      currentRoot = elm
      currentElem = currentRoot

      val newDocChildren = docChildren :+ currentRoot
      docChildren = newDocChildren

      fillOptionalXmlDeclaration()
    } else {
      require(currentElem ne null)

      currentElem.children :+= elm
      elm.parentOption = Some(currentElem)
      currentElem = elm
    }
  }

  final override def endElement(uri: String, localName: String, qName: String): Unit = {
    require(!namespaceContextStarted, s"Corrupt internal namespace state!")
    namespaceSupport.popContext()

    require(currentRoot ne null)
    require(currentElem ne null)

    currentElem = currentElem.parentOption collect { case e: InternalElemNode => e } getOrElse null
  }

  final override def characters(ch: Array[Char], start: Int, length: Int): Unit = {
    val isCData = this.currentlyInCData
    val txt: InternalTextNode = new InternalTextNode(new String(ch, start, length), isCData)

    if (currentRoot eq null) {
      // Ignore
      require(currentElem eq null)
    } else {
      require(currentElem ne null)

      currentElem.children :+= txt
    }
  }

  final override def processingInstruction(target: String, data: String): Unit = {
    val pi = new InternalProcessingInstructionNode(target, data)

    if (currentRoot eq null) {
      require(currentElem eq null)

      val newDocChildren = docChildren :+ pi
      docChildren = newDocChildren
    } else if (currentElem ne null) {
      currentElem.children :+= pi
    } else {
      val newDocChildren = docChildren :+ pi
      docChildren = newDocChildren
    }
  }

  final override def endDocument(): Unit = ()

  final override def ignorableWhitespace(ch: Array[Char], start: Int, length: Int): Unit = {
    // Self call. If ignorable whitespace makes it until here, we store it in the result tree.
    characters(ch, start, length)
  }

  // ContentHandler methods skippedEntity, setDocumentLocator not overridden

  final override def startPrefixMapping(prefix: String, uri: String): Unit = {
    pushContextIfNeeded()

    namespaceSupport.declarePrefix(prefix, uri)
  }

  final override def endPrefixMapping(prefix: String): Unit = {
  }

  final override def startCDATA(): Unit = {
    this.currentlyInCData = true
  }

  final override def endCDATA(): Unit = {
    this.currentlyInCData = false
  }

  final override def comment(ch: Array[Char], start: Int, length: Int): Unit = {
    val comment = new InternalCommentNode(new String(ch, start, length))

    if (currentRoot eq null) {
      require(currentElem eq null)

      val newDocChildren = docChildren :+ comment
      docChildren = newDocChildren
    } else if (currentElem ne null) {
      currentElem.children :+= comment
    } else {
      val newDocChildren = docChildren :+ comment
      docChildren = newDocChildren
    }
  }

  final override def startEntity(name: String): Unit = ()

  final override def endEntity(name: String): Unit = ()

  final override def startDTD(name: String, publicId: String, systemId: String): Unit = ()

  final override def endDTD(): Unit = ()

  final def resultingElem: Elem = {
    require(currentRoot ne null, "When parsing is ready, the current root must not be null")
    require(currentElem eq null, "When parsing is ready, the current path must be at the root")

    val root = currentRoot.toNode
    root
  }

  final def resultingDocument: Document = {
    val docElem = resultingElem
    new Document(None, xmlDeclarationOption, docChildren.map(n => n.toNode))
  }

  /**
   * Tries to fill the optional XML declaration, without the standalone value.
   */
  private def fillOptionalXmlDeclaration(): Unit = {
    val locator2Option: Option[Locator2] = Option(this.locator) match {
      case Some(loc: Locator2) => Some(loc)
      case _                   => None
    }

    this.xmlDeclarationOption = locator2Option flatMap { loc =>
      Option(loc.getXMLVersion).map(version => XmlDeclaration.fromVersion(version)) map { xmlDecl =>
        xmlDecl.withEncodingOption(Option(loc.getEncoding).map(enc => Charset.forName(enc)))
      }
    }
  }

  private def pushContextIfNeeded(): Unit = {
    if (!namespaceContextStarted) {
      namespaceSupport.pushContext()
      namespaceContextStarted = true
    }
  }

  private def startElementToInternalElemNode(
    uri: String, localName: String, qName: String, atts: Attributes, parentScope: Scope)(implicit qnameProvider: QNameProvider): InternalElemNode = {

    require(uri ne null)
    require(localName ne null)
    require(qName ne null)

    val elmQName: QName = if (qName != "") qnameProvider.parseQName(qName) else qnameProvider.getUnprefixedQName(localName)

    val nsDecls = extractDeclarations(namespaceSupport)
    val attrSeq = extractAttributeMap(atts)

    val newScope = parentScope.resolve(nsDecls)

    new InternalElemNode(
      parentOption = None,
      qname = elmQName,
      attributes = attrSeq,
      scope = newScope,
      children = mutable.IndexedSeq())
  }

  private def extractDeclarations(nsSupport: NamespaceSupport): Declarations = {
    val prefixIterator =
      nsSupport.getDeclaredPrefixes().asInstanceOf[java.util.Enumeration[String]].asScala

    val prefUriMap =
      (prefixIterator map { pref =>
        val uri = nsSupport.getURI(pref)
        (pref, if (uri == null) "" else uri)
      }).toMap

    Declarations.from(prefUriMap)
  }

  private def extractAttributeMap(atts: Attributes)(implicit qnameProvider: QNameProvider): immutable.IndexedSeq[(QName, String)] = {
    val result = attributeOrDeclarationSeq(atts) collect {
      case (qn, v) if !isNamespaceDeclaration(qn) =>
        val qname = qnameProvider.parseQName(qn)
        val attValue = v
        (qname -> attValue)
    }
    result
  }

  private def attributeOrDeclarationSeq(atts: Attributes): immutable.IndexedSeq[(String, String)] = {
    val result = (0 until atts.getLength).toIndexedSeq map { (idx: Int) => (atts.getQName(idx) -> atts.getValue(idx)) }
    result
  }

  /** Returns true if the attribute qualified (prefixed) name is a namespace declaration */
  private def isNamespaceDeclaration(attrQName: String): Boolean = {
    val arr = attrQName.split(':')
    require(arr.length >= 1 && arr.length <= 2)
    val result = arr(0) == "xmlns"
    result
  }

  private[parse] trait InternalNode {
    type NodeType <: Node

    def toNode: NodeType
  }

  private[parse] trait CanBeInternalDocumentChild extends InternalNode {
    override type NodeType <: CanBeDocumentChild
  }

  private[parse] final class InternalElemNode(
    var parentOption: Option[InternalElemNode],
    val qname: QName,
    val attributes: immutable.IndexedSeq[(QName, String)],
    val scope: Scope,
    var children: mutable.IndexedSeq[InternalNode]) extends CanBeInternalDocumentChild {

    type NodeType = Elem

    def toNode: Elem = {
      // Recursive (not tail-recursive)
      val childSeq = (children map { ch => ch.toNode }).toVector

      new Elem(
        qname = qname,
        attributes = attributes,
        scope = scope,
        children = childSeq)
    }
  }

  private[parse] final class InternalTextNode(text: String, isCData: Boolean) extends InternalNode {
    type NodeType = Text

    def toNode: Text = Text(text, isCData)
  }

  private[parse] final class InternalProcessingInstructionNode(target: String, data: String) extends CanBeInternalDocumentChild {
    type NodeType = ProcessingInstruction

    def toNode: ProcessingInstruction = ProcessingInstruction(target, data)
  }

  private[parse] final class InternalEntityRefNode(entity: String) extends InternalNode {
    type NodeType = EntityRef

    def toNode: EntityRef = EntityRef(entity)
  }

  private[parse] final class InternalCommentNode(text: String) extends CanBeInternalDocumentChild {
    type NodeType = Comment

    def toNode: Comment = Comment(text)
  }
}
