package trust40

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, JsNumber, JsValue, JsonFormat, deserializationError}
import trust40.k4case.{Position, Simulation, SimulationState, WorkerState}

trait MarshallersSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit object SimulationStateStateJsonFormat extends JsonFormat[Simulation.State.State] {
    def write(x: Simulation.State.State) = JsNumber(x.id)
    def read(value: JsValue) = value match {
      case JsNumber(x) => Simulation.State(x.toInt)
      case x => deserializationError("Expected Int as JsNumber, but got " + x)
    }
  }

  implicit val positionFormat = jsonFormat2(Position)
  implicit val workerStateFormat = jsonFormat1(WorkerState)
  implicit val simulationStateFormat = jsonFormat4(SimulationState)
}
