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
 * Namespace as provider of ENames in that namespace. Its purpose is to avoid an explosion of EName instances that are
 * equal (whether or not that is costly in the JVM configuration used). So, for example, instead of:
 * {{{
 * val elemDecls = doc.documentElement filterElems { e => e.resolvedName == EName(XsNs, "element") }
 * }}}
 * consider writing:
 * {{{
 * // Instantiate namespace once, with a well filled cache
 * val xsNs: Namespace = buildXsNamespace()
 *
 * val elemDecls = doc.documentElement filterElems { e => e.resolvedName == xsNs.ename("element") }
 * }}}
 *
 * @author Chris de Vreeze
 */
final class Namespace private (val namespaceUri: String, val enameCache: Map[String, EName]) extends Immutable {
  require(namespaceUri ne null)
  require(enameCache ne null)
  require(
    enameCache.values forall (ename => ename.namespaceUriOption == Some(namespaceUri)),
    "All enames must have namespace URI %s".format(namespaceUri))
  require(
    enameCache forall { case (localName, ename) => ename.localPart == localName },
    "All enames must match the local part used as map key")

  /**
   * Returns the EName with the given local name in this namespace, preferably returning a cached instance.
   */
  def ename(localPart: String): EName =
    enameCache.getOrElse(localPart, EName(Some(namespaceUri), localPart))

  /**
   * Returns the EName with the given local name in this namespace from the cache, if any, wrapped in an Option.
   */
  def cachedENameOption(localPart: String): Option[EName] =
    enameCache.get(localPart)

  /**
   * Returns a new Namespace which is the same as this Namespace, but with the given EName (given as local part) added, if needed.
   */
  def plusEName(localPart: String): Namespace =
    new Namespace(namespaceUri, enameCache + (localPart -> ename(localPart)))
}

object Namespace {

  /**
   * Constructs a Namespace from the given namespaceUri and local names.
   */
  def apply(namespaceUri: String, localNames: Set[String]): Namespace = {
    val nsUriOption = Some(namespaceUri)
    val enamesByLocalParts = localNames.map(localName => (localName -> EName(nsUriOption, localName))).toMap
    new Namespace(namespaceUri, enamesByLocalParts)
  }
}
