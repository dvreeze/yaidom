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

import eu.cdevreeze.yaidom.core.Declarations

/**
 * Abstract API for "indexed Scoped elements".
 *
 * @author Chris de Vreeze
 */
trait IndexedScopedElemApi extends IndexedClarkElemApi with ScopedElemApi {

  type ThisElemApi <: IndexedScopedElemApi

  /**
   * Returns the namespaces declared in this element.
   *
   * If the original parsed XML document contained duplicate namespace declarations (i.e. namespace declarations that are the same
   * as some namespace declarations in their context), these duplicate namespace declarations were lost during parsing of the
   * XML into an `Elem` tree. They therefore do not occur in the namespace declarations returned by this method.
   */
  def namespaces: Declarations
}

object IndexedScopedElemApi {

  /**
   * This query API type, fixing ThisElem and ThisElemApi to the type parameter.
   *
   * @tparam E The element self type
   */
  type Aux[E] = IndexedScopedElemApi { type ThisElem = E; type ThisElemApi = E }
}
