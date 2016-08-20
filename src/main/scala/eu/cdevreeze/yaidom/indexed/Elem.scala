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

  def apply(underlyingRootElem: simple.Elem): Elem = {
    IndexedScopedElem(underlyingRootElem)
  }

  def apply(docUriOption: Option[URI], underlyingRootElem: simple.Elem): Elem = {
    IndexedScopedElem(docUriOption, underlyingRootElem)
  }

  def apply(docUri: URI, underlyingRootElem: simple.Elem): Elem = {
    IndexedScopedElem(docUri, underlyingRootElem)
  }

  def apply(underlyingRootElem: simple.Elem, path: Path): Elem = {
    IndexedScopedElem(underlyingRootElem, path)
  }

  def apply(docUriOption: Option[URI], underlyingRootElem: simple.Elem, path: Path): Elem = {
    IndexedScopedElem(docUriOption, underlyingRootElem, path)
  }

  def apply(docUri: URI, underlyingRootElem: simple.Elem, path: Path): Elem = {
    IndexedScopedElem(docUri, underlyingRootElem, path)
  }
}
