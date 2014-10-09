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

import java.{ io => jio }

/**
 * Thread-local DocumentParser. This class exists because typical JAXP factory objects (DocumentBuilderFactory etc.) are
 * not thread-safe, but still expensive to create. Using this DocumentParser facade backed by a thread local DocumentParser,
 * we can create a ThreadLocalDocumentParser once, and re-use it all the time without having to worry about thread-safety
 * issues.
 *
 * Note that each ThreadLocalDocumentParser instance (!) has its own thread-local document parser. Typically it makes no sense
 * to have more than one ThreadLocalDocumentParser instance in one application. In a Spring application, for example, a single
 * instance of a ThreadLocalDocumentParser can be configured.
 *
 * @author Chris de Vreeze
 */
final class ThreadLocalDocumentParser(val docParserCreator: () => DocumentParser) extends AbstractDocumentParser {

  private val threadLocalDocParser: ThreadLocal[DocumentParser] = new ThreadLocal[DocumentParser] {

    protected override def initialValue(): DocumentParser = docParserCreator()
  }

  /**
   * Returns the DocumentParser instance attached to the current thread.
   */
  def documentParserOfCurrentThread: DocumentParser = threadLocalDocParser.get

  /** Parses the input stream into a yaidom `Document`, using the DocumentParser attached to the current thread. */
  def parse(inputStream: jio.InputStream): Document = documentParserOfCurrentThread.parse(inputStream)
}
