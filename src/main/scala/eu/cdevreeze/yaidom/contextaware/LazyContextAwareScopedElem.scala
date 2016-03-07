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
import eu.cdevreeze.yaidom.queryapi.ScopedContextPath
import eu.cdevreeze.yaidom.queryapi.ScopedElemApi
import eu.cdevreeze.yaidom.queryapi.ScopedElemLike
import eu.cdevreeze.yaidom.queryapi.ContextAwareScopedElemApi
import eu.cdevreeze.yaidom.queryapi.Nodes
import eu.cdevreeze.yaidom.queryapi.XmlBaseSupport

/**
 * Very lightweight lazy context-aware element implementation. It offers the `ContextAwareScopedElemApi` query API. It is optimized
 * for fast (just-in-time) element creation, not for fast querying. Other than that, it is much like `ContextAwareScopedElem`.
 *
 * @tparam U The underlying element type
 *
 * @author Chris de Vreeze
 */
final class LazyContextAwareScopedElem[U <: ScopedElemApi[U]] private (
  val docUriOption: Option[URI],
  val parentContextPath: ScopedContextPath,
  val elem: U,
  val uriResolver: XmlBaseSupport.UriResolver) extends Nodes.Elem with ScopedElemLike[LazyContextAwareScopedElem[U]] with ContextAwareScopedElemApi[LazyContextAwareScopedElem[U]] {

  private implicit val uTag: ClassTag[U] = classTag[U]

  final def contextPath: ScopedContextPath = {
    val entry = ScopedContextPath.Entry(elem.qname, elem.attributes.toVector, elem.scope)
    parentContextPath.append(entry)
  }

  final def findAllChildElems: immutable.IndexedSeq[LazyContextAwareScopedElem[U]] = {
    elem.findAllChildElems map { e =>
      new LazyContextAwareScopedElem(docUriOption, contextPath, e, uriResolver)
    }
  }

  final def resolvedName: EName = elem.resolvedName

  final def resolvedAttributes: immutable.Iterable[(EName, String)] =
    elem.resolvedAttributes

  final def text: String = elem.text

  final def qname: QName = elem.qname

  final def attributes: immutable.Iterable[(QName, String)] = elem.attributes

  final def scope: Scope = this.elem.scope

  final override def equals(obj: Any): Boolean = obj match {
    case other: LazyContextAwareScopedElem[U] =>
      (other.docUriOption == this.docUriOption) &&
        (other.elem == this.elem) &&
        (other.parentContextPath == this.parentContextPath)
    case _ => false
  }

  final override def hashCode: Int = (docUriOption, elem, parentContextPath).hashCode

  final def baseUriOption: Option[URI] = {
    XmlBaseSupport.findBaseUriByDocUriAndContextPath(docUriOption, contextPath)(uriResolver).orElse(docUriOption)
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

object LazyContextAwareScopedElem {

  /**
   * Builder of `LazyContextAwareScopedElem` objects. The builder has a chosen URI resolver strategy. Typically these
   * builders are long-lived global objects. Each element created with this builder will have the same URI resolver,
   * viz. the one passed as constructor argument of the builder.
   */
  final case class Builder[U <: ScopedElemApi[U]](
    val underlyingType: ClassTag[U],
    val uriResolver: XmlBaseSupport.UriResolver) {

    def build(elem: U): LazyContextAwareScopedElem[U] =
      build(None, elem)

    def build(docUriOption: Option[URI], elem: U): LazyContextAwareScopedElem[U] = {
      build(docUriOption, ScopedContextPath.Empty, elem)
    }

    /**
     * Fast factory method for lazy "context-aware elements".
     */
    def build(docUriOption: Option[URI], parentContextPath: ScopedContextPath, elem: U): LazyContextAwareScopedElem[U] = {
      new LazyContextAwareScopedElem(docUriOption, parentContextPath, elem, uriResolver)
    }
  }

  /**
   * Calls `Builder[U](classTag[U], XmlBaseSupport.JdkUriResolver).build(elem)`
   */
  def apply[U <: ScopedElemApi[U]: ClassTag](elem: U): LazyContextAwareScopedElem[U] = {
    Builder[U](classTag[U], XmlBaseSupport.JdkUriResolver).build(elem)
  }

  /**
   * Calls `Builder[U](classTag[U], XmlBaseSupport.JdkUriResolver).build(docUriOption, elem)`
   */
  def apply[U <: ScopedElemApi[U]: ClassTag](docUriOption: Option[URI], elem: U): LazyContextAwareScopedElem[U] = {
    Builder[U](classTag[U], XmlBaseSupport.JdkUriResolver).build(docUriOption, elem)
  }

  /**
   * Calls `Builder[U](classTag[U], XmlBaseSupport.JdkUriResolver).build(Some(docUri), elem)`
   */
  def apply[U <: ScopedElemApi[U]: ClassTag](docUri: URI, elem: U): LazyContextAwareScopedElem[U] = {
    Builder[U](classTag[U], XmlBaseSupport.JdkUriResolver).build(Some(docUri), elem)
  }

  /**
   * Calls `Builder[U](classTag[U], XmlBaseSupport.JdkUriResolver).build(docUriOption, parentContextPath, elem)`
   */
  def apply[U <: ScopedElemApi[U]: ClassTag](docUriOption: Option[URI], parentContextPath: ScopedContextPath, elem: U): LazyContextAwareScopedElem[U] = {
    Builder[U](classTag[U], XmlBaseSupport.JdkUriResolver).build(docUriOption, parentContextPath, elem)
  }

  /**
   * Calls `Builder(XmlBaseSupport.JdkUriResolver).build(Some(docUri), parentContextPath, elem)`
   */
  def apply[U <: ScopedElemApi[U]: ClassTag](docUri: URI, parentContextPath: ScopedContextPath, elem: U): LazyContextAwareScopedElem[U] = {
    Builder[U](classTag[U], XmlBaseSupport.JdkUriResolver).build(Some(docUri), parentContextPath, elem)
  }
}
