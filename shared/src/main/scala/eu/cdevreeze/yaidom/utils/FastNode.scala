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

package eu.cdevreeze.yaidom.utils

import eu.cdevreeze.yaidom.XmlStringUtils
import eu.cdevreeze.yaidom.queryapi.Nodes
import eu.cdevreeze.yaidom.queryapi.ScopedElemApi
import eu.cdevreeze.yaidom.queryapi.ScopedElemNodeApi
import eu.cdevreeze.yaidom.queryapi.TransformableElemApi
import eu.cdevreeze.yaidom.queryapi.UpdatableElemApi

object FastNode {

  /**
   * Node super-type of "fast" elements.
   *
   * @author Chris de Vreeze
   */
  sealed trait Node extends Nodes.Node

  sealed trait CanBeDocumentChild extends Node with Nodes.CanBeDocumentChild

  /**
   * "Fast element". Like simple elements in the API, but more like resolved elements internally.
   * In other words, the state is like that of simple elements except for replacing QNames by ENames.
   *
   * TODO Make this a final class, and implement it.
   *
   * @author Chris de Vreeze
   */
  trait Elem
    extends CanBeDocumentChild
    with Nodes.Elem
    with ScopedElemNodeApi
    with ScopedElemApi
    with UpdatableElemApi
    with TransformableElemApi {

    type ThisElem <: Elem
  }

  final case class Text(text: String, isCData: Boolean) extends Node with Nodes.Text {
    require(text ne null) // scalastyle:off null
    if (isCData) require(!text.containsSlice("]]>"))

    /** Returns `text.trim`. */
    def trimmedText: String = text.trim

    /** Returns `XmlStringUtils.normalizeString(text)` .*/
    def normalizedText: String = XmlStringUtils.normalizeString(text)
  }

  final case class ProcessingInstruction(target: String, data: String) extends CanBeDocumentChild with Nodes.ProcessingInstruction {
    require(target ne null) // scalastyle:off null
    require(data ne null) // scalastyle:off null
  }

  /**
   * An entity reference. For example:
   * {{{
   * &hello;
   * }}}
   * We obtain this entity reference as follows:
   * {{{
   * EntityRef("hello")
   * }}}
   */
  final case class EntityRef(entity: String) extends Node with Nodes.EntityRef {
    require(entity ne null) // scalastyle:off null
  }

  final case class Comment(text: String) extends CanBeDocumentChild with Nodes.Comment {
    require(text ne null) // scalastyle:off null
  }

  object Elem {
  }
}
