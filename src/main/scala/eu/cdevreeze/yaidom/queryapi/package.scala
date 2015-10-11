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
 * '''Generic code abstracting over yaidom element implementations should either use
 * trait `ClarkElemApi` or sub-trait `ScopedElemApi`, depending on the abstraction level.'''
 *
 * Most API traits are orthogonal, but some API traits are useful combinations of other ones. Examples include
 * the above-mentioned `ClarkElemApi` and `ScopedElemApi` traits.
 *
 * This package depends only on the core package in yaidom, but many other packages do depend on this one.
 *
 * @author Chris de Vreeze
 */
package object queryapi
