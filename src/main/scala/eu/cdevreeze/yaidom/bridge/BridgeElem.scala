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

package eu.cdevreeze.yaidom.bridge

import scala.collection.immutable

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope

/**
 * Bridge element that enables the `ScopedElemLike with IsNavigableLike` API on the classes delegating to this bridge element.
 *
 * @author Chris de Vreeze
 */
abstract class BridgeElem {

  /**
   * The backing element type, for example `docaware.Elem`
   */
  type BackingElem

  /**
   * The type of this bridge element itself
   */
  type SelfType <: BridgeElem

  def backingElem: BackingElem

  // Needed for the ScopedElemLike API

  def findAllChildElems: immutable.IndexedSeq[SelfType]

  def resolvedName: EName

  def resolvedAttributes: immutable.Iterable[(EName, String)]

  def qname: QName

  def attributes: immutable.Iterable[(QName, String)]

  def scope: Scope

  def text: String

  // Needed for the IsNavigable API

  def findChildElemByPathEntry(entry: Path.Entry): Option[SelfType]

  // Extra method

  def toElem: eu.cdevreeze.yaidom.simple.Elem
}
