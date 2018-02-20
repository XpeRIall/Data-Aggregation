import org.joda.time.DateTime
import play.api.libs.json.{Json, OFormat}

case class Adaptor(id: String,
                   name: String,
                   dimmer: Int,
                   locationId: String,
                   dimmer_prev: Int,
                   wasPaired: Boolean,
                   roomId: Option[String],
                   connectionId: Option[String],
                   sensorReadingProcessorName: String,
                   sensorReadingProcessorState: String,
                   mac: Option[String],
                   serialNr: Option[String],
                   hardware: Option[String],
                   createdAt: DateTime,
                   updatedAt: DateTime)


trait AdaptorFormatter extends DateTimeJsonFormat {
  //  type Adaptors = Seq[Adaptor]
  implicit val adaptor: OFormat[Adaptor] = Json.format[Adaptor]
  //  implicit val adaptors: OFormat[Adaptors] = Json.format[Adaptors]
}
