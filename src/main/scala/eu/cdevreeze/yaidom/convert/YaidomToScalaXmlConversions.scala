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
package convert

import java.{ util => jutil }
import javax.xml.XMLConstants
import scala.collection.JavaConverters._
import scala.collection.{ immutable, mutable }
import eu.cdevreeze.yaidom

/**
 * Converter from yaidom nodes to Scala XML nodes, in particular from [[eu.cdevreeze.yaidom.Elem]] to a `scala.xml.Elem`.
 *
 * There is no conversion from yaidom Documents to Scala XML documents, because there is no direct way to create Scala XML
 * documents.
 *
 * @author Chris de Vreeze
 */
trait YaidomToScalaXmlConversions extends ElemConverter[scala.xml.Elem] {

  /**
   * Converts a yaidom node to a Scala XML node.
   */
  final def convertNode(node: Node): scala.xml.Node = {
    node match {
      case e: Elem => convertElem(e)
      case t: Text => convertText(t)
      case pi: ProcessingInstruction => convertProcessingInstruction(pi)
      case er: EntityRef => convertEntityRef(er)
      case c: Comment => convertComment(c)
    }
  }

  /**
   * Converts a yaidom `Elem` to a Scala XML element.
   */
  final def convertElem(elm: Elem): scala.xml.Elem = {
    // Not tail-recursive, but the recursion depth should be limited

    val prefix = elm.qname.prefixOption.orNull
    val label = elm.qname.localPart

    val attributes = convertAttributes(elm.attributes)
    val scope = convertScope(elm.scope)

    val children: immutable.IndexedSeq[scala.xml.Node] = elm.children map { ch => convertNode(ch) }

    // Note that this constructor has been deprecated since Scala 2.10 (but it was the only constructor in older Scala versions)
    new scala.xml.Elem(prefix, label, attributes, scope, children: _*)
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

  private def convertScope(scope: Scope): scala.xml.NamespaceBinding = {
    if (scope.isEmpty) scala.xml.TopScope
    else {
      val scopeAsSeq = scope.map.toSeq map {
        case (pref, uri) =>
          if (pref.isEmpty) (null.asInstanceOf[String] -> uri) else (pref -> uri)
      }
      assert(!scopeAsSeq.isEmpty)

      var nsBinding: scala.xml.NamespaceBinding =
        scala.xml.NamespaceBinding(scopeAsSeq.head._1, scopeAsSeq.head._2, scala.xml.TopScope)

      for ((pref, nsUri) <- scopeAsSeq.tail) {
        nsBinding = scala.xml.NamespaceBinding(pref, nsUri, nsBinding)
      }

      nsBinding
    }
  }
}
