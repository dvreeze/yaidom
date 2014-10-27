/*
 * Copyright 2011-2014 Chris de Vreeze
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

package eu.cdevreeze.yaidom.utils

import scala.collection.immutable

import XmlSchemas.SchemaRoot
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.QNameProvider
import eu.cdevreeze.yaidom.simple.Elem
import eu.cdevreeze.yaidom.indexed

/**
 * Utility for creating QNameProviders by parsing XML schema files.
 *
 * @author Chris de Vreeze
 */
object QNameProviderUtils {

  /**
   * Expensive method creating an QNameProvider.QNameProviderUsingImmutableCache from parsed schema roots. Each such schema root
   * is queried for element declarations, attribute declarations, etc. These declarations have "target ENames", which are
   * collected, converted to QNames, and passed to the created QNameProvider.QNameProviderUsingImmutableCache.
   *
   * It is possible to use the returned QNameProvider only for its set of QNames, which can be saved to file, and used later
   * to directly create an QNameProvider.QNameProviderUsingImmutableCache from it.
   */
  def newQNameProviderUsingSchemas(schemaElems: immutable.IndexedSeq[Elem]): QNameProvider.QNameProviderUsingImmutableCache = {
    import XmlSchemas._

    val schemaRoots = schemaElems.map(e => SchemaRoot(indexed.Elem(e)))

    require(
      schemaRoots forall (e => e.targetNamespacePrefixOption.isDefined),
      "All schemas must have a @targetNamespace and a prefix corresponding to that target namespace")

    val globalElemDeclQNames = {
      val result = schemaRoots flatMap { elem =>
        elem.findAllGlobalElementDeclarations map { e =>
          val ename = e.targetEName
          if (ename.namespaceUriOption.isEmpty) QName(ename.localPart)
          else QName(elem.targetNamespacePrefixOption.get, ename.localPart)
        }
      }
      result.toSet
    }

    val globalAttrDeclQNames = {
      val result = schemaRoots flatMap { elem =>
        elem.findAllGlobalAttributeDeclarations map { e =>
          val ename = e.targetEName
          if (ename.namespaceUriOption.isEmpty) QName(ename.localPart)
          else QName(elem.targetNamespacePrefixOption.get, ename.localPart)
        }
      }
      result.toSet
    }

    val localElemDeclQNames = {
      val result = schemaRoots flatMap { elem =>
        elem.findAllLocalElementDeclarations map { e =>
          val ename = e.targetEName
          if (ename.namespaceUriOption.isEmpty) QName(ename.localPart)
          else QName(elem.targetNamespacePrefixOption.get, ename.localPart)
        }
      }
      result.toSet
    }

    val localAttrDeclQNames = {
      val result = schemaRoots flatMap { elem =>
        elem.findAllLocalAttributeDeclarations map { e =>
          val ename = e.targetEName
          if (ename.namespaceUriOption.isEmpty) QName(ename.localPart)
          else QName(elem.targetNamespacePrefixOption.get, ename.localPart)
        }
      }
      result.toSet
    }

    val qnames =
      globalElemDeclQNames union globalAttrDeclQNames union localElemDeclQNames union localAttrDeclQNames

    val qnameProvider = new QNameProvider.QNameProviderUsingImmutableCache(qnames)
    qnameProvider
  }
}
