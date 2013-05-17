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
 * "No-Namespace" as provider of ENames without any namespace. Its purpose is to avoid an explosion of EName instances that are
 * equal (whether or not that is costly in the JVM configuration used). So, for example, instead of:
 * {{{
 * val phoneElems = doc.documentElement filterElems { e => e.resolvedName == EName("phone") }
 * }}}
 * consider writing:
 * {{{
 * // Instantiate no-namespace once, with a well filled cache
 * val noNs: NoNamespace = buildNoNamespace()
 *
 * val elemDecls = doc.documentElement filterElems { e => e.resolvedName == noNs.ename("phone") }
 * }}}
 *
 * @author Chris de Vreeze
 */
final class NoNamespace private (val enameCache: Map[String, EName]) extends Immutable {
  require(enameCache ne null)
  require(
    enameCache.values forall (ename => ename.namespaceUriOption.isEmpty),
    "All enames must have no namespace URI")
  require(
    enameCache forall { case (localName, ename) => ename.localPart == localName },
    "All enames must match the local part used as map key")

  /**
   * Returns the EName with the given local name and no namespace, preferably returning a cached instance.
   */
  def ename(localPart: String): EName =
    enameCache.getOrElse(localPart, EName(None, localPart))

  /**
   * Returns the EName with the given local name and no namespace from the cache, if any, wrapped in an Option.
   */
  def cachedENameOption(localPart: String): Option[EName] =
    enameCache.get(localPart)

  /**
   * Returns a new NoNamespace which is the same as this NoNamespace, but with the given EName (given as local part) added, if needed.
   */
  def plusEName(localPart: String): NoNamespace =
    new NoNamespace(enameCache + (localPart -> ename(localPart)))
}

object NoNamespace {

  /**
   * Constructs a NoNamespace from the given local names.
   */
  def apply(localNames: Set[String]): NoNamespace = {
    val enamesByLocalParts = localNames.map(localName => (localName -> EName(localName))).toMap
    new NoNamespace(enamesByLocalParts)
  }
}
