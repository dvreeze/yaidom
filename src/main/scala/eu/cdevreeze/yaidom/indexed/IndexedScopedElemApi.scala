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

import eu.cdevreeze.yaidom.core.Declarations
import eu.cdevreeze.yaidom.queryapi.ScopedElemApi

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

  type Aux[A, B] = IndexedScopedElemApi {
    type ThisElem = A
    type UnderlyingElem = B
  }

  /**
   * API of builders of `IndexedScopedElemApi` objects. These builders keep a URI resolver for XML Base support.
   * Builder instances should be thread-safe global objects, encapsulating one chosen URI resolver.
   */
  trait Builder[E <: IndexedScopedElemApi.Aux[E, U], U <: ScopedElemApi.Aux[U]] extends IndexedClarkElemApi.Builder[E, U] {
  }
}
