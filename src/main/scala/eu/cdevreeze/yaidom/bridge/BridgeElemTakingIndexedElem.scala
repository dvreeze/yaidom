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
import eu.cdevreeze.yaidom.indexed
import eu.cdevreeze.yaidom.simple

/**
 * Overridable bridge element taking an `indexed.Elem`.
 *
 * @author Chris de Vreeze
 */
class BridgeElemTakingIndexedElem(val backingElem: indexed.Elem) extends IndexedBridgeElem {

  final type BackingElem = indexed.Elem

  final type SelfType = BridgeElemTakingIndexedElem

  final type UnwrappedBackingElem = simple.Elem

  final def findAllChildElems: immutable.IndexedSeq[SelfType] =
    backingElem.findAllChildElems.map(e => new BridgeElemTakingIndexedElem(e))

  final def resolvedName: EName = backingElem.resolvedName

  final def resolvedAttributes: immutable.Iterable[(EName, String)] = backingElem.resolvedAttributes

  final def qname: QName = backingElem.qname

  final def attributes: immutable.Iterable[(QName, String)] = backingElem.attributes

  final def scope: Scope = backingElem.scope

  final def text: String = backingElem.text

  final def findChildElemByPathEntry(entry: Path.Entry): Option[SelfType] =
    backingElem.findChildElemByPathEntry(entry).map(e => new BridgeElemTakingIndexedElem(e))

  final def toElem: simple.Elem = backingElem.elem

  final def rootElem: UnwrappedBackingElem = backingElem.rootElem

  final def path: Path = backingElem.path

  final def unwrappedBackingElem: UnwrappedBackingElem = backingElem.elem

  final override def equals(other: Any): Boolean = other match {
    case e: BridgeElemTakingIndexedElem => backingElem == e.backingElem
    case _ => false
  }

  final override def hashCode: Int = backingElem.hashCode
}
