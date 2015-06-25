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

package eu.cdevreeze.yaidom.queryapi

import scala.collection.immutable

/**
 * Abstract node trait hierarchy. This minimal node abstraction is complete enough to create resolved nodes from them.
 *
 * The down-side is that we have to consider mixing in these traits everywhere we create a node/element implementation.
 *
 * @author Chris de Vreeze
 */
object Nodes {

  /**
   * Arbitrary node
   */
  trait Node

  /**
   * Arbitrary element node that offers the `HasENameApi` query API. Typical implementations offer the
   * `ScopedElemApi`, or at least the `ClarkElemApi`.
   */
  trait Elem[N <: Node] extends Node with HasENameApi {

    def children: immutable.IndexedSeq[N]
  }

  /**
   * Arbitrary text node
   */
  trait Text extends Node {

    def text: String
  }
}
