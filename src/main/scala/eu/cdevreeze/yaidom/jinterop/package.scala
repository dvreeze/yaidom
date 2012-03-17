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
 * Java interoperability support. This package contains conversions between yaidom objects and JAXP DOM or StAX objects,
 * in both directions.
 *
 * This Java interop support is typically not used directly by consumers of the yaidom API, although it is easy to do so when needed.
 * This package is used by the Document parsers and printers in the `parse` and `print` packages, respectively.
 *
 * This package depends on the [[eu.cdevreeze.yaidom]] package, and not the other way around.
 * The [[eu.cdevreeze.yaidom.parse]] and [[eu.cdevreeze.yaidom.print]] packages depend on this package.
 *
 * @author Chris de Vreeze
 */
package object jinterop
