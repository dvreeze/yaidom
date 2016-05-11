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

import scala.reflect.classTag

import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.queryapi.ScopedContextPath
import eu.cdevreeze.yaidom.queryapi.XmlBaseSupport
import eu.cdevreeze.yaidom.simple

/**
 * Factory object for `Elem` instances, where `Elem` is a type alias for `ContextAwareScopedElem[simple.Elem]`.
 *
 * WARNING: THE CODE IN THIS PACKAGE IS CONSIDERED EXPERIMENTAL!
 *
 * @author Chris de Vreeze
 */
object Elem {

  /**
   * Builder of `Elem` objects. Somewhat more convenient than the corresponding `ContextAwareScopedElem` builder.
   */
  final case class Builder(val uriResolver: XmlBaseSupport.UriResolver) {

    final val wrappedBuilder = ContextAwareScopedElem.Builder(classTag[simple.Elem], uriResolver)

    def build(elem: simple.Elem): Elem = wrappedBuilder.build(elem)

    def build(docUriOption: Option[URI], elem: simple.Elem): Elem = wrappedBuilder.build(docUriOption, elem)

    def build(docUriOption: Option[URI], parentContextPath: ScopedContextPath, elem: simple.Elem): Elem = {
      wrappedBuilder.build(docUriOption, parentContextPath, elem)
    }
  }

  /**
   * Calls `Builder(XmlBaseSupport.JdkUriResolver).build(elem)`
   */
  def apply(elem: simple.Elem): Elem = {
    Builder(XmlBaseSupport.JdkUriResolver).build(elem)
  }

  /**
   * Calls `Builder(XmlBaseSupport.JdkUriResolver).build(docUriOption, elem)`
   */
  def apply(docUriOption: Option[URI], elem: simple.Elem): Elem = {
    Builder(XmlBaseSupport.JdkUriResolver).build(docUriOption, elem)
  }

  /**
   * Calls `Builder(XmlBaseSupport.JdkUriResolver).build(Some(docUri), elem)`
   */
  def apply(docUri: URI, elem: simple.Elem): Elem = {
    Builder(XmlBaseSupport.JdkUriResolver).build(Some(docUri), elem)
  }

  /**
   * Calls `Builder(XmlBaseSupport.JdkUriResolver).build(docUriOption, parentContextPath, elem)`
   */
  def apply(docUriOption: Option[URI], parentContextPath: ScopedContextPath, elem: simple.Elem): Elem = {
    Builder(XmlBaseSupport.JdkUriResolver).build(docUriOption, parentContextPath, elem)
  }

  /**
   * Calls `Builder(XmlBaseSupport.JdkUriResolver).build(Some(docUri), parentContextPath, elem)`
   */
  def apply(docUri: URI, parentContextPath: ScopedContextPath, elem: simple.Elem): Elem = {
    Builder(XmlBaseSupport.JdkUriResolver).build(Some(docUri), parentContextPath, elem)
  }
}
