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

package eu.cdevreeze.yaidom.utils

import java.util.concurrent.atomic.AtomicInteger

import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.queryapi.ClarkElemApi
import eu.cdevreeze.yaidom.resolved
import eu.cdevreeze.yaidom.simple

/**
 * Factory of EditableClarkElem objects.
 *
 * @author Chris de Vreeze
 */
sealed trait ClarkElemEditor {

  type N
  type E <: N with ClarkElemApi.Aux[E]

  def wrap(elem: E): EditableClarkElem
}

/**
 * Factory of EditableResolvedElem objects.
 */
object ResolvedElemEditor extends ClarkElemEditor {

  type N = resolved.Node
  type E = resolved.Elem

  def wrap(elem: E): EditableResolvedElem = new EditableResolvedElem(elem)
}

/**
 * Factory of EditableSimpleElem objects.
 */
final class SimpleElemEditor(val getFallbackPrefixForNamespace: String => String) extends ClarkElemEditor {

  type N = simple.Node
  type E = simple.Elem

  def wrap(elem: E): EditableSimpleElem = new EditableSimpleElem(elem, getFallbackPrefixForNamespace)
}

object SimpleElemEditor {

  def newInstanceUsingScopeAndPrefixGenerator(fallbackScope: Scope, generatePrefix: String => String): SimpleElemEditor = {

    def getPrefixForNamespace(nsUri: String): String = {
      fallbackScope.prefixForNamespace(nsUri, () => {
        generatePrefix(nsUri)
      })
    }

    new SimpleElemEditor(getPrefixForNamespace _)
  }

  def newInstanceUsingScopeAndDefaultPrefixGenerator(fallbackScope: Scope): SimpleElemEditor = {

    val generator = DefaultPrefixGenerator

    def generatePrefix(nsUri: String): String = generator(nsUri)

    newInstanceUsingScopeAndPrefixGenerator(fallbackScope, generatePrefix _)
  }

  /**
   * Prefix generator that generates prefixes ns0, ns1, ns2 etc.
   */
  object DefaultPrefixGenerator extends ((String) => String) {

    private val i = new AtomicInteger(0)

    def apply(nsUri: String): String = s"ns${i.getAndIncrement()}"
  }
}
