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

import scala.annotation.tailrec
import scala.collection.immutable

/**
 * Implementation trait for elements that can be asked for the ancestor elements, if any.
 *
 * This trait only knows about elements, not about documents as root element parents.
 *
 * Based on abstract method `parentOption` alone, this trait offers a rich API for querying the element ancestry of an element.
 *
 * @tparam E The captured element subtype
 *
 * @author Chris de Vreeze
 */
trait HasParent[E <: HasParent[E]] extends HasParentApi[E] { self: E =>

  // Implementation note: this is not DRY because it is pretty much the same code as in the corresponding type class.
  // Yet I did not want to depend on a val or def returning the appropriate type class instance, so chose for code repetition.

  /**
   * Returns the equivalent `parentOption.get`, throwing an exception if this is the root element
   */
  final def parent: E = parentOption.getOrElse(sys.error("There is no parent element"))

  /**
   * Returns all ancestor elements or self
   */
  final def ancestorsOrSelf: immutable.IndexedSeq[E] =
    self +: (parentOption.toIndexedSeq flatMap ((e: E) => e.ancestorsOrSelf))

  /**
   * Returns `ancestorsOrSelf.drop(1)`
   */
  final def ancestors: immutable.IndexedSeq[E] = ancestorsOrSelf.drop(1)

  /**
   * Returns the first found ancestor-or-self element obeying the given predicate, if any, wrapped in an Option
   */
  @tailrec
  final def findAncestorOrSelf(p: E => Boolean): Option[E] = {
    if (p(self)) Some(self) else {
      val optParent = parentOption
      if (optParent.isEmpty) None else optParent.get.findAncestorOrSelf(p)
    }
  }

  /**
   * Returns the first found ancestor element obeying the given predicate, if any, wrapped in an Option
   */
  final def findAncestor(p: E => Boolean): Option[E] = {
    parentOption flatMap { e => e.findAncestorOrSelf(p) }
  }
}

object HasParent {

  /**
   * The `HasParent` as type class trait. Each of the functions takes "this" element as first parameter.
   * Custom element implementations such as W3C DOM or Saxon NodeInfo can thus get this API without any wrapper object costs.
   */
  trait FunctionApi[E] extends HasParentApi.FunctionApi[E] {

    final def parent(thisElem: E): E = parentOption(thisElem: E).getOrElse(sys.error("There is no parent element"))

    final def ancestorsOrSelf(thisElem: E): immutable.IndexedSeq[E] = {
      thisElem +: (parentOption(thisElem: E).toIndexedSeq flatMap ((e: E) => ancestorsOrSelf(e)))
    }

    final def ancestors(thisElem: E): immutable.IndexedSeq[E] = ancestorsOrSelf(thisElem).drop(1)

    @tailrec
    final def findAncestorOrSelf(thisElem: E, p: E => Boolean): Option[E] = {
      if (p(thisElem)) Some(thisElem) else {
        val optParent = parentOption(thisElem)
        if (optParent.isEmpty) None else findAncestorOrSelf(optParent.get, p)
      }
    }

    final def findAncestor(thisElem: E, p: E => Boolean): Option[E] = {
      parentOption(thisElem) flatMap { e => findAncestorOrSelf(e, p) }
    }
  }
}
