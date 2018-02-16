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

import scala.collection.immutable

import eu.cdevreeze.yaidom.core.XmlDeclaration

/**
 * Backing document API, representing a document that contains a `BackingElemApi` root element.
 *
 * @author Chris de Vreeze
 */
trait BackingDocumentApi extends DocumentApi {

  type DocElemType <: BackingElemApi

  /**
   * Returns the child nodes of the document. Precisely one of them must be the document element.
   */
  def children: immutable.IndexedSeq[Nodes.CanBeDocumentChild]

  /**
   * Returns the comment child nodes of the document.
   */
  def comments: immutable.IndexedSeq[Nodes.Comment]

  /**
   * Returns the processing instruction child nodes of the document.
   */
  def processingInstructions: immutable.IndexedSeq[Nodes.ProcessingInstruction]

  /**
   * Returns the optional XML declaration.
   */
  def xmlDeclarationOption: Option[XmlDeclaration]
}

object BackingDocumentApi {

  /**
   * This document API type, restricting the type members to the passed type parameters.
   *
   * @tparam D The document type itself
   * @tparam E The document element type
   */
  type Aux[D, E <: BackingElemApi] = BackingDocumentApi { type ThisDoc = D; type DocElemType = E }
}
