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
 * in this package mix in the `ContextAwareElemApi` trait. Unlike "indexed" elements, they do not wrap entire
 * element trees of an underlying element type, but contain a `ContextPath` as context only. That makes them
 * easier to construct in an ad-hoc manner, easier to use in streaming and update scenarios, and more light-weight than
 * "indexed" elements. Therefore "indexed" elements can be deprecated.
 *
 * An example of where such a representation can be useful is XML Schema. After all, to interpret an element definition
 * in an XML schema, we need context of the element definition to determine the target namespace, or to determine whether the
 * element definition is top level, etc.
 *
 * Below follows a simple example query, using the uniform query API:
 * {{{
 * // Note the import of package contextaware, and not of its members. That is indeed a best practice!
 * import eu.cdevreeze.yaidom.contextaware
 *
 * val contextawareBookstoreElem = contextaware.Elem(bookstoreElem)
 *
 * val scalaBookAuthors =
 *   for {
 *     bookElem <- contextawareBookstoreElem \ EName("{http://bookstore/book}Book")
 *     if (bookElem \@ EName("ISBN")) == Some("978-0981531649")
 *     authorElem <- bookElem \\ EName("{http://bookstore/author}Author")
 *   } yield authorElem
 * }}}
 * The query for Scala book authors would have been exactly the same if normal `Elem`s had been used instead of `contextaware.Elem`s
 * (replacing `contextawareBookstoreElem` by `bookstoreElem`)!
 *
 * @author Chris de Vreeze
 */
package object contextaware {

  type Elem = ContextAwareScopedElem[simple.Elem]
}
