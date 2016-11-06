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

package eu.cdevreeze.yaidom.queryapi

/**
 * Super-trait for all document types, promising an element type for the document element.
 *
 * @author Chris de Vreeze
 */
trait AnyDocumentApi {

  /**
   * The document type itself. It must be restricted to a sub-type of the document API trait in question.
   *
   * Concrete document classes will restrict this type to that document class itself.
   */
  type ThisDoc <: AnyDocumentApi

  /**
   * The type of the document element.
   */
  type DocElemType <: AnyElemApi
}
