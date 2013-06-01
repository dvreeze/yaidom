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
package convert

/**
 * Conversions between yaidom nodes and Scala XML nodes, currently only in one direction, from Scala XML to yaidom.
 *
 * These conversions are handy when one wants to use XML literals (as offered by standard Scala XML) in combination with yaidom.
 *
 * Example usage:
 * {{{
 * val scalaXmlElem = <a xmlns="http://a"><b><c>test</c></b></a>
 *
 * val elem = ScalaXmlConversions.convertToElem(scalaXmlElem)
 *
 * useImmutableElem(elem)
 * }}}
 *
 * @author Chris de Vreeze
 */
object ScalaXmlConversions extends ScalaXmlToYaidomConversions
