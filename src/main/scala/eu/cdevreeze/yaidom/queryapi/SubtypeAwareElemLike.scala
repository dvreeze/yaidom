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
import scala.reflect.ClassTag

import ElemApi.anyElem

/**
 * Default implementation of SubtypeAwareElemApi.
 *
 * @author Chris de Vreeze
 */
trait SubtypeAwareElemLike[A <: SubtypeAwareElemLike[A]] extends ElemLike[A] with SubtypeAwareElemApi[A] { self: A =>

  // Implementation note: this is not DRY because it is pretty much the same code as in the corresponding potential type class.
  // Yet I did not want to depend on a val or def returning the appropriate type class instance, so chose for code repetition.

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
      case _                  => false
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
      case _                  => false
    }

    f(p2) collect {
      case elem: B => elem
    }
  }
}

object SubtypeAwareElemLike {

  /**
   * The `SubtypeAwareElemLike` as potential type class trait. Each of the functions takes "this" element as first parameter.
   * Custom element implementations such as W3C DOM or Saxon NodeInfo can thus get this API without any wrapper object costs.
   */
  trait FunctionApi[A] extends ElemLike.FunctionApi[A] with SubtypeAwareElemApi.FunctionApi[A] {

    final def findAllChildElemsOfType[B <: A](thisElem: A, subType: ClassTag[B]): immutable.IndexedSeq[B] = {
      filterChildElemsOfType(thisElem, subType)(_ => true)
    }

    final def filterChildElemsOfType[B <: A](thisElem: A, subType: ClassTag[B])(p: B => Boolean): immutable.IndexedSeq[B] = {
      filter(thisElem, subType)(p) { (e, pred) => filterChildElems(e, pred) }
    }

    final def findAllElemsOfType[B <: A](thisElem: A, subType: ClassTag[B]): immutable.IndexedSeq[B] = {
      filterElemsOfType(thisElem, subType)(_ => true)
    }

    final def filterElemsOfType[B <: A](thisElem: A, subType: ClassTag[B])(p: B => Boolean): immutable.IndexedSeq[B] = {
      filter(thisElem, subType)(p) { (e, pred) => filterElems(e, pred) }
    }

    final def findAllElemsOrSelfOfType[B <: A](thisElem: A, subType: ClassTag[B]): immutable.IndexedSeq[B] = {
      filterElemsOrSelfOfType(thisElem, subType)(_ => true)
    }

    final def filterElemsOrSelfOfType[B <: A](thisElem: A, subType: ClassTag[B])(p: B => Boolean): immutable.IndexedSeq[B] = {
      filter(thisElem, subType)(p) { (e, pred) => filterElemsOrSelf(e, pred) }
    }

    final def findChildElemOfType[B <: A](thisElem: A, subType: ClassTag[B])(p: B => Boolean): Option[B] = {
      find(thisElem, subType)(p) { (e, pred) => findChildElem(e, pred) }
    }

    final def findElemOfType[B <: A](thisElem: A, subType: ClassTag[B])(p: B => Boolean): Option[B] = {
      find(thisElem, subType)(p) { (e, pred) => findElem(e, pred) }
    }

    final def findElemOrSelfOfType[B <: A](thisElem: A, subType: ClassTag[B])(p: B => Boolean): Option[B] = {
      find(thisElem, subType)(p) { (e, pred) => findElemOrSelf(e, pred) }
    }

    final def findTopmostElemsOfType[B <: A](thisElem: A, subType: ClassTag[B])(p: B => Boolean): immutable.IndexedSeq[B] = {
      filter(thisElem, subType)(p) { (e, pred) => findTopmostElems(e, pred) }
    }

    final def findTopmostElemsOrSelfOfType[B <: A](thisElem: A, subType: ClassTag[B])(p: B => Boolean): immutable.IndexedSeq[B] = {
      filter(thisElem, subType)(p) { (e, pred) => findTopmostElemsOrSelf(e, pred) }
    }

    final def getChildElemOfType[B <: A](thisElem: A, subType: ClassTag[B])(p: B => Boolean): B = {
      val result = findChildElemOfType(thisElem, subType)(p)
      require(result.size == 1, s"Expected exactly 1 matching child element, but found ${result.size} of them")
      result.head
    }

    private final def filter[B <: A](
      thisElem: A, subType: ClassTag[B])(p: B => Boolean)(f: ((A, A => Boolean) => immutable.IndexedSeq[A])): immutable.IndexedSeq[B] = {

      // Implicit ClassTag[B] to make pattern matching below work (the implicit ClassTag "undoes" type erasure)
      implicit val ct = subType

      val p2: (A => Boolean) = {
        case elem: B if p(elem) => true
        case _                  => false
      }

      f(thisElem, p2) collect {
        case elem: B => elem
      }
    }

    private final def find[B <: A](
      thisElem: A, subType: ClassTag[B])(p: B => Boolean)(f: ((A, A => Boolean) => Option[A])): Option[B] = {

      // Implicit ClassTag[B] to make pattern matching below work (the implicit ClassTag "undoes" type erasure)
      implicit val ct = subType

      val p2: (A => Boolean) = {
        case elem: B if p(elem) => true
        case _                  => false
      }

      f(thisElem, p2) collect {
        case elem: B => elem
      }
    }
  }
}
