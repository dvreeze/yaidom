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
package utils

import scala.collection.immutable

/**
 * Utility for creating ENameProviders by parsing XML schema files.
 *
 * @author Chris de Vreeze
 */
object ENameProviderUtils {

  /**
   * Expensive method creating an ENameProvider.ENameProviderUsingImmutableCache from parsed schema roots. Each such schema root
   * is queried for element declarations, attribute declarations, etc. These declarations have "target ENames", which are
   * collected and passed to the created ENameProvider.ENameProviderUsingImmutableCache.
   *
   * It is possible to use the returned ENameProvider only for its set of ENames, which can be saved to file, and used later
   * to directly create an ENameProvider.ENameProviderUsingImmutableCache from it.
   */
  def newENameProviderUsingSchemas(schemaElems: immutable.IndexedSeq[Elem]): ENameProvider.ENameProviderUsingImmutableCache = {
    import XmlSchemas._

    val schemaRoots = schemaElems.map(e => new SchemaRoot(indexed.Elem(e)))

    val globalElemDeclENames = schemaRoots.flatMap(e => e.findAllGlobalElementDeclarations.map(_.targetEName)).toSet

    val globalAttrDeclENames = schemaRoots.flatMap(e => e.findAllGlobalAttributeDeclarations.map(_.targetEName)).toSet

    val localElemDeclENames = schemaRoots.flatMap(e => e.findAllLocalElementDeclarations.map(_.targetEName)).toSet

    val localAttrDeclENames = schemaRoots.flatMap(e => e.findAllLocalAttributeDeclarations.map(_.targetEName)).toSet

    val enames =
      globalElemDeclENames union globalAttrDeclENames union localElemDeclENames union localAttrDeclENames

    val enameProvider = new ENameProvider.ENameProviderUsingImmutableCache(enames)
    enameProvider
  }
}
