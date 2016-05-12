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

package eu.cdevreeze.yaidom.resolved

import scala.collection.immutable

import eu.cdevreeze.yaidom.queryapi.HasENameApi
import eu.cdevreeze.yaidom.queryapi.Nodes

/**
 * Abstract node trait hierarchy. It offers a common minimal API for different kinds of nodes that can be converted
 * (without any loss of information) to resolved nodes.
 *
 * @author Chris de Vreeze
 */
object ResolvedNodes {

  /**
   * Arbitrary node that can be converted to a resolved node.
   */
  trait Node extends Nodes.Node

  /**
   * Arbitrary element node that offers the `HasENameApi` query API and know its child nodes. Typical implementations
   * offer the `ScopedElemApi`, or at least the `ClarkElemApi`.
   */
  trait Elem extends Node with Nodes.Elem with HasENameApi {

    def children: immutable.IndexedSeq[Node]
  }

  /**
   * Arbitrary text node
   */
  trait Text extends Node with Nodes.Text {

    def text: String
  }

  object Elem {

    /**
     * The `Elem` as potential type class trait. Each of the functions takes "this" element as first parameter.
     * Custom element implementations such as W3C DOM or Saxon NodeInfo can thus get this API without any wrapper object costs.
     */
    trait FunctionApi[N, E <: N] extends HasENameApi.FunctionApi[E] {

      def children(thisElem: E): immutable.IndexedSeq[N]
    }
  }
}
