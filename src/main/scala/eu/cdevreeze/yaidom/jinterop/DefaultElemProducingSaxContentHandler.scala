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
package jinterop

import org.xml.sax.{ ContentHandler, Attributes, Locator }
import org.xml.sax.helpers.DefaultHandler
import org.xml.sax.ext.LexicalHandler
import scala.collection.immutable
import eu.cdevreeze.yaidom._
import DefaultElemProducingSaxContentHandler._

/**
 * Default [[eu.cdevreeze.yaidom.jinterop.ElemProducingSaxContentHandler]] implementation.
 *
 * This is a trait instead of a class, so it is easy to mix in EntityResolvers, ErrorHandlers, etc.
 */
trait DefaultElemProducingSaxContentHandler extends ElemProducingSaxContentHandler with LexicalHandler {

  // This content handler has a very simple state space, so is easy to reason about.
  // It may not be the fastest implementation, but it returns a thread-safe immutable reusable yaidom Document,
  // so in a bigger picture it is fast enough.

  @volatile private var documentState: DocumentState = DocumentState.Empty

  @volatile private var currentlyInCData: Boolean = false

  final override def startDocument() = ()

  final override def startElement(uri: String, localName: String, qName: String, atts: Attributes) {
    val elm = startElementToElem(uri, localName, qName, atts)

    if (documentState.documentOption.isEmpty) {
      val newState = documentState.withDocumentElement(elm)
      documentState = newState
    } else {
      val updatedDoc = documentState.documentOption.get.updated(documentState.elemPath, documentState.elem.plusChild(elm))

      val updatedParent = updatedDoc.documentElement.findWithElemPath(documentState.elemPath).get
      val pathEntry = elm.ownElemPathEntry(updatedParent)

      val newState = documentState.withDocumentAndPath(updatedDoc, documentState.elemPath.append(pathEntry))
      documentState = newState
    }
  }

  final override def endElement(uri: String, localName: String, qName: String) {
    require(documentState.documentOption.isDefined)
    val newState = documentState.withDocumentAndPath(documentState.documentOption.get, documentState.elemPath.parentPathOption.getOrElse(ElemPath.Root))
    documentState = newState
  }

  final override def characters(ch: Array[Char], start: Int, length: Int) {
    val isCData = this.currentlyInCData
    val text = Text(new String(ch, start, length), isCData)

    if (documentState.documentOption.isEmpty) {
      // Ignore
      require(documentState.elemPath == ElemPath.Root)
    } else {
      val updatedDoc = documentState.documentOption.get.updated(documentState.elemPath, documentState.elem.plusChild(text))
      val newState = documentState.withDocumentAndPath(updatedDoc, documentState.elemPath)
      documentState = newState
    }
  }

  final override def processingInstruction(target: String, data: String) {
    val pi = ProcessingInstruction(target, data)

    if (documentState.documentOption.isEmpty) {
      require(documentState.elemPath == ElemPath.Root)
      documentState = documentState.withTopLevelProcessingInstruction(pi)
    } else {
      val updatedDoc = documentState.documentOption.get.updated(documentState.elemPath, documentState.elem.plusChild(pi))
      val newState = documentState.withDocumentAndPath(updatedDoc, documentState.elemPath)
      documentState = newState
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
    val comment = Comment(new String(ch, start, length))

    if (documentState.documentOption.isEmpty) {
      require(documentState.elemPath == ElemPath.Root)
      documentState = documentState.withTopLevelComment(comment)
    } else {
      val updatedDoc = documentState.documentOption.get.updated(documentState.elemPath, documentState.elem.plusChild(comment))
      val newState = documentState.withDocumentAndPath(updatedDoc, documentState.elemPath)
      documentState = newState
    }
  }

  final override def startEntity(name: String) = ()

  final override def endEntity(name: String) = ()

  final override def startDTD(name: String, publicId: String, systemId: String) = ()

  final override def endDTD() = ()

  final def resultingElem: Elem = {
    require(documentState.elemPath.isRoot, "When parsing is ready, the current path must be at the root")
    documentState.documentOption.getOrElse(sys.error("No Document found. Was parsing ready?")).documentElement
  }

  final def resultingDocument: Document =
    documentState.documentOption.getOrElse(sys.error("No Document found. Was parsing ready?"))

  private def startElementToElem(uri: String, localName: String, qName: String, atts: Attributes): Elem = {
    require(uri ne null)
    require(localName ne null)
    require(qName ne null)

    val elmQName: QName = if (qName != "") qName.qname else localName.qname

    val newScope = documentState.scope.resolve(extractDeclarations(atts))
    val attrMap = extractAttributeMap(atts)

    Elem(qname = elmQName, attributes = attrMap, scope = newScope, children = immutable.IndexedSeq())
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

object DefaultElemProducingSaxContentHandler {

  /**
   * The Document state kept while building the Document. Roughly it contains the Document built so far,
   * and the current ElemPath.
   *
   * There are 2 phases: before parsing the document element, and after parsing it.
   * Before parsing the document element, top level processing instructions and comments can be set.
   * After parsing the document element, the document is kept (in property documentOption), and
   * those top level processing instructions and comments, if any, are only kept in the document.
   */
  final class DocumentState private (
    val documentOption: Option[Document],
    val elemPath: ElemPath,
    val topLevelProcessingInstructions: immutable.IndexedSeq[ProcessingInstruction],
    val topLevelComments: immutable.IndexedSeq[Comment]) extends Immutable {

    require(documentOption ne null)
    require(elemPath ne null)
    require(topLevelProcessingInstructions ne null)
    require(topLevelComments ne null)
    require(topLevelProcessingInstructions.isEmpty || documentOption.isEmpty)
    require(topLevelComments.isEmpty || documentOption.isEmpty)

    def elemOption: Option[Elem] = {
      documentOption map { doc => doc.documentElement } flatMap { root => root.findWithElemPath(elemPath) }
    }

    def elem: Elem = elemOption.getOrElse(sys.error("Expected an Elem at path '%s'".format(elemPath)))

    def documentElement: Elem = documentOption.getOrElse(sys.error("Expected Document to exist")).documentElement

    def scope: Scope = elemOption map { e => e.scope } getOrElse (Scope.Empty)

    /** Creates a new "before-document-root" state with added top level processing instruction */
    def withTopLevelProcessingInstruction(processingInstruction: ProcessingInstruction): DocumentState = {
      require(this.documentOption.isEmpty)
      new DocumentState(None, ElemPath.Root, this.topLevelProcessingInstructions :+ processingInstruction, this.topLevelComments)
    }

    /** Creates a new "before-document-root" state with added top level comment */
    def withTopLevelComment(comment: Comment): DocumentState = {
      require(this.documentOption.isEmpty)
      new DocumentState(None, ElemPath.Root, this.topLevelProcessingInstructions, this.topLevelComments :+ comment)
    }

    /** Creates an "after-document-root" state from a "before-document-root" state */
    def withDocumentElement(docElm: Elem): DocumentState = {
      require(this.documentOption.isEmpty)
      val doc = Document(None, docElm, this.topLevelProcessingInstructions, this.topLevelComments)
      new DocumentState(Some(doc), ElemPath.Root, immutable.IndexedSeq(), immutable.IndexedSeq())
    }

    /** Creates a new "after-document-root" state with an updated Document and ElemPath */
    def withDocumentAndPath(updatedDoc: Document, newPath: ElemPath): DocumentState = {
      require(this.documentOption.isDefined)
      new DocumentState(Some(updatedDoc), newPath, immutable.IndexedSeq(), immutable.IndexedSeq())
    }
  }

  object DocumentState {

    val Empty = new DocumentState(None, ElemPath.Root, immutable.IndexedSeq(), immutable.IndexedSeq())
  }
}
