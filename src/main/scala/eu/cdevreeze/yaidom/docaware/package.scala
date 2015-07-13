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

package eu.cdevreeze.yaidom

import eu.cdevreeze.yaidom.indexed.IndexedScopedElem

/**
 * This package used to contain element implementations like "indexed" elements, but with a mandatory URI as well.
 * Now there are only "indexed" elements, so this package can be phased out.
 *
 * @author Chris de Vreeze
 */
package object docaware {

  @deprecated(message = "Use 'indexed.Elem' instead", since = "1.4.0")
  type Elem = IndexedScopedElem[simple.Elem]

  @deprecated(message = "Use 'indexed.Elem' instead", since = "1.4.0")
  val Elem = indexed.Elem

  @deprecated(message = "Use 'indexed.Document' instead", since = "1.4.0")
  type Document = indexed.Document

  @deprecated(message = "Use 'indexed.Document' instead", since = "1.4.0")
  val Document = indexed.Document
}
