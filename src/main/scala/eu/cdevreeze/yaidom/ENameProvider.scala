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

/**
 * Provider of ENames, possibly from a cache of ENames. Typical implementations cache EName instances, to prevent any explosion
 * of equal EName instances, thus unnecessarily increasing the memory footprint.
 *
 * Yaidom itself internally uses this trait to reduce the memory footprint of parsed elements.
 *
 * @author Chris de Vreeze
 */
trait ENameProvider {

  /**
   * Gets an EName with the given optional namespace URI and local part
   */
  def getEName(namespaceUriOption: Option[String], localPart: String): EName

  /**
   * Gets an EName with the given namespace URI and local part
   */
  def getEName(namespaceUri: String, localPart: String): EName

  /**
   * Gets an EName with the given local part, and without any namespace
   */
  def getNoNsEName(localPart: String): EName

  /**
   * Gets an EName parsed from the given string representation (in James Clark notation)
   */
  def parseEName(s: String): EName
}

object ENameProvider {

  /**
   * Default, non-caching, EName provider.
   */
  final class DefaultENameProvider extends ENameProvider {

    def getEName(namespaceUriOption: Option[String], localPart: String): EName = EName(namespaceUriOption, localPart)

    def getEName(namespaceUri: String, localPart: String): EName = EName(namespaceUri, localPart)

    def getNoNsEName(localPart: String): EName = getEName(None, localPart)

    def parseEName(s: String): EName = EName.parse(s)
  }

  implicit val defaultInstance = new DefaultENameProvider

  /**
   * Simple EName provider using an immutable Map. It does not grow, and can be long-lived.
   */
  final class ENameProviderUsingImmutableMap(val enames: Set[EName]) extends ENameProvider {

    val cache: Map[(Option[String], String), EName] =
      enames.map(ename => (ename.namespaceUriOption, ename.localPart) -> ename).toMap

    def getEName(namespaceUriOption: Option[String], localPart: String): EName =
      cache.getOrElse((namespaceUriOption, localPart), EName(namespaceUriOption, localPart))

    def getEName(namespaceUri: String, localPart: String): EName =
      getEName(Some(namespaceUri), localPart)

    def getNoNsEName(localPart: String): EName = getEName(None, localPart)

    def parseEName(s: String): EName = {
      // First creates a very short-lived EName instance
      val ename = EName.parse(s)
      getEName(ename.namespaceUriOption, ename.localPart)
    }
  }

  /**
   * Simple caching EName provider. The underlying cache is based on a java.util.concurrent.ConcurrentHashMap, so the cache
   * can only grow. Therefore this EName provider is not meant to be a "global" cache with application scope, but it should
   * be rather short-lived.
   */
  final class SimpleCachingENameProvider extends ENameProvider {

    private val cache = new SimpleCache[(Option[String], String), EName] {

      protected def convertKeyToValue(key: (Option[String], String)): EName = EName(key._1, key._2)
    }

    def getEName(namespaceUriOption: Option[String], localPart: String): EName =
      cache.putIfAbsentAndGet((namespaceUriOption, localPart))

    def getEName(namespaceUri: String, localPart: String): EName =
      getEName(Some(namespaceUri), localPart)

    def getNoNsEName(localPart: String): EName = getEName(None, localPart)

    def parseEName(s: String): EName = {
      // First creates a very short-lived EName instance
      val ename = EName.parse(s)
      getEName(ename.namespaceUriOption, ename.localPart)
    }
  }

  def newSimpleCachingInstance: ENameProvider = new SimpleCachingENameProvider
}
