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
 * Support for parsing XML into yaidom `Document`s and `Elem`s. This package offers the [[eu.cdevreeze.yaidom.parse.DocumentParser]]
 * trait, as well as several implementations. Those implementations use JAXP (SAX, DOM or StAX), and the `jinterop` package
 * to convert JAXP artifacts to yaidom `Document`s.
 *
 * This package depends on the [[eu.cdevreeze.yaidom]] and [[eu.cdevreeze.yaidom.jinterop]] packages, and not the other way around.
 *
 * @author Chris de Vreeze
 */
package object parse
