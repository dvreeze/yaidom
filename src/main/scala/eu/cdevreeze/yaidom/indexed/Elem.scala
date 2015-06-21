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

package eu.cdevreeze.yaidom.indexed

import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.simple

/**
 * Factory object for `Elem` instances, where `Elem` is a type alias for `IndexedScopedElem[simple.Elem]`.
 *
 * @author Chris de Vreeze
 */
object Elem {

  /**
   * Calls `IndexedScopedElem[simple.Elem](rootElem)`
   */
  def apply(rootElem: simple.Elem): Elem = {
    IndexedScopedElem[simple.Elem](rootElem)
  }

  /**
   * Calls `IndexedScopedElem[simple.Elem](rootElem, path)`
   */
  def apply(rootElem: simple.Elem, path: Path): Elem = {
    IndexedScopedElem[simple.Elem](rootElem, path)
  }
}
