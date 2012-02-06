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
 * ask a Node for its "owning" document. Yet these nodes can be re-used in several XML trees.</li>
 * </ul>
 * Unlike Anti-XML:
 * <ul>
 * <li>This is just a DOM-like API, around immutable Nodes and immutable Scala Collections of Nodes,
 * without any XPath support. Despite the absence of selectors like those in Anti-XML, this DOM-like API
 * is still quite expressive, be it with somewhat verbose method names. This API is also simpler
 * in that CanBuildFrom "axioms" are absent, and implicit conversions are rare and rather "explicit" and therefore safe.
 * It is more verbose in that many ("DOM-like") convenience methods are offered.</li>
 * <li>This API distinguishes between QNames and ExpandedNames, making both first-class citizens in the API.
 * Moreover, Scopes are first-class citizens as well. By explicitly modeling QNames, ExpandedNames and Scopes,
 * the user of the API is somewhat shielded from some XML quirks.</li>
 * <li>This API is less ambitious. Like said above, XPath support is absent. So is support for "updates" through
 * zippers. So is true equality based on the exact tree. It is currently also less mature, and less well tested.</li>
 * </ul>
 *
 * @author Chris de Vreeze
 */
sealed trait Node extends Immutable {

  /**
   * Returns the AST (abstract syntax tree) as String, conforming to the DSL for NodeBuilders.
   *
   * There are a couple of advantages of this method compared to a "toXmlString" method which returns the XML string:
   * <ul>
   * <li>The AST is made explicit, which makes debugging far easier, especially since method toString delegates to this method</li>
   * <li>No need to handle the details of character escaping, entity resolving, output configuration options, etc.</li>
   * <li>Relatively low runtime costs</li>
   * <li>The output of method toAstString is itself Scala ("NodeBuilder") DSL code (for instance useful in REPL or unit tests)</li>
   * </ul>
   */
  final def toAstString(parentScope: Scope): String = toShiftedAstString(parentScope, 0)

  /** Same as toAstString(Scope.Empty) */
  final def toAstString: String = toAstString(Scope.Empty)

  /** Same as toAstString(parentScope), but shifted numberOrSpaces to the right. Used for implementing toAstString(parentScope). */
  def toShiftedAstString(parentScope: Scope, numberOfSpaces: Int): String

  /** Returns the AST string corresponding to this element. Possibly expensive! */
  final override def toString: String = toAstString
}

/** Document or Elem node */
trait ParentNode extends Node {

  def children: immutable.Seq[Node]
}

/**
 * Document node. Although the document element seems to be the root node, this is not entirely true.
 * For example, there may be comments at top level, outside the document root.
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

  override def children: immutable.Seq[Node] =
    processingInstructions ++ comments ++ immutable.IndexedSeq[Node](documentElement)

  /** Expensive method to obtain all processing instructions */
  def allProcessingInstructions: immutable.Seq[ProcessingInstruction] = {
    val result: immutable.Seq[immutable.Seq[ProcessingInstruction]] =
      documentElement.allElemsOrSelf collect { case e: Elem => e.children collect { case pi: ProcessingInstruction => pi } }
    val elemPIs = result.flatten
    processingInstructions ++ elemPIs
  }

  /** Expensive method to obtain all comments */
  def allComments: immutable.Seq[Comment] = {
    val result: immutable.Seq[immutable.Seq[Comment]] =
      documentElement.allElemsOrSelf collect { case e: Elem => e.children collect { case c: Comment => c } }
    val elemComments = result.flatten
    comments ++ elemComments
  }

  override def toShiftedAstString(parentScope: Scope, numberOfSpaces: Int): String = {
    require(parentScope == Scope.Empty, "A document has no parent scope")

    val unshiftedFormatString =
      """|document(
         |  baseUriOption = %s,
         |  documentElement =
         |%s,
         |  processingInstructions = List(
         |%s
         |  ),
         |  comments = List(
         |%s
         |  )
         |)""".stripMargin

    val formatString = {
      val result = unshiftedFormatString.lines.toList collect {
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
      val pisStringList: List[String] =
        processingInstructions.toList map { ch => ch.toShiftedAstString(parentScope, indent) }

      val separator = ",%n".format()
      val resultString: String = pisStringList.mkString(separator)
      resultString
    }

    val commentsString = {
      val indent = numberOfSpaces + 4
      val commentsStringList: List[String] =
        comments.toList map { ch => ch.toShiftedAstString(parentScope, indent) }

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
 * Element node. An Elem consists of a QName of the element, the attributes mapping attribute QNames to String values,
 * a Scope mapping prefixes to namespace URIs, and an immutable collection of child Nodes. The element QName and attribute
 * QNames must be in scope, according to the passed Scope. The Scope is absolute, typically containing a lot more than
 * the (implicit) Scope.Declarations of this element.
 *
 * Namespace declarations (and undeclarations) are not considered attributes in this API.
 *
 * The API is geared towards data-oriented XML that uses namespaces, and that is described in schemas (so that the user of this
 * API knows the structure of the XML being processed). The methods that return an Option say so in their name.
 *
 * No notion of (value) equality has been defined. When thinking about it, it is very hard to come up with any useful
 * notion of equality for Elems.
 *
 * The constructor is private. See the apply factory method on the companion object, and its documentation.
 */
final class Elem private (
  val qname: QName,
  val attributes: Map[QName, String],
  val scope: Scope,
  override val children: immutable.IndexedSeq[Node]) extends ParentNode with ElemLike[Elem] with TextParentLike[Text] { self =>

  require(qname ne null)
  require(attributes ne null)
  require(scope ne null)
  require(children ne null)

  /** The attribute Scope, which is the same Scope but without the default namespace (which is not used for attributes) */
  val attributeScope: Scope = scope.copy(defaultNamespace = None)

  /** The Elem name as ExpandedName, obtained by resolving the element QName against the Scope */
  override val resolvedName: ExpandedName =
    scope.resolveQName(qname).getOrElse(sys.error("Element name '%s' should resolve to an ExpandedName in scope [%s]".format(qname, scope)))

  /** The attributes as a Map from ExpandedNames (instead of QNames) to values, obtained by resolving attribute QNames against the attribute scope */
  override val resolvedAttributes: Map[ExpandedName, String] = {
    attributes map { kv =>
      val attName = kv._1
      val attValue = kv._2
      val expandedName = attributeScope.resolveQName(attName).getOrElse(sys.error("Attribute name '%s' should resolve to an ExpandedName in scope [%s]".format(attName, attributeScope)))
      (expandedName -> attValue)
    }
  }

  /** Returns all child elements */
  override def allChildElems: immutable.Seq[Elem] = children collect { case e: Elem => e }

  /** Returns the text children */
  override def textChildren: immutable.Seq[Text] = children collect { case t: Text => t }

  /** Returns the comment children */
  def commentChildren: immutable.Seq[Comment] = children collect { case c: Comment => c }

  /** Creates a copy, but with the children passed as parameter newChildren */
  def withChildren(newChildren: immutable.Seq[Node]): Elem = new Elem(qname, attributes, scope, newChildren.toIndexedSeq)

  /** Copies this Elem, but on encountering a descendant (or self) matching the given partial function, invokes that function instead */
  def copyAndTransform(root: Elem, f: PartialFunction[Elem.RootAndElem, Elem]): Elem = {
    // Recursive, but not tail-recursive. In practice, this should be no problem due to limited recursion depths.
    val rootAndElem = Elem.RootAndElem(root, self)

    if (f.isDefinedAt(rootAndElem)) f(rootAndElem) else {
      val newChildren = self.children map { (ch: Node) =>
        ch match {
          case ch: Elem => ch.copyAndTransform(root, f)
          case n => n
        }
      }
      self.withChildren(newChildren)
    }
  }

  override def toShiftedAstString(parentScope: Scope, numberOfSpaces: Int): String = {
    val declarations: Scope.Declarations = parentScope.relativize(self.scope)

    val unshiftedFormatString =
      """|elem(
         |  qname = %s,
         |  attributes = %s,
         |  namespaces = %s,
         |  children = List(
         |%s
         |  )
         |)""".stripMargin

    val formatString = {
      val result = unshiftedFormatString.lines.toList collect {
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
      val childrenStringList: List[String] =
        self.children.toList map { ch => ch.toShiftedAstString(self.scope, indent) }

      val separator = ",%n".format()
      val resultString: String = childrenStringList.mkString(separator)
      resultString
    }

    val resultString = formatString.format(qnameString, attributesString, namespacesString, childrenString)
    resultString
  }
}

final case class Text(text: String, isCData: Boolean) extends Node with TextLike {
  require(text ne null)
  if (isCData) require(!text.containsSlice("]]>"))

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
    processingInstructions: immutable.Seq[ProcessingInstruction] = Nil,
    comments: immutable.Seq[Comment] = Nil): Document = {

    new Document(baseUriOption, documentElement, processingInstructions.toIndexedSeq, comments.toIndexedSeq)
  }
}

object Elem {

  final case class RootAndElem(root: Elem, elem: Elem) extends Immutable

  /**
   * Use this constructor with care, because it is easy to use incorrectly (regarding passed Scopes).
   * To construct Elems, prefer using an ElemBuilder, via method <code>NodeBuilder.elem</code>.
   */
  def apply(
    qname: QName,
    attributes: Map[QName, String] = Map(),
    scope: Scope = Scope.Empty,
    children: immutable.Seq[Node] = immutable.Seq()): Elem = new Elem(qname, attributes, scope, children.toIndexedSeq)
}
