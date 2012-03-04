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
 * XLink-aware wrappers around [[eu.cdevreeze.yaidom.Elem]].
 *
 * The Elems in this layer wrap the yaidom Elems (other nodes are not wrapped, but that is ok, since the wrapped
 * elems are always accessible). Like the wrapped Elems, they are immutable. Each Elem in this layer that is an XLink
 * is indeed of type [[eu.cdevreeze.yaidom.xlink.XLink]] (typically of a subtype).
 *
 * The Elem companion object in this package has a factory method to quickly transform a yaidom Elem into an
 * XLink-aware Elem, which typically contains XLink instances.
 *
 * Do not use `xlink.Elem`s "globally" at a large scale, but rather use `xlink.XLink`s "locally" where they make sense.
 * See [[eu.cdevreeze.yaidom.xlink.Elem]].
 *
 * In Java speak, this package depends on the [[eu.cdevreeze.yaidom]] package, and not the other way around.
 * In Scala speak, this package is in the [[eu.cdevreeze.yaidom]] namespace.
 *
 * @author Chris de Vreeze
 */
package object xlink
