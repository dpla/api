package dpla.api.v2.analytics

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

object ITMockAnalyticsClient {

  def apply(): Behavior[AnalyticsClientCommand] = {
    Behaviors.receiveMessage[AnalyticsClientCommand] {

      case TrackSearch(_, _, _, _) =>
        Behaviors.same

      case _ =>
        Behaviors.unhandled
    }
  }
}
