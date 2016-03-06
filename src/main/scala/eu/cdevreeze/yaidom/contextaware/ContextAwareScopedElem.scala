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
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.queryapi.ContextAwareScopedElemApi
import eu.cdevreeze.yaidom.queryapi.Nodes
import eu.cdevreeze.yaidom.queryapi.ScopedContextPath
import eu.cdevreeze.yaidom.queryapi.ScopedElemApi
import eu.cdevreeze.yaidom.queryapi.ScopedElemLike
import eu.cdevreeze.yaidom.queryapi.XmlBaseSupport

/**
 * An element within its context. In other words, an element as a pair containing the element itself (of an underlying element type)
 * and a parent context path as the ancestry of this element. It also contains an optional URI of the containing document, if any.
 *
 * '''See the documentation of the mixed-in query API trait(s) for more details on the uniform query API offered by this class.'''
 *
 * A `ContextAwareScopedElem(rootElem)` can be seen as one '''immutable snapshot''' of an XML tree. All queries (using the `ElemApi` uniform
 * query API) on that snapshot return results within the same snapshot. Take care not to mix up query results from different
 * snapshots.
 *
 * The optional parent base URI is stored for very fast (optional) base URI computation. This is helpful in
 * an XBRL context, where URI resolution against a base URI is typically a very frequent operation.
 *
 * ==ContextAwareScopedElem examples==
 *
 * The following example code shows how to query for elements with a known ancestry,
 * regardless of the element implementation, if efficiency is not important:
 *
 * {{{
 * val iBookstore = ContextAwareScopedElem(bookstore)
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
 * ==ContextAwareScopedElem more formally==
 *
 * '''In order to use this class, this more formal section can safely be skipped.'''
 *
 * The ``ContextAwareScopedElem`` class can be understood in a precise <em>mathematical</em> sense, as shown below.
 *
 * Some properties of ContextAwareScopedElems are as follows:
 * {{{
 * // All child elements have the optional context path of the parent element as optional parent context path
 *
 * iElem.findAllChildElems.map(_.parentContextPath).distinct == List(iElem.contextPath)
 * }}}
 *
 * The correspondence between queries on ContextAwareScopedElem and the same queries on the underlying elements is as follows:
 * {{{
 * // Let p be a function from underlying element type E to Boolean
 *
 * ContextAwareScopedElem(rootElem).filterElemsOrSelf(e => p(e.elem)).map(_.elem) ==
 *   rootElem.filterElemsOrSelf(p)
 * }}}
 *
 * Analogous properties hold for the other query methods.
 *
 * @tparam U The underlying element type
 *
 * @author Chris de Vreeze
 */
final class ContextAwareScopedElem[U <: ScopedElemApi[U]] private (
  val docUriOption: Option[URI],
  val parentBaseUriOption: Option[URI],
  val parentContextPath: ScopedContextPath,
  val elem: U,
  childElems: immutable.IndexedSeq[ContextAwareScopedElem[U]],
  val uriResolver: XmlBaseSupport.UriResolver) extends Nodes.Elem with ScopedElemLike[ContextAwareScopedElem[U]] with ContextAwareScopedElemApi[ContextAwareScopedElem[U]] {

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

  final def contextPath: ScopedContextPath = {
    val entry = ScopedContextPath.Entry(elem.qname, elem.attributes.toVector, elem.scope)
    parentContextPath.append(entry)
  }

  final def findAllChildElems: immutable.IndexedSeq[ContextAwareScopedElem[U]] = childElems

  final def resolvedName: EName = elem.resolvedName

  final def resolvedAttributes: immutable.Iterable[(EName, String)] =
    elem.resolvedAttributes

  final def text: String = elem.text

  final def qname: QName = elem.qname

  final def attributes: immutable.Iterable[(QName, String)] = elem.attributes

  final def scope: Scope = this.elem.scope

  final override def equals(obj: Any): Boolean = obj match {
    case other: ContextAwareScopedElem[U] =>
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

object ContextAwareScopedElem {

  /**
   * Builder of `ContextAwareScopedElem` objects. The builder has a chosen URI resolver strategy. Typically these
   * builders are long-lived global objects. Each element created with this builder will have the same URI resolver,
   * viz. the one passed as constructor argument of the builder.
   */
  final case class Builder[U <: ScopedElemApi[U]](
    val underlyingType: ClassTag[U],
    val uriResolver: XmlBaseSupport.UriResolver) {

    def build(elem: U): ContextAwareScopedElem[U] =
      build(None, elem)

    def build(docUriOption: Option[URI], elem: U): ContextAwareScopedElem[U] = {
      build(docUriOption, ScopedContextPath.Empty, elem)
    }

    /**
     * Expensive recursive factory method for "context-aware elements".
     */
    def build(docUriOption: Option[URI], parentContextPath: ScopedContextPath, elem: U): ContextAwareScopedElem[U] = {
      val parentBaseUriOption: Option[URI] =
        XmlBaseSupport.findBaseUriByDocUriAndContextPath(docUriOption, parentContextPath)(uriResolver).orElse(docUriOption)

      build(docUriOption, parentBaseUriOption, parentContextPath, elem)
    }

    private def build(
      docUriOption: Option[URI],
      parentBaseUriOption: Option[URI],
      parentContextPath: ScopedContextPath,
      elem: U): ContextAwareScopedElem[U] = {

      val baseUriOption =
        XmlBaseSupport.findBaseUriByParentBaseUri(parentBaseUriOption, elem)(uriResolver)

      val contextPath: ScopedContextPath = {
        val entry = ScopedContextPath.Entry(elem.qname, elem.attributes.toVector, elem.scope)
        parentContextPath.append(entry)
      }

      // Recursive calls
      val childElems = elem.findAllChildElems map { e =>
        build(docUriOption, baseUriOption, contextPath, e)
      }

      new ContextAwareScopedElem(docUriOption, parentBaseUriOption, parentContextPath, elem, childElems, uriResolver)
    }
  }

  /**
   * Calls `Builder[U](classTag[U], XmlBaseSupport.JdkUriResolver).build(elem)`
   */
  def apply[U <: ScopedElemApi[U]: ClassTag](elem: U): ContextAwareScopedElem[U] = {
    Builder[U](classTag[U], XmlBaseSupport.JdkUriResolver).build(elem)
  }

  /**
   * Calls `Builder[U](classTag[U], XmlBaseSupport.JdkUriResolver).build(docUriOption, elem)`
   */
  def apply[U <: ScopedElemApi[U]: ClassTag](docUriOption: Option[URI], elem: U): ContextAwareScopedElem[U] = {
    Builder[U](classTag[U], XmlBaseSupport.JdkUriResolver).build(docUriOption, elem)
  }

  /**
   * Calls `Builder[U](classTag[U], XmlBaseSupport.JdkUriResolver).build(Some(docUri), elem)`
   */
  def apply[U <: ScopedElemApi[U]: ClassTag](docUri: URI, elem: U): ContextAwareScopedElem[U] = {
    Builder[U](classTag[U], XmlBaseSupport.JdkUriResolver).build(Some(docUri), elem)
  }

  /**
   * Calls `Builder[U](classTag[U], XmlBaseSupport.JdkUriResolver).build(docUriOption, parentContextPath, elem)`
   */
  def apply[U <: ScopedElemApi[U]: ClassTag](docUriOption: Option[URI], parentContextPath: ScopedContextPath, elem: U): ContextAwareScopedElem[U] = {
    Builder[U](classTag[U], XmlBaseSupport.JdkUriResolver).build(docUriOption, parentContextPath, elem)
  }

  /**
   * Calls `Builder(XmlBaseSupport.JdkUriResolver).build(Some(docUri), parentContextPath, elem)`
   */
  def apply[U <: ScopedElemApi[U]: ClassTag](docUri: URI, parentContextPath: ScopedContextPath, elem: U): ContextAwareScopedElem[U] = {
    Builder[U](classTag[U], XmlBaseSupport.JdkUriResolver).build(Some(docUri), parentContextPath, elem)
  }
}
