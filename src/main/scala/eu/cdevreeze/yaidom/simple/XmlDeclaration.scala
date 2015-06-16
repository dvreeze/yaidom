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

package eu.cdevreeze.yaidom.simple

/**
 * The XML Declaration, which can only occur as the first line in an XML document.
 *
 * @author Chris de Vreeze
 */
final case class XmlDeclaration(
  val version: String,
  val encodingOption: Option[String],
  val standaloneOption: Option[Boolean]) extends Immutable {

  require(version ne null)
  require(encodingOption ne null)
  require(standaloneOption ne null)

  def withEncodingOption(encodingOption: Option[String]): XmlDeclaration =
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
    case _     => sys.error(s"Not a valid standalone vaue: $standalone")
  }
}
