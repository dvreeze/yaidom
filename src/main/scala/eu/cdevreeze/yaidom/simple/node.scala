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

package eu.cdevreeze.yaidom.simple

import java.io.ObjectStreamException

import scala.Vector
import scala.collection.immutable
import scala.collection.mutable

import eu.cdevreeze.yaidom.PrettyPrinting.LineSeq
import eu.cdevreeze.yaidom.PrettyPrinting.LineSeqSeq
import eu.cdevreeze.yaidom.PrettyPrinting.toConcatenatedStringLiterals
import eu.cdevreeze.yaidom.PrettyPrinting.toStringLiteral
import eu.cdevreeze.yaidom.XmlStringUtils
import eu.cdevreeze.yaidom.core.Declarations
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.queryapi.ScopedElemLike
import eu.cdevreeze.yaidom.queryapi.TransformableElemLike
import eu.cdevreeze.yaidom.queryapi.UpdatableElemLike

/**
 * Immutable XML Node. It is the default XML node type in yaidom. There are subclasses for different types of nodes,
 * such as elements, text nodes, comments, entity references and processing instructions. See [[eu.cdevreeze.yaidom.simple.Elem]]
 * for the default element type in yaidom.
 *
 * @author Chris de Vreeze
 */
sealed trait Node extends Immutable with Serializable {

  /**
   * Returns the tree representation String, conforming to the tree representation DSL that creates `NodeBuilder`s.
   * That is, it does not correspond to the tree representation DSL of `Node`s, but of `NodeBuilder`s!
   *
   * There are a couple of advantages of this method compared to some "toXmlString" method which returns the XML string:
   * <ul>
   * <li>The parsed XML tree is made explicit, which makes debugging far easier, especially since method toString invokes this method</li>
   * <li>The output of method `toTreeRepr` clearly corresponds to a `NodeBuilder`, and can indeed be parsed into one</li>
   * <li>That `toTreeRepr` output is even valid Scala code</li>
   * <li>When parsing the string into a `NodeBuilder`, the following is out of scope: character escaping (for XML), entity resolving, "ignorable" whitespace handling, etc.</li>
   * </ul>
   */
  final def toTreeRepr(parentScope: Scope): String = {
    val sb = new StringBuilder
    toTreeReprAsLineSeq(parentScope, 0)(2).addToStringBuilder(sb)
    sb.toString
  }

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
 * <em>Immutable</em>, thread-safe <em>element node</em>. It is the <em>default</em> element implementation in yaidom. As the default
 * element implementation among several alternative element implementations, it strikes a balance between loss-less roundtripping
 * and composability.
 *
 * The parsers and serializers in packages [[eu.cdevreeze.yaidom.parse]] and [[eu.cdevreeze.yaidom.print]] return and take
 * these default elements (or the corresponding `Document` instances), respectively.
 *
 * As for its <em>query API</em>, class [[eu.cdevreeze.yaidom.simple.Elem]] is among the most powerful element implementations offered
 * by yaidom. These elements offer all of the [[eu.cdevreeze.yaidom.queryapi.ElemApi]], [[eu.cdevreeze.yaidom.queryapi.UpdatableElemApi]] and
 * [[eu.cdevreeze.yaidom.queryapi.TransformableElemApi]] query APIs, and more.
 *
 * '''See the documentation of the mixed-in query API traits for more details on the uniform query API offered by this class.'''
 *
 * The following example illustrates the use of the yaidom uniform query API in combination with some Elem-specific methods.
 * In this XML scripting example the namespace prefix "xsd" is replaced by prefix "xs", including those in QName-valued attributes. The trivial
 * XML file of this example is the following XML Schema:
 * {{{
 * <xsd:schema xmlns:xsd="http://www.w3.org/2001/XMLSchema" targetNamespace="http://book" elementFormDefault="qualified">
 *   <xsd:element name="book">
 *     <xsd:complexType>
 *       <xsd:sequence>
 *         <xsd:element name="isbn" type="xsd:string" />
 *         <xsd:element name="title" type="xsd:string" />
 *         <xsd:element name="authors" type="xsd:string" />
 *       </xsd:sequence>
 *     </xsd:complexType>
 *   </xsd:element>
 * </xsd:schema>
 * }}}
 *
 * The edit action can be performed on this `schemaElem` as follows, starting with some checks:
 * {{{
 * // All descendant-or-self elements have the same Scope, mapping only prefix "xsd".
 * require(schemaElem.findAllElemsOrSelf.map(_.scope).distinct == List(Scope.from("xsd" -> "http://www.w3.org/2001/XMLSchema")))
 *
 * // All descendant-or-self elements have a QName with prefix "xsd".
 * require(schemaElem.findAllElemsOrSelf.map(_.qname.prefixOption).distinct == List(Some("xsd")))
 *
 * // All descendant-or-self elements have unprefixed attributes only.
 * require(schemaElem.findAllElemsOrSelf.flatMap(_.attributes.toMap.keySet.map(_.prefixOption)).distinct == List(None))
 *
 * // All descendant-or-self elements with "type" attributes contain only QNames with prefix "xsd" in the values of those attributes.
 * require(schemaElem.filterElemsOrSelf(e => (e \@ EName("type")).isDefined).forall(e => e.attributeAsQName(EName("type")).prefixOption == Some("xsd")))
 *
 * // Replaces prefix "xsd" by "xs" throughout the element tree, including in "type" attributes.
 * val editedSchemaElem = schemaElem transformElemsOrSelf { elem =>
 *   val newScope = (elem.scope -- Set("xsd")) ++ Scope.from("xs" -> "http://www.w3.org/2001/XMLSchema")
 *   val newQName = QName("xs", elem.qname.localPart)
 *   val newTypeAttrOption = elem.attributeAsQNameOption(EName("type")).map(attr => QName("xs", attr.localPart).toString)
 *
 *   elem.copy(qname = newQName, scope = newScope).plusAttributeOption(QName("type"), newTypeAttrOption)
 * }
 * }}}
 * Note that besides the uniform query API, this example uses some `Elem`-specific methods, such as `attributeAsQName`, `copy` and
 * `plusAttributeOption`.
 *
 * Class `Elem` is immutable, and (should be) thread-safe. Hence, Elems do not know about their parent element, if any.
 *
 * An Elem has the following state:
 * <ul>
 * <li>the [[eu.cdevreeze.yaidom.core.QName]] of the element</li>
 * <li>the attributes of the element, mapping attribute [[eu.cdevreeze.yaidom.core.QName]]s to String values</li>
 * <li>a [[eu.cdevreeze.yaidom.core.Scope]] mapping prefixes to namespace URIs</li>
 * <li>an immutable collection of child nodes</li>
 * </ul>
 * Note that namespace declarations are not considered to be attributes in `Elem`, just like in the rest of yaidom.
 * Elem construction is unsuccessful if the element name and/or some attribute names cannot be resolved using the `Scope` of the
 * element (ignoring the default namespace, if any, for attributes). As can be seen from the above-mentioned state,
 * namespaces are first-class citizens.
 *
 * Elems can (relatively easily) be constructed manually in a bottom-up manner. Yet care must be taken to give the element and its
 * descendants the correct `Scope`. Otherwise it is easy to introduce (prefixed) namespace undeclarations, which are not
 * allowed in XML 1.0. The underlying issue is that <em>functional</em> Elem trees are created in a <em>bottom-up</em> manner,
 * whereas namespace scoping works in a <em>top-down</em> manner. This is not a big issue in practice, since manual Elem creation
 * is rather rare, and it is always possible to call method `notUndeclaringPrefixes` afterwards. An alternative method to create
 * element trees by hand uses class [[eu.cdevreeze.yaidom.simple.ElemBuilder]]. A manually created `ElemBuilder` can be converted to
 * an `Elem` by calling method `build`.
 *
 * <em>Round-tripping</em> (parsing and serializing) is not entirely loss-less, but (in spite of the good composability and rather small
 * state) not much is lost. Comments, processing instructions and entity references are retained. Attribute order is retained,
 * although according to the XML InfoSet this order is irrelevant. Namespace declaration order is not necessarily retained,
 * however. Superfluous namespace declarations are also lost. (That is because namespace declarations are not explicitly
 * stored in Elems, but are implicit, viz. `parentElem.scope.relativize(this.scope)`). The short versus long form of an empty
 * element is also not remembered.
 *
 * <em>Equality</em> has not been defined for class `Elem` (that is, it is reference equality). There is no clear sensible notion of equality
 * for XML trees at the abstraction level of `Elem`. For example, think about prefixes, "ignorable whitespace", DTDs and XSDs, etc.
 */
@SerialVersionUID(1L)
final class Elem(
  val qname: QName,
  val attributes: immutable.IndexedSeq[(QName, String)],
  val scope: Scope,
  override val children: immutable.IndexedSeq[Node]) extends Node with ScopedElemLike[Elem] with UpdatableElemLike[Node, Elem] with TransformableElemLike[Node, Elem] { self =>

  require(qname ne null)
  require(attributes ne null)
  require(scope ne null)
  require(children ne null)

  require(attributes.toMap.size == attributes.size, s"There are duplicate attribute names: $attributes")

  @throws(classOf[java.io.ObjectStreamException])
  private[yaidom] def writeReplace(): Any = new Elem.ElemSerializationProxy(qname, attributes, scope, children)

  /** The `Elem` name as `EName`, obtained by resolving the element `QName` against the `Scope` */
  override val resolvedName: EName =
    scope.resolveQNameOption(qname).getOrElse(sys.error(s"Element name '${qname}' should resolve to an EName in scope [${scope}]"))

  /** The attributes as an ordered mapping from `EName`s (instead of `QName`s) to values, obtained by resolving attribute `QName`s against the attribute scope */
  override val resolvedAttributes: immutable.IndexedSeq[(EName, String)] = {
    val attrScope = attributeScope

    attributes map { kv =>
      val attName = kv._1
      val attValue = kv._2
      val expandedName =
        attrScope.resolveQNameOption(attName).getOrElse(sys.error(s"Attribute name '${attName}' should resolve to an EName in scope [${attrScope}]"))
      (expandedName -> attValue)
    }
  }

  override def childNodeIndex(childPathEntry: Path.Entry): Int = {
    val filteredChildrenWithChildIndex = children.toStream.zipWithIndex filter {
      case (e: Elem, idx) if e.resolvedName == childPathEntry.elementName => true
      case _ => false
    }

    val childWithIndexOption = filteredChildrenWithChildIndex.drop(childPathEntry.index).headOption
    val result = childWithIndexOption collect { case (e: Elem, idx) => (e, idx) }
    assert(result.forall(_._1.resolvedName == childPathEntry.elementName))
    result.map(_._2).getOrElse(-1)
  }

  /** The attribute `Scope`, which is the same `Scope` but without the default namespace (which is not used for attributes) */
  def attributeScope: Scope = scope.withoutDefaultNamespace

  /** Returns the element children */
  override def findAllChildElems: immutable.IndexedSeq[Elem] = children collect { case e: Elem => e }

  /** Creates a copy, but with (only) the children passed as parameter `newChildren` */
  override def withChildren(newChildren: immutable.IndexedSeq[Node]): Elem = {
    copy(children = newChildren)
  }

  override def findChildElemByPathEntry(entry: Path.Entry): Option[Elem] = {
    val result =
      childNodeIndex(entry) match {
        case -1 => None
        case idx => Some(children(idx).asInstanceOf[Elem])
      }
    assert(result.forall(_.resolvedName == entry.elementName))
    result
  }

  override def transformChildElems(f: Elem => Elem): Elem = {
    val newChildren =
      children map {
        case e: Elem => f(e)
        case n: Node => n
      }
    withChildren(newChildren)
  }

  override def transformChildElemsToNodeSeq(f: Elem => immutable.IndexedSeq[Node]): Elem = {
    val newChildren =
      children flatMap {
        case e: Elem => f(e)
        case n: Node => Vector(n)
      }
    withChildren(newChildren)
  }

  /**
   * Returns all child elements with `Path` entries, in the correct order.
   * This method is also needed for fast recursive construction of docaware/indexed Elems.
   */
  def findAllChildElemsWithPathEntries: immutable.IndexedSeq[(Elem, Path.Entry)] = {
    childNodeIndexesByPathEntries.toVector.sortBy(_._2) map {
      case (entry, idx) =>
        (children(idx).asInstanceOf[Elem], entry)
    }
  }

  /**
   * Creates a copy, altered with the explicitly passed parameters (for qname, attributes, scope and children).
   */
  def copy(
    qname: QName = this.qname,
    attributes: immutable.IndexedSeq[(QName, String)] = this.attributes,
    scope: Scope = this.scope,
    children: immutable.IndexedSeq[Node] = this.children): Elem = {

    new Elem(qname, attributes, scope, children)
  }

  /** Creates a copy, but with the attributes passed as parameter `newAttributes` */
  def withAttributes(newAttributes: immutable.IndexedSeq[(QName, String)]): Elem = {
    copy(attributes = newAttributes)
  }

  /**
   * Functionally adds or updates the given attribute.
   *
   * More precisely, if an attribute with the same name exists at position `idx` (0-based),
   * `withAttributes(attributes.updated(idx, (attributeName -> attributeValue)))` is returned.
   * Otherwise, `withAttributes(attributes :+ (attributeName -> attributeValue))` is returned.
   */
  def plusAttribute(attributeName: QName, attributeValue: String): Elem = {
    val idx = attributes indexWhere { case (attr, value) => attr == attributeName }

    if (idx < 0) withAttributes(attributes :+ (attributeName -> attributeValue))
    else withAttributes(attributes.updated(idx, (attributeName -> attributeValue)))
  }

  /**
   * Functionally adds or updates the given attribute, if a value is given.
   * That is, returns `if (attributeValueOption.isEmpty) self else plusAttribute(attributeName, attributeValueOption.get)`.
   */
  def plusAttributeOption(attributeName: QName, attributeValueOption: Option[String]): Elem = {
    if (attributeValueOption.isEmpty) self else plusAttribute(attributeName, attributeValueOption.get)
  }

  /**
   * Functionally removes the given attribute, if present.
   *
   * More precisely, returns `withAttributes(self.attributes filterNot (_._1 == attributeName))`.
   */
  def minusAttribute(attributeName: QName): Elem = {
    val newAttributes = self.attributes filterNot { case (attrName, attrValue) => attrName == attributeName }
    withAttributes(newAttributes)
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
   * Returns an "equivalent" `Elem` in which the implicit namespace declarations throughout the tree do not contain any
   * prefixed namespace undeclarations, given the passed parent Scope.
   *
   * This method could be defined by recursion as follows:
   * {{{
   * val newScope = parentScope.withoutDefaultNamespace ++ this.scope
   * this.copy(scope = newScope) transformChildElems { e => e.notUndeclaringPrefixes(newScope) }
   * }}}
   *
   * It can be proven by structural induction that for each `parentScope` the XML remains the "same":
   * {{{
   * resolved.Elem(this.notUndeclaringPrefixes(parentScope)) == resolved.Elem(this)
   * }}}
   * Moreover, there are no prefixed namespace undeclarations:
   * {{{
   * NodeBuilder.fromElem(this)(parentScope).findAllElemsOrSelf.
   *   map(_.namespaces.withoutDefaultNamespace.retainingUndeclarations).toSet ==
   *     Set(Declarations.Empty)
   * }}}
   *
   * Note that XML 1.0 does not allow prefix undeclarations, and this method helps avoid them, while preserving the "same" XML.
   * So, when manipulating an Elem tree, calling `notUndeclaringPrefixes(Scope.Empty)` on the document element results in
   * an equivalent Elem that has no prefixed namespace undeclarations anywhere in the tree.
   */
  def notUndeclaringPrefixes(parentScope: Scope): Elem = {
    val newScope = parentScope.withoutDefaultNamespace ++ this.scope
    assert(this.scope.subScopeOf(newScope))
    assert(this.scope.defaultNamespaceOption == newScope.defaultNamespaceOption)

    // Recursive (non-tail-recursive) calls
    this.copy(scope = newScope) transformChildElems { e => e.notUndeclaringPrefixes(newScope) }
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

    val doStripWhitespace = (children forall (n => isWhitespaceText(n) || isElem(n))) && (!findAllChildElems.isEmpty)

    // Recursive, but not tail-recursive

    val newChildren = {
      val remainder = if (doStripWhitespace) findAllChildElems else children

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
  def prettify(indent: Int, useTab: Boolean = false, newLine: String = "\n"): Elem = {
    require(indent >= 0, "The indent can not be negative")
    require(
      newLine.size >= 1 && newLine.size <= 2 && (newLine.forall(c => c == '\n' || c == '\r')),
      "The newline must be a valid newline")

    def isText(n: Node): Boolean = n match {
      case t: Text => true
      case _ => false
    }

    val tabOrSpace = if (useTab) "\t" else " "

    // Not an efficient implementation. It is recursive, but not tail-recursive.

    def prettify(elm: Elem, currentIndent: Int): Elem = {
      val childNodes = elm.children
      val hasElemChild = findChildElem(e => true).isDefined
      val doPrettify = (childNodes forall (n => !isText(n))) && (hasElemChild)

      if (doPrettify) {
        val newIndent = currentIndent + indent
        val indentText = Text(newLine + (tabOrSpace * newIndent), false)
        val endIndentText = Text(newLine + (tabOrSpace * currentIndent), false)

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

    def containsWhitespaceOnly(elem: Elem): Boolean = {
      elem.children forall {
        case t: Text if t.text.trim.isEmpty => true
        case n => false
      }
    }

    def fixIfWhitespaceOnly(elem: Elem): Elem =
      if (containsWhitespaceOnly(elem)) elem.withChildren(Vector()) else elem

    prettify(this.removeAllInterElementWhitespace, 0).transformElemsOrSelf(fixIfWhitespaceOnly _)
  }

  private[yaidom] override def toTreeReprAsLineSeq(parentScope: Scope, indent: Int)(indentStep: Int): LineSeq = {
    val qnameLineSeq: LineSeq = {
      val line = s"qname = QName(${toStringLiteral(this.qname.toString)})"
      LineSeq(line)
    }

    val attributesLineSeqOption: Option[LineSeq] =
      if (this.attributes.isEmpty) None else {
        def attributeEntryString(qname: QName, attrValue: String): String = {
          s"QName(${toStringLiteral(qname.toString)}) -> ${toStringLiteral(attrValue)}"
        }

        val attributeEntryStrings = {
          val result = this.attributes map { kv => attributeEntryString(kv._1, kv._2) }
          result.mkString(", ")
        }

        val line = s"attributes = Vector(${attributeEntryStrings})"
        Some(LineSeq(line))
      }

    val declarations: Declarations = parentScope.relativize(self.scope)

    val namespacesLineSeqOption: Option[LineSeq] = {
      if (declarations.prefixNamespaceMap.isEmpty) None else {
        def namespaceEntryString(prefix: String, nsUri: String): String = {
          toStringLiteral(prefix) + " -> " + toStringLiteral(nsUri)
        }

        val namespaceEntryStrings = {
          val result = declarations.prefixNamespaceMap map { kv => namespaceEntryString(kv._1, kv._2) }
          result.mkString(", ")
        }

        val line = s"namespaces = Declarations.from(${namespaceEntryStrings})"
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

  private def childNodeIndexesByPathEntries: Map[Path.Entry, Int] = {
    // This implementation is O(n), where n is the number of children, and uses mutable collections for speed

    val elementNameCounts = mutable.Map[EName, Int]()
    val acc = mutable.ArrayBuffer[(Path.Entry, Int)]()

    for ((node, idx) <- self.children.zipWithIndex) {
      node match {
        case elm: Elem =>
          val ename = elm.resolvedName
          val countForName = elementNameCounts.getOrElse(ename, 0)
          val entry = Path.Entry(ename, countForName)
          elementNameCounts.update(ename, countForName + 1)
          acc += ((entry, idx))
        case _ => ()
      }
    }

    val result = acc.toMap
    result
  }
}

@SerialVersionUID(1L)
final case class Text(text: String, isCData: Boolean) extends Node {
  require(text ne null)
  if (isCData) require(!text.containsSlice("]]>"))

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

  private[yaidom] override def toTreeReprAsLineSeq(parentScope: Scope, indent: Int)(indentStep: Int): LineSeq = {
    val targetStringLiteral = toStringLiteral(target)
    val dataStringLiteral = toStringLiteral(data)
    LineSeq(s"processingInstruction(${targetStringLiteral}, ${dataStringLiteral})").shift(indent)
  }
}

/**
 * An entity reference. For example:
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

  private[yaidom] override def toTreeReprAsLineSeq(parentScope: Scope, indent: Int)(indentStep: Int): LineSeq = {
    val entityStringLiteral = toStringLiteral(entity)
    LineSeq(s"entityRef(${entityStringLiteral})").shift(indent)
  }
}

@SerialVersionUID(1L)
final case class Comment(text: String) extends Node {
  require(text ne null)

  private[yaidom] override def toTreeReprAsLineSeq(parentScope: Scope, indent: Int)(indentStep: Int): LineSeq = {
    toConcatenatedStringLiterals(text).prepend("comment(").append(")").shift(indent)
  }
}

object Elem {

  private[yaidom] final class ElemSerializationProxy(
    val qname: QName,
    val attributes: immutable.IndexedSeq[(QName, String)],
    val scope: Scope,
    val children: immutable.IndexedSeq[Node]) extends Serializable {

    @throws(classOf[java.io.ObjectStreamException])
    def readResolve(): Any = new Elem(qname, attributes, scope, children)
  }

  /**
   * Factory method that mimics the primary constructor, but has defaults for all parameters except the qname.
   *
   * Use this factory method with care, because it is easy to use incorrectly (regarding passed Scopes).
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
 * There is a catch, though. When using this DSL, scopes must be passed throughout the tree. These Scopes should typically be
 * the same (or parent element scopes should be subscopes of child element scopes), because otherwise the corresponding XML
 * may contain a lot of namespace undeclarations.
 *
 * Another thing to watch out for is that the "tree representations" conform to the NodeBuilder DSL, not to this one.
 *
 * In summary, the NodeBuilder DSL does have the advantage over this DSL that scopes do not have to be
 * passed around. On the other hand, this Node DSL has the advantage that exceptions due to missing scope data are thrown immediately
 * instead of later (when calling the build method, in the case of the NodeBuilder DSL).
 *
 * For example:
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
