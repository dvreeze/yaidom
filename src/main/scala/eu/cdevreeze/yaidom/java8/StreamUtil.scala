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

package eu.cdevreeze.yaidom.java8

import java.util.function.Function
import java.util.stream.Stream

import scala.collection.immutable
import scala.compat.java8.FunctionConverters.asJavaFunction
import scala.compat.java8.ScalaStreamSupport

/**
 * Utility for creating Java Streams from Scala collections. This utility is implemented with the scala-java8-compat
 * library.
 *
 * @author Chris de Vreeze
 */
object StreamUtil {

  /**
   * Returns `ScalaStreamSupport.stream(xs)`.
   */
  def toStream[A](xs: immutable.IndexedSeq[A]): Stream[A] = {
    ScalaStreamSupport.stream(xs)
  }

  /**
   * Returns `toStream(immutable.IndexedSeq(x))`.
   */
  def toSingletonStream[A](x: A): Stream[A] = {
    toStream(immutable.IndexedSeq(x))
  }

  /**
   * Returns `asJavaFunction { x: A => toStream(f(x)) }`.
   */
  def toJavaStreamFunction[A, B](f: A => immutable.IndexedSeq[B]): Function[A, Stream[B]] = {
    asJavaFunction { x: A => toStream(f(x)) }
  }
}
