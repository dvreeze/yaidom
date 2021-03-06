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

import java.{util => jutil}

import scala.jdk.CollectionConverters._

import eu.cdevreeze.yaidom.core.Scope
import javax.xml.XMLConstants
import javax.xml.namespace.NamespaceContext

/**
 * Utility to create JAXP NamespaceContext objects from yaidom Scope objects.
 *
 * @author Chris de Vreeze
 */
object NamespaceContexts {

  /**
   * Returns the Java NamespaceContext corresponding to the passed Scope. Note that this method is very useful if we want to create
   * a NamespaceContext in an easy manner. Indeed, yaidom Scopes make excellent NamespaceContext factories.
   */
  // scalastyle:off null
  def scopeToNamespaceContext(scope: Scope): NamespaceContext = {

    new NamespaceContext {

      def getNamespaceURI(prefix: String): String = {
        require(prefix ne null)

        prefix match {
          case XMLConstants.XML_NS_PREFIX =>
            XMLConstants.XML_NS_URI
          case XMLConstants.XMLNS_ATTRIBUTE =>
            XMLConstants.XMLNS_ATTRIBUTE_NS_URI
          case pref =>
            scope.prefixNamespaceMap.getOrElse(pref, XMLConstants.NULL_NS_URI)
        }
      }

      def getPrefix(namespaceURI: String): String = {
        require(namespaceURI ne null)

        val inverseWithoutDefaultNs = scope.withoutDefaultNamespace.inverse

        namespaceURI match {
          case XMLConstants.XML_NS_URI =>
            XMLConstants.XML_NS_PREFIX
          case XMLConstants.XMLNS_ATTRIBUTE_NS_URI =>
            XMLConstants.XMLNS_ATTRIBUTE
          case nsUri if scope.defaultNamespaceOption.exists(_ == nsUri) =>
            XMLConstants.DEFAULT_NS_PREFIX
          case nsUri =>
            inverseWithoutDefaultNs.get(nsUri).map(_.iterator.next()).orNull
        }
      }

      def getPrefixes(namespaceURI: String): jutil.Iterator[String] = {
        require(namespaceURI ne null)

        val inverseMap = scope.inverse

        namespaceURI match {
          case XMLConstants.XML_NS_URI =>
            Set(XMLConstants.XML_NS_PREFIX).iterator.asJava
          case XMLConstants.XMLNS_ATTRIBUTE_NS_URI =>
            Set(XMLConstants.XMLNS_ATTRIBUTE).iterator.asJava
          case nsUri =>
            inverseMap.getOrElse(nsUri, Set[String]()).iterator.asJava
        }
      }
    }
  }
}
