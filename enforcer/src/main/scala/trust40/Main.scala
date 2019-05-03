package trust40

import java.time.{Duration, LocalDateTime}

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.ActorMaterializer
import trust40.enforcer.MarshallersSupport
import trust40.k4case._

import scala.collection.mutable
import scala.io.StdIn

object Main extends MarshallersSupport {
  def main(args: Array[String]) {

    implicit val system = ActorSystem("trust40-enforcer")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher


    val startTime = LocalDateTime.parse("2018-12-03T08:00:00")
    val endTime = startTime plus Duration.ofHours(10)


    val scenarioSpec = TestScenario.createScenarioSpec(factoriesCount=1, workersPerWorkplaceCount=5, workersOnStandbyCount=2)
    val scenario = new TestScenario(scenarioSpec)

    val resolver = system.actorOf(Resolver.props(scenario), name = "resolver")
    val enforcer = system.actorOf(Enforcer.props(resolver), name = "enforcer")

    val workers = mutable.ListBuffer.empty[ActorRef]
    for (factoryId <- scenario.factoryIds) {
      // foremen
      workers += system.actorOf(SimulatedWorkerInShiftA.props(enforcer, s"$factoryId-A-foreman", startTime), name = s"$factoryId-A-foreman")
      workers += system.actorOf(SimulatedWorkerInShiftB.props(enforcer, s"$factoryId-B-foreman", startTime), name = s"$factoryId-B-foreman")
      workers += system.actorOf(SimulatedWorkerInShiftC.props(enforcer, s"$factoryId-C-foreman", startTime), name = s"$factoryId-C-foreman")

      // workers
      for (idx <- 1 to scenarioSpec.workersPerWorkplaceCount) {
        workers += system.actorOf(SimulatedWorkerInShiftA.props(enforcer, f"$factoryId%s-A-worker-$idx%03d", startTime), name = f"$factoryId%s-A-worker-$idx%03d")
        workers += system.actorOf(SimulatedWorkerInShiftB.props(enforcer, f"$factoryId%s-B-worker-$idx%03d", startTime), name = f"$factoryId%s-B-worker-$idx%03d")
        workers += system.actorOf(SimulatedWorkerInShiftC.props(enforcer, f"$factoryId%s-C-worker-$idx%03d", startTime), name = f"$factoryId%s-C-worker-$idx%03d")
      }
    }


    var currentTime = startTime
    while (currentTime.isBefore(endTime)) {
      for (worker <- workers) {
        worker ! Worker.Step(currentTime)
      }

      enforcer ! Enforcer.Step(currentTime)

      Thread.sleep(100)

      currentTime = currentTime plusSeconds(10)
    }


    println("Press ENTER to finish.")
    StdIn.readLine()

    system.terminate()

    /*
        val route =
          pathPrefix("api") {
            pathPrefix("persons") {
              path(IntNumber) { id =>
                get {
                  complete(Person(id, "John", "Doe", 3447.45))
                } ~
                put {
                  entity(as[Person]) { person =>
                    println(s"PUT: $person")
                    complete(OK)
                  }
                }
              } ~
              pathEnd { // This has to be here, otherwise, the POST handler would react also to /api/persons/xyz
                post {
                  entity(as[Person]) { person =>
                    println(s"POST: $person")
                    complete((OK, 2))
                  }
                }
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
     */
  }
}
