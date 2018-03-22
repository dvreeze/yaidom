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

package eu.cdevreeze.yaidom.xpath.jsdom

import org.scalajs.dom.{ raw => sjsdom }

import eu.cdevreeze.yaidom.xpath.XPathEvaluatorFactoryBuilder

/**
 * XPathEvaluatorFactory builder for JS-DOM XML (not HTML).
 *
 * @author Chris de Vreeze
 */
// scalastyle:off null
final class JsDomXPathEvaluatorFactoryBuilder private (
  val doc:                     sjsdom.Document,
  val namespaceResolverOption: Option[sjsdom.XPathNSResolver]) extends XPathEvaluatorFactoryBuilder {

  type XPathExpression = String

  type Node = sjsdom.Node

  type ContextItem = sjsdom.Node

  def withNamespaceResolver(newNamespaceResolver: sjsdom.XPathNSResolver): JsDomXPathEvaluatorFactoryBuilder = {
    new JsDomXPathEvaluatorFactoryBuilder(doc, Some(newNamespaceResolver))
  }

  def withNamespaceResolverFromElement(elem: sjsdom.Element): JsDomXPathEvaluatorFactoryBuilder = {
    val docElem = elem.ownerDocument.documentElement

    val namespaceResolver = doc.createNSResolver(docElem)

    withNamespaceResolver(namespaceResolver)
  }

  def build(): JsDomXPathEvaluatorFactory = {
    new JsDomXPathEvaluatorFactory(doc, namespaceResolverOption)
  }
}

object JsDomXPathEvaluatorFactoryBuilder {

  def apply(doc: sjsdom.Document): JsDomXPathEvaluatorFactoryBuilder = {
    new JsDomXPathEvaluatorFactoryBuilder(doc, None)
  }
}
