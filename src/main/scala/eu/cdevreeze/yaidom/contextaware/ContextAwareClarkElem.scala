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

package eu.cdevreeze.yaidom.contextaware

import java.net.URI

import scala.collection.immutable
import scala.reflect.ClassTag
import scala.reflect.classTag

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.queryapi.ClarkContextPath
import eu.cdevreeze.yaidom.queryapi.ClarkElemApi
import eu.cdevreeze.yaidom.queryapi.ClarkElemLike
import eu.cdevreeze.yaidom.queryapi.ContextAwareClarkElemApi
import eu.cdevreeze.yaidom.queryapi.Nodes
import eu.cdevreeze.yaidom.queryapi.XmlBaseSupport

/**
 * An element within its context. In other words, an element as a pair containing the element itself (of an underlying element type)
 * and a parent context path as the ancestry of this element. It also contains an optional URI of the containing document, if any.
 *
 * WARNING: THE CODE IN THIS PACKAGE IS CONSIDERED EXPERIMENTAL!
 *
 * '''See the documentation of the mixed-in query API trait(s) for more details on the uniform query API offered by this class.'''
 *
 * A `ContextAwareClarkElem(rootElem)` can be seen as one '''immutable snapshot''' of an XML tree. All queries (using the `ElemApi` uniform
 * query API) on that snapshot return results within the same snapshot. Take care not to mix up query results from different
 * snapshots.
 *
 * The optional parent base URI is stored for very fast (optional) base URI computation. This is helpful in
 * an XBRL context, where URI resolution against a base URI is typically a very frequent operation.
 *
 * ==ContextAwareClarkElem examples==
 *
 * The following example code shows how to query for elements with a known ancestry,
 * regardless of the element implementation, if efficiency is not important:
 *
 * {{{
 * val iBookstore = ContextAwareClarkElem(bookstore)
 *
 * val iTheBookAuthors =
 *   for {
 *     iAuthor <- iBookstore.filterElems(withLocalName("Author"))
 *     bookContextPath <- iAuthor.contextPath.findAncestorContextPath(_.lastEntry.resolvedName.localPart == "Book")
 *     iBook <- iBookstore.findElem(_.contextPath == bookContextPath)
 *     if iBook.getChildElem(withLocalName("Title")).elem.text.startsWith("Programming in Scala")
 *   } yield iAuthor
 * }}}
 *
 * ==ContextAwareClarkElem more formally==
 *
 * '''In order to use this class, this more formal section can safely be skipped.'''
 *
 * The ``ContextAwareClarkElem`` class can be understood in a precise <em>mathematical</em> sense, as shown below.
 *
 * Some properties of ContextAwareClarkElems are as follows:
 * {{{
 * // All child elements have the optional context path of the parent element as optional parent context path
 *
 * iElem.findAllChildElems.map(_.parentContextPath).distinct == List(iElem.contextPath)
 * }}}
 *
 * The correspondence between queries on ContextAwareClarkElem and the same queries on the underlying elements is as follows:
 * {{{
 * // Let p be a function from underlying element type E to Boolean
 *
 * ContextAwareClarkElem(rootElem).filterElemsOrSelf(e => p(e.elem)).map(_.elem) ==
 *   rootElem.filterElemsOrSelf(p)
 * }}}
 *
 * Analogous properties hold for the other query methods.
 *
 * @tparam U The underlying element type
 *
 * @author Chris de Vreeze
 */
final class ContextAwareClarkElem[U <: ClarkElemApi[U]] private (
  val docUriOption: Option[URI],
  val parentBaseUriOption: Option[URI],
  val parentContextPath: ClarkContextPath,
  val elem: U,
  childElems: immutable.IndexedSeq[ContextAwareClarkElem[U]],
  val uriResolver: XmlBaseSupport.UriResolver) extends Nodes.Elem with ClarkElemLike[ContextAwareClarkElem[U]] with ContextAwareClarkElemApi[ContextAwareClarkElem[U]] {

  private implicit val uTag: ClassTag[U] = classTag[U]

  /**
   * Asserts internal consistency of the element. That is, asserts that the redundant fields are mutually consistent.
   * These assertions are not invoked during element construction, for performance reasons. Test code may invoke this
   * method. Users of the API do not need to worry about this method. (In fact, looking at the implementation of this
   * class, it can be reasoned that these assertions must hold.)
   */
  private[yaidom] def assertConsistency(): Unit = {
    assert(childElems.map(_.elem) == elem.findAllChildElems, "Corrupt element!")
    assert(childElems.forall(_.docUriOption eq this.docUriOption), "Corrupt element!")
  }

  final def contextPath: ClarkContextPath = {
    val entry = ClarkContextPath.Entry(elem.resolvedName, elem.resolvedAttributes.toMap)
    parentContextPath.append(entry)
  }

  final def findAllChildElems: immutable.IndexedSeq[ContextAwareClarkElem[U]] = childElems

  final def resolvedName: EName = elem.resolvedName

  final def resolvedAttributes: immutable.Iterable[(EName, String)] =
    elem.resolvedAttributes

  final def text: String = elem.text

  final override def equals(obj: Any): Boolean = obj match {
    case other: ContextAwareClarkElem[U] =>
      (other.docUriOption == this.docUriOption) &&
        (other.elem == this.elem) &&
        (other.parentContextPath == this.parentContextPath)
    case _ => false
  }

  final override def hashCode: Int = (docUriOption, elem, parentContextPath).hashCode

  final def baseUriOption: Option[URI] = {
    XmlBaseSupport.findBaseUriByParentBaseUri(parentBaseUriOption, elem)(uriResolver)
  }

  /**
   * Returns the document URI, falling back to the empty URI if absent.
   */
  final def docUri: URI = docUriOption.getOrElse(new URI(""))

  /**
   * Returns the base URI, falling back to the empty URI if absent.
   */
  final def baseUri: URI = baseUriOption.getOrElse(new URI(""))
}

object ContextAwareClarkElem {

  /**
   * Builder of `ContextAwareClarkElem` objects. The builder has a chosen URI resolver strategy. Typically these
   * builders are long-lived global objects. Each element created with this builder will have the same URI resolver,
   * viz. the one passed as constructor argument of the builder.
   */
  final case class Builder[U <: ClarkElemApi[U]](
    val underlyingType: ClassTag[U],
    val uriResolver: XmlBaseSupport.UriResolver) {

    def build(elem: U): ContextAwareClarkElem[U] =
      build(None, elem)

    def build(docUriOption: Option[URI], elem: U): ContextAwareClarkElem[U] = {
      build(docUriOption, ClarkContextPath.Empty, elem)
    }

    /**
     * Expensive recursive factory method for "context-aware elements".
     */
    def build(docUriOption: Option[URI], parentContextPath: ClarkContextPath, elem: U): ContextAwareClarkElem[U] = {
      val parentBaseUriOption: Option[URI] =
        XmlBaseSupport.findBaseUriByDocUriAndContextPath(docUriOption, parentContextPath)(uriResolver).orElse(docUriOption)

      build(docUriOption, parentBaseUriOption, parentContextPath, elem)
    }

    private def build(
      docUriOption: Option[URI],
      parentBaseUriOption: Option[URI],
      parentContextPath: ClarkContextPath,
      elem: U): ContextAwareClarkElem[U] = {

      val baseUriOption =
        XmlBaseSupport.findBaseUriByParentBaseUri(parentBaseUriOption, elem)(uriResolver)

      val contextPath: ClarkContextPath = {
        val entry = ClarkContextPath.Entry(elem.resolvedName, elem.resolvedAttributes.toMap)
        parentContextPath.append(entry)
      }

      // Recursive calls
      val childElems = elem.findAllChildElems map { e =>
        build(docUriOption, baseUriOption, contextPath, e)
      }

      new ContextAwareClarkElem(docUriOption, parentBaseUriOption, parentContextPath, elem, childElems, uriResolver)
    }
  }

  /**
   * Calls `Builder[U](classTag[U], XmlBaseSupport.JdkUriResolver).build(elem)`
   */
  def apply[U <: ClarkElemApi[U]: ClassTag](elem: U): ContextAwareClarkElem[U] = {
    Builder[U](classTag[U], XmlBaseSupport.JdkUriResolver).build(elem)
  }

  /**
   * Calls `Builder[U](classTag[U], XmlBaseSupport.JdkUriResolver).build(docUriOption, elem)`
   */
  def apply[U <: ClarkElemApi[U]: ClassTag](docUriOption: Option[URI], elem: U): ContextAwareClarkElem[U] = {
    Builder[U](classTag[U], XmlBaseSupport.JdkUriResolver).build(docUriOption, elem)
  }

  /**
   * Calls `Builder[U](classTag[U], XmlBaseSupport.JdkUriResolver).build(Some(docUri), elem)`
   */
  def apply[U <: ClarkElemApi[U]: ClassTag](docUri: URI, elem: U): ContextAwareClarkElem[U] = {
    Builder[U](classTag[U], XmlBaseSupport.JdkUriResolver).build(Some(docUri), elem)
  }

  /**
   * Calls `Builder[U](classTag[U], XmlBaseSupport.JdkUriResolver).build(docUriOption, parentContextPath, elem)`
   */
  def apply[U <: ClarkElemApi[U]: ClassTag](docUriOption: Option[URI], parentContextPath: ClarkContextPath, elem: U): ContextAwareClarkElem[U] = {
    Builder[U](classTag[U], XmlBaseSupport.JdkUriResolver).build(docUriOption, parentContextPath, elem)
  }

  /**
   * Calls `Builder(XmlBaseSupport.JdkUriResolver).build(Some(docUri), parentContextPath, elem)`
   */
  def apply[U <: ClarkElemApi[U]: ClassTag](docUri: URI, parentContextPath: ClarkContextPath, elem: U): ContextAwareClarkElem[U] = {
    Builder[U](classTag[U], XmlBaseSupport.JdkUriResolver).build(Some(docUri), parentContextPath, elem)
  }
}
