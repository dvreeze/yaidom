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

package eu.cdevreeze.yaidom.java8.indexedelem

import java.net.URI
import java.util.Optional

import scala.compat.java8.OptionConverters.RichOptionForJava8

import eu.cdevreeze.yaidom.indexed
import eu.cdevreeze.yaidom.java8.queryapi.StreamingDocumentApi

/**
 * Wrapper around native yaidom indexed document.
 *
 * @author Chris de Vreeze
 */
final class IndexedDocument(val underlyingDocument: indexed.Document) extends StreamingDocumentApi[IndexedElem] {

  def documentElement: IndexedElem = {
    new IndexedElem(underlyingDocument.documentElement)
  }

  def uriOption: Optional[URI] = {
    underlyingDocument.uriOption.asJava
  }
}