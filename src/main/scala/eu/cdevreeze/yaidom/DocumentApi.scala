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

import java.net.URI

/**
 * Minimal API for Documents, having a type parameter for the element type.
 *
 * This is a purely abstract API trait.
 *
 * @tparam E The element type
 *
 * @author Chris de Vreeze
 */
trait DocumentApi[E <: ParentElemApi[E]] {

  /** Returns the document element */
  def documentElement: E

  /** Returns the optional document URI, wrapped in an Option */
  def uriOption: Option[URI]
}
