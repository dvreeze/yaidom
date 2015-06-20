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

import eu.cdevreeze.yaidom.core.Path

/**
 * This trait offers Path-based navigation support.
 *
 * '''This trait typically does not show up in application code using yaidom, yet its (uniform) API does. Hence, it makes sense
 * to read the documentation of this trait, knowing that the API is offered by multiple element implementations.'''
 *
 * This trait is purely <em>abstract</em>. The most common implementation of this trait is [[eu.cdevreeze.yaidom.queryapi.IsNavigable]].
 *
 * ==IsNavigableApi more formally==
 *
 * Some properties are expected to hold for "navigable elements":
 * {{{
 * getElemOrSelfByPath(Path.Root) == self
 *
 * findElemOrSelfByPath(path1).flatMap(e => e.findElemOrSelfByPath(path2)) == findElemOrSelfByPath(path1.append(path2))
 * }}}
 *
 * @tparam E The captured element subtype
 *
 * @author Chris de Vreeze
 */
trait IsNavigableApi[E <: IsNavigableApi[E]] { self: E =>

  /**
   * Finds the child element with the given `Path.Entry` (where this element is the root), if any, wrapped in an `Option`.
   */
  def findChildElemByPathEntry(entry: Path.Entry): Option[E]

  /** Returns (the equivalent of) `findChildElemByPathEntry(entry).get` */
  def getChildElemByPathEntry(entry: Path.Entry): E

  /**
   * Finds the element with the given `Path` (where this element is the root), if any, wrapped in an `Option`.
   *
   * Note that for each non-empty Path, we have:
   * {{{
   * findElemOrSelfByPath(path) == findChildElemByPathEntry(path.firstEntry) flatMap (e => e.findElemOrSelfByPath(path.withoutFirstEntry))
   * }}}
   */
  def findElemOrSelfByPath(path: Path): Option[E]

  /** Returns (the equivalent of) `findElemOrSelfByPath(path).get` */
  def getElemOrSelfByPath(path: Path): E
}
