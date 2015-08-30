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

package eu.cdevreeze.yaidom.indexed

import java.net.URI

import scala.reflect.classTag

import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.queryapi.XmlBaseSupport
import eu.cdevreeze.yaidom.simple

/**
 * Factory object for `Elem` instances, where `Elem` is a type alias for `IndexedScopedElem[simple.Elem]`.
 *
 * @author Chris de Vreeze
 */
object Elem {

  /**
   * Builder of `Elem` objects. Somewhat more convenient than the corresponding `IndexedScopedElem` builder.
   */
  final case class Builder(override val uriResolver: XmlBaseSupport.UriResolver) extends IndexedScopedElemApi.Builder[Elem, simple.Elem] {

    final val wrappedBuilder = IndexedScopedElem.Builder(classTag[simple.Elem], uriResolver)

    override def build(rootElem: simple.Elem): Elem =
      wrappedBuilder.build(rootElem)

    override def build(docUriOption: Option[URI], rootElem: simple.Elem): Elem =
      wrappedBuilder.build(docUriOption, rootElem)

    override def build(rootElem: simple.Elem, path: Path): Elem = {
      wrappedBuilder.build(rootElem, path)
    }

    override def build(docUriOption: Option[URI], rootElem: simple.Elem, path: Path): Elem = {
      wrappedBuilder.build(docUriOption, rootElem, path)
    }
  }

  /**
   * Calls `Builder(XmlBaseSupport.JdkUriResolver).build(rootElem)`
   */
  @deprecated(message = "Use 'Builder.build' instead", since = "1.4.1")
  def apply(rootElem: simple.Elem): Elem = {
    Builder(XmlBaseSupport.JdkUriResolver).build(rootElem)
  }

  /**
   * Calls `Builder(XmlBaseSupport.JdkUriResolver).build(docUriOption, rootElem)`
   */
  @deprecated(message = "Use 'Builder.build' instead", since = "1.4.1")
  def apply(docUriOption: Option[URI], rootElem: simple.Elem): Elem = {
    Builder(XmlBaseSupport.JdkUriResolver).build(docUriOption, rootElem)
  }

  /**
   * Calls `Builder(XmlBaseSupport.JdkUriResolver).build(Some(docUri), rootElem)`
   */
  @deprecated(message = "Use 'Builder.build' instead", since = "1.4.1")
  def apply(docUri: URI, rootElem: simple.Elem): Elem = {
    Builder(XmlBaseSupport.JdkUriResolver).build(Some(docUri), rootElem)
  }

  /**
   * Calls `Builder(XmlBaseSupport.JdkUriResolver).build(rootElem, path)`
   */
  @deprecated(message = "Use 'Builder.build' instead", since = "1.4.1")
  def apply(rootElem: simple.Elem, path: Path): Elem = {
    Builder(XmlBaseSupport.JdkUriResolver).build(rootElem, path)
  }

  /**
   * Calls `Builder(XmlBaseSupport.JdkUriResolver).build(docUriOption, rootElem, path)`
   */
  @deprecated(message = "Use 'Builder.build' instead", since = "1.4.1")
  def apply(docUriOption: Option[URI], rootElem: simple.Elem, path: Path): Elem = {
    Builder(XmlBaseSupport.JdkUriResolver).build(docUriOption, rootElem, path)
  }

  /**
   * Calls `Builder(XmlBaseSupport.JdkUriResolver).build(Some(docUri), rootElem, path)`
   */
  @deprecated(message = "Use 'Builder.build' instead", since = "1.4.1")
  def apply(docUri: URI, rootElem: simple.Elem, path: Path): Elem = {
    Builder(XmlBaseSupport.JdkUriResolver).build(Some(docUri), rootElem, path)
  }
}
