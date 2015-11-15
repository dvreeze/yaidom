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

/**
 * This package contains element representations that contain the "context" of the element. That is, the elements
 * in this package are pairs of a root element and a path (to the actual element itself). The "context" of an element
 * also contains an optional document URI.
 *
 * An example of where such a representation can be useful is XML Schema. After all, to interpret an element definition
 * in an XML schema, we need context of the element definition to determine the target namespace, or to determine whether the
 * element definition is top level, etc.
 *
 * Below follows a simple example query, using the uniform query API:
 * {{{
 * // Note the import of package indexed, and not of its members. That is indeed a best practice!
 * import eu.cdevreeze.yaidom.indexed
 *
 * val indexedBookstoreElem = indexed.Elem(bookstoreElem)
 *
 * val scalaBookAuthors =
 *   for {
 *     bookElem <- indexedBookstoreElem \ EName("{http://bookstore/book}Book")
 *     if (bookElem \@ EName("ISBN")) == Some("978-0981531649")
 *     authorElem <- bookElem \\ EName("{http://bookstore/author}Author")
 *   } yield authorElem
 * }}}
 * The query for Scala book authors would have been exactly the same if normal `Elem`s had been used instead of `indexed.Elem`s
 * (replacing `indexedBookstoreElem` by `bookstoreElem`)!
 *
 * There is no explicit functional update support for the indexed elements in this package. Of course the underlying
 * elements can be functionally updated (for element implementations that offer such update support), and indexed
 * elements can be created from the update results, but this is hardly efficient functional update support.
 *
 * One problem with efficient functional updates for indexed elements is that updating just one child element means
 * that all subsequent child elements may have to be updated as well, adapting the stored paths. In comparison, simple
 * elements do not have this restriction, and can be updated in isolation. Hence the functional update support for
 * simple elements but not for the different indexed element implementations.
 *
 * If efficient functional updates on indexed elements are required, consider using the "lazy" indexed elements
 * such as `LazyIndexedClarkElem` and `LazyIndexedScopedElem` instead of the "eager" indexed elements `IndexedClarkElem`
 * and `IndexedScopedElem`. After all, creation of the lazy indexed elements is fast.
 *
 * @author Chris de Vreeze
 */
package object indexed {

  type Elem = IndexedScopedElem[simple.Elem]
}
