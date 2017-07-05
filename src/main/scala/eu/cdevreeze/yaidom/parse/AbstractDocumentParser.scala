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

package eu.cdevreeze.yaidom.parse

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.URI

import org.xml.sax.InputSource

import eu.cdevreeze.yaidom.simple.Document

/**
 * Partial `DocumentParser` implementation, leaving only one of the `parse` methods abstract.
 *
 * @author Chris de Vreeze
 */
abstract class AbstractDocumentParser extends DocumentParser {

  /** Parses the content of the given input stream into a [[eu.cdevreeze.yaidom.simple.Document]]. */
  final def parse(inputStream: InputStream): Document = {
    // See http://docs.oracle.com/cd/E13222_01/wls/docs90/xml/best.html
    val inputSource = new InputSource(inputStream)
    parse(inputSource)
  }

  /** Parses the content of the given URI into a [[eu.cdevreeze.yaidom.simple.Document]]. */
  final def parse(uri: URI): Document = parse(uri.toURL.openStream())

  /** Parses the content of the given File into a [[eu.cdevreeze.yaidom.simple.Document]]. */
  final def parse(file: File): Document = parse(new FileInputStream(file))
}
