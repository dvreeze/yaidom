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
 * This package contains element representations that can be compared for (some notion of) equality.
 *
 * The most important difference with normal `Elem`s is that qualified names do not occur,
 * but only expanded (element and attribute) names. Moreover, the only nodes in this package are
 * element and text nodes.
 *
 * Do not do this:
 * {{{
 * import eu.cdevreeze.yaidom.resolved._
 *
 * }}}
 * Better is the following:
 * {{{
 * import eu.cdevreeze.yaidom.resolved
 *
 * val resolvedRootElm = resolved.Elem(rootElm)
 * }}}
 *
 * @author Chris de Vreeze
 */
package object resolved
