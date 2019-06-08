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

package eu.cdevreeze.yaidom.java8.scalaxmlelem

import java.net.URI
import java.util.Optional

import scala.compat.java8.OptionConverters.RichOptionForJava8

import eu.cdevreeze.yaidom.scalaxml
import eu.cdevreeze.yaidom.java8.queryapi.StreamingDocumentApi

/**
 * Wrapper around a `scala.xml.Document`.
 *
 * @author Chris de Vreeze
 */
final class ScalaXmlDocument(val underlyingDocument: scala.xml.Document) extends StreamingDocumentApi[ScalaXmlElem] {

  def documentElement: ScalaXmlElem = {
    new ScalaXmlElem(scalaxml.ScalaXmlDocument(underlyingDocument).documentElement.wrappedNode)
  }

  def uriOption: Optional[URI] = {
    scalaxml.ScalaXmlDocument(underlyingDocument).uriOption.asJava
  }
}

object ScalaXmlDocument {

  def apply(underlyingDocument: scala.xml.Document): ScalaXmlDocument = {
    new ScalaXmlDocument(underlyingDocument)
  }
}
