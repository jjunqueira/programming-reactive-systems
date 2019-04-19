package protocols

import akka.actor.typed.{Behavior, ExtensibleBehavior}
import akka.actor.typed.scaladsl._

object SelectiveReceive {
    /**
      * @return A behavior that stashes incoming messages unless they are handled
      *         by the underlying `initialBehavior`
      * @param bufferSize Maximum number of messages to stash before throwing a `StashOverflowException`
      *                   Note that 0 is a valid size and means no buffering at all (ie all messages should
      *                   always be handled by the underlying behavior)
      * @param initialBehavior Behavior to decorate
      * @tparam T Type of messages
      *
      * Hint: Implement an [[ExtensibleBehavior]], use a [[StashBuffer]] and [[Behavior]] helpers such as `start`,
      * `validateAsInitial`, `interpretMessage`,`canonicalize` and `isUnhandled`.
      */
    def apply[T](bufferSize: Int, initialBehavior: Behavior[T]): Behavior[T] = new ExtensibleBehavior[T]() {
      
      import Behavior.{start, canonicalize, validateAsInitial, interpretMessage, isUnhandled}

      val buffer = StashBuffer[T](bufferSize)

      def receive(ctx: akka.actor.typed.ActorContext[T],msg: T): Behavior[T] = {
          val started = validateAsInitial(start(initialBehavior, ctx))
          val next = interpretMessage(started, ctx, msg)
          if(isUnhandled(next)) {
            buffer.stash(msg)
            Behaviors.same
          } else {
            buffer.unstashAll(ctx.asScala, next)
          }
      }

      def receiveSignal(ctx: akka.actor.typed.ActorContext[T],msg: akka.actor.typed.Signal): Behavior[T] = Behaviors.unhandled

    }
}
