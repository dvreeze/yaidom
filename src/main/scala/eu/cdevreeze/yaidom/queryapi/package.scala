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
 * This package contains the query API traits. It contains both the purely abstract API traits as well as the
 * partial implementation traits.
 *
 * This package depends only on the core package in yaidom, but many other packages do depend on this one.
 *
 * ==Notes on migration from yaidom 1.0 to 1.1 and later==
 *
 * When migrating from yaidom 1.0 to 1.1 or later, there are many breaking changes in the query API traits to deal
 * with. Not only was the query API moved to its own package, but also was the inheritance hierarchy (of 1.0)
 * abandoned, and were some traits renamed. The most important renaming was from `ParentElemApi` to `ElemApi`,
 * where the 1.0 `ElemApi` became `ElemApi with HasENameApi` (where the `HasENameApi` companion object contains
 * an implicit class converting ENames to element predicates).
 *
 * Also, trait `PathAwareElemApi` has been removed. Typically, "indexed elements" are more powerful.
 *
 * The query API traits in 1.1 and later are more orthogonal (after all, the inheritance hierarchy is no longer there).
 * We can even simulate the 1.0 query API traits by combining 1.1 traits, keeping renaming in mind:
 * <ul>
 * <li>`ParentElemApi` (1.0) ==> `ElemApi` (1.1)</li>
 * <li>`ElemApi` (1.0) ==> `ElemApi with HasENameApi` (1.1)</li>
 * <li>`NavigableElemApi` (1.0) ==> `ElemApi with HasENameApi with IsNavigableApi` (1.1)</li>
 * <li>`UpdatableElemApi` minus `PathAwareElemApi` (1.0) ==> `ElemApi with HasENameApi with UpdatableElemApi` (1.1)</li>
 * <li>`TransformableElemApi` (1.0) ==> `TransformableElemApi` (1.1)</li>
 * <li>`SubtypeAwareParentElemApi` (1.0) ==> `SubtypeAwareElemApi` (1.1)</li>
 * </ul>
 *
 * The fact that the 1.0 query API traits can be simulated by (combinations of) 1.1 query API traits shows how
 * orthogonality of the query API has improved in version 1.1. For example, now we can abstract over `indexed` and `docaware`
 * elements by using the following type:
 * {{{
 * ElemApi[E] with HasPathApi with IsNavigableApi[E] with HasENameApi with HasTextApi
 * }}}
 *
 * This is a very powerful type, supporting queries using predicates in which the elements themselves contain Paths
 * (that can be navigated to).
 *
 * @author Chris de Vreeze
 */
package object queryapi
