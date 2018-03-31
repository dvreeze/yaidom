/*
 * Copyright 2011-2017 Chris de Vreeze
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
 * This package contains element representations that can be compared for (some notion of "value") equality, unlike normal yaidom nodes.
 * That notion of equality is simple to understand, but "naive". The user is of the API must take control over what is compared
 * for equality.
 *
 * See [[eu.cdevreeze.yaidom.resolved.Node]] for why this package is named `resolved`.
 *
 * The most important difference with normal `Elem`s is that qualified names do not occur,
 * but only expanded (element and attribute) names. This reminds of James Clark notation for XML trees and
 * expanded names, where qualified names are absent.
 *
 * Moreover, the only nodes in this package are element and text nodes.
 *
 * Below follows a simple example query, using the uniform query API:
 * {{{
 * // Note the import of package resolved, and not of its members. That is indeed a best practice!
 * import eu.cdevreeze.yaidom.resolved
 *
 * val resolvedBookstoreElem = resolved.Elem.from(bookstoreElem)
 *
 * val scalaBookAuthors =
 *   for {
 *     bookElem <- resolvedBookstoreElem \ EName("{http://bookstore/book}Book")
 *     if (bookElem \@ EName("ISBN")).contains("978-0981531649")
 *     authorElem <- bookElem \\ EName("{http://bookstore/author}Author")
 *   } yield authorElem
 * }}}
 * The query for Scala book authors would have been exactly the same if normal `Elem`s had been used instead of `resolved.Elem`s
 * (replacing `resolvedBookstoreElem` by `bookstoreElem`)!
 *
 * @author Chris de Vreeze
 */
package object resolved
