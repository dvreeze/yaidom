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

package eu.cdevreeze.yaidom.core.jvm

import com.github.benmanes.caffeine.cache.CacheLoader
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache

import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.QNameProvider

/**
 * QNameProvider using a Caffeine cache. Given a reusable Caffeine "QName cache", creation of this
 * QNameProvider is cheap.
 *
 * Note that Caffeine is used here for name pooling, to reduce memory footprint, and not for caching expensive
 * objects.
 *
 * @author Chris de Vreeze
 */
final class CaffeineQNameProvider(val cache: LoadingCache[(String, String), QName]) extends QNameProvider {

  def getQName(prefixOption: Option[String], localPart: String): QName =
    cache.get((prefixOption.getOrElse(""), localPart))

  def getQName(prefix: String, localPart: String): QName =
    cache.get((prefix, localPart))

  def getUnprefixedQName(localPart: String): QName =
    cache.get(("", localPart))

  def parseQName(s: String): QName = {
    val qname = QName.parse(s)
    cache.get((qname.prefixOption.getOrElse(""), qname.localPart))
  }
}

object CaffeineQNameProvider {

  /**
   * Create an "QName cache" with the given maximum size.
   */
  def createCache(maximumSize: Int): LoadingCache[(String, String), QName] = {
    createCache(maximumSize, false)
  }

  /**
   * Create an "QName cache" with the given maximum size, and the given record statistics flag.
   */
  def createCache(maximumSize: Int, recordStats: Boolean): LoadingCache[(String, String), QName] = {
    Caffeine.newBuilder()
      .maximumSize(maximumSize)
      .recordStats()
      .build(new CacheLoader[(String, String), QName] {
        def load(key: (String, String)): QName = getQName(key) // Pleasing Scala 2.11 compiler
      })
  }

  /**
   * Convenience method to create a CaffeineQNameProvider, using its own "QName cache".
   */
  def fromMaximumCacheSize(maximumSize: Int): CaffeineQNameProvider = {
    new CaffeineQNameProvider(createCache(maximumSize))
  }

  private def getQName(key: (String, String)): QName = {
    if (key._1.isEmpty) QName(None, key._2) else QName(Some(key._1), key._2)
  }
}
