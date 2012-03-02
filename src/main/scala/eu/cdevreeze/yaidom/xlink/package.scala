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

/**
 * XLink-aware wrappers around [[eu.cdevreeze.yaidom.Node]].
 *
 * The `Node`s in this layer wrap the yaidom `Node` (without the children, if applicable). Like the wrapped `Node`s, they are immutable.
 * Each `Elem` in this layer that is an XLink is indeed of type [[eu.cdevreeze.yaidom.xlink.XLink]] (typically of a subtype).
 *
 * The `Node` companion object in this package has a factory method to quickly transform a yaidom `Node` into an
 * XLink-aware `Node`, which typically contains `XLink` instances.
 *
 * In Java speak, this package depends on the [[eu.cdevreeze.yaidom]] package, and not the other way around.
 * In Scala speak, this package is in the [[eu.cdevreeze.yaidom]] namespace.
 *
 * @author Chris de Vreeze
 */
package object xlink
