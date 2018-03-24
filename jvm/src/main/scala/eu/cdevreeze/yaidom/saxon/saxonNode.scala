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

package eu.cdevreeze.yaidom.saxon

import java.net.URI

import scala.Vector
import scala.collection.immutable
import scala.collection.mutable

import eu.cdevreeze.yaidom.XmlStringUtils
import eu.cdevreeze.yaidom.core.Declarations
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.ENameProvider
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.QNameProvider
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.queryapi.BackingElemNodeApi
import eu.cdevreeze.yaidom.queryapi.Nodes
import net.sf.saxon.`type`.Type
import net.sf.saxon.om.AbsolutePath
import net.sf.saxon.om.AxisInfo
import net.sf.saxon.om.NodeInfo
import net.sf.saxon.pattern.NodeKindTest

/**
 * Saxon yaidom BackingElemNodeApi wrapper around Saxon (9.7+) NodeInfo backends.
 *
 * @author Chris de Vreeze
 */
// scalastyle:off null
sealed abstract class SaxonNode(val wrappedNode: NodeInfo) extends Nodes.Node {

  final override def toString: String = wrappedNode.toString

  protected def nodeInfo2EName(nodeInfo: NodeInfo): EName = {
    SaxonNode.nodeInfo2EName(nodeInfo)
  }

  protected def nodeInfo2QName(nodeInfo: NodeInfo): QName = {
    SaxonNode.nodeInfo2QName(nodeInfo)
  }

  final override def equals(obj: Any): Boolean = obj match {
    case other: SaxonNode => this.wrappedNode == other.wrappedNode
    case _                => false
  }

  final override def hashCode: Int = this.wrappedNode.hashCode

  final def children: immutable.IndexedSeq[SaxonNode] = {
    val it = wrappedNode.iterateAxis(AxisInfo.CHILD)

    val nodes = Stream.continually(it.next()).takeWhile(_ ne null).toIndexedSeq

    nodes.flatMap(nodeInfo => SaxonNode.wrapNodeOption(nodeInfo))
  }

  final protected def filterElemsByAxisAndPredicate(axisNumber: Byte, p: SaxonElem => Boolean): immutable.IndexedSeq[SaxonElem] = {
    val it = wrappedNode.iterateAxis(axisNumber, NodeKindTest.ELEMENT)

    val nodeStream = Stream.continually(it.next()).takeWhile(_ ne null)

    nodeStream.map(nodeInfo => SaxonNode.wrapElement(nodeInfo)).filter(p).toIndexedSeq
  }

  final protected def findElemByAxisAndPredicate(axisNumber: Byte, p: SaxonElem => Boolean): Option[SaxonElem] = {
    val it = wrappedNode.iterateAxis(axisNumber, NodeKindTest.ELEMENT)

    val nodeStream = Stream.continually(it.next()).takeWhile(_ ne null)

    nodeStream.map(nodeInfo => SaxonNode.wrapElement(nodeInfo)).find(p)
  }
}

sealed trait SaxonCanBeDocumentChild extends SaxonNode with Nodes.CanBeDocumentChild

// TODO Consider using Saxon Navigator and AbsolutePath classes for navigation, as a faster alternative to yaidom Path navigation.

/**
 * Saxon NodeInfo element wrapper. It is efficient, because of an entirely custom query API implementation tailored to Saxon.
 */
// scalastyle:off number.of.methods
final class SaxonElem(
  override val wrappedNode: NodeInfo) extends SaxonNode(wrappedNode) with Nodes.Elem with SaxonCanBeDocumentChild with BackingElemNodeApi {

  require(wrappedNode ne null)
  require(wrappedNode.getNodeKind == Type.ELEMENT, s"Expected element but got node kind ${wrappedNode.getNodeKind}")

  type ThisElem = SaxonElem

  type ThisNode = SaxonNode

  def thisElem: ThisElem = this

  // ClarkElemApi

  // ClarkElemApi: ElemApi part

  // scalastyle:off method.name
  def \(p: ThisElem => Boolean): immutable.IndexedSeq[ThisElem] = {
    filterChildElems(p)
  }

  // scalastyle:off method.name
  def \\(p: ThisElem => Boolean): immutable.IndexedSeq[ThisElem] = {
    filterElemsOrSelf(p)
  }

  // scalastyle:off method.name
  def \\!(p: ThisElem => Boolean): immutable.IndexedSeq[ThisElem] = {
    findTopmostElemsOrSelf(p)
  }

  def filterChildElems(p: ThisElem => Boolean): immutable.IndexedSeq[ThisElem] = {
    filterElemsByAxisAndPredicate(AxisInfo.CHILD, p)
  }

  def filterElems(p: ThisElem => Boolean): immutable.IndexedSeq[ThisElem] = {
    filterElemsByAxisAndPredicate(AxisInfo.DESCENDANT, p)
  }

  def filterElemsOrSelf(p: ThisElem => Boolean): immutable.IndexedSeq[ThisElem] = {
    filterElemsByAxisAndPredicate(AxisInfo.DESCENDANT_OR_SELF, p)
  }

  def findAllChildElems: immutable.IndexedSeq[ThisElem] = {
    filterChildElems(_ => true)
  }

  def findAllElems: immutable.IndexedSeq[ThisElem] = {
    filterElems(_ => true)
  }

  def findAllElemsOrSelf: immutable.IndexedSeq[ThisElem] = {
    filterElemsOrSelf(_ => true)
  }

  def findChildElem(p: ThisElem => Boolean): Option[ThisElem] = {
    findElemByAxisAndPredicate(AxisInfo.CHILD, p)
  }

  def findElem(p: ThisElem => Boolean): Option[ThisElem] = {
    findElemByAxisAndPredicate(AxisInfo.DESCENDANT, p)
  }

  def findElemOrSelf(p: ThisElem => Boolean): Option[ThisElem] = {
    findElemByAxisAndPredicate(AxisInfo.DESCENDANT_OR_SELF, p)
  }

  def findTopmostElems(p: ThisElem => Boolean): immutable.IndexedSeq[ThisElem] = {
    findAllChildElems flatMap { ch => ch findTopmostElemsOrSelf p }
  }

  def findTopmostElemsOrSelf(p: ThisElem => Boolean): immutable.IndexedSeq[ThisElem] = {
    val result = mutable.ArrayBuffer[ThisElem]()

    // Not tail-recursive, but the depth should typically be limited
    def accumulate(elm: ThisElem): Unit = {
      if (p(elm)) result += elm else {
        elm.findAllChildElems foreach { e => accumulate(e) }
      }
    }

    accumulate(this)
    result.toIndexedSeq
  }

  def getChildElem(p: ThisElem => Boolean): ThisElem = {
    val result = filterChildElems(p)
    require(result.size == 1, s"Expected exactly 1 matching child element, but found ${result.size} of them")
    result.head
  }

  // ClarkElemApi: IsNavigableApi part

  def findAllChildElemsWithPathEntries: immutable.IndexedSeq[(ThisElem, Path.Entry)] = {
    val nextEntries = mutable.Map[EName, Int]()

    findAllChildElems map { e =>
      val ename = e.resolvedName
      val entry = Path.Entry(ename, nextEntries.getOrElse(ename, 0))
      nextEntries.put(ename, entry.index + 1)
      (e, entry)
    }
  }

  def findChildElemByPathEntry(entry: Path.Entry): Option[ThisElem] = {
    val expandedName = entry.elementName

    val childElemOption = filterChildElems(_.resolvedName == expandedName).drop(entry.index).headOption
    childElemOption
  }

  def findElemOrSelfByPath(path: Path): Option[ThisElem] = {
    findReverseAncestryOrSelfByPath(path).map(_.last)
  }

  def findReverseAncestryOrSelfByPath(path: Path): Option[immutable.IndexedSeq[ThisElem]] = {
    // This implementation avoids "functional updates" on the path, and therefore unnecessary object creation

    val entryCount = path.entries.size

    def findReverseAncestryOrSelfByPath(
      currentRoot:     ThisElem,
      entryIndex:      Int,
      reverseAncestry: immutable.IndexedSeq[ThisElem]): Option[immutable.IndexedSeq[ThisElem]] = {

      assert(entryIndex >= 0 && entryIndex <= entryCount)

      if (entryIndex == entryCount) Some(reverseAncestry :+ currentRoot) else {
        val newRootOption: Option[ThisElem] = currentRoot.findChildElemByPathEntry(path.entries(entryIndex))
        // Recursive call. Not tail-recursive, but recursion depth should be limited.
        newRootOption flatMap { newRoot =>
          findReverseAncestryOrSelfByPath(newRoot, entryIndex + 1, reverseAncestry :+ currentRoot)
        }
      }
    }

    findReverseAncestryOrSelfByPath(this, 0, Vector())
  }

  def getChildElemByPathEntry(entry: Path.Entry): ThisElem = {
    findChildElemByPathEntry(entry).getOrElse(sys.error(s"Expected existing path entry $entry from root $this"))
  }

  def getElemOrSelfByPath(path: Path): ThisElem = {
    findElemOrSelfByPath(path).getOrElse(sys.error(s"Expected existing path $path from root $this"))
  }

  def getReverseAncestryOrSelfByPath(path: Path): immutable.IndexedSeq[ThisElem] = {
    findReverseAncestryOrSelfByPath(path).getOrElse(sys.error(s"Expected existing path $path from root $this"))
  }

  // ClarkElemApi: HasENameApi part

  def resolvedName: EName = {
    nodeInfo2EName(wrappedNode)
  }

  def localName: String = {
    resolvedName.localPart
  }

  def resolvedAttributes: immutable.IndexedSeq[(EName, String)] = {
    val it = wrappedNode.iterateAxis(AxisInfo.ATTRIBUTE)

    val nodes = Stream.continually(it.next()).takeWhile(_ ne null).toVector

    nodes map { nodeInfo => nodeInfo2EName(nodeInfo) -> nodeInfo.getStringValue }
  }

  // scalastyle:off method.name
  def \@(expandedName: EName): Option[String] = {
    attributeOption(expandedName)
  }

  def attributeOption(expandedName: EName): Option[String] = {
    resolvedAttributes find { case (en, v) => (en == expandedName) } map (_._2)
  }

  def attribute(expandedName: EName): String = {
    attributeOption(expandedName).getOrElse(sys.error(s"Missing attribute $expandedName"))
  }

  def findAttributeByLocalName(localName: String): Option[String] = {
    resolvedAttributes find { case (en, v) => en.localPart == localName } map (_._2)
  }

  // ClarkElemApi: HasTextApi part

  def text: String = {
    val textStrings = textChildren map { t => t.text }
    textStrings.mkString
  }

  def trimmedText: String = {
    text.trim
  }

  def normalizedText: String = {
    XmlStringUtils.normalizeString(text)
  }

  // ScopedElemApi, except for ClarkElemApi

  // ScopedElemApi: HasQNameApi part

  def qname: QName = {
    nodeInfo2QName(wrappedNode)
  }

  def attributes: immutable.IndexedSeq[(QName, String)] = {
    val it = wrappedNode.iterateAxis(AxisInfo.ATTRIBUTE)

    val nodes = Stream.continually(it.next()).takeWhile(_ ne null).toVector

    nodes map { nodeInfo => nodeInfo2QName(nodeInfo) -> nodeInfo.getStringValue }
  }

  // ScopedElemApi: HasScope part

  def scope: Scope = {
    val it = wrappedNode.iterateAxis(AxisInfo.NAMESPACE)

    val nodes = Stream.continually(it.next()).takeWhile(_ ne null).toVector

    val resultMap = {
      val result =
        nodes map { nodeInfo =>
          // Not very transparent: prefix is "display name" and namespace URI is "string value"
          val prefix = nodeInfo.getDisplayName
          val nsUri = nodeInfo.getStringValue
          (prefix -> nsUri)
        }
      result.toMap
    }

    Scope.from(resultMap - "xml")
  }

  // ScopedElemApi: own methods

  def textAsQName: QName = {
    QName(text.trim)
  }

  def textAsResolvedQName: EName = {
    scope.resolveQNameOption(textAsQName).getOrElse(
      sys.error(s"Could not resolve QName-valued element text $textAsQName, given scope [${scope}]"))
  }

  def attributeAsQNameOption(expandedName: EName): Option[QName] = {
    attributeOption(expandedName).map(v => QName(v.trim))
  }

  def attributeAsQName(expandedName: EName): QName = {
    attributeAsQNameOption(expandedName).getOrElse(
      sys.error(s"Missing QName-valued attribute $expandedName"))
  }

  def attributeAsResolvedQNameOption(expandedName: EName): Option[EName] = {
    attributeAsQNameOption(expandedName) map { qn =>
      scope.resolveQNameOption(qn).getOrElse(
        sys.error(s"Could not resolve QName-valued attribute value $qn, given scope [${scope}]"))
    }
  }

  def attributeAsResolvedQName(expandedName: EName): EName = {
    attributeAsResolvedQNameOption(expandedName).getOrElse(
      sys.error(s"Missing QName-valued attribute $expandedName"))
  }

  // Other functions, from IndexedClarkElemApi, IndexedScopedElemApi, HasParentApi etc.

  def baseUriOption: Option[URI] = {
    Option(wrappedNode.getBaseURI).map(u => URI.create(u))
  }

  def baseUri: URI = {
    baseUriOption.getOrElse(URI.create(""))
  }

  def docUriOption: Option[URI] = {
    Option(wrappedNode.getSystemId).map(u => URI.create(u))
  }

  def docUri: URI = {
    docUriOption.getOrElse(URI.create(""))
  }

  def parentBaseUriOption: Option[URI] = {
    parentOption.flatMap(_.baseUriOption).orElse(docUriOption)
  }

  def path: Path = {
    // Not too slow, but not very fast either

    val pathEntries: immutable.IndexedSeq[Path.Entry] = reverseAncestryOrSelf.tail map { elm =>
      val saxonAbsolutePathString = AbsolutePath.pathToNode(elm.wrappedNode).getPathUsingUris

      val lastSquareStartBracketIdx = saxonAbsolutePathString.lastIndexOf('[')
      require(lastSquareStartBracketIdx > 0, s"Found no '[' in '${saxonAbsolutePathString}'")
      require(saxonAbsolutePathString.endsWith("]"), s"Found no ']' at the end of '${saxonAbsolutePathString}'")

      val elementIndex =
        saxonAbsolutePathString.substring(lastSquareStartBracketIdx + 1, saxonAbsolutePathString.length - 1).toInt - 1

      Path.Entry(elm.resolvedName, elementIndex)
    }

    Path(pathEntries)
  }

  def rootElem: ThisElem = {
    val optPe = parentOption
    // Recursive call
    optPe.map(_.rootElem).getOrElse(this)
  }

  def reverseAncestryOrSelf: immutable.IndexedSeq[ThisElem] = {
    ancestorsOrSelf.reverse
  }

  def reverseAncestry: immutable.IndexedSeq[ThisElem] = {
    ancestors.reverse
  }

  def reverseAncestryOrSelfENames: immutable.IndexedSeq[EName] = {
    reverseAncestryOrSelf.map(_.resolvedName)
  }

  def reverseAncestryENames: immutable.IndexedSeq[EName] = {
    reverseAncestryOrSelfENames.init
  }

  def namespaces: Declarations = {
    val parentScope = parentOption map { _.scope } getOrElse (Scope.Empty)
    parentScope.relativize(scope)
  }

  def parentOption: Option[ThisElem] = {
    findElemByAxisAndPredicate(AxisInfo.PARENT, _ => true)
  }

  def parent: ThisElem = {
    parentOption.getOrElse(sys.error("There is no parent element"))
  }

  def ancestors: immutable.IndexedSeq[ThisElem] = {
    ancestorsOrSelf.tail
  }

  def ancestorsOrSelf: immutable.IndexedSeq[ThisElem] = {
    this +: (parentOption.toIndexedSeq flatMap ((e: ThisElem) => e.ancestorsOrSelf))
  }

  def findAncestor(p: ThisElem => Boolean): Option[ThisElem] = {
    parentOption flatMap { e => e.findAncestorOrSelf(p) }
  }

  def findAncestorOrSelf(p: ThisElem => Boolean): Option[ThisElem] = {
    if (p(this)) Some(this) else {
      parentOption.flatMap(pe => pe.findAncestorOrSelf(p))
    }
  }

  // Extra methods

  /** Returns the text children */
  def textChildren: immutable.IndexedSeq[SaxonText] = children collect { case t: SaxonText => t }

  /** Returns the comment children */
  def commentChildren: immutable.IndexedSeq[SaxonComment] = children collect { case c: SaxonComment => c }
}

final class SaxonText(override val wrappedNode: NodeInfo) extends SaxonNode(wrappedNode) with Nodes.Text {
  require(wrappedNode ne null)
  require(
    wrappedNode.getNodeKind == Type.TEXT || wrappedNode.getNodeKind == Type.WHITESPACE_TEXT,
    s"Expected text (or whitespace text) but got node kind ${wrappedNode.getNodeKind}")

  def text: String = wrappedNode.getStringValue

  def trimmedText: String = text.trim

  def normalizedText: String = XmlStringUtils.normalizeString(text)
}

final class SaxonProcessingInstruction(override val wrappedNode: NodeInfo) extends SaxonNode(wrappedNode)
  with SaxonCanBeDocumentChild with Nodes.ProcessingInstruction {

  require(wrappedNode ne null)
  require(wrappedNode.getNodeKind == Type.PROCESSING_INSTRUCTION, s"Expected processing instruction but got node kind ${wrappedNode.getNodeKind}")

  def target: String = wrappedNode.getDisplayName // ???

  def data: String = wrappedNode.getStringValue // ???
}

final class SaxonComment(override val wrappedNode: NodeInfo) extends SaxonNode(wrappedNode) with SaxonCanBeDocumentChild with Nodes.Comment {
  require(wrappedNode ne null)
  require(wrappedNode.getNodeKind == Type.COMMENT, s"Expected comment but got node kind ${wrappedNode.getNodeKind}")

  def text: String = wrappedNode.getStringValue
}

object SaxonNode {

  import ENameProvider.globalENameProvider._
  import QNameProvider.globalQNameProvider._

  def wrapNodeOption(node: NodeInfo): Option[SaxonNode] = {
    node.getNodeKind match {
      case Type.ELEMENT                => Some(new SaxonElem(node))
      case Type.TEXT                   => Some(new SaxonText(node))
      case Type.WHITESPACE_TEXT        => Some(new SaxonText(node))
      case Type.PROCESSING_INSTRUCTION => Some(new SaxonProcessingInstruction(node))
      case Type.COMMENT                => Some(new SaxonComment(node))
      case _                           => None
    }
  }

  def wrapElement(elm: NodeInfo): SaxonElem = new SaxonElem(elm)

  def nodeInfo2EName(nodeInfo: NodeInfo): EName = {
    val ns: String = nodeInfo.getURI
    val nsOption: Option[String] = if (ns == "") None else Some(ns)
    getEName(nsOption, nodeInfo.getLocalPart)
  }

  def nodeInfo2QName(nodeInfo: NodeInfo): QName = {
    val pref: String = nodeInfo.getPrefix
    val prefOption: Option[String] = if (pref == "") None else Some(pref)
    getQName(prefOption, nodeInfo.getLocalPart)
  }
}
