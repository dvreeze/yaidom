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

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.indexed

/**
 * Strategy trait for finding TextENameExtractors in a document. Hence implementations of this trait
 * can retrieve all ENames and therefore all namespaces used in a document.
 *
 * @author Chris de Vreeze
 */
trait DocumentENameExtractor {

  /**
   * Finds the optional `TextENameExtractor` for the element text content in the given element.
   * The root element of the given indexed element must be the root element of the document.
   *
   * If there is a corresponding XML Schema, it would possibly specify that this element text is of type xs:QName.
   */
  def findElemTextENameExtractor(elem: indexed.Elem): Option[TextENameExtractor]

  /**
   * Finds the optional `TextENameExtractor` for the attribute with the given name in the given element.
   * The root element of the given indexed element must be the root element of the document.
   *
   * If there is a corresponding XML Schema, it would possibly specify that this attribute value is of type xs:QName.
   */
  def findAttributeValueENameExtractor(elem: indexed.Elem, attributeEName: EName): Option[TextENameExtractor]
}

object DocumentENameExtractor {

  val NoOp = new DocumentENameExtractor {

    def findElemTextENameExtractor(elem: indexed.Elem): Option[TextENameExtractor] = None

    def findAttributeValueENameExtractor(elem: indexed.Elem, attributeEName: EName): Option[TextENameExtractor] = None
  }
}
