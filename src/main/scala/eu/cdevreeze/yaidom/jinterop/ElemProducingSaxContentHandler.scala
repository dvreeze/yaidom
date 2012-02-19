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
import eu.cdevreeze.yaidom._
import ElemProducingSaxContentHandler._

/**
 * SAX ContentHandler that, once ready, can be asked for the resulting [[eu.cdevreeze.yaidom.Elem]] using
 * method <code>resultingElem</code>, or the resulting [[eu.cdevreeze.yaidom.Document]] using method
 * <code>resultingDocument</code>.
 */
final class ElemProducingSaxContentHandler extends DefaultHandler {

  // Robust "functional" but inefficient implementation, but, who knows, maybe it is fast enough...

  @volatile var currentState: State = State.Empty

  override def startDocument() = ()

  override def startElement(uri: String, localName: String, qName: String, atts: Attributes) {
    val elm = startElementToElem(uri, localName, qName, atts)

    if (currentState.documentOption.isEmpty) {
      val newState = new State(Some(Document(None, elm)), ElemPath.Root)
      currentState = newState
    } else {
      val newParent = currentState.documentElement.findWithElemPath(currentState.elemPath).get

      val updatedRoot = currentState.documentElement updated {
        case p if p == currentState.elemPath => newParent.plusChild(elm)
      }

      val updatedNewParent = updatedRoot.findWithElemPath(currentState.elemPath).get

      val newDoc = currentState.documentOption.get.withDocumentElement(updatedRoot)
      val pathEntry = elm.ownElemPathEntry(updatedNewParent)
      val newState = new State(Some(newDoc), currentState.elemPath.append(pathEntry))
      currentState = newState
    }
  }

  override def endElement(uri: String, localName: String, qName: String) {
    require(currentState.documentOption.isDefined)
    val newState = new State(currentState.documentOption, currentState.elemPath.parentPathOption.getOrElse(ElemPath.Root))
    currentState = newState
  }

  override def characters(ch: Array[Char], start: Int, length: Int) {
    val text = Text(new String(ch, start, length), false)

    if (currentState.documentOption.isEmpty) {
      // Currently ignored
    } else {
      val updatedRoot = currentState.documentElement updated {
        case p if p == currentState.elemPath => currentState.elem.plusChild(text)
      }

      val newDoc = currentState.documentOption.get.withDocumentElement(updatedRoot)
      val newState = new State(Some(newDoc), currentState.elemPath)
      currentState = newState
    }
  }

  override def processingInstruction(target: String, data: String) {
    val pi = ProcessingInstruction(target, data)

    if (currentState.documentOption.isEmpty) {
      // Currently ignored
    } else {
      val updatedRoot = currentState.documentElement updated {
        case p if p == currentState.elemPath => currentState.elem.plusChild(pi)
      }

      val newDoc = currentState.documentOption.get.withDocumentElement(updatedRoot)
      val newState = new State(Some(newDoc), currentState.elemPath)
      currentState = newState
    }
  }

  override def endDocument() = ()

  override def ignorableWhitespace(ch: Array[Char], start: Int, length: Int) {
    // Self call. If ignorable whitespace makes it until here, we store it in the result tree.
    characters(ch, start, length)
  }

  // ContentHandler methods startPrefixMapping, endPrefixMapping, skippedEntity and setDocumentLocator not overridden

  /** Returns the resulting Elem. Do not call before SAX parsing is ready. */
  def resultingElem: Elem = {
    val root = currentState.documentOption.getOrElse(sys.error("No Document found. Was parsing ready?")).documentElement
    root.findWithElemPath(currentState.elemPath).getOrElse(sys.error("Wrong ElemPath '%s'".format(currentState.elemPath)))
  }

  /** Returns the resulting Document. Do not call before SAX parsing is ready. */
  def resultingDocument: Document = currentState.documentOption.getOrElse(sys.error("No Document found. Was parsing ready?"))

  private def startElementToElem(uri: String, localName: String, qName: String, atts: Attributes): Elem = {
    require(uri ne null)
    require(localName ne null)
    require(qName ne null)

    val elmQName: QName = if (qName != "") qName.qname else localName.qname

    val newScope = currentState.scope.resolve(extractDeclarations(atts))
    val attrMap = extractAttributeMap(atts)

    Elem(
      qname = elmQName,
      attributes = attrMap,
      scope = newScope,
      children = Nil)
  }

  private def extractDeclarations(atts: Attributes): Scope.Declarations = {
    val attMap: Map[String, String] = {
      val result = (0 until atts.getLength).toIndexedSeq map { (idx: Int) => (atts.getQName(idx) -> atts.getValue(idx)) }
      result.toMap
    }

    val result = attMap filterKeys { qname => isNamespaceDeclaration(qname) } map { kv =>
      val key = kv._1.qname
      val prefix = if (key.prefixOption.isEmpty) "" else key.localPart
      val nsUri = kv._2
      (prefix -> nsUri)
    }
    Scope.Declarations.fromMap(result)
  }

  private def extractAttributeMap(atts: Attributes): Map[QName, String] = {
    val attMap: Map[String, String] = {
      val result = (0 until atts.getLength).toIndexedSeq map { (idx: Int) => (atts.getQName(idx) -> atts.getValue(idx)) }
      result.toMap
    }

    val result = attMap filterKeys { qname => !isNamespaceDeclaration(qname) } map { kv =>
      val qname = kv._1.qname
      val attValue = kv._2
      (qname -> attValue)
    }
    result
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
    val elemPath: ElemPath) extends Immutable {

    require(documentOption ne null)
    require(elemPath ne null)

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
