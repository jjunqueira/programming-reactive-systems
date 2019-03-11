/**
  * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
  */
package actorbintree

import akka.actor._
import scala.collection.immutable.Queue

object BinaryTreeSet {

  trait Operation {
    def requester: ActorRef
    def id: Int
    def elem: Int
  }

  trait OperationReply {
    def id: Int
  }

  /** Request with identifier `id` to insert an element `elem` into the tree.
    * The actor at reference `requester` should be notified when this operation
    * is completed.
    */
  case class Insert(requester: ActorRef, id: Int, elem: Int) extends Operation

  /** Request with identifier `id` to check whether an element `elem` is present
    * in the tree. The actor at reference `requester` should be notified when
    * this operation is completed.
    */
  case class Contains(requester: ActorRef, id: Int, elem: Int) extends Operation

  /** Request with identifier `id` to remove the element `elem` from the tree.
    * The actor at reference `requester` should be notified when this operation
    * is completed.
    */
  case class Remove(requester: ActorRef, id: Int, elem: Int) extends Operation

  /** Request to perform garbage collection*/
  case object GC

  /** Holds the answer to the Contains request with identifier `id`.
    * `result` is true if and only if the element is present in the tree.
    */
  case class ContainsResult(id: Int, result: Boolean) extends OperationReply

  /** Message to signal successful completion of an insert or remove operation. */
  case class OperationFinished(id: Int) extends OperationReply

}

class BinaryTreeSet extends Actor {
  import BinaryTreeSet._
  import BinaryTreeNode._

  def createRoot: ActorRef =
    context.actorOf(BinaryTreeNode.props(0, initiallyRemoved = true))

  var root = createRoot

  // optional
  var pendingQueue = Queue.empty[Operation]

  // optional
  def receive = normal

  // optional
  /** Accepts `Operation` and `GC` messages. */
  val normal: Receive = {
    case GC                            => ???
    case Insert(requestor, id, elem)   => root ! Insert(requestor, id, elem)
    case Contains(requestor, id, elem) => root ! Contains(requestor, id, elem)
    case Remove(requestor, id, elem)   => root ! Remove(requestor, id, elem)
  }

  // optional
  /** Handles messages while garbage collection is performed.
    * `newRoot` is the root of the new binary tree where we want to copy
    * all non-removed elements into.
    */
  def garbageCollecting(newRoot: ActorRef): Receive = ???

}

object BinaryTreeNode {
  trait Position

  case object Left  extends Position
  case object Right extends Position

  case class CopyTo(treeNode: ActorRef)
  case object CopyFinished

  def props(elem: Int, initiallyRemoved: Boolean) =
    Props(classOf[BinaryTreeNode], elem, initiallyRemoved)
}

class BinaryTreeNode(val elem: Int, initiallyRemoved: Boolean) extends Actor {
  import BinaryTreeNode._
  import BinaryTreeSet._

  var subtrees = Map[Position, ActorRef]()
  var removed  = initiallyRemoved

  // optional
  def receive = normal

  // optional
  /** Handles `Operation` messages and `CopyTo` requests. */
  val normal: Receive = {

    case Insert(requestor, id, elem) =>
      if (elem == this.elem) {
        removed = false
        requestor ! OperationFinished(id)
      } else if (elem > this.elem) {
        val right =
          context.actorOf(BinaryTreeNode.props(elem, initiallyRemoved = false))
        subtrees = subtrees updated (Right, right)
        requestor ! OperationFinished(id)
      } else {
        val left =
          context.actorOf(BinaryTreeNode.props(elem, initiallyRemoved = false))
        subtrees = subtrees updated (Left, left)
        requestor ! OperationFinished(id)
      }

    case Contains(requestor, id, elem) =>
      if (elem == this.elem) {
        requestor ! ContainsResult(id, true)
      } else if (subtrees.isEmpty) {
        requestor ! ContainsResult(id, false)
      } else if(elem > this.elem) {
        if(subtrees.contains(Right)) subtrees(Right) ! Contains(requestor, id, elem)
        else requestor ! ContainsResult(id, false)
      } else {
        if(subtrees.contains(Left)) subtrees(Left) ! Contains(requestor, id, elem)
        else requestor ! ContainsResult(id, false)
      }

    case Remove(requestor, id, elem) =>
      if (elem == this.elem) {
        removed = true
        requestor ! OperationFinished(id)
      } else if (subtrees.isEmpty) {
        requestor ! OperationFinished(id)
      } else if(elem > this.elem) {
        if(subtrees.contains(Right)) subtrees(Right) ! Remove(requestor, id, elem)
        else requestor ! OperationFinished(id)
      } else {
        if(subtrees.contains(Left)) subtrees(Left) ! Remove(requestor, id, elem)
        else requestor ! OperationFinished(id)
      }

    case CopyTo(parent) => ???

  }

  // optional
  /** `expected` is the set of ActorRefs whose replies we are waiting for,
    * `insertConfirmed` tracks whether the copy of this node to the new tree has been confirmed.
    */
  def copying(expected: Set[ActorRef], insertConfirmed: Boolean): Receive = ???

}
