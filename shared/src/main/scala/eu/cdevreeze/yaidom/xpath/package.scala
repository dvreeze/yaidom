/*
 * Copyright 2011-2017 Chris de Vreeze
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
 * XPath evaluation abstraction. It is for yaidom what the standard Java XPath API is for Java.
 * Like the standard Java XPath API, this yaidom XPath API is abstract in the sense that it allows for
 * many different implementations. This yaidom XPath API has even an implementation targeting Scala-JS,
 * so it has no dependencies on JAXP.
 *
 * This API should be useful for any XPath version, even as old as 1.0. Indeed, there is no XDM (XPath Data Model)
 * abstraction in this API.
 *
 * Preferably implementations use (the same) yaidom element types for context items and returned nodes, or use
 * types for them that have yaidom wrappers. This would make it easy to mix XPath evaluations with
 * yaidom queries, at reasonably low runtime costs.
 *
 * Currently there are no classes in this XPath API for functions, function resolvers and variable
 * resolvers.
 *
 * The remainder of yaidom has no dependency on this `xpath` package and its sub-packages.
 *
 * Implementing XPath support is error-prone. This is yet another reason why it is important that the remainder
 * of yaidom does not depend on its XPath support!
 *
 * @author Chris de Vreeze
 */
package object xpath
