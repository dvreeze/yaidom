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

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.ENameProvider

/**
 * ENameProvider using a Caffeine cache. Given a reusable Caffeine "EName cache", creation of this
 * ENameProvider is cheap.
 *
 * Note that Caffeine is used here for name pooling, to reduce memory footprint, and not for caching expensive
 * objects.
 *
 * @author Chris de Vreeze
 */
final class CaffeineENameProvider(val cache: LoadingCache[(String, String), EName]) extends ENameProvider {

  def getEName(namespaceUriOption: Option[String], localPart: String): EName =
    cache.get((namespaceUriOption.getOrElse(""), localPart))

  def getEName(namespaceUri: String, localPart: String): EName =
    cache.get((namespaceUri, localPart))

  def getNoNsEName(localPart: String): EName =
    cache.get(("", localPart))

  def parseEName(s: String): EName = {
    val ename = EName.parse(s)
    cache.get((ename.namespaceUriOption.getOrElse(""), ename.localPart))
  }
}

object CaffeineENameProvider {

  /**
   * Create an "EName cache" with the given maximum size.
   */
  def createCache(maximumSize: Int): LoadingCache[(String, String), EName] = {
    createCache(maximumSize, false)
  }

  /**
   * Create an "EName cache" with the given maximum size, and the given record statistics flag.
   */
  def createCache(maximumSize: Int, recordStats: Boolean): LoadingCache[(String, String), EName] = {
    Caffeine.newBuilder()
      .maximumSize(maximumSize)
      .recordStats()
      .build(new CacheLoader[(String, String), EName] {
        def load(key: (String, String)): EName = getEName(key) // Pleasing Scala 2.11 compiler
      })
  }

  /**
   * Convenience method to create a CaffeineENameProvider, using its own "EName cache".
   */
  def fromMaximumCacheSize(maximumSize: Int): CaffeineENameProvider = {
    new CaffeineENameProvider(createCache(maximumSize))
  }

  private def getEName(key: (String, String)): EName = {
    if (key._1.isEmpty) EName(None, key._2) else EName(Some(key._1), key._2)
  }
}
