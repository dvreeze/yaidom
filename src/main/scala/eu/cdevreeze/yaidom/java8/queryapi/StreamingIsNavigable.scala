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

package eu.cdevreeze.yaidom.java8.queryapi

import java.util.Optional
import java.util.stream.Collectors
import java.util.stream.Stream

import scala.Vector
import scala.collection.immutable
import scala.compat.java8.OptionConverters.RichOptionForJava8
import scala.compat.java8.OptionConverters.RichOptionalGeneric
import scala.compat.java8.ScalaStreamSupport

import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.java8.ElemPathEntryPair

/**
 * Equivalent of `IsNavigable`, but returning Java 8 Streams and taking Java 8 Predicates, to be used
 * in Java code.
 *
 * @author Chris de Vreeze
 */
trait StreamingIsNavigable[E <: StreamingIsNavigable[E]] extends StreamingIsNavigableApi[E] { self: E =>

  // IsNavigableApi methods

  def findAllChildElemsWithPathEntries: Stream[ElemPathEntryPair[E]]

  def findChildElemByPathEntry(entry: Path.Entry): Optional[E]

  final def getChildElemByPathEntry(entry: Path.Entry): E = {
    findChildElemByPathEntry(entry).asScala.getOrElse(sys.error(s"Expected existing path entry $entry from root $self"))
  }

  final def findElemOrSelfByPath(path: Path): Optional[E] = {
    val reverseAncestryOrSelfOption: Option[java.util.List[E]] =
      findReverseAncestryOrSelfByPath(path).asScala.map(_.collect(Collectors.toList[E]))

    reverseAncestryOrSelfOption.map(raos => raos.get(raos.size - 1)).asJava
  }

  final def getElemOrSelfByPath(path: Path): E = {
    findElemOrSelfByPath(path).asScala.getOrElse(sys.error(s"Expected existing path $path from root $self"))
  }

  final def findReverseAncestryOrSelfByPath(path: Path): Optional[Stream[E]] = {
    // This implementation avoids "functional updates" on the path, and therefore unnecessary object creation

    val entryCount = path.entries.size

    def findReverseAncestryOrSelfByPath(
      currentRoot: E,
      entryIndex: Int,
      reverseAncestry: immutable.IndexedSeq[E]): Option[immutable.IndexedSeq[E]] = {

      assert(entryIndex >= 0 && entryIndex <= entryCount)

      if (entryIndex == entryCount) Some(reverseAncestry :+ currentRoot) else {
        val newRootOption: Option[E] =
          currentRoot.findChildElemByPathEntry(path.entries(entryIndex)).asScala
        // Recursive call. Not tail-recursive, but recursion depth should be limited.
        newRootOption flatMap { newRoot =>
          findReverseAncestryOrSelfByPath(newRoot, entryIndex + 1, reverseAncestry :+ currentRoot)
        }
      }
    }

    findReverseAncestryOrSelfByPath(self, 0, Vector()).map(elms => ScalaStreamSupport.stream(elms)).asJava
  }

  final def getReverseAncestryOrSelfByPath(path: Path): Stream[E] = {
    findReverseAncestryOrSelfByPath(path).asScala.getOrElse(sys.error(s"Expected existing path $path from root $self"))
  }

  /**
   * Turns a Java Optional into a Java Stream, for lack of such a conversion in Java 8.
   */
  private def toStream[A](x: Optional[A]): Stream[A] = {
    ScalaStreamSupport.stream(x.asScala.toSeq)
  }
}
