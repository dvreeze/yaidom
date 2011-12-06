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

import scala.collection.immutable

/**
 * DSL to build Elems without having to pass parent Scopes around.
 * Example:
 * <pre>
 * import NodeBuilder._
 *
 * elem(
 *   qname = QName("Magazine"),
 *   attributes = Map(QName("Month") -> "February", QName("Year") -> "2009"),
 *   namespaces = Map("dbclass" -> "http://www.db-class.org")),
 *   children = List(
 *     elem(
 *       qname = QName("Title"),
 *       children = List(text("Newsweek"))))).build()
 * </pre>
 *
 * TODO Indeed support easier creation of namespace declarations.
 * 
 * @author Chris de Vreeze
 */
sealed trait NodeBuilder extends Immutable {

  type NodeType <: Node

  def build(scope: Scope): NodeType

  final def build(): NodeType = build(Scope.Empty)
}

final class ElemBuilder(
  val qname: QName,
  val attributes: Map[QName, String],
  val namespaces: Scope.Declarations,
  val children: immutable.IndexedSeq[NodeBuilder]) extends NodeBuilder { self =>

  require(qname ne null)
  require(attributes ne null)
  require(namespaces ne null)
  require(children ne null)

  type NodeType = Elem

  def build(scope: Scope): Elem = {
    val newScope = scope.resolve(namespaces)

    Elem(
      qname,
      attributes,
      newScope,
      children.map(ch => ch.build(newScope)))
  }

  def withChildNodes(childNodes: immutable.Seq[Node])(scope: Scope): ElemBuilder = {
    new ElemBuilder(
      qname = self.qname,
      attributes = self.attributes,
      namespaces = self.namespaces,
      children = childNodes.map(ch => NodeBuilder.fromNode(ch)(scope)).toIndexedSeq)
  }
}

final case class TextBuilder(text: String) extends NodeBuilder {
  require(text ne null)

  type NodeType = Text

  def build(scope: Scope): Text = Text(text)
}

final case class ProcessingInstructionBuilder(target: String, data: String) extends NodeBuilder {
  require(target ne null)
  require(data ne null)

  type NodeType = ProcessingInstruction

  def build(scope: Scope): ProcessingInstruction = ProcessingInstruction(target, data)
}

final case class CDataBuilder(text: String) extends NodeBuilder {
  require(text ne null)

  type NodeType = CData

  def build(scope: Scope): CData = CData(text)
}

final case class EntityRefBuilder(entity: String) extends NodeBuilder {
  require(entity ne null)

  type NodeType = EntityRef

  def build(scope: Scope): EntityRef = EntityRef(entity)
}

object NodeBuilder {

  def elem(
    qname: QName,
    attributes: Map[QName, String] = Map(),
    namespaces: Scope.Declarations = new Scope.Declarations(Scope.Empty),
    children: immutable.Seq[NodeBuilder] = immutable.IndexedSeq()): ElemBuilder = {

    new ElemBuilder(qname, attributes, namespaces, children.toIndexedSeq)
  }

  def text(textValue: String): TextBuilder = TextBuilder(textValue)

  def processingInstruction(target: String, data: String): ProcessingInstructionBuilder =
    ProcessingInstructionBuilder(target, data)

  def cdata(textValue: String): CDataBuilder = CDataBuilder(textValue)

  def entityRef(entity: String): EntityRefBuilder = EntityRefBuilder(entity)

  /**
   * Converts a Node to a NodeBuilder, given a Scope.
   *
   * The following must always hold: fromNode(node)(scope).build(scope) "is structurally equal to" node
   */
  def fromNode(node: Node)(scope: Scope): NodeBuilder = node match {
    case Text(s) => TextBuilder(s)
    case ProcessingInstruction(target, data) => ProcessingInstructionBuilder(target, data)
    case CData(s) => CDataBuilder(s)
    case EntityRef(entity) => EntityRefBuilder(entity)
    case e: Elem =>
      require(scope.resolve(scope.relativize(e.scope)) == e.scope)

      // Recursive call, but not tail-recursive
      new ElemBuilder(
        qname = e.qname,
        attributes = e.attributes,
        namespaces = scope.relativize(e.scope),
        children = e.children.map(ch => fromNode(ch)(e.scope)))
  }

  def fromElem(elm: Elem)(scope: Scope): ElemBuilder = fromNode(elm)(scope).asInstanceOf[ElemBuilder]
}
