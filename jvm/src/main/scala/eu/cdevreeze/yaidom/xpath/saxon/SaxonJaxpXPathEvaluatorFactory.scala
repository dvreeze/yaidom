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

package eu.cdevreeze.yaidom.xpath.saxon

import java.io.File
import java.io.FileInputStream
import java.net.URI

import eu.cdevreeze.yaidom.xpath.XPathEvaluatorFactory
import javax.xml.transform.Source
import javax.xml.transform.URIResolver
import javax.xml.transform.stream.StreamSource
import javax.xml.{ xpath => jxpath }
import net.sf.saxon.Configuration
import net.sf.saxon.event.Builder
import net.sf.saxon.om.NodeInfo

/**
 * XPathEvaluatorFactory using the JAXP XPath API and backed by a Saxon implementation.
 *
 * The used Saxon Configuration must use the (default) tiny tree object model!
 *
 * See `SaxonJaxpXPathEvaluator` for more remarks about the used Saxon Configuration.
 *
 * See http://saxonica.com/html/documentation/xpath-api/jaxp-xpath/factory.html.
 *
 * @author Chris de Vreeze
 */
final class SaxonJaxpXPathEvaluatorFactory(
  val underlyingEvaluatorFactory: net.sf.saxon.xpath.XPathFactoryImpl) extends XPathEvaluatorFactory {

  require(
    underlyingEvaluatorFactory.getConfiguration.getTreeModel == Builder.TINY_TREE,
    s"Expected Saxon Configuration requiring the tiny tree model, but found tree model ${underlyingEvaluatorFactory.getConfiguration.getTreeModel}")

  type XPathExpression = jxpath.XPathExpression

  type Node = NodeInfo

  type ContextItem = NodeInfo

  /**
   * Returns the same object, but mutated in-place by setting the JAXP URIResolver on the underlying
   * Saxon XPath evaluator factory.
   *
   * The URIResolver should build Saxon tiny trees using the same Configuration as the one underlying this factory.
   * Consider passing a SimpleUriResolver.
   */
  def settingJaxpUriResolver(newUriResolver: URIResolver): SaxonJaxpXPathEvaluatorFactory = {
    underlyingEvaluatorFactory.getConfiguration.setURIResolver(newUriResolver)
    this
  }

  /**
   * Creates an XPathEvaluator from the constructor argument.
   */
  def newXPathEvaluator(): SaxonJaxpXPathEvaluator = {
    val saxonXPathEvaluator =
      underlyingEvaluatorFactory.newXPath().asInstanceOf[net.sf.saxon.xpath.XPathEvaluator]

    new SaxonJaxpXPathEvaluator(saxonXPathEvaluator)
  }
}

object SaxonJaxpXPathEvaluatorFactory {

  /**
   * Simple JAXP URIResolver created from a function mapping an original URI to a local URI.
   */
  final class SimpleJaxpUriResolver(val uriConverter: URI => URI) extends URIResolver {

    /**
     * First builds the original URI as `baseURI.resolve(new URI(href))`, then converts it to a local
     * URI from which the returned `Source` is created. The original URI is set as document URI ("system ID").
     */
    def resolve(href: String, base: String): Source = {
      val baseURI = new URI(Option(base).getOrElse(""))

      // Resolve the location if necessary
      val resolvedUri = baseURI.resolve(new URI(href))

      val localUri = uriConverter(resolvedUri)

      new StreamSource(new FileInputStream(new File(localUri)), resolvedUri.toString)
    }
  }

  def apply(underlyingEvaluatorFactory: net.sf.saxon.xpath.XPathFactoryImpl): SaxonJaxpXPathEvaluatorFactory = {
    new SaxonJaxpXPathEvaluatorFactory(underlyingEvaluatorFactory)
  }

  def apply(configuration: Configuration): SaxonJaxpXPathEvaluatorFactory = {
    apply(new net.sf.saxon.xpath.XPathFactoryImpl(configuration))
  }
}
