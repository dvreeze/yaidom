/*
 * Copyright 2011 Chris de Vreeze
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

package eu.cdevreeze.yaidom

import scala.collection.immutable

/**
 * Supertrait for [[eu.cdevreeze.yaidom.Elem]] and other element-like classes, such as [[eu.cdevreeze.yaidom.xlink.Elem]],
 * offering methods to obtain text children.
 *
 * The only abstract method is <code>textChildren</code>.
 * Based on this method alone, this trait offers a richer API for querying text.
 *
 * Typical element-like classes mix in this trait as well as [[eu.cdevreeze.yaidom.ElemLike]].
 */
trait HasText[T <: { def text: String }] {

  /** Returns the text children */
  def textChildren: immutable.Seq[T]

  /** Returns the first text child, if any, and None otherwise */
  final def firstTextChildOption: Option[T] = textChildren.headOption

  /** Returns the first text child's value, if any, and None otherwise */
  final def firstTextValueOption: Option[String] = textChildren.headOption map { _.text }

  /** Returns the first text child, if any, and None otherwise */
  final def firstTextChild: T = firstTextChildOption.getOrElse(sys.error("Missing text child"))

  /** Returns the first text child's value, if any, and None otherwise */
  final def firstTextValue: String = firstTextValueOption.getOrElse(sys.error("Missing text child"))
}
