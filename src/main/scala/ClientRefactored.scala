import java.io.File

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.github.tototoshi.csv._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.json._
import play.api.libs.ws._
import play.api.libs.ws.ahc._

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import play.api.libs.ws.JsonBodyWritables._
import scala.concurrent.ExecutionContext.Implicits._


object ClientRefactored extends ReadingFormatter with UserFormatter {

  implicit object MyFormat extends DefaultCSVFormat {
    override val delimiter = ';'
  }

  val adaptorName = "standard_lamp"

  def main(args: Array[String]): Unit = {
    // Create Akka system for thread and streaming management
    implicit val system: ActorSystem = ActorSystem()
    system.registerOnTermination {
      System.exit(0)
    }
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    // Create the standalone WS client
    // no argument defaults to a AhcWSClientConfig created from
    // "AhcWSClientConfigFactory.forConfig(ConfigFactory.load, this.getClass.getClassLoader)"
    val wsClient = StandaloneAhcWSClient()

    val adaptors = Map(
      "basement" -> "05beac6c-1c2f-4802-94a9-30e6300c6e15",
      "eu_and" -> "05beac6c-1c2f-4802-94a9-30e6300c6e15",
      "eu_and02" -> "90520fdd-88f5-4263-9b94-7a1731f623b3",
      "basement2" -> "3c118036-0e1a-4702-aeab-6c592cd6910c",
      "eu_and03" -> "ce02dc19-51ad-4207-a471-269f520dd806",
      "attic-st" -> "b0302b9b-931d-482e-9c67-21898d977e03",
      "sef02" -> "ccc8f193-7ecd-4987-ab3c-32a350d2da6e",
      "standard_lamp" -> "88f00cc9-d83d-4f02-9ccd-450fbccf633e",
      "garage-st" -> "d10a40ef-7c9d-4dea-a53e-2a15b9e5723d",
      "Attic-new1" -> "b9c53aee-18e9-4587-b3af-7e72e6d73c5c",
      "Attic-new2" -> "952992f6-6871-419d-b99f-326f569516a9",
      "Bathroom3" -> "ceac6f3c-ecfb-43c2-b1ea-41f65f9129e5")

    authorize(wsClient).andThen { case _ =>
      call(wsClient, adaptors(adaptorName), authToken.toString)
        .andThen { case _ => wsClient.close() }
        .andThen { case _ => system.terminate() }
    }
  }

  var authToken: String = new String

  def authorize(wsClient: StandaloneWSClient): Future[Unit] = {
    val userData = Json.obj("email" -> "dmva@opinov8.com", "password" -> "12345678")
    wsClient
      .url("http://13.93.44.232:5090/api/auth/signin")
      .addHttpHeaders("Content-Type" -> "application/json")
      .post(userData).flatMap { wsResponse =>
      if (!(200 to 299).contains(wsResponse.status)) {
        Future.failed(new Exception(s"Status : ${wsResponse.status}. Body : ${wsResponse.body}"))
      }

      val resultParsed: JsValue = Json.parse(wsResponse.body)
      val resValidated: JsResult[User] = resultParsed.validate[User]

      resValidated.map(f => {
        authToken = "Bearer " + f.jwtToken
      })
      Json.parse(wsResponse.body) match {
        case s: JsSuccess[Readings] =>
          Future.successful(s.get)
        case e: JsError => Future.failed(new Exception(e.toString))
      }
    }
  }

  def call(wsClient: StandaloneWSClient, adapId: String, token: String): Future[Unit] = {
    // checking id of adaptor to get right csv from station
    var input_file = new String
    val writer: CSVWriter = CSVWriter.open(new File("/Users/xperiall/Work/OP_8/data/src/main/Output/data_4-6_with_corr.csv"), append = true)
    val reader: CSVReader = CSVReader.open(new File(input_file))

    adapId match {
      case "b9c53aee-18e9-4587-b3af-7e72e6d73c5c" => input_file = "/Users/xperiall/Work/OP_8/data/src/main/Input/Jan_6_02/Jan module_6_2_2018.csv"
      case "952992f6-6871-419d-b99f-326f569516a9" => input_file = "/Users/xperiall/Work/OP_8/data/src/main/Input/Jan_6_02/Jan module_6_2_2018.csv"
      case "ceac6f3c-ecfb-43c2-b1ea-41f65f9129e5" => input_file = "/Users/xperiall/Work/OP_8/data/src/main/Input/Jan_6_02/Station_6_2_2018.csv"
      case "ccc8f193-7ecd-4987-ab3c-32a350d2da6e" => input_file = "/Users/xperiall/Work/OP_8/data/src/main/Input/Jan_6_02/Station_6_2_2018.csv"
      case "88f00cc9-d83d-4f02-9ccd-450fbccf633e" => input_file = "/Users/xperiall/Work/OP_8/data/src/main/Input/Jan_6_02/Station_6_2_2018.csv"
    }

    wsClient
      .url("http://devices.anyware.solutions/adaptors/" + adapId + "/readings/page/1/perpage/200000?sensors=temperature,humidity,temperature_corrected,humidity_corrected")
      .addHttpHeaders(
        "Content-Type" -> "application/json",
        "Authorization" -> token)
      .get()
      .flatMap {
        wsResponse =>
          if (!(200 to 299).contains(wsResponse.status)) {
            Future.failed(new Exception(s"Status : ${wsResponse.status}. Body : ${wsResponse.body}"))
          }

          val jsonResult: JsResult[Readings] = Json.parse(wsResponse.body).validate[Readings]

          // workaround
          val temp = new ListBuffer[Seq[Any]]()
          val humidity = new ListBuffer[Seq[Any]]()

          val temp_corr = new ListBuffer[Seq[Any]]()
          val humid_corr = new ListBuffer[Seq[Any]]()

          val temp_real = new ListBuffer[Seq[Any]]()

          val consump_burn_hours = new ListBuffer[Seq[Any]]()
          val consump_watt_hours = new ListBuffer[Seq[Any]]()
          val light_dimmer = new ListBuffer[Seq[Any]]()

          // seq (humid, temp, datestamp)
          reader.foreach(line => {
            temp_real += Seq(line(3), line(2), DateTimeFormat.forPattern("yyyy/mm/dd HH:mm:ss").parseDateTime(line(1)).plusMonths(1))
          })

          jsonResult.map(readings => {
            readings.data.foreach(reading => {
              reading.sensorId match {
                case "temperature" =>
                  temp += Seq(reading.value, reading.updatedAt.toDateTime.plusHours(1))
                case "humidity" =>
                  humidity += Seq(reading.value, reading.updatedAt.toDateTime.plusHours(1))
                case "temperature_corrected" =>
                  temp_corr += Seq(reading.value, reading.updatedAt.toDateTime.plusHours(1))
                case "humidity_corrected" =>
                  humid_corr += Seq(reading.value, reading.updatedAt.toDateTime.plusHours(1))
                case "consumption_burn_hours" =>
                  consump_burn_hours += Seq(reading.value, reading.updatedAt.toDateTime.plusHours(1))
                case "consumption_watt_hours" =>
                  consump_watt_hours += Seq(reading.value, reading.updatedAt.toDateTime.plusHours(1))
                case "light_dimmer" =>
                  if (reading.value != 0) {
                    light_dimmer += Seq(reading.value, reading.updatedAt.toDateTime.plusHours(1))
                  }
              }
            })
          })

          /*
                  temp_real.foreach(dataPoint => {
                    val ts = new DateTime(dataPoint(2)).getMillis / 1000
                    temp.foreach(date => {
                      val ts1 = new DateTime(date(1)).getMillis / 1000
                      if ((ts1 - ts) < 100
                        && (ts1 - ts) > 0
                        && ((date.head.toString.toDouble - dataPoint(1).toString.toDouble) > 25)
                        && ((date.head.toString.toDouble - dataPoint(1).toString.toDouble) < 18)) {
                        humidity.filter((x: Seq[Any]) => (new DateTime(x(1)).getMillis / 1000) == ts1).foreach(hum => {
                          for ((k, v) <- mapPreparedTotal) {
                            if (k == (new DateTime(date(1)).dayOfMonth().toInterval.getStart.getMillis / 1000)) {
                              println("add")
                              writer.writeRow(Seq(
                                BigDecimal(v / 100).setScale(5, BigDecimal.RoundingMode.HALF_UP).toDouble,
                                date.head.toString.toDouble / 100,
                                hum.head.toString.toDouble / 100,
                                dataPoint(1).toString.toDouble / 100,
                                dataPoint.head.toString.toDouble / 100
                              ))
                            }
                          }
                        })
                      }
                    })
                  })
          */

          /*temp_real.foreach(dataPoint => {
            temp
              .filter(date =>
                (((new DateTime(date(1)).getMillis / 1000) - (new DateTime(dataPoint(2)).getMillis / 1000)) < 150)
                  && ((date.head.toString.toDouble - dataPoint(1).toString.toDouble) > 0)
                  && ((date.head.toString.toDouble - dataPoint(1).toString.toDouble) < 30))
              .foreach(date => {
                humidity
                  .filter((x: Seq[Any]) => (new DateTime(x(1)).getMillis / 1000) == (new DateTime(date(1)).getMillis / 1000))
                  .foreach(hum => {
                    println("found")
                    writer.writeRow(Seq(
                      date.head.toString.toFloat / 100,
                      hum.head.toString.toFloat / 100,
                      dataPoint(1).toString.toFloat / 100,
                      dataPoint.head.toString.toFloat / 100))
                  })

              })
          })*/
          temp_real.foreach(dataPoint => {
            val ts = new DateTime(dataPoint(2)).getMillis / 1000
            temp.foreach(date => {
              val ts1 = new DateTime(date(1)).getMillis / 1000
              if ((ts1 - ts) < 150 && (ts1 - ts) > 0 && ((date.head.toString.toDouble - dataPoint(1).toString.toDouble) > 0)
                && ((date.head.toString.toDouble - dataPoint(1).toString.toDouble) < 30)) {
                humidity
                  .filter((x: Seq[Any]) => (new DateTime(x(1)).getMillis / 1000) == ts1)
                  .foreach(hum => {
                    val _bufTemp = temp_corr
                      .filter((point: Seq[Any]) => (new DateTime(point(1)).getMillis / 1000) == new DateTime(hum(1)).getMillis / 1000)
                    _bufTemp.foreach(res => {
                      humid_corr.foreach(el => {
                        if (new DateTime(el(1)).getMillis / 1000 == new DateTime(res(1)).getMillis / 1000) {
                          println("found")
                          writer.writeRow(Seq(
                            adapId,
                            date(1),
                            adaptorName,
                            date.head.toString.toFloat,
                            hum.head.toString.toFloat,
                            res.head,
                            el.head,
                            dataPoint(1).toString.toFloat,
                            dataPoint.head.toString.toFloat))
                        }
                      })
                    })
                  })
              }
            })
          })
          jsonResult match {
            case s: JsSuccess[Readings] =>
              Future.successful(s.get)
            case e: JsError => Future.failed(new Exception(e.toString))
          }
      }
  }
}

