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

import scala.collection.immutable

import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.simple.Comment
import eu.cdevreeze.yaidom.simple.Elem
import eu.cdevreeze.yaidom.simple.ElemConverter
import eu.cdevreeze.yaidom.simple.EntityRef
import eu.cdevreeze.yaidom.simple.Node
import eu.cdevreeze.yaidom.simple.ProcessingInstruction
import eu.cdevreeze.yaidom.simple.Text

/**
 * Converter from yaidom nodes to Scala XML nodes, in particular from [[eu.cdevreeze.yaidom.simple.Elem]] to a `scala.xml.Elem`.
 *
 * There is no conversion from yaidom Documents to Scala XML documents, because there is no direct way to create Scala XML
 * documents.
 *
 * @author Chris de Vreeze
 */
trait YaidomToScalaXmlConversions extends ElemConverter[scala.xml.Elem] {

  /**
   * Converts a yaidom node to a Scala XML node, given a parent Scala XML scope.
   *
   * The parent NamespaceBinding is passed as extra parameter, in order to try to prevent the creation of any unnecessary
   * namespace declarations.
   */
  final def convertNode(node: Node, parentNamespaceBinding: scala.xml.NamespaceBinding): scala.xml.Node = {
    node match {
      case e: Elem => convertElem(e, parentNamespaceBinding)
      case t: Text => convertText(t)
      case pi: ProcessingInstruction => convertProcessingInstruction(pi)
      case er: EntityRef => convertEntityRef(er)
      case c: Comment => convertComment(c)
    }
  }

  /**
   * Converts a yaidom `Elem` to a Scala XML element.
   */
  final def convertElem(elm: Elem): scala.xml.Elem =
    convertElem(elm, scala.xml.TopScope)

  /**
   * Converts a yaidom `Elem` to a Scala XML element, given a parent Scala XML scope.
   *
   * The parent NamespaceBinding is passed as extra parameter, in order to try to prevent the creation of any unnecessary
   * namespace declarations.
   */
  final def convertElem(elm: Elem, parentNamespaceBinding: scala.xml.NamespaceBinding): scala.xml.Elem = {
    // Not tail-recursive, but the recursion depth should be limited

    val prefix = elm.qname.prefixOption.orNull
    val label = elm.qname.localPart

    val attributes = convertAttributes(elm.attributes)

    val decls = toScope(parentNamespaceBinding).relativize(elm.scope)
    val nsBinding = convertScope(elm.scope, parentNamespaceBinding)

    val children: immutable.IndexedSeq[scala.xml.Node] = elm.children.map(ch => convertNode(ch, nsBinding))

    val minimizeEmpty = children.isEmpty
    new scala.xml.Elem(prefix, label, attributes, nsBinding, minimizeEmpty, children: _*)
  }

  /**
   * Converts a yaidom `Text` to a Scala XML `Atom[String]`.
   */
  final def convertText(text: Text): scala.xml.Atom[String] = {
    if (text.isCData) scala.xml.PCData(text.text) else scala.xml.Text(text.text)
  }

  /**
   * Converts a yaidom `ProcessingInstruction` to a Scala XML `ProcInstr`.
   */
  final def convertProcessingInstruction(
    processingInstruction: ProcessingInstruction): scala.xml.ProcInstr = {

    new scala.xml.ProcInstr(processingInstruction.target, processingInstruction.data)
  }

  /**
   * Converts a yaidom `EntityRef` to a Scala XML `EntityRef`.
   */
  final def convertEntityRef(entityRef: EntityRef): scala.xml.EntityRef = {
    new scala.xml.EntityRef(entityRef.entity)
  }

  /**
   * Converts a yaidom `Comment` to a Scala XML `Comment`.
   */
  final def convertComment(comment: Comment): scala.xml.Comment = {
    new scala.xml.Comment(comment.text)
  }

  private def convertAttributes(attributes: Iterable[(QName, String)]): scala.xml.MetaData = {
    var result: scala.xml.MetaData = scala.xml.Null

    for (attr <- attributes) {
      result =
        result.append(
          scala.xml.Attribute(
            attr._1.prefixOption,
            attr._1.localPart,
            Seq(scala.xml.Text(attr._2)),
            result))
    }

    result
  }

  /**
   * Converts the yaidom Scope to a Scala XML NamespaceBinding.
   */
  private def convertScope(scope: Scope): scala.xml.NamespaceBinding = {
    def editedPrefix(pref: String): String =
      if ((pref ne null) && pref.isEmpty) null.asInstanceOf[String] else pref

    if (scope.isEmpty) scala.xml.TopScope
    else {
      val scopeAsSeq = scope.prefixNamespaceMap.toSeq map {
        case (pref, uri) => (editedPrefix(pref) -> uri)
      }
      assert(!scopeAsSeq.isEmpty)

      val topScope: scala.xml.NamespaceBinding = scala.xml.TopScope
      val nsBinding: scala.xml.NamespaceBinding = scopeAsSeq.foldLeft(topScope) {
        case (acc, (pref, nsUri)) =>
          scala.xml.NamespaceBinding(pref, nsUri, acc)
      }
      nsBinding
    }
  }

  /**
   * Converts the yaidom Scope to a Scala XML NamespaceBinding, but tries to keep re-use the parent NamespaceBinding
   * (possible prepending some "children") as much as possible. This helps in preventing many creations of duplicate
   * namespace declarations.
   *
   * See method scala.xml.NamespaceBinding.buildString(StringBuilder, scala.xml.NamespaceBinding) for how (implicit) namespace
   * declarations are inferred from a NamespaceBinding and one of its ancestors.
   */
  private def convertScope(scope: Scope, parentNamespaceBinding: scala.xml.NamespaceBinding): scala.xml.NamespaceBinding = {
    val decls = toScope(parentNamespaceBinding).relativize(scope)

    if (!decls.retainingUndeclarations.isEmpty) {
      // No way to re-use the immutable parent NamespaceBinding, so converting the scope without using the parent NamespaceBinding
      convertScope(scope)
    } else {
      val nsBinding = decls.prefixNamespaceMap.foldLeft(parentNamespaceBinding) {
        case (acc, (pref, uri)) =>
          val editedPrefix = if ((pref ne null) && (pref.isEmpty)) null.asInstanceOf[String] else pref

          scala.xml.NamespaceBinding(editedPrefix, uri, acc)
      }
      nsBinding
    }
  }

  /**
   * Converts the `scala.xml.NamespaceBinding` to a yaidom `Scope`.
   *
   * This implementation is brittle because of bug: SI 6939: Namespace binding (xmlns) is duplicated if a child redefines a prefix.
   * (see https://issues.scala-lang.org/browse/SI-6939 and https://github.com/scala/scala/pull/1858). Still, this implementation
   * tries to work around that bug.
   *
   * This method is the same as extractScope in ScalaXmlToYaidomConversions, but repeated here in order not to depend on that
   * other trait.
   */
  private def toScope(scope: scala.xml.NamespaceBinding): Scope = {
    if ((scope eq null) || (scope.uri eq null) || (scope == scala.xml.TopScope)) Scope.Empty
    else {
      val prefix = if (scope.prefix eq null) "" else scope.prefix

      // Recursive call (not tail-recursive), and working around the above-mentioned bug

      val parentScope = toScope(scope.parent)

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
}
