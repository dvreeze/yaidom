package eu.cdevreeze.yaidom

import java.{ util => jutil }
import scala.annotation.tailrec
import scala.collection.immutable

/**
 * Immutable XML node. The API is inspired by Anti-XML, but it is less ambitious,
 * and differs in some key respects. Like Anti-XML:
 * <ul>
 * <li>Nodes in this API are truly immutable and thread-safe, backed by immutable
 * Scala collections.</li>
 * <li>Nodes have no reference to their parent/ancestor nodes. These nodes can be re-used in several
 * XML trees.</li>
 * <li>Documents are absent in both APIs, so "owning" documents are not modeled.
 * Comments are absent as well.</li>
 * </ul>
 * Unlike Anti-XML:
 * <ul>
 * <li>This is just a DOM API, around immutable Nodes and immutable Scala Collections of Nodes,
 * without any XPath support. Despite the absence of selectors like those in Anti-XML, this DOM API
 * is still quite expressive, be it with somewhat verbose method names. This API is also simpler
 * in that CanBuildFrom "axioms" are absent, and implicit conversions are absent or at least rare.
 * It is more verbose in that many ("DOM-like") convenience methods are offered.</li>
 * <li>This API distinguishes between QNames and ExpandedNames, making both first-class citizens in the API.
 * Moreover, Scopes are first-class citizens as well. By explicitly modeling QNames, ExpandedNames and Scopes,
 * the user of the API is somewhat shielded from some XML quirks.</li>
 * <li>This API is less ambitious. Like said above, XPath support is absent. So is support for "updates" through
 * zippers. So is true equality based on the exact tree. It is currently also less mature, and less well tested.</li>
 * </ul>
 */
sealed trait Node extends Immutable {

  /** The unique ID of the immutable Node (and its subtree). "Updates" result in new UUIDs. */
  val uuid: jutil.UUID
}

/**
 * Element node. An Element consists of a QName of the element, the attributes mapping attribute QNames to String values,
 * a Scope mapping prefixes to namespace URIs, and an immutable collection of child Nodes. The element QName and attribute
 * QNames must be in scope, according to the passed Scope. The Scope is absolute, typically containing a lot more than
 * the (implicit) Scope.Declarations of this element.
 *
 * Namespace declarations (and undeclarations) are not considered attributes in this API.
 *
 * The API is geared towards data-oriented XML that uses namespaces, and that is described in schemas (so that the user of this
 * API knows the structure of the XML being processed). The methods that return an Option say so in their name.
 */
final class Element(
  val qname: QName,
  val attributes: Map[QName, String],
  val scope: Scope,
  val children: immutable.IndexedSeq[Node]) extends Node { self =>

  require(qname ne null)
  require(attributes ne null)
  require(scope ne null)
  require(children ne null)

  override val uuid: jutil.UUID = jutil.UUID.randomUUID

  /** The attribute Scope, which is the same Scope but without the default namespace (which is not used for attributes) */
  val attributeScope: Scope = scope.copy(defaultNamespace = None)

  /** The Element name as ExpandedName, obtained by resolving the element QName against the Scope */
  val resolvedName: ExpandedName =
    scope.resolveQName(qname).getOrElse(sys.error("Element name '%s' should resolve to an ExpandedName in scope [%s]".format(qname, scope)))

  /** The attributes as a Map from ExpandedNames (instead of QNames) to values, obtained by resolving attribute QNames against the attribute scope */
  val resolvedAttributes: Map[ExpandedName, String] = {
    attributes map { kv =>
      val attName = kv._1
      val attValue = kv._2
      val expandedName = attributeScope.resolveQName(attName).getOrElse(sys.error("Attribute name '%s' should resolve to an ExpandedName in scope [%s]".format(attName, attributeScope)))
      (expandedName -> attValue)
    }
  }

  /** Returns the value of the attribute with the given expanded name, if any, and None otherwise */
  def attributeOption(expandedName: ExpandedName): Option[String] = resolvedAttributes.get(expandedName)

  /** Returns the value of the attribute with the given expanded name, and throws an exception otherwise */
  def attribute(expandedName: ExpandedName): String = attributeOption(expandedName).getOrElse(sys.error("Missing attribute %s".format(expandedName)))

  /** Returns the child elements */
  def childElems: immutable.Seq[Element] = children collect { case e: Element => e }

  /** Returns the child elements obeying the given predicate */
  def childElems(p: Element => Boolean): immutable.Seq[Element] = childElems.filter(p)

  /** Returns the child elements with the given expanded name */
  def childElems(expandedName: ExpandedName): immutable.Seq[Element] = childElems(e => e.resolvedName == expandedName)

  /** Returns the child elements with the given expanded name, obeying the given predicate */
  def childElems(expandedName: ExpandedName, p: Element => Boolean): immutable.Seq[Element] =
    childElems(e => (e.resolvedName == expandedName) && p(e))

  /** Returns the single child element with the given expanded name, if any, and None otherwise */
  def childElemOption(expandedName: ExpandedName): Option[Element] = {
    val result = childElems(expandedName)
    require(result.size <= 1, "Expected at most 1 child element %s, but found %d of them".format(expandedName, result.size))
    result.headOption
  }

  /** Returns the single child element with the given expanded name, and throws an exception otherwise */
  def childElem(expandedName: ExpandedName): Element = {
    val result = childElems(expandedName)
    require(result.size == 1, "Expected exactly 1 child element %s, but found %d of them".format(expandedName, result.size))
    result.head
  }

  /** Returns the descendant elements (not including this element) */
  def descendants: immutable.Seq[Element] = {
    @tailrec
    def descendants(elems: immutable.IndexedSeq[Element], acc: immutable.IndexedSeq[Element]): immutable.IndexedSeq[Element] = {
      val childElms: immutable.IndexedSeq[Element] = elems.flatMap(_.childElems)

      if (childElms.isEmpty) acc else descendants(childElms, acc ++ childElms)
    }

    descendants(immutable.IndexedSeq(self), immutable.IndexedSeq())
  }

  /** Returns the descendant elements obeying the given predicate */
  def descendants(p: Element => Boolean): immutable.Seq[Element] = descendants.filter(p)

  /** Returns the descendant elements with the given expanded name */
  def descendants(expandedName: ExpandedName): immutable.Seq[Element] = descendants(e => e.resolvedName == expandedName)

  /** Returns the descendant elements with the given expanded name, obeying the given predicate */
  def descendants(expandedName: ExpandedName, p: Element => Boolean): immutable.Seq[Element] =
    descendants(e => (e.resolvedName == expandedName) && p(e))

  /** Returns the text children */
  def textChildren: immutable.Seq[Text] = children collect { case t: Text => t }

  /** Returns the first text child, if any, and None otherwise */
  def firstTextChildOption: Option[Text] = textChildren.headOption

  /** Returns the first text child's value, if any, and None otherwise */
  def firstTextValueOption: Option[String] = textChildren.headOption.map(_.text)

  /** Returns the first text child, if any, and None otherwise */
  def firstTextChild: Text = firstTextChildOption.getOrElse(sys.error("Missing text child"))

  /** Returns the first text child's value, if any, and None otherwise */
  def firstTextValue: String = firstTextValueOption.getOrElse(sys.error("Missing text child"))

  /** Finds the parent element, if any, searching in the tree with the given root element */
  def parentInTreeOption(root: Element): Option[Element] =
    (root.descendants :+ root).find(e => e.childElems.exists(ch => ch == self))

  /** Creates a copy, but with the children passed as parameter newChildren */
  def withChildren(newChildren: immutable.Seq[Node]): Element = new Element(qname, attributes, scope, newChildren.toIndexedSeq)

  /** Equality based on the UUID. Fast but depends not only on the tree itself, but also the time of creation */
  override def equals(other: Any): Boolean = other match {
    case e: Element => self.uuid == e.uuid
    case _ => false
  }

  /** Hash code, consistent with equals */
  override def hashCode: Int = uuid.hashCode

  /** Returns the XML string corresponding to this element, without the children (but ellipsis instead) */
  override def toString: String = withChildren(immutable.Seq[Node](Text(" ... "))).toXmlString(Scope.Empty)

  /** Returns the XML string corresponding to this element */
  def toXmlString: String = toXmlString(Scope.Empty)

  /** Returns the XML string corresponding to this element, taking the given parent Scope into account */
  def toXmlString(parentScope: Scope): String = toLines("", parentScope).mkString("%n".format())

  private def toLines(indent: String, parentScope: Scope): List[String] = {
    val declarations: Scope.Declarations = parentScope.relativize(self.scope)
    val declarationsString = declarations.toStringInXml
    val attrsString = attributes.map(kv => """%s="%s"""".format(kv._1, kv._2)).mkString(" ")

    if (self.children.isEmpty) {
      val line = "<%s />".format(List(qname, declarationsString, attrsString).filterNot(_ == "").mkString(" "))
      List(line).map(ln => indent + ln)
    } else if (this.childElems.isEmpty) {
      val line = "<%s>%s</%s>".format(
        List(qname, declarationsString, attrsString).filterNot(_ == "").mkString(" "),
        children.map(_.toString).mkString,
        qname)
      List(line).map(ln => indent + ln)
    } else {
      val firstLine: String = "<%s>".format(List(qname, declarationsString, attrsString).filterNot(_ == "").mkString(" "))
      val lastLine: String = "</%s>".format(qname)
      // Recursive (not tail-recursive) calls, ignoring non-element children
      val childElementLines: List[String] = self.childElems.toList.flatMap({ e =>
        e.toLines("  ", self.scope)
      })
      (firstLine :: childElementLines ::: List(lastLine)).map(ln => indent + ln)
    }
  }
}

object Element {

  def apply(
    qname: QName,
    attributes: Map[QName, String] = Map(),
    scope: Scope = Scope.Empty,
    children: immutable.Seq[Node] = immutable.Seq()): Element = new Element(qname, attributes, scope, children.toIndexedSeq)
}

final case class Text(text: String) extends Node {
  require(text ne null)

  override val uuid: jutil.UUID = jutil.UUID.randomUUID

  override def toString: String = text
}

final case class ProcessingInstruction(target: String, data: String) extends Node {
  require(target ne null)
  require(data ne null)

  override val uuid: jutil.UUID = jutil.UUID.randomUUID

  override def toString: String = """<?%s %s?>""".format(target, data)
}

final case class CData(text: String) extends Node {
  require(text ne null)

  override val uuid: jutil.UUID = jutil.UUID.randomUUID

  override def toString: String = """<![CDATA[%s]]>""".format(text)
}

final case class EntityRef(entity: String) extends Node {
  require(entity ne null)

  override val uuid: jutil.UUID = jutil.UUID.randomUUID

  override def toString: String = """&%s;""".format(entity)
}
