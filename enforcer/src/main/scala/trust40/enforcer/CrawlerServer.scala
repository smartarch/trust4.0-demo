package trust40.enforcer
/*
import java.math.BigInteger
import java.net.URL
import java.security.MessageDigest

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorRef, ActorSystem, OneForOneStrategy, Props, Stash, Terminated}
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.{complete, get, path, _}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout

import scala.collection.mutable
import scala.concurrent.duration._
import scala.io.StdIn


object Crawler {
  def props(noOfWorkers: Int = 8, hostConcurrency: Int = 2) = Props(new Crawler(noOfWorkers, hostConcurrency))

  final case class GetPageHash(url: URL)
  final case class AddLinks(urls: Seq[URL])
  final case class Hash(url: URL, hash: Array[Byte])
}


class Crawler(private val noOfWorkers: Int, private val hostConcurrency: Int) extends Actor with Stash {
  import Crawler._

  val log = Logging(context.system, this)

  override val supervisorStrategy = OneForOneStrategy() {
    case _: Exception => Stop
  }

  private val toBeProcessed = mutable.Set.empty[URL]
  private val processing = mutable.Set.empty[URL]
  private val processed = mutable.Set.empty[URL]

  private val freeWorkers = mutable.Set.empty[ActorRef]
  private val busyWorkers = mutable.Map.empty[ActorRef, URL]

  private val hostLoad = mutable.Map.empty[String, Int]

  private val digests = mutable.SortedMap.empty[URL, Array[Byte]](Ordering.by(_.toString))

  private var requestor: ActorRef = _

  for (i <- 1 to noOfWorkers) {
    val worker = context.actorOf(Worker.props)
    freeWorkers += worker
  }


  def checkWork(): Unit = {
    if (toBeProcessed.isEmpty && processing.isEmpty) {
      log.info(s"Processing finished")

      val msgDigest = MessageDigest.getInstance("MD5")

      for (hash <- digests.values) {
        msgDigest.update(hash)
      }

      val hashHex = new BigInteger(1, msgDigest.digest).toString(16)

      requestor ! hashHex

      processed.clear()

      context.become(receive)
      unstashAll()


    } else {
      for (url <- toBeProcessed) {
        if (!freeWorkers.isEmpty) {
          val worker = freeWorkers.head

          val host = url.getHost
          val noOfWorkers = hostLoad.getOrElse(host, 0)

          if (noOfWorkers < 2) {
            toBeProcessed -= url
            processing += url
            hostLoad(host) = noOfWorkers + 1
            freeWorkers -= worker
            busyWorkers(worker) = url

            worker ! Worker.ProcessUrl(url)
          }

        } else {
          return
        }
      }
    }
  }


  def receive: Receive = {
    case GetPageHash(url) =>
      log.info(s"Starting processing for $url")

      toBeProcessed += url

      requestor = sender()
      context.become(inProcessing)
      checkWork()
  }


  def inProcessing: Receive = {
    case GetPageHash(url) =>
      stash()


    case AddLinks(urls) =>
      for (url <- urls) {
        if (!processed.contains(url) && !processing.contains(url)) {
          toBeProcessed += url
        }
      }

      checkWork()


    case Hash(url, hash) =>
      processing -= url

      val host = url.getHost
      val noOfWorkers = hostLoad.getOrElse(host, 0)
      hostLoad(host) = noOfWorkers - 1
      freeWorkers += sender()
      busyWorkers -= sender()

      processed += url

      digests(url) = hash

      checkWork()
  }
}


object CrawlerServer {
  def main(args: Array[String]) {

    implicit val system = ActorSystem("CrawlerServer")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    implicit val timeout = Timeout(10 seconds)

    val crawler = system.actorOf(Crawler.props(8, 2))

    val route =
      path("crawl") {
        parameter('url) { url =>
          get {
            complete((crawler ? Crawler.GetPageHash(new URL(url))).mapTo[String])
          }
        }
      }

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

    println("Listening on localhost:8080.")
    println("Press ENTER to finish.")
    StdIn.readLine()

    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())

  }
}
*/