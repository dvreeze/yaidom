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
 * This package contains element representations that contain the "context" of the element, including the URI of the containing
 * document. In other words, this package contains element representations like `indexed.Elem`, except that the document URI
 * is stored as well in each element.
 *
 * Below follows a simple example query, using the uniform query API:
 * {{{
 * // Note the import of package docaware, and not of its members. That is indeed a best practice!
 * import eu.cdevreeze.yaidom.docaware
 *
 * val docawareBookstoreElem = docaware.Elem(bookstoreElem)
 *
 * val scalaBookAuthors =
 *   for {
 *     bookElem <- docawareBookstoreElem \ EName("{http://bookstore/book}Book")
 *     if (bookElem \@ EName("ISBN")) == Some("978-0981531649")
 *     authorElem <- bookElem \\ EName("{http://bookstore/author}Author")
 *   } yield authorElem
 * }}}
 * The query for Scala book authors would have been exactly the same if normal `Elem`s had been used instead of `docaware.Elem`s
 * (replacing `docawareBookstoreElem` by `bookstoreElem`)!
 *
 * @author Chris de Vreeze
 */
package object docaware {

  type Elem = IndexedScopedElem[simple.Elem]

  type Document = indexed.Document
}
