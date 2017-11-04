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

package eu.cdevreeze.yaidom.core

/**
 * Provider of QNames, possibly from a cache of QNames. Typical implementations cache QName instances, to prevent any explosion
 * of equal QName instances, thus unnecessarily increasing the memory footprint.
 *
 * ==Implementation notes==
 *
 * It may seem rather lame that only the global QNameProvider variable can be updated. On the other hand, implicit QNameProvider
 * parameters in many places in the API would change the API quite a bit. These implicit parameters would be implementation
 * details leaking into the API. It was therefore decided not to introduce those implicit parameters, with the exception of only
 * a few places inside implementation code in the library.
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
   * Being a val holding an UpdatableQNameProvider, instead of a var holding any QNameProvider, the identifier
   * `QNameProvider.globalQNameProvider` is stable, and therefore its members can be imported.
   *
   * Be careful: this global instance should be updated only during the "startup phase" of the application.
   * Also be careful to choose an instance that is thread-safe and designed for a "long life" (unlike caching providers
   * that can only grow a lot).
   */
  implicit val globalQNameProvider: UpdatableQNameProvider = new UpdatableQNameProvider(defaultInstance)

  /**
   * "Updatable" QName provider.
   */
  final class UpdatableQNameProvider(@volatile var currentInstance: QNameProvider) extends QNameProvider {

    def become(instance: QNameProvider): Unit = {
      currentInstance = instance
    }

    def reset(): Unit = become(defaultInstance)

    def getQName(prefixOption: Option[String], localPart: String): QName =
      currentInstance.getQName(prefixOption, localPart)

    def getQName(prefix: String, localPart: String): QName =
      currentInstance.getQName(prefix, localPart)

    def getUnprefixedQName(localPart: String): QName =
      currentInstance.getUnprefixedQName(localPart)

    def parseQName(s: String): QName =
      currentInstance.parseQName(s)
  }

  /**
   * Simple QName provider using an immutable cache. It can be long-lived.
   */
  final class QNameProviderUsingImmutableCache(val qnames: Set[QName]) extends QNameProvider {

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
}
