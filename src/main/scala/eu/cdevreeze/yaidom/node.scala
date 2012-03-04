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
import scala.annotation.tailrec
import scala.collection.immutable

/**
 * Immutable XML node. The API is inspired by Anti-XML, but it is less ambitious,
 * and differs in some key respects. Like Anti-XML:
 * <ul>
 * <li>Nodes in this API are truly immutable and thread-safe, backed by immutable
 * Scala collections.</li>
 * <li>Nodes have no reference to their parent/ancestor nodes. Hence, for example, you cannot
 * ask a `Node` for its "owning" document. Yet these nodes can be re-used in several XML trees.</li>
 * </ul>
 * Unlike Anti-XML:
 * <ul>
 * <li>This is just a DOM-like API, around immutable nodes and immutable Scala Collections of nodes,
 * without any XPath(-like) support. Despite the absence of selectors like those in Anti-XML, this DOM-like API
 * is still quite expressive, be it somewhat more verbose.</li>
 * <li>This API distinguishes between [[eu.cdevreeze.yaidom.QName]] and [[eu.cdevreeze.yaidom.ExpandedName]], making both
 * first-class citizens in the API. Moreover, the concept of a [[eu.cdevreeze.yaidom.Scope]] is a first-class citizen as well.
 * By explicitly modeling `QName`s, `ExpandedName`s and `Scope`s, the user of the API is somewhat shielded from some XML quirks.</li>
 * <li>This API is less ambitious. Like said above, XPath(-like) support is absent. So is support for "updates" through
 * zippers. So is "true" equality based on the exact tree.</li>
 * </ul>
 *
 * @author Chris de Vreeze
 */
sealed trait Node extends Immutable {

  /**
   * Returns the AST (abstract syntax tree) as `String`, conforming to the DSL for `NodeBuilder`s.
   *
   * There are a couple of advantages of this method compared to a "toXmlString" method which returns the XML string:
   * <ul>
   * <li>The AST is made explicit, which makes debugging far easier, especially since method toString delegates to this method</li>
   * <li>No need to handle the details of character escaping, entity resolving, output configuration options, etc.</li>
   * <li>Lower runtime costs</li>
   * <li>The output of method `toAstString` is itself Scala ("NodeBuilder") DSL code (for instance useful in REPL or unit tests)</li>
   * </ul>
   */
  final def toAstString(parentScope: Scope): String = toShiftedAstString(parentScope, 0)

  /** Same as `toAstString(emptyScope)` */
  final def toAstString: String = toAstString(Scope.Empty)

  /** Same as `toAstString(parentScope)`, but shifted numberOrSpaces to the right. Used for implementing `toAstString(parentScope)`. */
  def toShiftedAstString(parentScope: Scope, numberOfSpaces: Int): String

  /** Returns the AST string corresponding to this element. Possibly expensive! */
  final override def toString: String = toAstString
}

/** `Document` or `Elem` node */
trait ParentNode extends Node {

  def children: immutable.IndexedSeq[Node]
}

/**
 * `Document` node. Although at first sight the document root element seems to be the root node, this is not entirely true.
 * For example, there may be comments at top level, outside the document root element.
 */
final class Document(
  val baseUriOption: Option[URI],
  val documentElement: Elem,
  val processingInstructions: immutable.IndexedSeq[ProcessingInstruction],
  val comments: immutable.IndexedSeq[Comment]) extends ParentNode {

  require(baseUriOption ne null)
  require(documentElement ne null)
  require(processingInstructions ne null)
  require(comments ne null)

  override def children: immutable.IndexedSeq[Node] =
    processingInstructions ++ comments ++ immutable.IndexedSeq[Node](documentElement)

  /** Expensive method to obtain all processing instructions */
  def allProcessingInstructions: immutable.IndexedSeq[ProcessingInstruction] = {
    val result: immutable.IndexedSeq[immutable.IndexedSeq[ProcessingInstruction]] =
      documentElement.allElemsOrSelf collect { case e: Elem => e.children collect { case pi: ProcessingInstruction => pi } }
    val elemPIs = result.flatten
    processingInstructions ++ elemPIs
  }

  /** Expensive method to obtain all comments */
  def allComments: immutable.IndexedSeq[Comment] = {
    val result: immutable.IndexedSeq[immutable.IndexedSeq[Comment]] =
      documentElement.allElemsOrSelf collect { case e: Elem => e.children collect { case c: Comment => c } }
    val elemComments = result.flatten
    comments ++ elemComments
  }

  /** Creates a copy, but with the new documentElement passed as parameter newRoot */
  def withDocumentElement(newRoot: Elem): Document = new Document(
    baseUriOption = this.baseUriOption,
    documentElement = newRoot,
    processingInstructions = this.processingInstructions,
    comments = this.comments)

  /** Returns `withDocumentElement(this.documentElement updated pf)` */
  def updated(pf: PartialFunction[ElemPath, Elem]): Document = withDocumentElement(this.documentElement updated pf)

  /** Returns `withDocumentElement(this.documentElement.updated(path)(f))`. */
  def updated(path: ElemPath)(f: Elem => Elem): Document = withDocumentElement(this.documentElement.updated(path)(f))

  /** Returns `updated(path) { e => docElm }` */
  def updated(path: ElemPath, docElm: Elem): Document = updated(path) { e => docElm }

  override def toShiftedAstString(parentScope: Scope, numberOfSpaces: Int): String = {
    require(parentScope == Scope.Empty, "A document has no parent scope")

    val newline = "%n".format()

    val startFormatString =
      """|document(
         |  baseUriOption = %s,
         |  documentElement =
         |%s,
         |""".stripMargin

    val pisFormatString = if (processingInstructions.isEmpty) """  processingInstructions = immutable.IndexedSeq(%s),""" + newline else
      """|  processingInstructions = immutable.IndexedSeq(
         |%s
         |  ),
         |""".stripMargin

    val commentsFormatString = if (comments.isEmpty) """  comments = immutable.IndexedSeq(%s)""" + newline else
      """|  comments = immutable.IndexedSeq(
         |%s
         |  )
         |""".stripMargin

    val unshiftedFormatString = startFormatString + pisFormatString + commentsFormatString + ")"

    val formatString = {
      val result = unshiftedFormatString.lines.toIndexedSeq[String] collect {
        case ln if ln.trim == "%s" => ln // "%s" not indented here!
        case ln => (" " * numberOfSpaces) + ln
      }
      result.mkString("%n".format())
    }

    val baseUriOptionString: String =
      if (baseUriOption.isEmpty) "None" else """Some(%s%s%s)""".format("\"\"\"", baseUriOption.get.toString, "\"\"\"")

    val documentElementString: String = documentElement.toShiftedAstString(parentScope, numberOfSpaces + 4)

    val pisString = {
      val indent = numberOfSpaces + 4
      val pisStringList: Seq[String] =
        processingInstructions map { ch => ch.toShiftedAstString(parentScope, indent) }

      val separator = ",%n".format()
      val resultString: String = pisStringList.mkString(separator)
      resultString
    }

    val commentsString = {
      val indent = numberOfSpaces + 4
      val commentsStringList: Seq[String] =
        comments map { ch => ch.toShiftedAstString(parentScope, indent) }

      val separator = ",%n".format()
      val resultString: String = commentsStringList.mkString(separator)
      resultString
    }

    val result: String = formatString.format(
      baseUriOptionString, documentElementString, pisString, commentsString)
    result
  }
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
 * the (implicit) [[eu.cdevreeze.yaidom.Scope.Declarations]] of this element.
 *
 * Namespace declarations (and undeclarations) are not considered attributes in this API.
 *
 * The API is geared towards data-oriented XML that uses namespaces, and that typically is described in schemas (so that the
 * user of this API knows the structure of the XML being processed). The methods that return an Option say so in their name.
 *
 * No notion of (value) equality has been defined. When thinking about it, it is very hard to come up with any useful
 * notion of equality for representations of XML elements. Think about prefixes, "ignorable whitespace", DTDs and XSDs, etc.
 *
 * Use the constructor with care, because it is easy to use incorrectly (regarding passed Scopes).
 * To construct `Elem`s by hand, prefer using an `ElemBuilder`, via method `NodeBuilder.elem`.
 * Typically, however, `Elem`s are constructed by parsing an XML source.
 */
final class Elem(
  val qname: QName,
  val attributes: Map[QName, String],
  val scope: Scope,
  override val children: immutable.IndexedSeq[Node]) extends ParentNode with ElemAsElemContainer[Elem] { self =>

  require(qname ne null)
  require(attributes ne null)
  require(scope ne null)
  require(children ne null)

  /** The attribute `Scope`, which is the same `Scope` but without the default namespace (which is not used for attributes) */
  val attributeScope: Scope = scope.copy(defaultNamespaceOption = None)

  /** The `Elem` name as `ExpandedName`, obtained by resolving the element `QName` against the `Scope` */
  override val resolvedName: ExpandedName =
    scope.resolveQName(qname).getOrElse(sys.error("Element name '%s' should resolve to an ExpandedName in scope [%s]".format(qname, scope)))

  /** The attributes as a `Map` from `ExpandedName`s (instead of `QName`s) to values, obtained by resolving attribute `QName`s against the attribute scope */
  override val resolvedAttributes: Map[ExpandedName, String] = {
    attributes map { kv =>
      val attName = kv._1
      val attValue = kv._2
      val expandedName = attributeScope.resolveQName(attName).getOrElse(sys.error("Attribute name '%s' should resolve to an ExpandedName in scope [%s]".format(attName, attributeScope)))
      (expandedName -> attValue)
    }
  }

  /** Returns all child elements */
  override def allChildElems: immutable.IndexedSeq[Elem] = children collect { case e: Elem => e }

  /** Returns the text children */
  def textChildren: immutable.IndexedSeq[Text] = children collect { case t: Text => t }

  /** Returns the comment children */
  def commentChildren: immutable.IndexedSeq[Comment] = children collect { case c: Comment => c }

  /**
   * Returns the concatenation of the texts of text children, including whitespace and CData. Non-text children are ignored.
   * If there are no text children, the empty string is returned.
   */
  final def text: String = {
    val textStrings = textChildren map { t => t.text }
    textStrings.mkString
  }

  /** Returns `text.trim`. */
  final def trimmedText: String = text.trim

  /** Returns `XmlStringUtils.normalizeString(text)`. */
  final def normalizedText: String = XmlStringUtils.normalizeString(text)

  /** Creates a copy, but with (only) the children passed as parameter newChildren */
  def withChildren(newChildren: immutable.IndexedSeq[Node]): Elem = {
    new Elem(qname, attributes, scope, newChildren)
  }

  /** Returns `withChildren(self.children :+ newChild)`. */
  def plusChild(newChild: Node): Elem = withChildren(self.children :+ newChild)

  /**
   * "Functionally updates" the tree with this element as root element, by applying the passed partial function to the elements
   * for which the partial function is defined. The partial function is defined for an element if that element has an [[eu.cdevreeze.yaidom.ElemPath]]
   * (w.r.t. this element as root) for which it is defined. Tree traversal is top-down.
   *
   * This is an expensive method.
   */
  def updated(pf: PartialFunction[ElemPath, Elem]): Elem = {
    def updated(currentPath: ElemPath): Elem = {
      val elm: Elem = self.findWithElemPath(currentPath).getOrElse(sys.error("Undefined path %s for root element %s".format(currentPath, self)))

      currentPath match {
        case p if pf.isDefinedAt(p) => pf(p)
        case p =>
          val childResults: immutable.IndexedSeq[Node] = elm.children map {
            case e: Elem =>
              val ownPathEntry = e.ownElemPathEntry(elm)
              val ownPath = currentPath.append(ownPathEntry)

              // Recursive call, but not tail-recursive
              val updatedElm = updated(ownPath)
              updatedElm
            case n => n
          }

          elm.withChildren(childResults)
      }
    }

    updated(ElemPath.Root)
  }

  /**
   * "Functionally updates" the tree with this element as root element, by applying the passed function to the element
   * that has the given [[eu.cdevreeze.yaidom.ElemPath]] (compared to this element as root). The method throws an exception
   * if no element is found with the given path.
   */
  def updated(path: ElemPath)(f: Elem => Elem): Elem = {
    // This implementation has been inspired by Scala's immutable Vector, which offers efficient
    // "functional updates" (among other efficient operations).

    if (path.entries.isEmpty) f(self) else {
      val firstEntry = path.firstEntry
      val idx = childIndexOf(firstEntry)
      require(idx >= 0, "The path %s does not exist".format(path))
      val childElm = children(idx).asInstanceOf[Elem]

      // Recursive, but not tail-recursive
      val updatedChildren = children.updated(idx, childElm.updated(path.withoutFirstEntry)(f))
      self.withChildren(updatedChildren)
    }
  }

  /** Returns `updated(path) { e => elm }` */
  def updated(path: ElemPath, elm: Elem): Elem = updated(path) { e => elm }

  /**
   * Returns the index of the child with the given `ElemPath` `Entry` (taking this element as parent), or -1 if not found.
   * Must be fast.
   */
  def childIndexOf(pathEntry: ElemPath.Entry): Int = {
    var cnt = 0
    var idx = -1
    while (cnt <= pathEntry.index) {
      val newIdx = children indexWhere ({
        case e: Elem if e.resolvedName == pathEntry.elementName => true
        case _ => false
      }, idx + 1)
      idx = newIdx
      cnt += 1
    }
    idx
  }

  override def toShiftedAstString(parentScope: Scope, numberOfSpaces: Int): String = {
    val declarations: Scope.Declarations = parentScope.relativize(self.scope)

    val newline = "%n".format()

    val startFormatString =
      """|elem(
         |  qname = %s,
         |  attributes = %s,
         |  namespaces = %s,
         |""".stripMargin

    val childrenFormatString = if (self.children.isEmpty) """  children = immutable.IndexedSeq(%s)""" + newline else
      """|  children = immutable.IndexedSeq(
         |%s
         |  )
         |""".stripMargin

    val unshiftedFormatString = startFormatString + childrenFormatString + ")"

    val formatString = {
      val result = unshiftedFormatString.lines.toIndexedSeq[String] collect {
        case ln if ln.trim == "%s" => ln // "%s" not indented here!
        case ln => (" " * numberOfSpaces) + ln
      }
      result.mkString("%n".format())
    }

    val qnameString = "\"\"\"%s\"\"\".qname".format(self.qname.toString)

    val attributesString = {
      val result = self.attributes map { kv =>
        val qn: QName = kv._1
        val value: String = kv._2
        val qnameString = "\"\"\"%s\"\"\".qname".format(qn.toString)
        val valueString = "\"\"\"%s\"\"\"".format(value)
        (qnameString -> valueString)
      }
      result.toString
    }

    val namespacesString = {
      if (declarations.toMap.isEmpty) {
        "Scope.Declarations.Empty"
      } else {
        val result = declarations.toMap map { kv =>
          val prefix: String = kv._1
          val nsUri: String = kv._2
          val prefixString = "\"\"\"%s\"\"\"".format(prefix)
          val nsUriString = "\"\"\"%s\"\"\"".format(nsUri)
          (prefixString -> nsUriString)
        }
        "%s.namespaces".format(result.toString)
      }
    }

    val childrenString = {
      val indent = numberOfSpaces + 4
      val childrenStringList: Seq[String] =
        self.children map { ch => ch.toShiftedAstString(self.scope, indent) }

      val separator = ",%n".format()
      val resultString: String = childrenStringList.mkString(separator)
      resultString
    }

    val resultString = formatString.format(qnameString, attributesString, namespacesString, childrenString)
    resultString
  }
}

final case class Text(text: String, isCData: Boolean) extends Node {
  require(text ne null)
  if (isCData) require(!text.containsSlice("]]>"))

  /** Returns `text.trim`. */
  final def trimmedText: String = text.trim

  /** Returns `XmlStringUtils.normalizeString(text)` .*/
  final def normalizedText: String = XmlStringUtils.normalizeString(text)

  override def toShiftedAstString(parentScope: Scope, numberOfSpaces: Int): String = {
    if (isCData) {
      val result = "cdata(\"\"\"%s\"\"\")".format(text)
      (" " * numberOfSpaces) + result
    } else {
      val result = "text(\"\"\"%s\"\"\")".format(text)
      (" " * numberOfSpaces) + result
    }
  }
}

final case class ProcessingInstruction(target: String, data: String) extends Node {
  require(target ne null)
  require(data ne null)

  override def toShiftedAstString(parentScope: Scope, numberOfSpaces: Int): String = {
    val result = "processingInstruction(\"\"\"%s\"\"\", \"\"\"%s\"\"\")".format(target, data)
    (" " * numberOfSpaces) + result
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
final case class EntityRef(entity: String) extends Node {
  require(entity ne null)

  override def toShiftedAstString(parentScope: Scope, numberOfSpaces: Int): String = {
    val result = "entityRef(\"\"\"%s\"\"\")".format(entity)
    (" " * numberOfSpaces) + result
  }
}

final case class Comment(text: String) extends Node {
  require(text ne null)

  override def toShiftedAstString(parentScope: Scope, numberOfSpaces: Int): String = {
    val result = "comment(\"\"\"%s\"\"\")".format(text)
    (" " * numberOfSpaces) + result
  }
}

object Document {

  def apply(
    baseUriOption: Option[URI],
    documentElement: Elem,
    processingInstructions: immutable.IndexedSeq[ProcessingInstruction] = immutable.IndexedSeq(),
    comments: immutable.IndexedSeq[Comment] = immutable.IndexedSeq()): Document = {

    new Document(baseUriOption, documentElement, processingInstructions, comments)
  }
}

object Elem {

  /**
   * Use this constructor with care, because it is easy to use incorrectly (regarding passed Scopes).
   * To construct `Elem`s, prefer using an `ElemBuilder`, via method `NodeBuilder.elem`.
   */
  def apply(
    qname: QName,
    attributes: Map[QName, String] = Map(),
    scope: Scope = Scope.Empty,
    children: immutable.IndexedSeq[Node] = immutable.IndexedSeq()): Elem = new Elem(qname, attributes, scope, children)
}
