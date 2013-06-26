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

import scala.collection.{ immutable, mutable }

/**
 * Transformable element. It defines a contract for transformations of element trees.
 *
 * '''Most users of the yaidom API do not use this trait directly, so may skip the documentation of this trait.'''
 *
 * Based on abstract method `withMappedChildElems`, this trait offers a somewhat richer API for transforming elements.
 *
 * @tparam N The node supertype of the element subtype
 * @tparam E The captured element subtype
 *
 * @author Chris de Vreeze
 */
trait TransformableElemLike[N, E <: N with TransformableElemLike[N, E]] extends TransformableElemApi[N, E] { self: E =>

  def withMappedChildElems(f: E => E): E

  def withFlatMappedChildElems(f: E => immutable.IndexedSeq[N]): E

  final def transform(f: E => E): E = {
    f(withMappedChildElems(e => e.transform(f)))
  }

  final def transform(f: (E, immutable.IndexedSeq[E]) => E, ancestry: immutable.IndexedSeq[E]): E = {
    f(withMappedChildElems(e => e.transform(f, (ancestry :+ self))), ancestry)
  }

  final def transformToNodeSeq(f: E => immutable.IndexedSeq[N]): immutable.IndexedSeq[N] = {
    f(withFlatMappedChildElems(e => e.transformToNodeSeq(f)))
  }

  final def transformToNodeSeq(
    f: (E, immutable.IndexedSeq[E]) => immutable.IndexedSeq[N],
    ancestry: immutable.IndexedSeq[E]): immutable.IndexedSeq[N] = {

    f(withFlatMappedChildElems(e => e.transformToNodeSeq(f, (ancestry :+ self))), ancestry)
  }
}
