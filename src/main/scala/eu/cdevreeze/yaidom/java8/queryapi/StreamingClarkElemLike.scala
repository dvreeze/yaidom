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

package eu.cdevreeze.yaidom.java8.queryapi

import java.util.Optional
import java.util.stream.Stream

import scala.annotation.elidable
import scala.annotation.elidable.ASSERTION
import scala.collection.mutable
import scala.compat.java8.FunctionConverters.asJavaFunction
import scala.compat.java8.FunctionConverters.asJavaPredicate

import eu.cdevreeze.yaidom.XmlStringUtils
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.java8.ElemPathEntryPair
import eu.cdevreeze.yaidom.java8.ResolvedAttr

/**
 * Equivalent of `ClarkElemLike`, but returning Java 8 Streams and taking Java 8 Predicates, to be used
 * in Java code.
 *
 * @author Chris de Vreeze
 */
trait StreamingClarkElemLike[E <: StreamingClarkElemLike[E]] extends StreamingClarkElemApi[E] with StreamingElemLike[E] with StreamingIsNavigable[E] { self: E =>

  final override def findChildElemByPathEntry(entry: Path.Entry): Optional[E] = {
    var sameENameIdx = 0

    val matchingChildElems = findAllChildElems filter (asJavaPredicate({ e =>
      val ename = e.resolvedName
      if (ename == entry.elementName) {
        if (entry.index == sameENameIdx) true
        else {
          sameENameIdx += 1
          false
        }
      } else false
    }))

    assert(matchingChildElems.allMatch(asJavaPredicate(_.resolvedName == entry.elementName)))
    val matchingChildElemOption = matchingChildElems.findFirst
    matchingChildElemOption
  }

  final override def findAllChildElemsWithPathEntries: Stream[ElemPathEntryPair[E]] = {
    val nextEntries = mutable.Map[EName, Int]()

    findAllChildElems map (asJavaFunction({ e =>
      val ename = e.resolvedName
      val entry = Path.Entry(ename, nextEntries.getOrElse(ename, 0))
      nextEntries.put(ename, entry.index + 1)
      new ElemPathEntryPair(e, entry)
    }))
  }

  // HasENameApi methods

  def resolvedName: EName

  def resolvedAttributes: Stream[ResolvedAttr]

  final def localName: String = {
    resolvedName.localPart
  }

  final def attributeOption(expandedName: EName): Optional[String] = {
    val filteredAttrs = resolvedAttributes.filter(asJavaPredicate(attr => attr.ename == expandedName))
    filteredAttrs.map[String](asJavaFunction(_.value)).findFirst
  }

  final def attribute(expandedName: EName): String = {
    attributeOption(expandedName).orElseGet(sys.error(s"Missing attribute $expandedName"))
  }

  final def findAttributeByLocalName(localName: String): Optional[String] = {
    val filteredAttrs = resolvedAttributes.filter(asJavaPredicate(attr => attr.ename.localPart == localName))
    filteredAttrs.map[String](asJavaFunction(_.value)).findFirst
  }

  // HasTextApi

  def text: String

  final def trimmedText: String = {
    text.trim
  }

  final def normalizedText: String = {
    XmlStringUtils.normalizeString(text)
  }
}
