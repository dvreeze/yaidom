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

import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.queryapi.ElemApi
import eu.cdevreeze.yaidom.queryapi.HasENameApi

/**
 * Bridge element that adds support for "indexed elements".
 *
 * @author Chris de Vreeze
 */
abstract class IndexedBridgeElem extends BridgeElem {

  override type SelfType <: IndexedBridgeElem

  /**
   * The unwrapped backing element type, for example `simple.Elem`
   */
  type UnwrappedBackingElem <: ElemApi[UnwrappedBackingElem] with HasENameApi

  def rootElem: UnwrappedBackingElem

  def path: Path

  def unwrappedBackingElem: UnwrappedBackingElem
}
