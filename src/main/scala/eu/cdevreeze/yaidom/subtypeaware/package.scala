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
 * Query traits that resemble ParentElemApi and ParentElemLike, but offering query support for object hierarchies.
 *
 * For example, XML Schema can be modeled with an object hierarchy, starting with some XsdElem super-type which
 * mixes in trait SubtypeAwareParentElemApi, among other query traits. The object hierarchy could contain sub-classes
 * of XsdElem such as XsdRootElem, GlobalElementDeclaration, etc. Then the SubtypeAwareParentElemApi trait makes it
 * easy to query for all or some global element declarations, etc.
 *
 * There is no magic in these traits: it is just ParentElemApi and ParentElemLike underneath. It is only the syntactic
 * convenience that makes the difference.
 *
 * @author Chris de Vreeze
 */
package object subtypeaware
