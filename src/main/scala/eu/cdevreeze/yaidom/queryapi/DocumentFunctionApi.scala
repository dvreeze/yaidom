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

package eu.cdevreeze.yaidom.queryapi

import java.net.URI

/**
 * The equivalent of DocumentApi, but as a functional API where each function takes the document itself as first parameter.
 *
 * @author Chris de Vreeze
 */
trait DocumentFunctionApi {

  type Doc

  type Elem

  def documentElement: Elem

  def uriOption(doc: Doc): Option[URI]
}

object DocumentFunctionApi {

  /**
   * This query function API type, restricting Doc and Elem to the type parameters.
   *
   * @tparam D The document self type
   * @tparam E The element type
   */
  type Aux[D, E] = DocumentFunctionApi { type Doc = D; type Elem = E }
}
