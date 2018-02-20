import org.joda.time.DateTime
import play.api.libs.json._

trait DateTimeJsonFormat {
  def jodaDateTimeFormat = "dd/MM/yyyy HH:mm:ss"

  implicit val dateTimeWrites: Writes[DateTime] = JodaWrites.jodaDateWrites(jodaDateTimeFormat)
  implicit val dateTimeReads: Reads[DateTime] = JodaReads.jodaDateReads(jodaDateTimeFormat)
  implicit val dateTimeFormat: Format[DateTime] = Format(dateTimeReads, dateTimeWrites)
}