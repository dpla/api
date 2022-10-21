package dpla.api.v2.analytics

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import dpla.api.v2.analytics.AnalyticsClient.{AnalyticsClientCommand, TrackFetch, TrackSearch}

object ITMockAnalyticsClient {

  def apply(): Behavior[AnalyticsClientCommand] = {
    Behaviors.receiveMessage[AnalyticsClientCommand] {

      case TrackSearch(_, _, _, _, _) =>
        Behaviors.same

      case TrackFetch(_, _, _, _) =>
        Behaviors.same

      case _ =>
        Behaviors.unhandled
    }
  }
}
