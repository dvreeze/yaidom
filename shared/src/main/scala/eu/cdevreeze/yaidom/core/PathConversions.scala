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
 * Conversions between absolute paths and navigation paths.
 *
 * @author Chris de Vreeze
 */
object PathConversions {

  /**
   * Converts a navigation path to an absolute path, adding a root entry with the given element name.
   */
  def convertPathToAbsolutePath(path: Path, rootElementName: EName): AbsolutePath = {
    val nonRootEntries = path.entries.map(e => AbsolutePath.Entry(e.elementName, e.index))
    val rootEntry = AbsolutePath.createRoot(rootElementName).firstEntry

    AbsolutePath(rootEntry +: nonRootEntries)
  }

  /**
   * Converts an absolute path to a navigation path, dropping the root entry of the absolute path.
   */
  def convertAbsolutePathToPath(absolutePath: AbsolutePath): Path = {
    val entries = absolutePath.entries.drop(1).map(e => Path.Entry(e.elementName, e.index))
    Path(entries)
  }
}
