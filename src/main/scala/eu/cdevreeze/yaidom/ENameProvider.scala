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
 * Provider of ENames, possibly from a cache of ENames. Typical implementations cache EName instances, to prevent any explosion
 * of equal EName instances, thus unnecessarily increasing the memory footprint.
 *
 * ==Implementation notes==
 *
 * The chosen implementation strategy for (globally) setting the ENameProvider is as follows:
 * <ul>
 * <li>The public API remains backward compatible as much as possible, and possibly implicit parameters are introduced for
 * implicit EName providers.</li>
 * <li>Still, implicit parameters are used in moderation, and not in many places throughout the API. This reduces the risk
 * of polluting the API, and of many future deprecation warnings. Moreover, ENameProviders are implementation details.</li>
 * <li>The query API should be stable, and therefore unaffected by (possibly implicit) ENameProviders.</li>
 * <li>There is one implicit ENameProvider, that can be updated as the globally used ENameProvider.</li>
 * </ul>
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
   * Trivial, non-caching, EName provider.
   */
  final class TrivialENameProvider extends ENameProvider {

    def getEName(namespaceUriOption: Option[String], localPart: String): EName = EName(namespaceUriOption, localPart)

    def getEName(namespaceUri: String, localPart: String): EName = EName(namespaceUri, localPart)

    def getNoNsEName(localPart: String): EName = getEName(None, localPart)

    def parseEName(s: String): EName = EName.parse(s)
  }

  val defaultInstance: ENameProvider = new TrivialENameProvider

  /**
   * The implicit global ENameProvider is by default a "trivial" ENameProvider, but can be updated.
   */
  @volatile implicit var globalMutableInstance: ENameProvider = defaultInstance

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
  final class SimpleCachingENameProvider(val cacheFilter: (Option[String], String) => Boolean) extends ENameProvider {

    def this() = this((namespaceUriOption, localPart) => true)

    private val cache = new SimpleCache[(Option[String], String), EName] {

      protected def convertKeyToValue(key: (Option[String], String)): EName = EName(key._1, key._2)
    }

    def getEName(namespaceUriOption: Option[String], localPart: String): EName = {
      if (cacheFilter(namespaceUriOption, localPart))
        cache.get((namespaceUriOption, localPart))
      else
        EName(namespaceUriOption, localPart)
    }

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
   * Thread-local ENameProvider. This class exists because there is precisely one globally used ENameProvider, and by using
   * this thread-local ENameProvider it is possible to make the global ENameProvider configurable per thread again. Also note
   * that the ENameProviders bound to a thread are local to that thread, so they do not suffer from any thread-safety issues
   * (unless a non-thread-safe EName provider instance is shared).
   *
   * Note that each ThreadLocalENameProvider instance (!) has its own thread-local EName provider. Typically it makes no sense
   * to have more than one ThreadLocalENameProvider instance in one application. In a Spring application, for example, a single
   * instance of a ThreadLocalENameProvider can be configured.
   */
  final class ThreadLocalENameProvider(val enameProviderCreator: () => ENameProvider) extends ENameProvider {

    private val threadLocalENameProvider: ThreadLocal[ENameProvider] = new ThreadLocal[ENameProvider] {

      protected override def initialValue(): ENameProvider = enameProviderCreator()
    }

    /**
     * Returns the ENameProvider instance attached to the current thread.
     */
    def enameProviderOfCurrentThread: ENameProvider = threadLocalENameProvider.get

    /**
     * Updates the ENameProvider instance attached to the current thread.
     */
    def setENameProviderOfCurrentThread(enameProvider: ENameProvider): Unit = {
      threadLocalENameProvider.set(enameProvider)
    }

    def getEName(namespaceUriOption: Option[String], localPart: String): EName =
      enameProviderOfCurrentThread.getEName(namespaceUriOption, localPart)

    def getEName(namespaceUri: String, localPart: String): EName =
      enameProviderOfCurrentThread.getEName(namespaceUri, localPart)

    def getNoNsEName(localPart: String): EName =
      enameProviderOfCurrentThread.getNoNsEName(localPart)

    def parseEName(s: String): EName =
      enameProviderOfCurrentThread.parseEName(s)
  }
}
