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

package eu.cdevreeze.yaidom.simple

import scala.Vector
import scala.collection.immutable
import scala.collection.mutable

import eu.cdevreeze.yaidom.PrettyPrinting.Line
import eu.cdevreeze.yaidom.PrettyPrinting.toStringLiteralAsSeq
import eu.cdevreeze.yaidom.XmlStringUtils
import eu.cdevreeze.yaidom.core.Declarations
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.QNameProvider
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.queryapi.ClarkNodes
import eu.cdevreeze.yaidom.queryapi.ScopedElemLike
import eu.cdevreeze.yaidom.queryapi.ScopedNodes
import eu.cdevreeze.yaidom.queryapi.TransformableElemLike
import eu.cdevreeze.yaidom.queryapi.UpdatableElemLike

/**
 * Immutable XML Node. It is the default XML node type in yaidom. There are subclasses for different types of nodes,
 * such as elements, text nodes, comments, entity references and processing instructions. See [[eu.cdevreeze.yaidom.simple.Elem]]
 * for the default element type in yaidom.
 *
 * @author Chris de Vreeze
 */
sealed trait Node extends ScopedNodes.Node with Serializable {

  /**
   * Returns the tree representation String, conforming to the tree representation DSL that creates `NodeBuilder`s.
   * That is, it does not correspond to the tree representation DSL of `Node`s, but of `NodeBuilder`s!
   *
   * There are a couple of advantages of this method compared to some "toXmlString" method which returns the XML string:
   * <ul>
   * <li>The parsed XML tree is made explicit, which makes debugging far easier, especially since method toString invokes this method</li>
   * <li>The output of method `toTreeRepr` clearly corresponds to a `NodeBuilder`, and can indeed be parsed into one</li>
   * <li>That `toTreeRepr` output is even valid Scala code</li>
   * <li>When parsing the string into a `NodeBuilder`, the following is out of scope: character escaping (for XML), entity resolving,
   * "ignorable" whitespace handling, etc.</li>
   * </ul>
   */
  final def toTreeRepr(parentScope: Scope): String = {
    val sb = new StringBuilder
    Line.addLinesToStringBuilder(toTreeReprAsLineSeq(parentScope, 0)(2), sb)
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
  private[yaidom] def toTreeReprAsLineSeq(parentScope: Scope, indent: Int)(indentStep: Int): immutable.IndexedSeq[Line]
}

sealed trait CanBeDocumentChild extends Node with ScopedNodes.CanBeDocumentChild

/**
 * <em>Immutable</em>, thread-safe <em>element node</em>. It is the <em>default</em> element implementation in yaidom. As the default
 * element implementation among several alternative element implementations, it strikes a balance between loss-less roundtripping
 * and composability.
 *
 * The parsers and serializers in packages `eu.cdevreeze.yaidom.parse` and `eu.cdevreeze.yaidom.print` return and take
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
 * require(schemaElem.filterElemsOrSelf(e => (e \@ EName("type")).isDefined).forall(e => e.attributeAsQName(EName("type")).prefixOption.contains("xsd")))
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
 * although according to the XML Infoset this order is irrelevant. Namespace declaration order is not necessarily retained,
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
  override val children: immutable.IndexedSeq[Node])
  extends CanBeDocumentChild
  with ScopedNodes.Elem
  with ScopedElemLike
  with UpdatableElemLike
  with TransformableElemLike {

  require(qname ne null) // scalastyle:off null
  require(attributes ne null) // scalastyle:off null
  require(scope ne null) // scalastyle:off null
  require(children ne null) // scalastyle:off null

  require(attributes.toMap.size == attributes.size, s"There are duplicate attribute names: $attributes")

  type ThisNode = Node

  type ThisElem = Elem

  def thisElem: ThisElem = this

  @throws(classOf[java.io.ObjectStreamException])
  private[yaidom] def writeReplace(): Any = new Elem.ElemSerializationProxy(qname, attributes, scope, children)

  /** The `Elem` name as `EName`, obtained by resolving the element `QName` against the `Scope` */
  override val resolvedName: EName =
    scope.resolveQNameOption(qname).getOrElse(sys.error(s"Element name '${qname}' should resolve to an EName in scope [${scope}]"))

  /**
   * The attributes as an ordered mapping from `EName`s (instead of `QName`s) to values, obtained by resolving attribute `QName`s against
   * the attribute scope
   */
  override val resolvedAttributes: immutable.IndexedSeq[(EName, String)] = {
    val attrScope = attributeScope

    attributes.map { case (attName, attValue) =>
      val expandedName =
        attrScope.resolveQNameOption(attName).getOrElse(sys.error(s"Attribute name '${attName}' should resolve to an EName in scope [${attrScope}]"))
      (expandedName -> attValue)
    }
  }

  override def collectChildNodeIndexes(pathEntries: Set[Path.Entry]): Map[Path.Entry, Int] = {
    filterChildElemsWithPathEntriesAndNodeIndexes(pathEntries).map(triple => (triple._2, triple._3)).toMap
  }

  /** The attribute `Scope`, which is the same `Scope` but without the default namespace (which is not used for attributes) */
  def attributeScope: Scope = scope.withoutDefaultNamespace

  /** Returns the element children */
  override def findAllChildElems: immutable.IndexedSeq[Elem] = children collect { case e: Elem => e }

  /** Creates a copy, but with (only) the children passed as parameter `newChildren` */
  override def withChildren(newChildren: immutable.IndexedSeq[Node]): Elem = {
    copy(children = newChildren)
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

    if (idx < 0) {
      withAttributes(attributes :+ (attributeName -> attributeValue))
    } else {
      withAttributes(attributes.updated(idx, (attributeName -> attributeValue)))
    }
  }

  /**
   * Functionally adds or updates the given attribute, if a value is given.
   * That is, returns `if (attributeValueOption.isEmpty) self else plusAttribute(attributeName, attributeValueOption.get)`.
   */
  def plusAttributeOption(attributeName: QName, attributeValueOption: Option[String]): Elem = {
    if (attributeValueOption.isEmpty) thisElem else plusAttribute(attributeName, attributeValueOption.get)
  }

  /**
   * Functionally removes the given attribute, if present.
   *
   * More precisely, returns `withAttributes(thisElem.attributes filterNot (_._1 == attributeName))`.
   */
  def minusAttribute(attributeName: QName): Elem = {
    val newAttributes = thisElem.attributes filterNot { case (attrName, attrValue) => attrName == attributeName }
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
   * def notUndeclaringPrefixes(parentScope: Scope): Elem = {
   *   val newScope = parentScope.withoutDefaultNamespace ++ this.scope
   *   this.copy(scope = newScope) transformChildElems { e => e.notUndeclaringPrefixes(newScope) }
   * }
   * }}}
   *
   * It can be proven by structural induction that for each `parentScope` the XML remains the "same":
   * {{{
   * resolved.Elem.from(this.notUndeclaringPrefixes(parentScope)) == resolved.Elem.from(this)
   * }}}
   * Moreover, there are no prefixed namespace undeclarations:
   * {{{
   * NodeBuilder.fromElem(this.notUndeclaringPrefixes(parentScope))(Scope.Empty).findAllElemsOrSelf.
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
    this.copy(scope = newScope).transformChildElems { _.notUndeclaringPrefixes(newScope) }
  }

  /**
   * Returns a copy where inter-element whitespace has been removed, throughout the node tree.
   *
   * That is, for each descendant-or-self element determines if it has at least one child element and no non-whitespace
   * text child nodes, and if so, removes all (whitespace) text children.
   *
   * This method is useful if it is known that whitespace around element nodes is used for formatting purposes, and (in
   * the absence of an XML Schema or DTD) can therefore be treated as "ignorable whitespace". In the case of "mixed content"
   * (if text around element nodes is not all whitespace), this method will not remove any text children of the parent element.
   *
   * XML space attributes (xml:space) are not respected by this method. If such whitespace preservation functionality is needed,
   * it can be written as a transformation where for specific elements this method is not called.
   */
  def removeAllInterElementWhitespace: Elem = {
    def isWhitespaceText(n: Node): Boolean = n match {
      case t: Text if t.trimmedText.isEmpty => true
      case _ => false
    }

    def isNonTextNode(n: Node): Boolean = n match {
      case t: Text => false
      case n => true
    }

    val doStripWhitespace = (findChildElem(_ => true).nonEmpty) && (children forall (n => isWhitespaceText(n) || isNonTextNode(n)))

    // Recursive, but not tail-recursive

    val newChildren = {
      val remainder = if (doStripWhitespace) children.filter(n => isNonTextNode(n)) else children

      remainder map {
        case e: Elem =>
          // Recursive call
          e.removeAllInterElementWhitespace
        case n =>
          n
      }
    }

    thisElem.withChildren(newChildren)
  }

  /**
   * Returns a copy where adjacent text nodes have been combined into one text node, throughout the node tree.
   * After combining the adjacent text nodes, all text nodes are transformed by calling the passed function.
   */
  def coalesceAllAdjacentTextAndPostprocess(f: Text => Text): Elem = {
    // Recursive, but not tail-recursive

    def accumulate(childNodes: Seq[Node], newChildrenBuffer: mutable.ArrayBuffer[Node]): Unit = {
      if (childNodes.nonEmpty) {
        val head = childNodes.head

        head match {
          case t: Text =>
            val (textNodes, remainder) = childNodes span {
              case t: Text => true
              case _ => false
            }

            val combinedText: String = textNodes collect { case t: Text => t.text } mkString ""

            newChildrenBuffer += f(Text(combinedText, false)) // No CDATA?

            // Recursive call
            accumulate(remainder, newChildrenBuffer)
          case n: Node =>
            newChildrenBuffer += n

            // Recursive call
            accumulate(childNodes.tail, newChildrenBuffer)
        }
      }
    }

    thisElem transformElemsOrSelf { elm =>
      val newChildrenBuffer = mutable.ArrayBuffer[Node]()

      accumulate(elm.children, newChildrenBuffer)

      elm.withChildren(newChildrenBuffer.toIndexedSeq)
    }
  }

  /** Returns a copy where adjacent text nodes have been combined into one text node, throughout the node tree */
  def coalesceAllAdjacentText: Elem = {
    coalesceAllAdjacentTextAndPostprocess(t => t)
  }

  /**
   * Returns a copy where adjacent text nodes have been combined into one text node, and where all
   * text is normalized, throughout the node tree. Same as calling `coalesceAllAdjacentText` followed by `normalizeAllText`,
   * but more efficient.
   */
  def coalesceAndNormalizeAllText: Elem = {
    coalesceAllAdjacentTextAndPostprocess(t => Text(XmlStringUtils.normalizeString(t.text), t.isCData))
  }

  /**
   * Returns a copy where text nodes have been transformed, throughout the node tree.
   */
  def transformAllText(f: Text => Text): Elem = {
    thisElem transformElemsOrSelf { elm =>
      val newChildren: immutable.IndexedSeq[Node] = {
        elm.children map { (n: Node) =>
          n match {
            case t: Text => f(t)
            case n => n
          }
        }
      }

      elm.withChildren(newChildren)
    }
  }

  /**
   * Returns a copy where text nodes have been normalized, throughout the node tree.
   * Note that it makes little sense to call this method before `coalesceAllAdjacentText`.
   */
  def normalizeAllText: Elem = {
    transformAllText(t => Text(t.normalizedText, false)) // No CDATA?
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

    val tabOrSpace = if (useTab) "\t" else " "

    def indentToIndentString(totalIndent: Int): String = {
      // String concatenation. If this function is called as little as possible, performance will not suffer too much.
      newLine + (tabOrSpace * totalIndent)
    }

    // Not a very efficient implementation, but string concatenation is kept to a minimum.
    // The nested prettify function is recursive, but not tail-recursive.

    val indentStringsByIndent = mutable.Map[Int, String]()

    def containsWhitespaceOnly(elem: Elem): Boolean = {
      elem.children forall {
        case t: Text if t.text.trim.isEmpty => true
        case n => false
      }
    }

    def fixIfWhitespaceOnly(elem: Elem): Elem =
      if (containsWhitespaceOnly(elem)) elem.withChildren(Vector()) else elem

    prettify(this.removeAllInterElementWhitespace, 0, indent, indentStringsByIndent, indentToIndentString).
      transformElemsOrSelf(fixIfWhitespaceOnly _)
  }

  private def prettify(
    elm: Elem,
    currentIndent: Int,
    indent: Int,
    indentStringsByIndent: mutable.Map[Int, String],
    indentToIndentString: Int => String): Elem = {

    def isText(n: Node): Boolean = n match {
      case t: Text => true
      case _ => false
    }

    val childNodes = elm.children
    val hasElemChild = elm.findChildElem(e => true).isDefined
    val doPrettify = hasElemChild && (childNodes forall (n => !isText(n)))

    if (doPrettify) {
      val newIndent = currentIndent + indent

      val indentTextString = indentStringsByIndent.getOrElseUpdate(newIndent, indentToIndentString(newIndent))
      val endIndentTextString = indentStringsByIndent.getOrElseUpdate(currentIndent, indentToIndentString(currentIndent))

      // Recursive calls
      val prettifiedChildNodes = childNodes map {
        case e: Elem => prettify(e, newIndent, indent, indentStringsByIndent, indentToIndentString)
        case n => n
      }

      val prefixedPrettifiedChildNodes = prettifiedChildNodes flatMap { n => List(Text(indentTextString, false), n) }
      val newChildNodes = prefixedPrettifiedChildNodes :+ Text(endIndentTextString, false)

      elm.withChildren(newChildNodes)
    } else {
      // Once we have encountered text-only content or mixed content, the formatting stops right there for that part of the DOM tree.
      elm
    }
  }

  private[yaidom] override def toTreeReprAsLineSeq(parentScope: Scope, indent: Int)(indentStep: Int): immutable.IndexedSeq[Line] = {
    // Given that this method is recursive, using structural recursion on the element tree, the non-recursive part
    // must be as fast as possible. This implies that for example we must not repeatedly "shift" the same group of lines multiple times.

    val innerIndent = indent + indentStep

    val qnameLineSeq: immutable.IndexedSeq[Line] =
      immutable.IndexedSeq(
        Line.fromIndexAndPrefixAndPartsAndSuffix(innerIndent, "qname = QName(", toStringLiteralAsSeq(this.qname.toString), ")"))

    val attributesLineSeqOption: Option[immutable.IndexedSeq[Line]] = getAttributesLineSeqOption(innerIndent)

    val namespacesLineSeqOption: Option[immutable.IndexedSeq[Line]] = getNamespacesLineSeqOption(innerIndent, parentScope)

    val childrenLineSeqOption: Option[immutable.IndexedSeq[Line]] =
      if (this.children.isEmpty) {
        None
      } else {
        val firstLine = new Line(innerIndent, "children = Vector(")

        val contentLines: immutable.IndexedSeq[Line] = {
          // Recursive calls
          val groups: immutable.IndexedSeq[immutable.IndexedSeq[Line]] =
            thisElem.children map { child =>
              // Mind the indentation below.
              child.toTreeReprAsLineSeq(thisElem.scope, innerIndent + indentStep)(indentStep)
            }

          val result = Line.mkLineSeq(groups, ",")
          result
        }
        val lastLine = new Line(innerIndent, ")")

        Some((firstLine +: contentLines) :+ lastLine)
      }

    val contentParts: immutable.IndexedSeq[immutable.IndexedSeq[Line]] =
      immutable.IndexedSeq(Some(qnameLineSeq), attributesLineSeqOption, namespacesLineSeqOption, childrenLineSeqOption).flatten

    // All content parts must now be properly indented
    val content: immutable.IndexedSeq[Line] = Line.mkLineSeq(contentParts, ",")

    val elemFunctionNameWithOpeningBracket: String = if (childrenLineSeqOption.isEmpty) "emptyElem(" else "elem("

    val firstLine = new Line(indent, elemFunctionNameWithOpeningBracket)
    val lastLine = new Line(indent, ")")

    (firstLine +: content) :+ lastLine
  }

  private def getAttributesLineSeqOption(innerIndent: Int): Option[immutable.IndexedSeq[Line]] = {
    if (this.attributes.isEmpty) {
      None
    } else {
      val attributeEntryStrings: immutable.IndexedSeq[String] = {
        val rawResult = this.attributes flatMap { kv => attributeEntryStringSeq(kv._1, kv._2) :+ ", " }
        rawResult.ensuring(_.nonEmpty).ensuring(_.last == ", ").dropRight(1)
      }

      val line = Line.fromIndexAndPrefixAndPartsAndSuffix(innerIndent, "attributes = Vector(", attributeEntryStrings, ")")
      Some(immutable.IndexedSeq(line))
    }
  }

  private def getNamespacesLineSeqOption(innerIndent: Int, parentScope: Scope): Option[immutable.IndexedSeq[Line]] = {
    val declarations: Declarations = parentScope.relativize(thisElem.scope)

    if (declarations.prefixNamespaceMap.isEmpty) {
      None
    } else {
      val namespaceEntryStrings: immutable.IndexedSeq[String] = {
        val rawResult = declarations.prefixNamespaceMap.toIndexedSeq flatMap { kv => namespaceEntryStringSeq(kv._1, kv._2) :+ ", " }
        rawResult.ensuring(_.nonEmpty).ensuring(_.last == ", ").dropRight(1)
      }

      val line = Line.fromIndexAndPrefixAndPartsAndSuffix(innerIndent, "namespaces = Declarations.from(", namespaceEntryStrings, ")")
      Some(immutable.IndexedSeq(line))
    }
  }

  private def attributeEntryStringSeq(qn: QName, attrValue: String): immutable.IndexedSeq[String] = {
    ("QName(" +: toStringLiteralAsSeq(qn.toString) :+ ") -> ") ++ toStringLiteralAsSeq(attrValue)
  }

  private def namespaceEntryStringSeq(prefix: String, nsUri: String): immutable.IndexedSeq[String] = {
    (toStringLiteralAsSeq(prefix) :+ " -> ") ++ toStringLiteralAsSeq(nsUri)
  }

  private def filterChildElemsWithPathEntriesAndNodeIndexes(pathEntries: Set[Path.Entry]): immutable.IndexedSeq[(Elem, Path.Entry, Int)] = {
    // Implementation inspired by findAllChildElemsWithPathEntries.
    // The fewer path entries passed, the more efficient this method is.

    var remainingPathEntries = pathEntries
    val nextEntries = mutable.Map[EName, Int]()

    children.zipWithIndex flatMap {
      case (n: Node, idx) if remainingPathEntries.isEmpty =>
        None
      case (e: Elem, idx) =>
        val ename = e.resolvedName

        if (remainingPathEntries.exists(_.elementName == ename)) {
          val entry = Path.Entry(ename, nextEntries.getOrElse(ename, 0))
          nextEntries.put(ename, entry.index + 1)

          if (pathEntries.contains(entry)) {
            remainingPathEntries -= entry
            Some((e, entry, idx))
          } else {
            None
          }
        } else {
          None
        }
      case (n: Node, idx) =>
        None
    }
  }
}

@SerialVersionUID(1L)
final case class Text(text: String, isCData: Boolean) extends Node with ScopedNodes.Text {
  require(text ne null) // scalastyle:off null
  if (isCData) require(!text.containsSlice("]]>"))

  /** Returns `text.trim`. */
  def trimmedText: String = text.trim

  /** Returns `XmlStringUtils.normalizeString(text)` .*/
  def normalizedText: String = XmlStringUtils.normalizeString(text)

  private[yaidom] override def toTreeReprAsLineSeq(parentScope: Scope, indent: Int)(indentStep: Int): immutable.IndexedSeq[Line] = {
    val parts = toStringLiteralAsSeq(text)

    val prefix = if (isCData) "cdata(" else "text("

    if (parts.forall(_.isEmpty)) {
      immutable.IndexedSeq()
    } else {
      immutable.IndexedSeq(Line.fromIndexAndPrefixAndPartsAndSuffix(indent, prefix, parts, ")"))
    }
  }
}

@SerialVersionUID(1L)
final case class ProcessingInstruction(target: String, data: String) extends CanBeDocumentChild with ScopedNodes.ProcessingInstruction {
  require(target ne null) // scalastyle:off null
  require(data ne null) // scalastyle:off null

  private[yaidom] override def toTreeReprAsLineSeq(parentScope: Scope, indent: Int)(indentStep: Int): immutable.IndexedSeq[Line] = {
    val targetStringLiteral = toStringLiteralAsSeq(target)
    val dataStringLiteral = toStringLiteralAsSeq(data)
    val partOfLine = (targetStringLiteral :+ ", ") ++ dataStringLiteral

    immutable.IndexedSeq(
      Line.fromIndexAndPrefixAndPartsAndSuffix(indent, "processingInstruction(", partOfLine, ")"))
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
final case class EntityRef(entity: String) extends Node with ScopedNodes.EntityRef {
  require(entity ne null) // scalastyle:off null

  private[yaidom] override def toTreeReprAsLineSeq(parentScope: Scope, indent: Int)(indentStep: Int): immutable.IndexedSeq[Line] = {
    val entityStringLiteral = toStringLiteralAsSeq(entity)
    immutable.IndexedSeq(Line.fromIndexAndPrefixAndPartsAndSuffix(indent, "entityRef(", entityStringLiteral, ")"))
  }
}

@SerialVersionUID(1L)
final case class Comment(text: String) extends CanBeDocumentChild with ScopedNodes.Comment {
  require(text ne null) // scalastyle:off null

  private[yaidom] override def toTreeReprAsLineSeq(parentScope: Scope, indent: Int)(indentStep: Int): immutable.IndexedSeq[Line] = {
    val parts = toStringLiteralAsSeq(text)

    if (parts.forall(_.isEmpty)) {
      immutable.IndexedSeq()
    } else {
      immutable.IndexedSeq(Line.fromIndexAndPrefixAndPartsAndSuffix(indent, "comment(", parts, ")"))
    }
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

  /**
   * Extractor of Elems, to be used for pattern matching.
   */
  def unapply(e: Elem): Option[(QName, immutable.IndexedSeq[(QName, String)], Scope, immutable.IndexedSeq[Node])] = {
    Some((e.qname, e.attributes, e.scope, e.children))
  }

  /**
   * Converts any `ScopedNodes.Elem` element to a "simple" `Elem`.
   */
  def from(e: ScopedNodes.Elem): Elem = {
    val children = e.children collect {
      case e: ScopedNodes.Elem => e
      case t: ScopedNodes.Text => t
      case c: ScopedNodes.Comment => c
      case pi: ScopedNodes.ProcessingInstruction => pi
      case er: ScopedNodes.EntityRef => er
    }

    // Recursion, with Node.from and Elem.from being mutually dependent
    val simpleChildren = children map { node => Node.from(node) }

    Elem(e.qname, e.attributes.toIndexedSeq, e.scope, simpleChildren)
  }

  /**
   * Converts any `ClarkNodes.Elem` element to a "simple" `Elem`, given a Scope needed for
   * computing QNames from ENames (of elements and attributes). The passed Scope must not contain the default namespace.
   *
   * Preferably the passed Scope is invertible.
   *
   * The resulting element has its attributes sorted on the name (QName), throughout the element tree.
   */
  def from(e: ClarkNodes.Elem, scope: Scope): Elem = {
    require(scope.defaultNamespaceOption.isEmpty, s"No default namespace allowed, but got scope $scope")

    val children = e.children collect {
      case e: ClarkNodes.Elem => e
      case t: ClarkNodes.Text => t
      case c: ClarkNodes.Comment => c
      case pi: ClarkNodes.ProcessingInstruction => pi
      case er: ClarkNodes.EntityRef => er
    }

    // Recursion, with Node.from and Elem.from being mutually dependent
    val simpleChildren = children map { node => Node.from(node, scope) }

    val qname = Node.enameToQName(e.resolvedName, scope)

    assert(scope.defaultNamespaceOption.isEmpty)

    val attributes =
      e.resolvedAttributes.toIndexedSeq map { case (en, v) => Node.enameToQName(en, scope) -> v }

    Elem(qname, attributes.sortBy(_._1.toString), scope, simpleChildren)
  }
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

  /**
   * Converts any element, text, comment, PI or entity reference `ScopedNodes.Node` to a "simple" `Node`.
   */
  def from(n: ScopedNodes.Node): Node = n match {
    case e: ScopedNodes.Elem => Elem.from(e)
    case t: ScopedNodes.Text => Text(t.text, false)
    case c: ScopedNodes.Comment => Comment(c.text)
    case pi: ScopedNodes.ProcessingInstruction => ProcessingInstruction(pi.target, pi.data)
    case er: ScopedNodes.EntityRef => EntityRef(er.entity)
    case n => sys.error(s"Not an element, text, comment, processing instruction or entity reference node: $n")
  }

  /**
   * Converts any element, text, comment, PI or entity reference `ClarkNodes.Node` to a "simple" `Node`, given a Scope needed for
   * computing QNames from ENames (of elements and attributes). The passed Scope must not contain the default namespace.
   *
   * Preferably the passed Scope is invertible.
   */
  def from(node: ClarkNodes.Node, scope: Scope): Node = {
    require(scope.defaultNamespaceOption.isEmpty, s"No default namespace allowed, but got scope $scope")

    node match {
      case e: ClarkNodes.Elem => Elem.from(e, scope)
      case t: ClarkNodes.Text => Text(t.text, false)
      case c: ClarkNodes.Comment => Comment(c.text)
      case pi: ClarkNodes.ProcessingInstruction => ProcessingInstruction(pi.target, pi.data)
      case er: ClarkNodes.EntityRef => EntityRef(er.entity)
      case n => sys.error(s"Not an element, text, comment, processing instruction or entity reference node: $n")
    }
  }

  def elem(
    qname: QName,
    scope: Scope,
    children: immutable.IndexedSeq[Node]): Elem = {

    elem(qname, Vector(), scope, children)
  }

  def elem(
    qname: QName,
    attributes: immutable.IndexedSeq[(QName, String)],
    scope: Scope,
    children: immutable.IndexedSeq[Node]): Elem = {

    new Elem(qname, attributes, scope, children)
  }

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

  def emptyElem(qname: QName, scope: Scope): Elem = {
    emptyElem(qname, Vector(), scope)
  }

  def emptyElem(
    qname: QName,
    attributes: immutable.IndexedSeq[(QName, String)],
    scope: Scope): Elem = {

    new Elem(qname, attributes, scope, Vector())
  }

  def text(textValue: String): Text = Text(text = textValue, isCData = false)

  def cdata(textValue: String): Text = Text(text = textValue, isCData = true)

  def processingInstruction(target: String, data: String): ProcessingInstruction =
    ProcessingInstruction(target, data)

  def entityRef(entity: String): EntityRef = EntityRef(entity)

  def comment(textValue: String): Comment = Comment(textValue)

  /**
   * Converts an EName to a QName, using the passed scope.
   *
   * The scope must have no default namespace (so a created QName without prefix will have no namespace),
   * and it must find a prefix for the namespaces used in the EName.
   */
  private[simple] def enameToQName(ename: EName, scope: Scope)(implicit qnameProvider: QNameProvider): QName = {
    assert(scope.defaultNamespaceOption.isEmpty, s"No default namespace allowed, but got scope $scope")

    ename.namespaceUriOption match {
      case None =>
        qnameProvider.getUnprefixedQName(ename.localPart)
      case Some(ns) =>
        val prefix = scope.prefixForNamespace(ns, () => sys.error(s"No prefix found for namespace '$ns'"))
        qnameProvider.getQName(prefix, ename.localPart)
    }
  }
}
