/*
 * Copyright 2011-2014 Chris de Vreeze
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
package parse

import org.xml.sax.{ ContentHandler, Locator }
import org.xml.sax.helpers.DefaultHandler
import scala.collection.immutable
import net.jcip.annotations.NotThreadSafe
import eu.cdevreeze.yaidom._

/**
 * Mixin extending `DefaultHandler` that contains a `Locator`. Typically this `Locator` is used by an `ErrorHandler` mixed in after this trait.
 *
 * @author Chris de Vreeze
 */
@NotThreadSafe
trait SaxHandlerWithLocator extends DefaultHandler {

  var locator: Locator = _

  final override def setDocumentLocator(locator: Locator): Unit = {
    this.locator = locator
  }
}
