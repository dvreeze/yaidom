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
 * Provider of ENames, possibly from a cache of ENames. Typical implementations cache EName instances, to prevent any explosion
 * of equal EName instances, thus unnecessarily increasing the memory footprint.
 *
 * ==Implementation notes==
 *
 * It may seem rather lame that only the global ENameProvider variable can be updated. On the other hand, implicit ENameProvider
 * parameters in many places in the API would change the API quite a bit. These implicit parameters would be implementation
 * details leaking into the API. It was therefore decided not to introduce those implicit parameters, with the exception of only
 * a few places inside implementation code in the library.
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
   * Being a val holding an UpdatableENameProvider, instead of a var holding any ENameProvider, the identifier
   * `ENameProvider.globalENameProvider` is stable, and therefore its members can be imported.
   *
   * Be careful: this global instance should be updated only during the "startup phase" of the application.
   * Also be careful to choose an instance that is thread-safe and designed for a "long life" (unlike caching providers
   * that can only grow a lot).
   */
  implicit val globalENameProvider: UpdatableENameProvider = new UpdatableENameProvider(defaultInstance)

  /**
   * "Updatable" EName provider.
   */
  final class UpdatableENameProvider(@volatile var currentInstance: ENameProvider) extends ENameProvider {

    def become(instance: ENameProvider): Unit = {
      currentInstance = instance
    }

    def reset(): Unit = become(defaultInstance)

    def getEName(namespaceUriOption: Option[String], localPart: String): EName =
      currentInstance.getEName(namespaceUriOption, localPart)

    def getEName(namespaceUri: String, localPart: String): EName =
      currentInstance.getEName(namespaceUri, localPart)

    def getNoNsEName(localPart: String): EName =
      currentInstance.getNoNsEName(localPart)

    def parseEName(s: String): EName = currentInstance.parseEName(s)
  }

  /**
   * Simple EName provider using an immutable cache. It can be long-lived.
   */
  final class ENameProviderUsingImmutableCache(val enames: Set[EName]) extends ENameProvider {

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
}
