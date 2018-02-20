import java.io.File

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.github.tototoshi.csv._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.json._
import play.api.libs.ws.{StandaloneWSClient, _}
import play.api.libs.ws.ahc._

import scala.collection.mutable.ListBuffer
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

object Client extends ReadingFormatter
  with UserFormatter
  with UserInfoFormatter
  with AdaptorFormatter
  with LocationFormatter {

  import play.api.libs.ws.JsonBodyWritables._

  import scala.concurrent.ExecutionContext.Implicits._

  implicit object MyFormat extends DefaultCSVFormat {
    override val delimiter = ','
  }

  val name = "Attic-new2"

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
    //val wsClient = StandaloneAhcWSClient()

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


    val userIds = List(
      "27b77aa0-f623-4419-b9c6-6a039a122321",
      "dc31e90b-fc39-4c2b-822e-42cb9b0b319a",
      "be3569b1-ed7a-4d8a-b85c-543657f7fea7",
      "71508623-7ac1-4e2f-9477-2a82a40e9242",
      "c6b4f0e0-349e-49a8-a770-cadefa1914d7",
      "aa5e52b4-c300-467d-8430-50c9dd2cefba")

    val locationIds = List(
      "839acf9c-175d-4697-b8c2-c60a9836675b",
      "41679b68-1d86-4796-bf7f-9f83f059e382",
      "6d93da6b-9232-494d-891f-c8e5a5ecbba0",
      "1e501c79-8c0c-4a90-b449-df809728ada3",
      "55cb28ef-7121-439e-a8c7-72fd9b825783",
      "f5e49b65-a49d-4099-ad18-e61904053f71")

    val adaptorsIds = List(
      "b14f90ed-a2c4-44a8-a904-8e5319e9c5e1",
      "c8325f9e-4968-46ef-94c7-385c736ba2b8",
      "95f1b6cb-385c-4732-a1d1-8bcf0ef0e4af",
      "06e3e539-95ed-4ff6-b573-9b374bb7ad39",
      "1488cc8d-95a1-4d4c-a38b-a4ad47182cbe",
      "ca4f41fb-f6e5-4394-8565-c43f0c28be7b",
      "11cfad3d-6b27-42e4-ace3-c12e4bf12ed9",
      "1430a0b5-337c-4717-9e9b-dddecf9223b9",
      "174bef56-281b-4b90-a9be-48c44ecddc9f",
      "f1433f62-0364-4156-96ae-37e08e542793",
      "544e4e0e-190b-4917-9822-aec7fefd0293",
      "93962264-b75d-42c4-ab0e-1e9e6e1e739f",
      "10ac9c8e-8e51-43fa-9334-b25cb6567ab5",
      "fdb3fe2c-51a3-4286-b856-bd1059dd398a",
      "b8c604bd-b0b5-432f-8c89-a352ab79b3fd",
      "5ac4e669-8196-41ee-933a-66fde2787776")


    Await.result(authorize flatMap {
      token =>
        val adaptorInfoWriter: CSVWriter = CSVWriter.open(new File("/Users/xperiall/Work/OP_8/data/src/main/UserStats/adaptorsData.csv"), append = true)
        Future.sequence(adaptorsIds.map(adaptorId => {
          AggregateAdaptorsData(adaptorInfoWriter, token, adaptorId)
            .recoverWith {
              case e: Throwable => println(e); Future.successful()
            }
        }))
    }, 20 second)
  }

  //val token: StringBuilder = new StringBuilder

  def authorize(implicit materializer: ActorMaterializer): Future[String] = {
    val userData = Json.obj("email" -> "dmva@opinov8.com", "password" -> "12345678")
    StandaloneAhcWSClient()
      .url("http://13.93.44.232:5090/api/auth/signin")
      .addHttpHeaders("Content-Type" -> "application/json")
      .post(userData).flatMap { wsResponse =>
      if (!(200 to 299).contains(wsResponse.status)) {
        Future.failed(new Exception(s"Status : ${wsResponse.status}. Body : ${wsResponse.body}"))
      }

      val resultParsed: JsValue = Json.parse(wsResponse.body)
      val resValidated: JsResult[User] = resultParsed.validate[User]

      resValidated match {
        case s: JsSuccess[User] =>
          Future.successful("Bearer " + s.get.jwtToken)
        case e: JsError => Future.failed(new Exception(e.toString))
      }

    }
  }

  def PrepareDataForNN(wsClient: StandaloneWSClient, adapId: String, token: String): Future[Unit] = {
    // checking id of adaptor to get right csv from station
    var input_file = new String
    adapId match {
      case "b9c53aee-18e9-4587-b3af-7e72e6d73c5c" => input_file = "/Users/xperiall/Work/OP_8/data/src/main/Input/Jan_7-9_02/Station_9_2_2018.csv"
      case "952992f6-6871-419d-b99f-326f569516a9" => input_file = "/Users/xperiall/Work/OP_8/data/src/main/Input/Jan_7-9_02/Station_9_2_2018.csv"
      case "ceac6f3c-ecfb-43c2-b1ea-41f65f9129e5" => input_file = "/Users/xperiall/Work/OP_8/data/src/main/Input/Jan_6_02/Station_6_2_2018.csv"
      case "ccc8f193-7ecd-4987-ab3c-32a350d2da6e" => input_file = "/Users/xperiall/Work/OP_8/data/src/main/Input/Jan_6_02/Station_6_2_2018.csv"
      case "88f00cc9-d83d-4f02-9ccd-450fbccf633e" => input_file = "/Users/xperiall/Work/OP_8/data/src/main/Input/Jan_6_02/Station_6_2_2018.csv"
    }

    val writer: CSVWriter = CSVWriter.open(new File("/Users/xperiall/Work/OP_8/data/src/main/Output/Outdor_7-9_corr.csv"), append = true)
    val reader: CSVReader = CSVReader.open(new File(input_file))

    wsClient
      .url("http://devices.anyware.solutions/adaptors/" + adapId + "/readings/page/1/perpage/200000?sensors=temperature,humidity")
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
                    //                  val _bufTemp = temp_corr
                    //                    .filter((point: Seq[Any]) => (new DateTime(point(1)).getMillis / 1000) == new DateTime(hum(1)).getMillis / 1000)
                    //                  _bufTemp.foreach(res => {
                    //                    humid_corr.foreach(el => {
                    //                      if (new DateTime(el(1)).getMillis / 1000 == new DateTime(res(1)).getMillis / 1000) {
                    println("found")
                    writer.writeRow(Seq(
                      //adapId,
                      //date(1),
                      //name,
                      date.head.toString.toFloat / 100,
                      hum.head.toString.toFloat / 100,
                      //res.head,
                      // el.head,
                      dataPoint(1).toString.toFloat / 100,
                      dataPoint.head.toString.toFloat / 100))
                    //                      }
                    //                    })
                    //                  })
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

  def AggregateUserData(wsClient: StandaloneWSClient, token: String, userId: String): Future[Unit] = {

    val userInfoWriter: CSVWriter = CSVWriter.open(new File("/Users/xperiall/Work/OP_8/data/src/main/UserStats/userInfo.csv"), append = true)

    wsClient
      .url("https://devices.anyware.solutions/users/" + userId)
      .addHttpHeaders("Content-Type" -> "application/json",
        "Authorization" -> token)
      .get()
      .flatMap {
        wsResponse =>
          if (!(200 to 299).contains(wsResponse.status)) {
            Future.failed(new Exception(s"Status : ${wsResponse.status}. Body : ${wsResponse.body}"))
          }
          val jsonResult: JsResult[UserInfo] = Json.parse(wsResponse.body).validate[UserInfo]
          jsonResult.map(user => {
            println(user)
            userInfoWriter.writeRow(Seq(user.id, "XXX", "XXX", "XXX", "XXX", "XXX", user.language, user.createdAt))

          })
          jsonResult match {
            case s: JsSuccess[UserInfo] =>
              Future.successful(s.get)
            case e: JsError => Future.failed(new Exception(e.toString))
          }
      }
  }

  def AggregateLocationUserData(wsClient: StandaloneWSClient, token: String, userId: String): Future[Unit] = {
    val locationInfoWriter: CSVWriter = CSVWriter.open(new File("/Users/xperiall/Work/OP_8/data/src/main/UserStats/userLocationInfo.csv"), append = true)

    wsClient
      .url("https://devices.anyware.solutions/users/" + userId + "/locations")
      .addHttpHeaders("Content-Type" -> "application/json",
        "Authorization" -> token)
      .get()
      .flatMap {
        wsResponse =>
          if (!(200 to 299).contains(wsResponse.status)) {
            Future.failed(new Exception(s"Status : ${wsResponse.status}. Body : ${wsResponse.body}"))
          }
          val jsonResult: JsResult[Seq[Location]] = Json.parse(wsResponse.body).validate[Seq[Location]]
          jsonResult.map(locations => {
            locations.foreach(location => {
              println(location)
              locationInfoWriter.writeRow(Seq(userId, location.id, "XXX", "XXX", "XXX", location.gmtOffset, location.createdAt))
            })
          })
          jsonResult match {
            case s: JsSuccess[Seq[Location]] =>
              Future.successful(s.get)
            case e: JsError => Future.failed(new Exception(e.toString))
          }
      }
  }

  def AggregateAdaptors(wsClient: StandaloneWSClient, token: String, locationId: String): Future[Unit] = {
    val adaptorInfoWriter: CSVWriter = CSVWriter.open(new File("/Users/xperiall/Work/OP_8/data/src/main/UserStats/AdaptorsInfo.csv"), append = true)

    wsClient
      .url("https://devices.anyware.solutions/locations/" + locationId + "/adaptors")
      .addHttpHeaders("Content-Type" -> "application/json",
        "Authorization" -> token)
      .get()
      .flatMap {
        wsResponse =>
          if (!(200 to 299).contains(wsResponse.status)) {
            Future.failed(new Exception(s"Status : ${wsResponse.status}. Body : ${wsResponse.body}"))
          }
          val jsonResult: JsResult[Seq[Adaptor]] = Json.parse(wsResponse.body).validate[Seq[Adaptor]]
          // println(jsonResult)
          jsonResult.foreach(adaptors => {
            adaptors.foreach(adaptor => {
              println(adaptor)
              adaptorInfoWriter.writeRow(Seq(
                adaptor.id,
                adaptor.name,
                adaptor.dimmer,
                adaptor.locationId,
                adaptor.dimmer_prev,
                adaptor.wasPaired,
                adaptor.roomId.map(_.toString()).getOrElse(""),
                adaptor.connectionId.map(_.toString()).getOrElse(""),
                adaptor.mac.map(_.toString()).getOrElse(""),
                adaptor.serialNr.map(_.toString()).getOrElse(""),
                adaptor.hardware.map(_.toString()).getOrElse(""),
                adaptor.createdAt,
                adaptor.updatedAt))
            })
          })
          jsonResult match {
            case s: JsSuccess[Seq[Adaptor]] =>
              Future.successful(s.get)
            case e: JsError => Future.failed(new Exception(e.toString))
          }
      }
  }

  def AggregateAdaptorsData(file: CSVWriter, token: String, adaptorId: String)(implicit materializer: ActorMaterializer): Future[Readings] = {
    StandaloneAhcWSClient()
      .url("https://devices.anyware.solutions/adaptors/" + adaptorId + "/readings/page/1/perpage/10000?sensors=ambient_light,consumption_burn_hours,consumption_watt_hours,humidity,humidity_corrected,humidity_startup,light_dimmer,sound_db_average,sound_db_max,status_notification,temperature,temperature_corrected,temperature_startup")
      .addHttpHeaders("Content-Type" -> "application/json",
        "Authorization" -> token)
      .get()
      .flatMap {
        wsResponse =>
          if (!(200 to 299).contains(wsResponse.status)) {
            println(s"Status : ${wsResponse.status}. Body : ${wsResponse.body}")
            Future.failed(new Exception("failed"))
          } else {
            val jsonResult: JsResult[Readings] = Json.parse(wsResponse.body).validate[Readings]
            //println(s"$adaptorId, $jsonResult")
            jsonResult.map(readings => {
              readings.data.foreach(reading => {
                println(reading)
                file.writeRow(Seq(adaptorId, reading.value, reading.sensorId, reading.createdAt, reading.updatedAt))
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
}

