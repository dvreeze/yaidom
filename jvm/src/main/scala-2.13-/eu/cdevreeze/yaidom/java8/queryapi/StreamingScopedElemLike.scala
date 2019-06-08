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

package eu.cdevreeze.yaidom.java8.queryapi

import java.util.Optional
import java.util.stream.Stream

import scala.compat.java8.OptionConverters.RichOptionForJava8
import scala.compat.java8.OptionConverters.RichOptionalGeneric

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.java8.Attr

/**
 * Equivalent of `ScopedElemLike`, but returning Java 8 Streams and taking Java 8 Predicates, to be used
 * in Java code.
 *
 * @author Chris de Vreeze
 */
trait StreamingScopedElemLike[E <: StreamingScopedElemLike[E]] extends StreamingScopedElemApi[E] with StreamingClarkElemLike[E] { self: E =>

  // HasQNameApi methods

  def qname: QName

  def attributes: Stream[Attr]

  // HasScopeApi

  def scope: Scope

  // ScopedElemApi own methods

  final def attributeAsQNameOption(expandedName: EName): Optional[QName] = {
    attributeOption(expandedName).asScala.map(v => QName(v.trim)).asJava
  }

  final def attributeAsQName(expandedName: EName): QName = {
    attributeAsQNameOption(expandedName).orElseGet(
      sys.error(s"Missing QName-valued attribute $expandedName"))
  }

  final def attributeAsResolvedQNameOption(expandedName: EName): Optional[EName] = {
    (attributeAsQNameOption(expandedName).asScala map { qn =>
      scope.resolveQNameOption(qn).getOrElse(
        sys.error(s"Could not resolve QName-valued attribute value $qn, given scope [${scope}]"))
    }).asJava
  }

  final def attributeAsResolvedQName(expandedName: EName): EName = {
    attributeAsResolvedQNameOption(expandedName).orElseGet(
      sys.error(s"Missing QName-valued attribute $expandedName"))
  }

  final def textAsQName: QName = {
    QName(text.trim)
  }

  final def textAsResolvedQName: EName = {
    scope.resolveQNameOption(textAsQName).getOrElse(
      sys.error(s"Could not resolve QName-valued element text $textAsQName, given scope [${scope}]"))
  }
}
