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

package eu.cdevreeze.yaidom.queryapi

import scala.collection.immutable

import eu.cdevreeze.yaidom.core.Path

/**
 * Shorthand for `ElemApi[E] with IsNavigableApi[E] with HasENameApi with HasTextApi`. In other words, the minimal element query API
 * corresponding to James Clark's "labelled element tree" abstraction, which is implemented as yaidom "resolved"
 * elements.
 *
 * If a yaidom element implementation (whether in yaidom itself or a "yaidom extension")
 * does not mix in the `ClarkElemApi` trait, it is probably not to be considered "XML".
 * Indeed, in yaidom only the `ElemBuilder` class does not mix in this trait, and indeed
 * it is not "XML" (lacking any knowledge about expanded names etc.), only a builder of "XML".
 * Hence this trait is very important in yaidom, as the "minimal XML element query API".
 *
 * '''Generic code abstracting over yaidom element implementations should either use
 * this trait, or sub-trait `ScopedElemApi`, depending on the abstraction level.'''
 *
 * @tparam E The captured element subtype
 *
 * @author Chris de Vreeze
 */
trait ClarkElemApi[E <: ClarkElemApi[E]] extends ElemApi[E] with IsNavigableApi[E] with HasENameApi with HasTextApi { self: E =>
}
