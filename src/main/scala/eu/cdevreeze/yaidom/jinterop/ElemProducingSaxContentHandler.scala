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
import scala.collection.immutable
import eu.cdevreeze.yaidom._
import ElemProducingSaxContentHandler._

/**
 * SAX ContentHandler that, once ready, can be asked for the resulting [[eu.cdevreeze.yaidom.Elem]] using
 * method <code>resultingElem</code>, or the resulting [[eu.cdevreeze.yaidom.Document]] using method
 * <code>resultingDocument</code>.
 *
 * Can be used as mixin, mixing in some EntityResolver as well, for example.
 */
trait ElemProducingSaxContentHandler extends DefaultHandler {

  // This content handler has a very simple state space, so is easy to reason about.
  // It may not be the fastest implementation, but it returns a thread-safe immutable reusable yaidom Document,
  // so in a bigger picture it is fast enough.

  @volatile var currentState: State = State.Empty

  final override def startDocument() = ()

  final override def startElement(uri: String, localName: String, qName: String, atts: Attributes) {
    val elm = startElementToElem(uri, localName, qName, atts)

    if (currentState.documentOption.isEmpty) {
      val doc = Document(None, elm, currentState.topLevelProcessingInstructions, immutable.IndexedSeq())
      val newState = new State(Some(doc), ElemPath.Root)
      currentState = newState
    } else {
      val updatedDoc = currentState.documentOption.get.updated(currentState.elemPath, { e => currentState.elem.plusChild(elm) })

      val newParent = updatedDoc.documentElement.findWithElemPath(currentState.elemPath).get
      val pathEntry = elm.ownElemPathEntry(newParent)

      val newState = new State(Some(updatedDoc), currentState.elemPath.append(pathEntry))
      currentState = newState
    }
  }

  final override def endElement(uri: String, localName: String, qName: String) {
    require(currentState.documentOption.isDefined)
    val newState = new State(currentState.documentOption, currentState.elemPath.parentPathOption.getOrElse(ElemPath.Root))
    currentState = newState
  }

  final override def characters(ch: Array[Char], start: Int, length: Int) {
    val text = Text(new String(ch, start, length), false)

    if (currentState.documentOption.isEmpty) {
      // Ignore
      require(currentState.elemPath == ElemPath.Root)
    } else {
      val updatedDoc = currentState.documentOption.get.updated(currentState.elemPath, { e => currentState.elem.plusChild(text) })
      val newState = new State(Some(updatedDoc), currentState.elemPath)
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
        currentState.topLevelProcessingInstructions :+ pi)
    } else {
      val updatedDoc = currentState.documentOption.get.updated(currentState.elemPath, { e => currentState.elem.plusChild(pi) })
      val newState = new State(Some(updatedDoc), currentState.elemPath)
      currentState = newState
    }
  }

  final override def endDocument() = ()

  final override def ignorableWhitespace(ch: Array[Char], start: Int, length: Int) {
    // Self call. If ignorable whitespace makes it until here, we store it in the result tree.
    characters(ch, start, length)
  }

  // ContentHandler methods startPrefixMapping, endPrefixMapping, skippedEntity and setDocumentLocator not overridden

  /** Returns the resulting Elem. Do not call before SAX parsing is ready. */
  final def resultingElem: Elem = {
    val root = currentState.documentOption.getOrElse(sys.error("No Document found. Was parsing ready?")).documentElement
    root.findWithElemPath(currentState.elemPath).getOrElse(sys.error("Wrong ElemPath '%s'".format(currentState.elemPath)))
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

  final class State(
    val documentOption: Option[Document],
    val elemPath: ElemPath,
    val topLevelProcessingInstructions: immutable.IndexedSeq[ProcessingInstruction] = immutable.IndexedSeq()) extends Immutable {

    require(documentOption ne null)
    require(elemPath ne null)
    require(topLevelProcessingInstructions ne null)
    require(topLevelProcessingInstructions.isEmpty || documentOption.isEmpty)

    def elemOption: Option[Elem] = {
      documentOption map { doc => doc.documentElement } flatMap { root => root.findWithElemPath(elemPath) }
    }

    def elem: Elem = elemOption.getOrElse(sys.error("Expected an Elem at path '%s'".format(elemPath)))

    def documentElement: Elem = documentOption.getOrElse(sys.error("Expected Document to exist")).documentElement

    def scope: Scope = elemOption map { e => e.scope } getOrElse (Scope.Empty)
  }

  object State {

    val Empty = new State(None, ElemPath.Root)
  }
}
