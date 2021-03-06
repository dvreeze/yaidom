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

package eu.cdevreeze.yaidom.queryapitests.nodeinfo

import java.io.File

import eu.cdevreeze.yaidom.queryapi.BackingNodes
import eu.cdevreeze.yaidom.queryapitests.AbstractSubtypeAwareElemLikeQueryTest
import eu.cdevreeze.yaidom.saxon.SaxonDocument
import net.sf.saxon.s9api.Processor

/**
 * Query test case for an XML dialect using Saxon NodeInfo wrapper elements.
 *
 * @author Chris de Vreeze
 */
class SubtypeAwareElemLikeQueryTest extends AbstractSubtypeAwareElemLikeQueryTest {

  private val processor = new Processor(false)

  protected val wrappedDocumentContent: BackingNodes.Elem = {
    val docBuilder = processor.newDocumentBuilder()

    val docUri = classOf[AbstractSubtypeAwareElemLikeQueryTest].getResource("content.xml").toURI
    val parseResult = docBuilder.build(new File(docUri))
    val doc = SaxonDocument.wrapDocument(parseResult.getUnderlyingNode.getTreeInfo)

    doc.documentElement
  }
}
