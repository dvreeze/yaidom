/*
 * Copyright 2011 Chris de Vreeze
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

import java.{ util => jutil }
import java.net.URI
import java.rmi.server.UID
import scala.annotation.tailrec
import scala.collection.{ immutable, mutable }
import PrettyPrinting._

/**
 * Immutable XML node. The API is inspired by Anti-XML, but it is less ambitious,
 * and differs in some key respects. Like Anti-XML:
 * <ul>
 * <li>Nodes in this API are truly immutable, and thread-safe, backed by immutable Scala collections.</li>
 * <li>The immutable strictly evaluated nodes have no reference to their parent/ancestor nodes. Hence, for example, you cannot
 * ask a `Node` for its "owning" document. Yet these nodes can be re-used in several XML trees.</li>
 * </ul>
 * Unlike Anti-XML:
 * <ul>
 * <li>This is just a DOM-like API, around immutable nodes and immutable Scala Collections of nodes,
 * without any XPath(-like) support. Despite the absence of selectors like those in Anti-XML, this DOM-like API
 * is still quite expressive, be it somewhat more verbose.</li>
 * <li>This API distinguishes between [[eu.cdevreeze.yaidom.QName]] and [[eu.cdevreeze.yaidom.EName]], making both
 * first-class citizens in the API. Moreover, the concept of a [[eu.cdevreeze.yaidom.Scope]] is a first-class citizen as well.
 * By explicitly modeling `QName`s, `EName`s and `Scope`s, the user of the API is somewhat shielded from some XML quirks.</li>
 * <li>This API is less ambitious. Like said above, XPath(-like) support is absent. So is support for "updates" through
 * zippers. So is "true" equality based on the exact tree.</li>
 * </ul>
 *
 * Nodes are serializable. Serialized Node instances may well be an interesting storage format for parsed XML stored
 * in a database. Of course, this would be a non-standard format. Moreover, as far as queries are concerned, these columns
 * are mere BLOBs (unless using Java Stored Procedures written in Scala). Besides, serialized NodeBuilders tend to be smaller
 * than serialized Nodes.
 *
 * @author Chris de Vreeze
 */
sealed trait Node extends Immutable with Serializable {

  /**
   * Returns a unique ID of the node. It can be used to associate metadata such as `ElemPath`s with elements, for example.
   * The UIDs would then be the Map keys, and the metadata the mapped values.
   *
   * Be careful: if a node is "functionally updated", effectively creating a new node, the old UID still only refers to the old
   * node before the "update".
   */
  def uid: UID

  /**
   * Returns the tree representation String, conforming to the tree representation DSL that creates `NodeBuilder`s.
   * That is, it does not correspond to the tree representation DSL of `Node`s, but of `NodeBuilder`s!
   *
   * There are a couple of advantages of this method compared to some "toXmlString" method which returns the XML string:
   * <ul>
   * <li>The parsed XML tree is made explicit, which makes debugging far easier, especially since method toString delegates to this method</li>
   * <li>The output of method `toTreeRepr` clearly corresponds to a `NodeBuilder`, and can indeed be parsed into one</li>
   * <li>That `toTreeRepr` output is even valid Scala code</li>
   * <li>When parsing the string into a `NodeBuilder`, the following is out of scope: character escaping (for XML), entity resolving, "ignorable" whitespace handling, etc.</li>
   * </ul>
   */
  final def toTreeRepr(parentScope: Scope): String = toTreeReprAsLineSeq(parentScope, 0)(2).mkString

  /** Same as `toTreeRepr(emptyScope)` */
  final def toTreeRepr: String = toTreeRepr(Scope.Empty)

  /**
   * Returns the tree representation string corresponding to this element, that is, `toTreeRepr`.
   *
   * Possibly expensive, especially for large XML trees! Note that the `toString` method is often called implicitly,
   * for example in logging statements. So, if the `toString` method is not used carefully, OutOfMemoryErrors may occur.
   */
  final override def toString: String = toTreeRepr

  /** Returns the tree representation as LineSeq, shifted indent spaces to the right */
  private[yaidom] def toTreeReprAsLineSeq(parentScope: Scope, indent: Int)(indentStep: Int): LineSeq
}

/**
 * Element node. An [[eu.cdevreeze.yaidom.Elem]] contains:
 * <ol>
 * <li>the [[eu.cdevreeze.yaidom.QName]] of the element</li>
 * <li>the attributes of the element, mapping attribute [[eu.cdevreeze.yaidom.QName]]s to String values</li>
 * <li>a [[eu.cdevreeze.yaidom.Scope]] mapping prefixes to namespace URIs</li>
 * <li>an immutable collection of child nodes</li>
 * </ol>
 *
 * The [[eu.cdevreeze.yaidom.Scope]] is absolute, typically containing a lot more than
 * the (implicit) [[eu.cdevreeze.yaidom.Declarations]] of this element.
 *
 * Note that `Elem` instances are immutable, so they do not know about parent nodes. Moreover, `Elem`s must be building blocks
 * for larger (ancestor) `Elem`s, and at the same time they must contain enough context for resolving element and attribute
 * (un)qualified names. Therefore an `Elem` contains a `Scope`, and not a `Declarations` (whereas for `ElemBuilder` it is the
 * other way around). Once an `Elem` tree is complete, the (implicit) `Declarations` of the element are
 * `parentElm.scope.relativize(this.scope)`.
 *
 * Namespace declarations (and undeclarations) are not considered attributes in this API.
 *
 * The API is geared towards data-oriented XML that uses namespaces, and that typically is described in schemas (so that the
 * user of this API knows the structure of the XML being processed). The methods that return an Option say so in their name.
 *
 * No notion of (value) equality has been defined. When thinking about it, it is very hard to come up with any useful
 * notion of equality for representations of XML elements. Think about prefixes, "ignorable whitespace", DTDs and XSDs, etc.
 *
 * Use the constructor with care, because it is easy to use incorrectly (regarding passed Scopes, causing implicit namespace
 * undeclarations). To construct `Elem`s by hand, prefer using an `ElemBuilder`, via method `NodeBuilder.elem`.
 * If `Elem`s are still constructed manually (without using `ElemBuilder`s), consider calling method `notUndeclaringPrefixes`
 * afterwards, thus getting rid of unnecessary (implicit) namespace undeclarations. Typically, however, `Elem`s are constructed
 * by parsing an XML source.
 */
@SerialVersionUID(1L)
final class Elem(
  val qname: QName,
  val attributes: immutable.IndexedSeq[(QName, String)],
  val scope: Scope,
  override val children: immutable.IndexedSeq[Node]) extends Node with UpdatableElemLike[Node, Elem] with HasText { self =>

  require(qname ne null)
  require(attributes ne null)
  require(scope ne null)
  require(children ne null)

  require(attributes.toMap.size == attributes.size, "There are duplicate attribute names: %s".format(attributes))

  override val uid: UID = new UID

  /** The attribute `Scope`, which is the same `Scope` but without the default namespace (which is not used for attributes) */
  val attributeScope: Scope = Scope(scope.map - "")

  /** The `Elem` name as `EName`, obtained by resolving the element `QName` against the `Scope` */
  override val resolvedName: EName =
    scope.resolveQName(qname).getOrElse(sys.error("Element name '%s' should resolve to an EName in scope [%s]".format(qname, scope)))

  /** The attributes as an ordered mapping from `EName`s (instead of `QName`s) to values, obtained by resolving attribute `QName`s against the attribute scope */
  override val resolvedAttributes: immutable.IndexedSeq[(EName, String)] = {
    attributes map { kv =>
      val attName = kv._1
      val attValue = kv._2
      val expandedName = attributeScope.resolveQName(attName).getOrElse(sys.error("Attribute name '%s' should resolve to an EName in scope [%s]".format(attName, attributeScope)))
      (expandedName -> attValue)
    }
  }

  /** Returns the element children */
  override def allChildElems: immutable.IndexedSeq[Elem] = children collect { case e: Elem => e }

  /** Creates a copy, but with (only) the children passed as parameter `newChildren` */
  override def withChildren(newChildren: immutable.IndexedSeq[Node]): Elem = {
    new Elem(qname, attributes, scope, newChildren)
  }

  override def childNodeIndex(childPathEntry: ElemPath.Entry): Int = {
    (0 to childPathEntry.index).foldLeft(-1) {
      case (acc, nextPathEntryIndex) =>
        children.indexWhere({
          case e: Elem if e.resolvedName == childPathEntry.elementName => true
          case n: Node => false
        }, acc + 1)
    }
  }

  override def findChildPathEntry(idx: Int): Option[ElemPath.Entry] = {
    val node = children(idx)

    node match {
      case e: Elem =>
        val cnt = children.take(idx) count {
          case che: Elem if che.resolvedName == e.resolvedName => true
          case chn: Node => false
        }
        Some(ElemPath.Entry(e.resolvedName, cnt))
      case n: Node => None
    }
  }

  /** Returns `withChildren(self.children :+ newChild)`. */
  def plusChild(newChild: Node): Elem = withChildren(self.children :+ newChild)

  /** Creates a copy, but with the attributes passed as parameter `newAttributes` */
  def withAttributes(newAttributes: immutable.IndexedSeq[(QName, String)]): Elem = {
    new Elem(qname, newAttributes, scope, children)
  }

  /**
   * Returns the index of the child with the given `ElemPath` `Entry` (taking this element as parent), or -1 if not found.
   * Must be fast.
   */
  def childIndexOf(pathEntry: ElemPath.Entry): Int = {
    val childNodes = children

    var cnt = 0
    var idx = -1
    while (cnt <= pathEntry.index) {
      val newIdx = childNodes indexWhere ({
        case e: Elem =>
          e.resolvedName == pathEntry.elementName
        case _ => false
      }, idx + 1)

      idx = newIdx
      if (idx < 0) {
        assert(idx == -1)
        return idx
      }
      cnt += 1
    }
    idx
  }

  /** Returns the text children */
  def textChildren: immutable.IndexedSeq[Text] = children collect { case t: Text => t }

  /** Returns the comment children */
  def commentChildren: immutable.IndexedSeq[Comment] = children collect { case c: Comment => c }

  /** Returns the processing instruction children */
  def processingInstructionChildren: immutable.IndexedSeq[ProcessingInstruction] =
    children collect { case pi: ProcessingInstruction => pi }

  /**
   * Returns the concatenation of the texts of text children, including whitespace and CData. Non-text children are ignored.
   * If there are no text children, the empty string is returned.
   */
  override def text: String = {
    val textStrings = textChildren map { t => t.text }
    textStrings.mkString
  }

  /**
   * Returns a `Map` from the element UIDs in the tree with this element as root to the `ElemPath`s relative to this root.
   * This effectively enriches this element and its descendant elements with their `ElemPath`s relative to this element.
   */
  def getElemPaths: Map[UID, ElemPath] = {
    val result = mutable.Map[UID, ElemPath]()

    // Not tail-recursive, but the depth should typically be limited
    def accumulate(elm: Elem, path: ElemPath): Unit = {
      result += (elm.uid -> path)

      val childPaths = elm.allChildElemPathEntries map { entry => path.append(entry) }
      val childElms = elm.allChildElems
      assert(childPaths.size == childElms.size)

      val childElmPathPairs = childElms.zip(childPaths)

      childElmPathPairs foreach { pair => accumulate(pair._1, pair._2) }
    }

    accumulate(self, ElemPath.Root)
    result.toMap
  }

  /**
   * Returns an "equivalent" `Elem` in which the implicit namespace declarations throughout the tree do not contain any
   * prefixed namespace undeclarations, given the passed parent scope.
   */
  def notUndeclaringPrefixes(parentScope: Scope): Elem = {
    val newScope = parentScope.notUndeclaringPrefixes(this.scope)
    assert(this.scope.subScopeOf(newScope))

    // Recursive (non-tail-recursive) call
    val newChildren = children map {
      case e: Elem => e.notUndeclaringPrefixes(newScope)
      case n => n
    }

    Elem(this.qname, this.attributes, newScope, newChildren)
  }

  /** Returns a copy where inter-element whitespace has been removed, throughout the node tree */
  def removeAllInterElementWhitespace: Elem = {
    def isWhitespaceText(n: Node): Boolean = n match {
      case t: Text if t.trimmedText.isEmpty => true
      case _ => false
    }

    def isElem(n: Node): Boolean = n match {
      case e: Elem => true
      case _ => false
    }

    val doStripWhitespace = (children forall (n => isWhitespaceText(n) || isElem(n))) && (!allChildElems.isEmpty)

    // Recursive, but not tail-recursive

    val newChildren = {
      val remainder = if (doStripWhitespace) allChildElems else children

      remainder map {
        case e: Elem => e.removeAllInterElementWhitespace
        case n => n
      }
    }

    self.withChildren(newChildren)
  }

  /**
   * "Prettifies" this Elem. That is, first calls method `removeAllInterElementWhitespace`, and then transforms the result
   * by inserting text nodes with newlines and whitespace for indentation.
   */
  def prettify(indent: Int): Elem = {
    require(indent >= 0, "The indent can not be negative")

    def isWhitespaceText(n: Node): Boolean = n match {
      case t: Text if t.trimmedText.isEmpty => true
      case _ => false
    }

    def isText(n: Node): Boolean = n match {
      case t: Text => true
      case _ => false
    }

    // Not an efficient implementation. It is recursive, but not tail-recursive.

    def prettify(elm: Elem, currentIndent: Int): Elem = {
      val childNodes = elm.children
      val hasElemChild = (childNodes find {
        case e: Elem => true
        case n: Node => false
      }).isDefined
      val doPrettify = (childNodes forall (n => !isText(n))) && (hasElemChild)

      if (doPrettify) {
        val newIndent = currentIndent + indent
        val indentText = Text("\n" + (" " * newIndent), false)
        val endIndentText = Text("\n" + (" " * currentIndent), false)

        val prettifiedChildNodes = childNodes map {
          case e: Elem => prettify(e, newIndent)
          case n => n
        }

        val prefixedPrettifiedChildNodes = prettifiedChildNodes flatMap { n => List(indentText, n) }
        val newChildNodes = prefixedPrettifiedChildNodes :+ endIndentText

        elm.withChildren(newChildNodes)
      } else {
        elm
      }
    }

    prettify(this.removeAllInterElementWhitespace, 0)
  }

  private[yaidom] override def toTreeReprAsLineSeq(parentScope: Scope, indent: Int)(indentStep: Int): LineSeq = {
    val qnameLineSeq: LineSeq = {
      val line = "qname = QName(%s)".format(toStringLiteral(this.qname.toString))
      LineSeq(line)
    }

    val attributesLineSeqOption: Option[LineSeq] =
      if (this.attributes.isEmpty) None else {
        def attributeEntryString(qname: QName, attrValue: String): String = {
          "QName(%s) -> %s".format(toStringLiteral(qname.toString), toStringLiteral(attrValue))
        }

        val attributeEntryStrings = {
          val result = this.attributes map { kv => attributeEntryString(kv._1, kv._2) }
          result.mkString(", ")
        }

        val line = "attributes = Vector(%s)".format(attributeEntryStrings)
        Some(LineSeq(line))
      }

    val declarations: Declarations = parentScope.relativize(self.scope)

    val namespacesLineSeqOption: Option[LineSeq] = {
      if (declarations.map.isEmpty) None else {
        def namespaceEntryString(prefix: String, nsUri: String): String = {
          toStringLiteral(prefix) + " -> " + toStringLiteral(nsUri)
        }

        val namespaceEntryStrings = {
          val result = declarations.map map { kv => namespaceEntryString(kv._1, kv._2) }
          result.mkString(", ")
        }

        val line = "namespaces = Declarations.from(%s)".format(namespaceEntryStrings)
        Some(LineSeq(line))
      }
    }

    val childrenLineSeqOption: Option[LineSeq] =
      if (this.children.isEmpty) None else {
        val firstLine = LineSeq("children = Vector(")
        val contentLines = {
          val groups =
            self.children map { child =>
              child.toTreeReprAsLineSeq(self.scope, indentStep)(indentStep)
            }
          val result = LineSeqSeq(groups: _*).mkLineSeq(",")
          result
        }
        val lastLine = LineSeq(")")

        Some(LineSeqSeq(firstLine, contentLines, lastLine).mkLineSeq)
      }

    val contentParts: Vector[LineSeq] = Vector(Some(qnameLineSeq), attributesLineSeqOption, namespacesLineSeqOption, childrenLineSeqOption).flatten
    val content: LineSeq = LineSeqSeq(contentParts: _*).mkLineSeq(",").shift(indentStep)

    LineSeqSeq(
      LineSeq("elem("),
      content,
      LineSeq(")")).mkLineSeq.shift(indent)
  }
}

@SerialVersionUID(1L)
final case class Text(text: String, isCData: Boolean) extends Node {
  require(text ne null)
  if (isCData) require(!text.containsSlice("]]>"))

  override val uid: UID = new UID

  /** Returns `text.trim`. */
  def trimmedText: String = text.trim

  /** Returns `XmlStringUtils.normalizeString(text)` .*/
  def normalizedText: String = XmlStringUtils.normalizeString(text)

  private[yaidom] override def toTreeReprAsLineSeq(parentScope: Scope, indent: Int)(indentStep: Int): LineSeq = {
    if (isCData) {
      toConcatenatedStringLiterals(text).prepend("cdata(").append(")").shift(indent)
    } else {
      toConcatenatedStringLiterals(text).prepend("text(").append(")").shift(indent)
    }
  }
}

@SerialVersionUID(1L)
final case class ProcessingInstruction(target: String, data: String) extends Node {
  require(target ne null)
  require(data ne null)

  override val uid: UID = new UID

  private[yaidom] override def toTreeReprAsLineSeq(parentScope: Scope, indent: Int)(indentStep: Int): LineSeq = {
    val targetStringLiteral = toStringLiteral(target)
    val dataStringLiteral = toStringLiteral(data)
    LineSeq("processingInstruction(%s, %s)".format(targetStringLiteral, dataStringLiteral)).shift(indent)
  }
}

/**
 * An entity reference. Example:
 * {{{
 * &hello;
 * }}}
 * We obtain this entity reference as follows:
 * {{{
 * EntityRef("hello")
 * }}}
 */
@SerialVersionUID(1L)
final case class EntityRef(entity: String) extends Node {
  require(entity ne null)

  override val uid: UID = new UID

  private[yaidom] override def toTreeReprAsLineSeq(parentScope: Scope, indent: Int)(indentStep: Int): LineSeq = {
    val entityStringLiteral = toStringLiteral(entity)
    LineSeq("entityRef(%s)".format(entityStringLiteral)).shift(indent)
  }
}

@SerialVersionUID(1L)
final case class Comment(text: String) extends Node {
  require(text ne null)

  override val uid: UID = new UID

  private[yaidom] override def toTreeReprAsLineSeq(parentScope: Scope, indent: Int)(indentStep: Int): LineSeq = {
    toConcatenatedStringLiterals(text).prepend("comment(").append(")").shift(indent)
  }
}

object Elem {

  /**
   * Use this constructor with care, because it is easy to use incorrectly (regarding passed Scopes).
   * To construct `Elem`s, prefer using an `ElemBuilder`, via method `NodeBuilder.elem`.
   */
  def apply(
    qname: QName,
    attributes: immutable.IndexedSeq[(QName, String)] = Vector(),
    scope: Scope = Scope.Empty,
    children: immutable.IndexedSeq[Node] = immutable.IndexedSeq()): Elem = new Elem(qname, attributes, scope, children)
}

/**
 * This singleton object contains a DSL to easily create deeply nested Elems.
 * It looks a lot like the DSL for NodeBuilders, using the same method names (so a local import for Node singleton object members may be needed).
 *
 * There is a catch, though. When using this DSL, scopes must be passed throughout the tree. These Scopes had better be
 * the same (or parent element scopes should be subscopes of child element scopes), because otherwise the corresponding XML
 * may contain a lot of namespace undeclarations.
 *
 * Another thing to watch out for is that the "tree representations" conform to the NodeBuilder DSL, not to this one.
 *
 * Hence, choose your poison. The NodeBuilder DSL does have the advantage over this DSL that scopes do not have to be
 * passed around. On the other hand, this Node DSL has the advantage that exceptions due to missing scope data are thrown immediately instead of
 * later (when calling the build method, in the case of the NodeBuilder DSL).
 *
 * Example:
 * {{{
 * import Node._
 *
 * val scope = Scope.from("dbclass" -> "http://www.db-class.org")
 *
 * elem(
 *   qname = QName("dbclass:Magazine"),
 *   attributes = Vector(QName("Month") -> "February", QName("Year") -> "2009"),
 *   scope = scope,
 *   children = Vector(
 *     elem(
 *       qname = QName("dbclass:Title"),
 *       scope = scope,
 *       children = Vector(text("Newsweek")))))
 * }}}
 *
 * The latter expression could also be written as follows:
 * {{{
 * elem(
 *   qname = QName("dbclass:Magazine"),
 *   attributes = Vector(QName("Month") -> "February", QName("Year") -> "2009"),
 *   scope = scope,
 *   children = Vector(
 *     textElem(QName("dbclass:Title"), scope, "Newsweek")))
 * }}}
 */
object Node {

  def document(
    baseUriOption: Option[String] = None,
    documentElement: Elem,
    processingInstructions: immutable.IndexedSeq[ProcessingInstruction] = Vector(),
    comments: immutable.IndexedSeq[Comment] = Vector()): Document = {

    new Document(
      baseUriOption map { uriString => new URI(uriString) },
      documentElement,
      processingInstructions,
      comments)
  }

  def elem(
    qname: QName,
    attributes: immutable.IndexedSeq[(QName, String)] = Vector(),
    scope: Scope,
    children: immutable.IndexedSeq[Node] = Vector()): Elem = {

    new Elem(qname, attributes, scope, children)
  }

  def text(textValue: String): Text = Text(text = textValue, isCData = false)

  def cdata(textValue: String): Text = Text(text = textValue, isCData = true)

  def processingInstruction(target: String, data: String): ProcessingInstruction =
    ProcessingInstruction(target, data)

  def entityRef(entity: String): EntityRef = EntityRef(entity)

  def comment(textValue: String): Comment = Comment(textValue)

  def textElem(qname: QName, scope: Scope, txt: String): Elem = {
    textElem(qname, Vector(), scope, txt)
  }

  def textElem(
    qname: QName,
    attributes: immutable.IndexedSeq[(QName, String)],
    scope: Scope,
    txt: String): Elem = {

    new Elem(qname, attributes, scope, Vector(text(txt)))
  }
}
