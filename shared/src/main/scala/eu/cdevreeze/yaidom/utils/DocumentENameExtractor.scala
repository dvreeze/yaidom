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

package eu.cdevreeze.yaidom.utils

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.queryapi.BackingElemApi

/**
 * Strategy trait for finding TextENameExtractors in a document. Hence implementations of this trait
 * can retrieve all ENames and therefore all namespaces used in a document.
 *
 * @author Chris de Vreeze
 */
trait DocumentENameExtractor {

  /**
   * Finds the optional `TextENameExtractor` for the element text content in the given element.
   *
   * The root element of the given `BackingElemApi` element must be the root element of the document.
   *
   * If there is a corresponding XML Schema, and it specifies that this element text is of type xs:QName,
   * then this optional TextENameExtractor should be `SimpleTextENameExtractor`.
   */
  def findElemTextENameExtractor(elem: BackingElemApi): Option[TextENameExtractor]

  /**
   * Finds the optional `TextENameExtractor` for the attribute with the given name in the given element.
   *
   * The root element of the given `BackingElemApi` element must be the root element of the document.
   *
   * If there is a corresponding XML Schema, and it specifies that this attribute value is of type xs:QName,
   * then this optional TextENameExtractor should be `SimpleTextENameExtractor`.
   */
  def findAttributeValueENameExtractor(elem: BackingElemApi, attributeEName: EName): Option[TextENameExtractor]
}

object DocumentENameExtractor {

  /**
   * DocumentENameExtractor that never returns any TextENameExtractor for element text or attribute values.
   */
  val NoOp = new DocumentENameExtractor {

    def findElemTextENameExtractor(elem: BackingElemApi): Option[TextENameExtractor] = None

    def findAttributeValueENameExtractor(elem: BackingElemApi, attributeEName: EName): Option[TextENameExtractor] = None
  }
}
