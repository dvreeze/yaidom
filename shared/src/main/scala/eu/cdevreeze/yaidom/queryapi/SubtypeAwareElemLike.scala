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

package eu.cdevreeze.yaidom.queryapi

import scala.collection.immutable
import scala.reflect.ClassTag

import ElemApi.anyElem

/**
 * Default implementation of SubtypeAwareElemApi.
 *
 * All methods are overridable. Hence element implementations mixing in this partial implementation trait can change the
 * implementation without breaking its API, caused by otherwise needed removal of this mixin. Arguably this trait should not
 * exist as part of the public API, because implementation details should not be part of the public API. Such implementation details
 * may be subtle, such as the (runtime) boundary on the ThisElem type member.
 *
 * @author Chris de Vreeze
 */
trait SubtypeAwareElemLike extends ElemLike with SubtypeAwareElemApi {

  type ThisElem <: SubtypeAwareElemLike.Aux[ThisElem]

  def findAllChildElemsOfType[B <: ThisElem](subType: ClassTag[B]): immutable.IndexedSeq[B] = {
    filterChildElemsOfType(subType)(anyElem)
  }

  def filterChildElemsOfType[B <: ThisElem](subType: ClassTag[B])(p: B => Boolean): immutable.IndexedSeq[B] = {
    filter(subType)(p) { pred => filterChildElems(pred) }
  }

  def findAllElemsOfType[B <: ThisElem](subType: ClassTag[B]): immutable.IndexedSeq[B] = {
    filterElemsOfType(subType)(anyElem)
  }

  def filterElemsOfType[B <: ThisElem](subType: ClassTag[B])(p: B => Boolean): immutable.IndexedSeq[B] = {
    filter(subType)(p) { pred => filterElems(pred) }
  }

  def findAllElemsOrSelfOfType[B <: ThisElem](subType: ClassTag[B]): immutable.IndexedSeq[B] = {
    filterElemsOrSelfOfType(subType)(anyElem)
  }

  def filterElemsOrSelfOfType[B <: ThisElem](subType: ClassTag[B])(p: B => Boolean): immutable.IndexedSeq[B] = {
    filter(subType)(p) { pred => filterElemsOrSelf(pred) }
  }

  def findChildElemOfType[B <: ThisElem](subType: ClassTag[B])(p: B => Boolean): Option[B] = {
    find(subType)(p) { pred => findChildElem(pred) }
  }

  def findElemOfType[B <: ThisElem](subType: ClassTag[B])(p: B => Boolean): Option[B] = {
    find(subType)(p) { pred => findElem(pred) }
  }

  def findElemOrSelfOfType[B <: ThisElem](subType: ClassTag[B])(p: B => Boolean): Option[B] = {
    find(subType)(p) { pred => findElemOrSelf(pred) }
  }

  def findTopmostElemsOfType[B <: ThisElem](subType: ClassTag[B])(p: B => Boolean): immutable.IndexedSeq[B] = {
    filter(subType)(p) { pred => findTopmostElems(pred) }
  }

  def findTopmostElemsOrSelfOfType[B <: ThisElem](subType: ClassTag[B])(p: B => Boolean): immutable.IndexedSeq[B] = {
    filter(subType)(p) { pred => findTopmostElemsOrSelf(pred) }
  }

  def getChildElemOfType[B <: ThisElem](subType: ClassTag[B])(p: B => Boolean): B = {
    val result = findChildElemOfType(subType)(p)
    require(result.size == 1, s"Expected exactly 1 matching child element, but found ${result.size} of them")
    result.head
  }

  // Note that the filter and find methods below make the code above very concise. Arguably without these filter and
  // find methods the code would have been more readable, at the cost of code repetition. We could even have gone further
  // and combined the filter and find methods into one method. After all, they duplicate the same code. On the other
  // hand, when seeking a balance between OO and functional programming (where OO stands for abstraction/naming),
  // combining the 2 functions would have made it more difficult to come up with a good method name, making the method
  // more difficult to understand.

  private def filter[B <: ThisElem](
    subType: ClassTag[B])(p: B => Boolean)(f: (ThisElem => Boolean) => immutable.IndexedSeq[ThisElem]): immutable.IndexedSeq[B] = {

    // Implicit ClassTag[B] to make pattern matching below work (the implicit ClassTag "undoes" type erasure)
    implicit val ct: ClassTag[B] = subType

    val p2: ThisElem => Boolean = {
      case elem: B if p(elem) => true
      case _                  => false
    }

    f(p2) collect {
      case elem: B => elem
    }
  }

  private def find[B <: ThisElem](
    subType: ClassTag[B])(p: B => Boolean)(f: (ThisElem => Boolean) => Option[ThisElem]): Option[B] = {

    // Implicit ClassTag[B] to make pattern matching below work (the implicit ClassTag "undoes" type erasure)
    implicit val ct: ClassTag[B] = subType

    val p2: ThisElem => Boolean = {
      case elem: B if p(elem) => true
      case _                  => false
    }

    f(p2) collect {
      case elem: B => elem
    }
  }
}

object SubtypeAwareElemLike {

  /**
   * This query API type, restricting ThisElem to the type parameter.
   *
   * @tparam E The element self type
   */
  type Aux[E] = SubtypeAwareElemLike { type ThisElem = E }
}
