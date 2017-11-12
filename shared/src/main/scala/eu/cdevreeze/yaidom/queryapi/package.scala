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
 * This package contains the (renewed) query API traits. It contains both the purely abstract API traits as well as the
 * partial implementation traits.
 *
 * '''Generic code abstracting over yaidom element implementations should either use
 * trait `ClarkElemApi` or sub-trait `ScopedElemApi`, or even `BackingElemApi`, depending on the abstraction level.'''
 *
 * Most API traits are orthogonal, but some API traits are useful combinations of other ones. Examples include
 * the above-mentioned `ClarkElemApi` and `ScopedElemApi` traits.
 *
 * <em>Simplicity</em> and <em>consistency</em> of the entire query API are 2 important design considerations. For example, the query
 * API methods themselves use no parameterized types. Note how the resulting API with type members is essentially the same as the
 * "old" yaidom query API using type parameters, except that the purely abstract traits are less constrained in the type members.
 *
 * This package depends only on the core package in yaidom, but many other packages do depend on this one.
 *
 * Note: whereas the old query API used F-bounded polymorphism with type parameters extensively, this new query API
 * essentially just uses type member ThisElem, defined in a common super-trait. The old query API may be somewhat easier to develop
 * (that is, convincing the compiler), but the new query API is easier to use as generic "backend" element query API. As an example,
 * common "bridge" element query APIs come to mind, used within type-safe XML dialect DOM tree implementations. The reason
 * this is easier with the new API is intuitively that fewer type constraints leak to the query API client code.
 *
 * @author Chris de Vreeze
 */
package object queryapi
