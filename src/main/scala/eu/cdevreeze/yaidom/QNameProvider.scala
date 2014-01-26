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

import scala.util.Try

/**
 * Provider of QNames, possibly from a cache of QNames. Typical implementations cache QName instances, to prevent any explosion
 * of equal QName instances, thus unnecessarily increasing the memory footprint.
 *
 * ==Implementation notes==
 *
 * The chosen implementation strategy for (globally) setting the QNameProvider is as follows:
 * <ul>
 * <li>The public API remains backward compatible as much as possible, and possibly implicit parameters are introduced for
 * implicit QName providers.</li>
 * <li>Still, implicit parameters are used in moderation, and not in many places throughout the API. This reduces the risk
 * of polluting the API, and of many future deprecation warnings. Moreover, QNameProviders are implementation details.</li>
 * <li>The query API should be stable, and therefore unaffected by (possibly implicit) QNameProviders.</li>
 * <li>There is one implicit QNameProvider, that can be updated as the globally used QNameProvider.</li>
 * </ul>
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
   * Trivial, non-caching, QName provider.
   */
  final class TrivialQNameProvider extends QNameProvider {

    def getQName(prefixOption: Option[String], localPart: String): QName = QName(prefixOption, localPart)

    def getQName(prefix: String, localPart: String): QName = PrefixedName(prefix, localPart)

    def getUnprefixedQName(localPart: String): QName = UnprefixedName(localPart)

    def parseQName(s: String): QName = QName.parse(s)
  }

  val defaultInstance: QNameProvider = new TrivialQNameProvider

  /**
   * The implicit global QNameProvider is by default a "trivial" QNameProvider, but can be updated.
   */
  @volatile implicit var globalMutableInstance: QNameProvider = defaultInstance

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
   * Simple caching QName provider. The underlying cache is based on a normal Map implementation, so the cache
   * can only grow. Therefore this QName provider is not meant to be a "global" cache with application scope, but it should
   * be rather short-lived.
   */
  final class SimpleCachingQNameProvider(val cacheFilter: (Option[String], String) => Boolean) extends QNameProvider {

    def this() = this((prefixOption, localPart) => true)

    private val cache = new SimpleCache[(Option[String], String), QName] {

      protected def convertKeyToValue(key: (Option[String], String)): QName = QName(key._1, key._2)
    }

    def getQName(prefixOption: Option[String], localPart: String): QName = {
      if (cacheFilter(prefixOption, localPart))
        cache.get((prefixOption, localPart))
      else
        QName(prefixOption, localPart)
    }

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
   * Thread-local QNameProvider. This class exists because there is precisely one globally used QNameProvider, and by using
   * this thread-local QNameProvider it is possible to make the global QNameProvider configurable per thread again. Also note
   * that the QNameProviders bound to a thread are local to that thread, so they do not suffer from any thread-safety issues
   * (unless a non-thread-safe QName provider instance is shared).
   *
   * Note that each ThreadLocalQNameProvider instance (!) has its own thread-local QName provider. Typically it makes no sense
   * to have more than one ThreadLocalQNameProvider instance in one application. In a Spring application, for example, a single
   * instance of a ThreadLocalQNameProvider can be configured.
   */
  final class ThreadLocalQNameProvider(val qnameProviderCreator: () => QNameProvider) extends QNameProvider {

    private val threadLocalQNameProvider: ThreadLocal[QNameProvider] = new ThreadLocal[QNameProvider] {

      protected override def initialValue(): QNameProvider = qnameProviderCreator()
    }

    /**
     * Returns the QNameProvider instance attached to the current thread.
     */
    def qnameProviderOfCurrentThread: QNameProvider = threadLocalQNameProvider.get

    /**
     * Updates the QNameProvider instance attached to the current thread.
     */
    def setQNameProviderOfCurrentThread(qnameProvider: QNameProvider): Unit = {
      threadLocalQNameProvider.set(qnameProvider)
    }

    def getQName(prefixOption: Option[String], localPart: String): QName =
      qnameProviderOfCurrentThread.getQName(prefixOption, localPart)

    def getQName(prefix: String, localPart: String): QName =
      qnameProviderOfCurrentThread.getQName(prefix, localPart)

    def getUnprefixedQName(localPart: String): QName =
      qnameProviderOfCurrentThread.getUnprefixedQName(localPart)

    def parseQName(s: String): QName = qnameProviderOfCurrentThread.parseQName(s)
  }
}
