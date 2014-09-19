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
package subtypeaware

import scala.collection.immutable
import scala.reflect.ClassTag
import SubtypeAwareParentElemApi.anyElem

/**
 * Default implementation of SubtypeAwareParentElemApi.
 *
 * @author Chris de Vreeze
 */
trait SubtypeAwareParentElemLike[A <: SubtypeAwareParentElemLike[A]] extends ParentElemLike[A] with SubtypeAwareParentElemApi[A] { self: A =>

  final def findAllChildElemsOfType[B <: A](subType: ClassTag[B]): immutable.IndexedSeq[B] = {
    filterChildElemsOfType(subType)(anyElem)
  }

  final def filterChildElemsOfType[B <: A](subType: ClassTag[B])(p: B => Boolean): immutable.IndexedSeq[B] = {
    filter(subType)(p) { pred => filterChildElems(pred) }
  }

  final def findAllElemsOfType[B <: A](subType: ClassTag[B]): immutable.IndexedSeq[B] = {
    filterElemsOfType(subType)(anyElem)
  }

  final def filterElemsOfType[B <: A](subType: ClassTag[B])(p: B => Boolean): immutable.IndexedSeq[B] = {
    filter(subType)(p) { pred => filterElems(pred) }
  }

  final def findAllElemsOrSelfOfType[B <: A](subType: ClassTag[B]): immutable.IndexedSeq[B] = {
    filterElemsOrSelfOfType(subType)(anyElem)
  }

  final def filterElemsOrSelfOfType[B <: A](subType: ClassTag[B])(p: B => Boolean): immutable.IndexedSeq[B] = {
    filter(subType)(p) { pred => filterElemsOrSelf(pred) }
  }

  final def findChildElemOfType[B <: A](subType: ClassTag[B])(p: B => Boolean): Option[B] = {
    find(subType)(p) { pred => findChildElem(pred) }
  }

  final def findElemOfType[B <: A](subType: ClassTag[B])(p: B => Boolean): Option[B] = {
    find(subType)(p) { pred => findElem(pred) }
  }

  final def findElemOrSelfOfType[B <: A](subType: ClassTag[B])(p: B => Boolean): Option[B] = {
    find(subType)(p) { pred => findElemOrSelf(pred) }
  }

  final def findTopmostElemsOfType[B <: A](subType: ClassTag[B])(p: B => Boolean): immutable.IndexedSeq[B] = {
    filter(subType)(p) { pred => findTopmostElems(pred) }
  }

  final def findTopmostElemsOrSelfOfType[B <: A](subType: ClassTag[B])(p: B => Boolean): immutable.IndexedSeq[B] = {
    filter(subType)(p) { pred => findTopmostElemsOrSelf(pred) }
  }

  final def getChildElemOfType[B <: A](subType: ClassTag[B])(p: B => Boolean): B = {
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

  private final def filter[B <: A](
    subType: ClassTag[B])(p: B => Boolean)(f: ((A => Boolean) => immutable.IndexedSeq[A])): immutable.IndexedSeq[B] = {

    // Implicit ClassTag[B] to make pattern matching below work (the implicit ClassTag "undoes" type erasure)
    implicit val ct = subType

    val p2: (A => Boolean) = {
      case elem: B if p(elem) => true
      case _ => false
    }

    f(p2) collect {
      case elem: B => elem
    }
  }

  private final def find[B <: A](
    subType: ClassTag[B])(p: B => Boolean)(f: ((A => Boolean) => Option[A])): Option[B] = {

    // Implicit ClassTag[B] to make pattern matching below work (the implicit ClassTag "undoes" type erasure)
    implicit val ct = subType

    val p2: (A => Boolean) = {
      case elem: B if p(elem) => true
      case _ => false
    }

    f(p2) collect {
      case elem: B => elem
    }
  }
}
