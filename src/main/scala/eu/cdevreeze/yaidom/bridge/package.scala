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
 * The classes in this package offer support for generalizing over multiple element implementations in a non-intrusive
 * way (except for some trivial boilerplate). For example, to support an XML dialect with elements that offer the
 * `SubtypeAwareElemApi` trait, but that must be backed by multiple possible underlying element implementations, the
 * classes in this package may come in handy.
 *
 * Note that at the cost of indirection (composition), these bridge classes avoid "type gymnastics" and the cake
 * pattern.
 *
 * @author Chris de Vreeze
 */
package object bridge
