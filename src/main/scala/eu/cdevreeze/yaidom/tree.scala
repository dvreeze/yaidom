package eu.cdevreeze.yaidom

import java.{ util => jutil }
import scala.annotation.tailrec
import scala.collection.immutable

/**
 * Nodes as they occur in a tree of nodes. This augments nodes with a UUID of the parent (if not a root).
 * Whereas Nodes can freely be used (as "leaf trees") in arbitrary trees, TreeNodes are themselves (parts of) such trees.
 *
 * TreeNodes are particularly useful for transformations of XML trees, as an alternative to XSLT.
 */
sealed trait TreeNode extends Immutable {

  type NodeType <: Node

  val node: NodeType

  val parentUuidOption: Option[jutil.UUID]

  require(node ne null)
  require(parentUuidOption ne null)

  final override def equals(other: Any): Boolean = other match {
    case otherTreeNode: TreeNode => node == otherTreeNode.node
    case _ => false
  }

  final override def hashCode: Int = node.hashCode
}

trait TreeElem extends TreeNode with ElemLike[TreeElem] { self =>

  final override type NodeType = Elem

  def children: immutable.Seq[TreeNode]

  final override val resolvedName: ExpandedName = node.resolvedName

  final override def childElems: immutable.Seq[TreeElem] = children collect { case e: TreeElem => e }
}

trait TreeText extends TreeNode {

  final override type NodeType = Text
}

trait TreeProcessingInstruction extends TreeNode {

  final override type NodeType = ProcessingInstruction
}

trait TreeCData extends TreeNode {

  final override type NodeType = CData
}

trait TreeEntityRef extends TreeNode {

  final override type NodeType = EntityRef
}

final class Tree(val root: TreeElem) extends Immutable {

  require(root ne null)
  require(root.parentUuidOption.isEmpty)
}

object TreeNode {

  def apply(node: Node): TreeNode = {
    // Not tail-recursive, which typically should be no problem because recursion depth is limited in practice
    node match {
      case txt: Text => new TreeTextImpl(txt, None)
      case pi: ProcessingInstruction => new TreeProcessingInstructionImpl(pi, None)
      case cdata: CData => new TreeCDataImpl(cdata, None)
      case entityRef: EntityRef => new TreeEntityRefImpl(entityRef, None)
      case elm: Elem => {
        val children = elm.children.map(ch => apply(ch).asInstanceOf[TreeNodeImpl].withParentUuidOption(Some(elm.uuid)))
        val result = new TreeElemImpl(elm, None, children)
        require(result.children.forall(ch => ch.parentUuidOption == Some(elm.uuid)))
        result
      }
    }
  }

  private abstract class TreeNodeImpl(
    val parentUuidOption: Option[jutil.UUID]) extends TreeNode {

    def withParentUuidOption(newParentUuidOption: Option[jutil.UUID]): TreeNode
  }

  private final class TreeTextImpl(
    val node: Text,
    override val parentUuidOption: Option[jutil.UUID]) extends TreeNodeImpl(parentUuidOption) with TreeText {

    override def withParentUuidOption(newParentUuidOption: Option[jutil.UUID]): TreeText = {
      new TreeTextImpl(node, newParentUuidOption)
    }
  }

  private final class TreeProcessingInstructionImpl(
    val node: ProcessingInstruction,
    override val parentUuidOption: Option[jutil.UUID]) extends TreeNodeImpl(parentUuidOption) with TreeProcessingInstruction {

    override def withParentUuidOption(newParentUuidOption: Option[jutil.UUID]): TreeProcessingInstruction = {
      new TreeProcessingInstructionImpl(node, newParentUuidOption)
    }
  }

  private final class TreeCDataImpl(
    val node: CData,
    override val parentUuidOption: Option[jutil.UUID]) extends TreeNodeImpl(parentUuidOption) with TreeCData {

    override def withParentUuidOption(newParentUuidOption: Option[jutil.UUID]): TreeCData = {
      new TreeCDataImpl(node, newParentUuidOption)
    }
  }

  private final class TreeEntityRefImpl(
    val node: EntityRef,
    override val parentUuidOption: Option[jutil.UUID]) extends TreeNodeImpl(parentUuidOption) with TreeEntityRef {

    override def withParentUuidOption(newParentUuidOption: Option[jutil.UUID]): TreeEntityRef = {
      new TreeEntityRefImpl(node, newParentUuidOption)
    }
  }

  private final class TreeElemImpl(
    val node: Elem,
    override val parentUuidOption: Option[jutil.UUID],
    val children: immutable.Seq[TreeNode]) extends TreeNodeImpl(parentUuidOption) with TreeElem {

    require(node.children == children.map(ch => ch.node))

    override def withParentUuidOption(newParentUuidOption: Option[jutil.UUID]): TreeElem = {
      new TreeElemImpl(node, newParentUuidOption, children)
    }
  }
}

object Tree {

  def apply(elem: Elem): Tree = new Tree(TreeNode(elem).asInstanceOf[TreeElem])
}
