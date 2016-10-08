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

package eu.cdevreeze.yaidom.java8.functionapi

import java.util.Optional
import java.util.function.Function
import java.util.stream.Stream

import scala.collection.immutable
import scala.compat.java8.FunctionConverters.enrichAsJavaFunction
import scala.compat.java8.OptionConverters.RichOptionForJava8
import scala.compat.java8.OptionConverters.RichOptionalGeneric
import scala.compat.java8.ScalaStreamSupport

/**
 * Utility for creating element Streams, etc. It is a very small wrapper around some functions offered by the
 * scala-java8-compat library. It is mainly used internally, but can also be used in user code.
 *
 * @author Chris de Vreeze
 */
object JavaStreamUtil {

  def makeStream[A](xs: immutable.IndexedSeq[A]): Stream[A] = {
    ScalaStreamSupport.stream(xs)
  }

  def makeSingletonStream[A](x: A): Stream[A] = {
    makeStream(immutable.IndexedSeq(x))
  }

  /**
   * Turns a Scala function into a Java Function. In other words, returns `f.asJava`.
   */
  def makeJavaFunction[A, B](f: A => B): Function[A, B] = {
    f.asJava
  }

  /**
   * Turns a Scala function returning a Scala immutable IndexedSeq into a Java function returning a Java Stream.
   */
  def makeJavaStreamFunction[A, B](f: A => immutable.IndexedSeq[B]): Function[A, Stream[B]] = {
    makeJavaFunction { (x: A) => makeStream(f(x)) }
  }

  /**
   * Turns a Scala function returning a Scala Option into a Java function returning a Java Optional.
   */
  def makeJavaOptionalFunction[A, B](f: A => Option[B]): Function[A, Optional[B]] = {
    makeJavaFunction { (x: A) => f(x).asJava }
  }

  /**
   * Turns a Java Optional into a Java Stream, for lack of such a conversion in Java 8.
   */
  def toStream[A](x: Optional[A]): Stream[A] = {
    ScalaStreamSupport.stream(x.asScala.toSeq)
  }
}
