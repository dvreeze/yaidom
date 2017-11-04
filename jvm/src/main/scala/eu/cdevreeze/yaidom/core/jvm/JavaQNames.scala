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

package eu.cdevreeze.yaidom.core.jvm

import eu.cdevreeze.yaidom.core.EName
import javax.xml.XMLConstants
import javax.xml.namespace.{ QName => JQName }

/**
 * Utility to create JAXP QName objects from yaidom EName objects and vice versa.
 *
 * @author Chris de Vreeze
 */
object JavaQNames {

  /**
   * Given an optional prefix, creates a `javax.xml.namespace.QName` from the passed EName.
   */
  def enameToJavaQName(ename: EName, prefixOption: Option[String]): JQName = {
    require(
      ename.namespaceUriOption.isDefined || prefixOption.isEmpty, s"Prefix only allowed if namespace non-empty in EName '${ename}'")

    new JQName(
      ename.namespaceUriOption.getOrElse(XMLConstants.NULL_NS_URI),
      ename.localPart,
      prefixOption.getOrElse(XMLConstants.DEFAULT_NS_PREFIX))
  }

  /**
   * Creates an `EName` from a `javax.xml.namespace.QName`.
   */
  def javaQNameToEName(jqname: JQName): EName = EName(jqname.toString)
}
