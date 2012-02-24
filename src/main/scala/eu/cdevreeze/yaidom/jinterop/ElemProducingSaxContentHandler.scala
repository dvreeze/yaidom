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
import ElemProducingSaxContentHandler._

/**
 * SAX ContentHandler that, once ready, can be asked for the resulting [[eu.cdevreeze.yaidom.Elem]] using
 * method <code>resultingElem</code>, or the resulting [[eu.cdevreeze.yaidom.Document]] using method
 * <code>resultingDocument</code>.
 *
 * This is a trait instead of a class, so it is easy to mix in EntityResolvers, ErrorHandlers, etc.
 */
trait ElemProducingSaxContentHandler extends DefaultHandler with LexicalHandler {

  // This content handler has a very simple state space, so is easy to reason about.
  // It may not be the fastest implementation, but it returns a thread-safe immutable reusable yaidom Document,
  // so in a bigger picture it is fast enough.

  @volatile var currentState: State = State.Empty

  @volatile var currentlyInCData: Boolean = false

  final override def startDocument() = ()

  final override def startElement(uri: String, localName: String, qName: String, atts: Attributes) {
    val elm = startElementToElem(uri, localName, qName, atts)

    if (currentState.documentOption.isEmpty) {
      val doc = Document(None, elm, currentState.topLevelProcessingInstructions, currentState.topLevelComments)
      val newState = State(Some(doc), ElemPath.Root)
      currentState = newState
    } else {
      val updatedDoc = currentState.documentOption.get.updated(currentState.elemPath, currentState.elem.plusChild(elm))

      val updatedParent = updatedDoc.documentElement.findWithElemPath(currentState.elemPath).get
      val pathEntry = elm.ownElemPathEntry(updatedParent)

      val newState = State(Some(updatedDoc), currentState.elemPath.append(pathEntry))
      currentState = newState
    }
  }

  final override def endElement(uri: String, localName: String, qName: String) {
    require(currentState.documentOption.isDefined)
    val newState = State(currentState.documentOption, currentState.elemPath.parentPathOption.getOrElse(ElemPath.Root))
    currentState = newState
  }

  final override def characters(ch: Array[Char], start: Int, length: Int) {
    val isCData = this.currentlyInCData
    val text = Text(new String(ch, start, length), isCData)

    if (currentState.documentOption.isEmpty) {
      // Ignore
      require(currentState.elemPath == ElemPath.Root)
    } else {
      val updatedDoc = currentState.documentOption.get.updated(currentState.elemPath, currentState.elem.plusChild(text))
      val newState = State(Some(updatedDoc), currentState.elemPath)
      currentState = newState
    }
  }

  final override def processingInstruction(target: String, data: String) {
    val pi = ProcessingInstruction(target, data)

    if (currentState.documentOption.isEmpty) {
      require(currentState.elemPath == ElemPath.Root)
      currentState = new State(
        None,
        ElemPath.Root,
        currentState.topLevelProcessingInstructions :+ pi,
        currentState.topLevelComments)
    } else {
      val updatedDoc = currentState.documentOption.get.updated(currentState.elemPath, currentState.elem.plusChild(pi))
      val newState = State(Some(updatedDoc), currentState.elemPath)
      currentState = newState
    }
  }

  final override def endDocument() = ()

  final override def ignorableWhitespace(ch: Array[Char], start: Int, length: Int) {
    // Self call. If ignorable whitespace makes it until here, we store it in the result tree.
    characters(ch, start, length)
  }

  // ContentHandler methods startPrefixMapping, endPrefixMapping, skippedEntity and setDocumentLocator not overridden

  final override def startCDATA() {
    this.currentlyInCData = true
  }

  final override def endCDATA() {
    this.currentlyInCData = false
  }

  final override def comment(ch: Array[Char], start: Int, length: Int) {
    val comment = Comment(new String(ch, start, length))

    if (currentState.documentOption.isEmpty) {
      require(currentState.elemPath == ElemPath.Root)
      currentState = new State(
        None,
        ElemPath.Root,
        currentState.topLevelProcessingInstructions,
        currentState.topLevelComments :+ comment)
    } else {
      val updatedDoc = currentState.documentOption.get.updated(currentState.elemPath, currentState.elem.plusChild(comment))
      val newState = State(Some(updatedDoc), currentState.elemPath)
      currentState = newState
    }
  }

  final override def startEntity(name: String) = ()

  final override def endEntity(name: String) = ()

  final override def startDTD(name: String, publicId: String, systemId: String) = ()

  final override def endDTD() = ()

  /** Returns the resulting Elem. Do not call before SAX parsing is ready. */
  final def resultingElem: Elem = {
    require(currentState.elemPath.isRoot, "When parsing is ready, the current path must be at the root")
    currentState.documentOption.getOrElse(sys.error("No Document found. Was parsing ready?")).documentElement
  }

  /** Returns the resulting Document. Do not call before SAX parsing is ready. */
  final def resultingDocument: Document =
    currentState.documentOption.getOrElse(sys.error("No Document found. Was parsing ready?"))

  private def startElementToElem(uri: String, localName: String, qName: String, atts: Attributes): Elem = {
    require(uri ne null)
    require(localName ne null)
    require(qName ne null)

    val elmQName: QName = if (qName != "") qName.qname else localName.qname

    val newScope = currentState.scope.resolve(extractDeclarations(atts))
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

object ElemProducingSaxContentHandler {

  /**
   * The state kept while building the Document. Roughly it contains the Document built so far,
   * and the current ElemPath.
   *
   * There are 2 phases: before parsing the document element, and after parsing it.
   * Before parsing the document element, top level processing instructions and comments can be set.
   * After parsing the document element, the document is kept (in property documentOption), and
   * those top level processing instructions and comments, if any, are only kept in the document.
   */
  final class State(
    val documentOption: Option[Document],
    val elemPath: ElemPath,
    val topLevelProcessingInstructions: immutable.IndexedSeq[ProcessingInstruction],
    val topLevelComments: immutable.IndexedSeq[Comment]) extends Immutable {

    require(documentOption ne null)
    require(elemPath ne null)
    require(topLevelProcessingInstructions ne null)
    require(topLevelComments ne null)
    require(topLevelProcessingInstructions.isEmpty || documentOption.isEmpty)

    def elemOption: Option[Elem] = {
      documentOption map { doc => doc.documentElement } flatMap { root => root.findWithElemPath(elemPath) }
    }

    def elem: Elem = elemOption.getOrElse(sys.error("Expected an Elem at path '%s'".format(elemPath)))

    def documentElement: Elem = documentOption.getOrElse(sys.error("Expected Document to exist")).documentElement

    def scope: Scope = elemOption map { e => e.scope } getOrElse (Scope.Empty)
  }

  object State {

    val Empty = new State(None, ElemPath.Root, immutable.IndexedSeq(), immutable.IndexedSeq())

    def apply(docOption: Option[Document], path: ElemPath) = new State(
      docOption, path, immutable.IndexedSeq(), immutable.IndexedSeq())
  }
}
