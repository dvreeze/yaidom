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

package eu.cdevreeze.yaidom

import scala.collection.mutable

/**
 * Simple cache, backed by a mutable Map. It is not meant as a real cache, because it can only grow. Therefore
 * it can be used to cache ENames etc. within yaidom while parsing one XML document, but it should not be used as a cache
 * with application scope.
 *
 * This class is mainly used to reduce memory footprint of parsed XML, by preventing the storage of many equivalent QName
 * or EName instances in the resulting yaidom element tree.
 *
 * This simple cache is not efficient, but it is safe to use in a multi-threaded environment.
 *
 * @author Chris de Vreeze
 */
private[yaidom] abstract class SimpleCache[K, V] {

  private var cache = mutable.Map[K, V]()

  protected def convertKeyToValue(key: K): V

  /**
   * Retrieves a value from the cache, if possible, and otherwise creates a new value instance and adds it to the cache.
   */
  def get(key: K): V = {
    cache.synchronized {
      val cachedValueOption = cache.get(key)

      if (cachedValueOption.isDefined) cachedValueOption.get
      else {
        val newValue = convertKeyToValue(key)
        cache += (key -> newValue)
        newValue
      }
    }
  }

  /**
   * Makes the cache empty
   */
  def clear(): Unit = {
    cache.synchronized {
      cache.clear()
    }
  }
}
