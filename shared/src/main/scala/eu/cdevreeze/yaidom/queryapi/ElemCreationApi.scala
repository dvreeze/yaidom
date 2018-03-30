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

import eu.cdevreeze.yaidom.core.EName

/**
 * This is the generic element creation API. It fits in the overall philosophy of yaidom in that it is
 * based on ENames, not on QNames.
 *
 * @author Chris de Vreeze
 */
trait ElemCreationApi {

  type Node

  type Elem <: Node

  def elem(ename: EName, children: immutable.IndexedSeq[Node]): Elem

  def elem(
    ename:      EName,
    attributes: immutable.IndexedSeq[(EName, String)],
    children:   immutable.IndexedSeq[Node]): Elem

  def textElem(ename: EName, txt: String): Elem

  def textElem(
    ename:      EName,
    attributes: immutable.IndexedSeq[(EName, String)],
    txt:        String): Elem

  def emptyElem(ename: EName): Elem

  def emptyElem(
    ename:      EName,
    attributes: immutable.IndexedSeq[(EName, String)]): Elem
}

object ElemCreationApi {

  /**
   * This element creation API type, restricting Node and Elem to the passed type parameters.
   *
   * @tparam N The node type
   * @tparam E The element type
   */
  type Aux[N, E] = ElemCreationApi { type Node = N; type Elem = E }
}
