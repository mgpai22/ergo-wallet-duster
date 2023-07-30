package app

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import execute.akkaFunctions

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, DurationInt}

class PhoenixJob() extends Actor with ActorLogging {
  override def receive: Receive = { case _ =>
    execute()
  }

  def execute(): Unit = {
    val akka = new akkaFunctions

    akka.main()
  }
}

object Main extends App {
  val schedulerActorSystem = ActorSystem("PhoenixBot")
  val jobs: ActorRef = schedulerActorSystem.actorOf(
    Props(
      new PhoenixJob()
    ),
    "scheduler"
  )
  schedulerActorSystem.scheduler.scheduleAtFixedRate(
    initialDelay = 2.seconds,
    interval = 60.seconds,
    receiver = jobs,
    message = ""
  )
  // Keep the main thread alive until the actor system is manually terminated.
  Await.result(schedulerActorSystem.whenTerminated, Duration.Inf)
}
