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

package eu.cdevreeze.yaidom.utils.saxon

import eu.cdevreeze.yaidom.convert
import eu.cdevreeze.yaidom.saxon.SaxonDocument
import eu.cdevreeze.yaidom.saxon.SaxonElem
import eu.cdevreeze.yaidom.simple
import net.sf.saxon.event.ReceivingContentHandler
import net.sf.saxon.s9api.Processor
import net.sf.saxon.tree.tiny.TinyBuilder

/**
 * Converter from yaidom simple elements and documents to Saxon wrapper elements and documents.
 *
 * We exploit the fact that yaidom simple elements and documents can be excellent builders for
 * Saxon wrapper elements and documents (or builders for other element implementations).
 *
 * Performance of this SAX-based solution is also pretty good. The implementation uses the fact
 * that yaidom can convert a simple Document or Elem to a stream of SAX events on any SAX handler,
 * and Saxon can provide such a SAX handler that happens to create a Saxon tiny tree (if that is the
 * tree model used).
 *
 * @author Chris de Vreeze
 */
final class SimpleElemToSaxonElemConverter(val processor: Processor) {
  require(processor ne null) // scalastyle:off null

  def convertSimpleDocument(doc: simple.Document): SaxonDocument = {
    // See http://saxon-xslt-and-xquery-processor.13853.n7.nabble.com/Constructing-a-tiny-tree-from-SAX-events-td5192.html.
    // The idea is that yaidom can convert a simple Document or Elem to SAX events pushed on any SAX handler, and that the
    // SAX handler used here is a Saxon ReceivingContentHandler, which uses a Saxon TinyBuilder as Saxon Receiver.

    val pipe = processor.getUnderlyingConfiguration.makePipelineConfiguration()

    val builder = new TinyBuilder(pipe)

    if (doc.uriOption.isDefined) {
      builder.setSystemId(doc.uriOption.get.toString)
    }

    val receivingContentHandler = new ReceivingContentHandler()
    receivingContentHandler.setPipelineConfiguration(pipe)
    receivingContentHandler.setReceiver(builder)

    val saxConversions = new convert.YaidomToSaxEventsConversions {}

    saxConversions.convertDocument(doc)(receivingContentHandler)

    val tree = builder.getTree

    if (doc.uriOption.isDefined) {
      require(
        tree.getRootNode.getSystemId == doc.uriOption.get.toString,
        s"Expected document URI '${doc.uriOption.get}' but encountered document URI '${tree.getRootNode.getSystemId}'")
    }

    val saxonDoc = SaxonDocument.wrapDocument(tree)
    saxonDoc
  }

  def convertSimpleElem(elem: simple.Elem): SaxonElem = {
    convertSimpleDocument(simple.Document(None, elem)).documentElement
  }
}
