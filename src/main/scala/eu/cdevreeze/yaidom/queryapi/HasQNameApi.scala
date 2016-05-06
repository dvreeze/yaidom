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

import eu.cdevreeze.yaidom.core.QName

/**
 * Trait defining the contract for elements that have a QName, as well as attributes with QName keys.
 *
 * Using this trait (possibly in combination with other "element traits") we can abstract over several element implementations.
 *
 * @author Chris de Vreeze
 */
trait HasQNameApi {

  /**
   * The QName of the element
   */
  def qname: QName

  /**
   * The attributes of the element as mapping from QNames to values
   */
  def attributes: immutable.Iterable[(QName, String)]
}

object HasQNameApi {

  /**
   * The `HasQNameApi` as type class trait. Each of the functions takes "this" element as first parameter.
   * Custom element implementations such as W3C DOM or Saxon NodeInfo can thus get this API without any wrapper object costs.
   */
  trait FunctionApi[E] {

    def qname(thisElem: E): QName

    def attributes(thisElem: E): immutable.Iterable[(QName, String)]
  }
}
