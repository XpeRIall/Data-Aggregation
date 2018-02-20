import org.joda.time.DateTime
import play.api.libs.json.{Json, OFormat}

case class ReadingResponse(id: Long,
                           value: Double,
                           adaptorId: String,
                           sensorId: String,
                           createdAt: DateTime,
                           updatedAt: DateTime)

case class Readings(data: List[ReadingResponse])

trait ReadingFormatter extends DateTimeJsonFormat {
  implicit val readingResponse: OFormat[ReadingResponse] = Json.format[ReadingResponse]
  implicit val readings: OFormat[Readings] = Json.format[Readings]
}