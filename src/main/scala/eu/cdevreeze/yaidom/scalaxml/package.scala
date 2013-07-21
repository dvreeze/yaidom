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
 * Wrapper around class `scala.xml.Elem`, adapting it to the [[eu.cdevreeze.yaidom.ElemLike]] API.
 *
 * This wrapper is useful for scenarios where Scala XML literals with parameters are used. Typical parameters are
 * Scala XML node sequences, and they can be built in a "yaidom way", using these wrappers.
 *
 * For some namespace-related pitfalls, see [[eu.cdevreeze.yaidom.scalaxml.ScalaXmlElem]].
 *
 * @author Chris de Vreeze
 */
package object scalaxml
