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

import java.net.URI

import scala.collection.JavaConverters.asScalaBufferConverter
import scala.collection.JavaConverters.asScalaIteratorConverter
import scala.collection.JavaConverters.setAsJavaSetConverter
import scala.collection.immutable
import scala.util.Failure
import scala.util.Success
import scala.util.Try

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.queryapi.BackingDocumentApi
import eu.cdevreeze.yaidom.queryapi.BackingElemNodeApi
import eu.cdevreeze.yaidom.saxon.SaxonDocument
import eu.cdevreeze.yaidom.saxon.SaxonElem
import eu.cdevreeze.yaidom.saxon.SaxonNode
import eu.cdevreeze.yaidom.xpath.XPathEvaluator
import javax.xml.namespace.NamespaceContext
import javax.xml.xpath
import javax.xml.xpath.XPathConstants
import net.sf.saxon
import net.sf.saxon.event.Builder
import net.sf.saxon.om.NamespaceResolver
import net.sf.saxon.om.NodeInfo
import net.sf.saxon.pull.NamespaceContextImpl
import net.sf.saxon.tree.linked.DocumentImpl
import net.sf.saxon.value.AtomicValue

/**
 * XPathEvaluator using the JAXP XPath API and backed by a Saxon implementation.
 *
 * The Saxon Configuration of the passed evaluator must be the same one that built the documents used with the XPath
 * expressions that are compiled using this SaxonJaxpXPathEvaluator. The Saxon Configuration must also use the
 * (default) tiny tree object model!
 *
 * The passed context items in the evaluation functions must also use the tiny tree model, provided as NodeInfo objects
 * (wrapped in an Option).
 *
 * See http://saxonica.com/html/documentation/xpath-api/jaxp-xpath/factory.html.
 *
 * @author Chris de Vreeze
 */
// scalastyle:off null
final class SaxonJaxpXPathEvaluator(val underlyingEvaluator: saxon.xpath.XPathEvaluator) extends XPathEvaluator {
  require(
    underlyingEvaluator.getConfiguration.getTreeModel == Builder.TINY_TREE,
    s"Expected Saxon Configuration requiring the tiny tree model, but found tree model ${underlyingEvaluator.getConfiguration.getTreeModel}")

  type XPathExpression = xpath.XPathExpression

  type Node = NodeInfo

  type ContextItem = NodeInfo

  // Configuration methods

  /**
   * Returns the same object, but mutated in-place by setting the optional base URI on the underlying
   * Saxon XPath evaluator.
   */
  def settingBaseUriOption(newDocUriOption: Option[URI]): SaxonJaxpXPathEvaluator = {
    newDocUriOption foreach { docUri =>
      underlyingEvaluator.getStaticContext().setBaseURI(docUri.toString)
    }
    this
  }

  /**
   * Returns `settingBaseUriOption(Some(newDocUri))`.
   */
  def settingBaseUri(newDocUri: URI): SaxonJaxpXPathEvaluator = {
    settingBaseUriOption(Some(newDocUri))
  }

  /**
   * Returns the same object, but mutated in-place by setting the namespace context on the underlying
   * Saxon XPath evaluator. The default namespace is always set to "http://www.w3.org/2005/xpath-functions", however.
   */
  def settingScope(newScope: Scope): SaxonJaxpXPathEvaluator = {
    // Just passing scope.toNamespaceContext will lead to an UnsupportedOperationException in
    // net.sf.saxon.xpath.JAXPXPathStaticContext.iteratePrefixes later on. Hence we create a Saxon NamespaceResolver
    // and turn that into a JAXP NamespaceContext that is also a Saxon NamespaceResolver.

    underlyingEvaluator.setNamespaceContext(
      new NamespaceContextImpl(
        SaxonJaxpXPathEvaluator.makeSaxonNamespaceResolver(
          scope.withoutDefaultNamespace ++ Scope.from("" -> "http://www.w3.org/2005/xpath-functions"))))
    this
  }

  /**
   * Returns `settingScope(SaxonJaxpXPathEvaluator.MinimalScope)`.
   */
  def settingMinimalScope(): SaxonJaxpXPathEvaluator = {
    settingScope(SaxonJaxpXPathEvaluator.MinimalScope)
  }

  /**
   * Returns `settingScope(scope ++ addedScope)`.
   */
  def addingScope(addedScope: Scope): SaxonJaxpXPathEvaluator = {
    settingScope(scope ++ addedScope)
  }

  // Evaluation methods

  def evaluateAsString(expr: XPathExpression, contextItemOption: Option[ContextItem]): String = {
    require(!contextItemOption.contains(null), s"Null context not allowed. Use empty Option instead.")

    transformXPathException {
      expr.evaluate(adaptNoneContextItem(contextItemOption))
    }
  }

  def evaluateAsNode(expr: XPathExpression, contextItemOption: Option[ContextItem]): Node = {
    require(!contextItemOption.contains(null), s"Null context not allowed. Use empty Option instead.")

    transformXPathException {
      val result = expr.evaluate(adaptNoneContextItem(contextItemOption), XPathConstants.NODE)
      result.asInstanceOf[NodeInfo]
    }
  }

  def evaluateAsNodeSeq(expr: XPathExpression, contextItemOption: Option[ContextItem]): immutable.IndexedSeq[Node] = {
    require(!contextItemOption.contains(null), s"Null context not allowed. Use empty Option instead.")

    transformXPathException {
      val result = expr.evaluate(adaptNoneContextItem(contextItemOption), XPathConstants.NODESET)

      // See http://saxonica.com/html/documentation/xpath-api/jaxp-xpath/return-types.html.
      result match {
        case results: java.util.List[_] =>
          // This is very sensitive (and undoubtedly incomplete) code!
          results.asScala.toIndexedSeq map { retVal =>
            retVal match {
              case n: NodeInfo    => n
              case v: AtomicValue => sys.error(s"Atomic values as results are not supported by this method.")
              case v              => sys.error(s"Values of type ${v.getClass} as results are not supported by this method.")
            }
          }
        case _ =>
          sys.error(s"Unsupported result type: ${result.getClass}. Only java.util.List is supported (we do not allow org.w3c.dom.NodeList).")
      }
    }
  }

  def evaluateAsBackingDocument(expr: XPathExpression, contextItemOption: Option[ContextItem]): BackingDocumentApi = {
    val nodeResult = evaluateAsNode(expr, contextItemOption)
    // Assuming the result to be a document
    SaxonDocument.wrapDocument(nodeResult.getTreeInfo)
  }

  def evaluateAsBackingElem(expr: XPathExpression, contextItemOption: Option[ContextItem]): BackingElemNodeApi = {
    val nodeResult = evaluateAsNode(expr, contextItemOption)
    // Assuming the result to be an element node
    SaxonNode.wrapElement(nodeResult)
  }

  def evaluateAsBackingElemSeq(
    expr:              XPathExpression,
    contextItemOption: Option[ContextItem]): immutable.IndexedSeq[BackingElemNodeApi] = {

    val nodeSeqResult = evaluateAsNodeSeq(expr, contextItemOption)
    // Assuming all results to be element nodes
    nodeSeqResult.flatMap(n => SaxonNode.wrapNodeOption(n)) collect { case e: SaxonElem => e }
  }

  def evaluateAsBigDecimal(expr: XPathExpression, contextItemOption: Option[ContextItem]): BigDecimal = {
    require(!contextItemOption.contains(null), s"Null context not allowed. Use empty Option instead.")

    transformXPathException {
      val result = expr.evaluate(adaptNoneContextItem(contextItemOption), XPathConstants.NUMBER)
      BigDecimal(result.toString)
    }
  }

  def evaluateAsBoolean(expr: XPathExpression, contextItemOption: Option[ContextItem]): Boolean = {
    require(!contextItemOption.contains(null), s"Null context not allowed. Use empty Option instead.")

    transformXPathException {
      val result = expr.evaluate(adaptNoneContextItem(contextItemOption), XPathConstants.BOOLEAN)
      result.asInstanceOf[java.lang.Boolean].booleanValue
    }
  }

  def evaluateAsEName(expr: XPathExpression, contextItemOption: Option[ContextItem]): EName = {
    transformXPathException {
      val stringResult = evaluateAsString(expr, contextItemOption)

      // Is the string result a lexical QName or James Clark notation for an EName? I don't know, so let's handle both cases.
      if (stringResult.contains("{") || !stringResult.contains(":")) {
        EName.parse(stringResult)
      } else {
        val qname = QName(stringResult)

        val msg =
          s"Could not resolve QName $qname. Expression: '${toString(expr)}'. " +
            s"Base URI: ${underlyingEvaluator.getStaticContext.getStaticBaseURI}.\n\tScope: $scope"
        scope.resolveQNameOption(qname) getOrElse {
          sys.error(msg)
        }
      }
    }
  }

  def makeXPathExpression(xpathString: String): XPathExpression = {
    transformXPathException {
      underlyingEvaluator.compile(xpathString)
    }
  }

  def toString(expr: XPathExpression): String = expr match {
    case expr: net.sf.saxon.xpath.XPathExpressionImpl =>
      expr.getInternalExpression.toString
    case expr =>
      expr.toString
  }

  def scope: Scope = {
    val defaultNamespace = "http://www.w3.org/2005/xpath-functions"

    if (underlyingEvaluator.getNamespaceContext == null) {
      Scope.from("" -> defaultNamespace)
    } else if (!underlyingEvaluator.getNamespaceContext.isInstanceOf[NamespaceContext with NamespaceResolver]) {
      // Cannot deal with such a NamespaceContext now
      Scope.from("" -> defaultNamespace)
    } else {
      val namespaceContext = underlyingEvaluator.getNamespaceContext.asInstanceOf[NamespaceContext with NamespaceResolver]
      val scope = getScope(namespaceContext)
      scope ++ Scope.from("" -> defaultNamespace)
    }
  }

  private def getScope(namespaceContext: NamespaceContext with NamespaceResolver): Scope = {
    val unfilteredPrefixes: immutable.IndexedSeq[String] =
      Try(namespaceContext.iteratePrefixes.asInstanceOf[java.util.Iterator[String]].asScala.toIndexedSeq).getOrElse(Vector())

    val filteredPrefixes = unfilteredPrefixes.filterNot(Set("xml", "xmlns"))

    val prefNsPairs: Map[String, String] =
      filteredPrefixes.map(pref => (pref -> namespaceContext.getNamespaceURI(pref))).filter(_._2.nonEmpty).toMap

    Scope.from(prefNsPairs)
  }

  private def transformXPathException[A](block: => A): A = {
    Try(block) match {
      case Success(v) => v
      case Failure(e) => throw new IllegalStateException(e)
    }
  }

  /**
   * Turns an absent item into an "empty" item that Saxon can handle.
   */
  private def adaptNoneContextItem(itemOption: Option[ContextItem]): ContextItem = {
    if (itemOption.isEmpty) {
      // This seems to be a hack, but how can we otherwise create an empty sequence that is accepted as context item?
      val result = new DocumentImpl
      result.setConfiguration(underlyingEvaluator.getConfiguration)
      result
    } else {
      itemOption.get
    }
  }
}

object SaxonJaxpXPathEvaluator {

  /**
   * Minimal scope used for XPath processing.
   */
  val MinimalScope: Scope = {
    Scope.from(
      "" -> "http://www.w3.org/2005/xpath-functions",
      "fn" -> "http://www.w3.org/2005/xpath-functions",
      "math" -> "http://www.w3.org/2005/xpath-functions/math",
      "map" -> "http://www.w3.org/2005/xpath-functions/map",
      "array" -> "http://www.w3.org/2005/xpath-functions/array",
      "xfi" -> "http://www.xbrl.org/2008/function/instance",
      "xs" -> "http://www.w3.org/2001/XMLSchema")
  }

  /**
   * Creates a Saxon NamespaceResolver from a yaidom Scope. The result can be wrapped in a NamespaceContextImpl,
   * which in turn can be set on a Saxon XPathEvaluator. This way of setting a NamespaceContext on the Saxon
   * XPathEvaluator ensures that JAXPXPathStaticContext.iteratePrefixes does not throw an UnsupportedOperationException.
   *
   * This method is called by function `withScope`, but can also be called by user code.
   */
  def makeSaxonNamespaceResolver(scope: Scope): NamespaceResolver = {
    new NamespaceResolver {

      override def iteratePrefixes(): java.util.Iterator[String] = {
        val prefixes = (scope.keySet + "xml")
        prefixes.asJava.iterator
      }

      override def getURIForPrefix(prefix: String, useDefault: Boolean): String = {
        val effectiveScope = if (useDefault) scope else scope.withoutDefaultNamespace

        prefix match {
          case "xml" =>
            "http://www.w3.org/XML/1998/namespace"
          case pref =>
            effectiveScope.prefixNamespaceMap.getOrElse(pref, "")
        }
      }
    }
  }
}
