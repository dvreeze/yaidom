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

package eu.cdevreeze.yaidom.core

import java.nio.charset.Charset

/**
 * The XML Declaration, which can only occur as the first line in an XML document.
 *
 * @author Chris de Vreeze
 */
final case class XmlDeclaration(
    val version: String,
    val encodingOption: Option[Charset],
    val standaloneOption: Option[Boolean]) {

  require(version ne null) // scalastyle:off null
  require(encodingOption ne null) // scalastyle:off null
  require(standaloneOption ne null) // scalastyle:off null

  def withEncodingOption(encodingOption: Option[Charset]): XmlDeclaration =
    this.copy(encodingOption = encodingOption)

  def withStandaloneOption(standaloneOption: Option[Boolean]): XmlDeclaration =
    this.copy(standaloneOption = standaloneOption)
}

object XmlDeclaration {

  def fromVersion(version: String): XmlDeclaration =
    new XmlDeclaration(version, None, None)

  def parseStandalone(standalone: String): Boolean = standalone match {
    case "yes" => true
    case "no"  => false
    case _     => sys.error(s"Not a valid standalone value: $standalone")
  }
}
