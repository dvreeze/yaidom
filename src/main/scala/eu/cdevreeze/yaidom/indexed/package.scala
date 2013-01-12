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
 * This package contains element representations that contain the "context" of the element. That is, the elements
 * in this package are pairs of a root element and an element path (to the actual element itself).
 *
 * An example of where such a representation can be useful is XML Schema. After all, to interpret an element definition
 * in an XML schema, we need context of the element definition to determine the target namespace, or to determine whether the
 * element definition is top level, etc.
 *
 * Do not do this:
 * {{{
 * import eu.cdevreeze.yaidom.indexed._
 *
 * }}}
 * Better is the following:
 * {{{
 * import eu.cdevreeze.yaidom.indexed
 *
 * val indexedRootElm = indexed.Elem(rootElm)
 * }}}
 *
 * @author Chris de Vreeze
 */
package object indexed
