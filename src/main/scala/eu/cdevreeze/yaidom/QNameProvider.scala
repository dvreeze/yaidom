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
 * Provider of QNames, possibly from a cache of QNames. Typical implementations cache QName instances, to prevent any explosion
 * of equal QName instances, thus unnecessarily increasing the memory footprint.
 *
 * Yaidom itself internally uses this trait to reduce the memory footprint of parsed elements.
 *
 * @author Chris de Vreeze
 */
trait QNameProvider {

  /**
   * Gets an QName with the given optional prefix and local part
   */
  def getQName(prefixOption: Option[String], localPart: String): QName

  /**
   * Gets an QName with the given prefix and local part
   */
  def getQName(prefix: String, localPart: String): QName

  /**
   * Gets an QName with the given local part, and without any prefix
   */
  def getUnprefixedQName(localPart: String): QName

  /**
   * Gets an QName parsed from the given string representation
   */
  def parseQName(s: String): QName
}

object QNameProvider {

  /**
   * Default, non-caching, QName provider.
   */
  final class DefaultQNameProvider extends QNameProvider {

    def getQName(prefixOption: Option[String], localPart: String): QName = QName(prefixOption, localPart)

    def getQName(prefix: String, localPart: String): QName = PrefixedName(prefix, localPart)

    def getUnprefixedQName(localPart: String): QName = UnprefixedName(localPart)

    def parseQName(s: String): QName = QName.parse(s)
  }

  implicit val defaultInstance = new DefaultQNameProvider

  /**
   * Simple QName provider using an immutable Map. It does not grow, and can be long-lived.
   */
  final class QNameProviderUsingImmutableMap(val qnames: Set[QName]) extends QNameProvider {

    val cache: Map[(Option[String], String), QName] =
      qnames.map(qname => (qname.prefixOption, qname.localPart) -> qname).toMap

    def getQName(prefixOption: Option[String], localPart: String): QName =
      cache.getOrElse((prefixOption, localPart), QName(prefixOption, localPart))

    def getQName(prefix: String, localPart: String): QName =
      getQName(Some(prefix), localPart)

    def getUnprefixedQName(localPart: String): QName = getQName(None, localPart)

    def parseQName(s: String): QName = {
      // First creates a very short-lived QName instance
      val qname = QName.parse(s)
      getQName(qname.prefixOption, qname.localPart)
    }
  }

  /**
   * Simple caching QName provider. The underlying cache is based on a java.util.concurrent.ConcurrentHashMap, so the cache
   * can only grow. Therefore this QName provider is not meant to be a "global" cache with application scope, but it should
   * be rather short-lived.
   */
  final class SimpleCachingQNameProvider extends QNameProvider {

    private val cache = new SimpleCache[(Option[String], String), QName] {

      protected def convertKeyToValue(key: (Option[String], String)): QName = QName(key._1, key._2)
    }

    def getQName(prefixOption: Option[String], localPart: String): QName =
      cache.putIfAbsentAndGet((prefixOption, localPart))

    def getQName(prefix: String, localPart: String): QName =
      getQName(Some(prefix), localPart)

    def getUnprefixedQName(localPart: String): QName = getQName(None, localPart)

    def parseQName(s: String): QName = {
      // First creates a very short-lived QName instance
      val qname = QName.parse(s)
      getQName(qname.prefixOption, qname.localPart)
    }
  }

  def newSimpleCachingInstance: QNameProvider = new SimpleCachingQNameProvider
}
