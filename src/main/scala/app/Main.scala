package app

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import execute.akkaFunctions
//import execute.akkaFunctions

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

class LiliumJob() extends Actor with ActorLogging {
  override def receive: Receive = { case _ =>
    mint()
  }

  def mint(): Unit = {
    val akka = new akkaFunctions

    akka.main()
  }
}

object Main extends App {

  override def main(args: Array[String]): Unit = { //main function that runs with jar

    val schedulerActorSystem = ActorSystem("LiliumBot")

    val jobs: ActorRef = schedulerActorSystem.actorOf(
      Props(
        new LiliumJob()
      ),
      "scheduler"
    )

    schedulerActorSystem.scheduler.scheduleAtFixedRate(
      initialDelay = 2.seconds,
      interval = 60.seconds,
      receiver = jobs,
      message = ""
    )

  }

}
