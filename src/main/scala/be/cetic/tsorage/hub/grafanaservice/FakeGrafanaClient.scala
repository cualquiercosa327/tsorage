package be.cetic.tsorage.hub.grafanaservice

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpEntity, HttpMethods, HttpRequest, HttpResponse}
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.stream.ActorMaterializer

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

object FakeGrafanaClient {
  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    // Test the "/" route.
    var uri = s"http://${GrafanaBackend.host}:${GrafanaBackend.port}/" // The "/" route.
    var responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = uri))
    responseFuture
      .onComplete {
        case Success(response) => println(response)
        case Failure(failure) => failure.printStackTrace()
      }

    // Test the search route ("/search")
    var content = HttpEntity(`application/json`,
      """
      {
        "target":"upper_50"
      }
      """.stripMargin)

    uri = s"http://${GrafanaBackend.host}:${GrafanaBackend.port}/search" // The search route.
    responseFuture = Http().singleRequest(HttpRequest(HttpMethods.POST, uri = uri, entity = content))
    responseFuture
      .onComplete {
        case Success(response) => println(response)
        case Failure(failure) => failure.printStackTrace()
      }

    // Test the query route ("/query")
    content = HttpEntity(`application/json`,
      """
      {
        "panelId":1,
        "range":{
          "from":"2019-09-20T16:00:00.000Z",
          "to":"2019-09-21T07:30:00.000Z",
          "raw":{
            "from":"now-16h",
            "to":"now"
          }
        },
        "rangeRaw":{
          "from":"now-16h",
          "to":"now"
        },
        "interval":"1h",
        "intervalMs":3600000,
        "targets":[
          {
            "target":"temperature",
            "refId":"A",
            "type":"timeserie"
          },
          {
            "target":"pressure",
            "refId":"B",
            "type":"timeserie"
          }
        ],
        "format":"json",
        "maxDataPoints":5
      }
      """.stripMargin)
    //content = HttpEntity(`application/json`, """{"requestId":"Q119","timezone":"","panelId":2,"dashboardId":null,"range":{"from":"2019-09-24T08:13:20.835Z","to":"2019-09-24T14:13:20.835Z","raw":{"from":"now-6h","to":"now"}},"interval":"30s","intervalMs":30000,"targets":[{"target":"humidity","refId":"A","type":"timeserie"}],"maxDataPoints":840,"scopedVars":{"__interval":{"text":"30s","value":"30s"},"__interval_ms":{"text":"30000","value":30000}},"startTime":1569334400839,"rangeRaw":{"from":"now-6h","to":"now"},"adhocFilters":[]}""".stripMargin)

    uri = s"http://${GrafanaBackend.host}:${GrafanaBackend.port}/query" // "/query" route.
    responseFuture = Http().singleRequest(HttpRequest(HttpMethods.POST, uri = uri, entity = content))
    responseFuture
      .onComplete {
        case Success(response) => println(response)
        case Failure(failure) => failure.printStackTrace()
      }


    // Test the annotation route ("/annotations").
    content = HttpEntity(`application/json`,
      """
      {
         "range":{
            "from":"2019-09-20T16:00:00.000Z",
            "to":"2019-09-20T21:30:00.000Z"
         },
         "rangeRaw":{
            "from":"now-3h",
            "to":"now"
         },
         "annotation":{
            "datasource":"generic datasource",
            "enable":true,
            "name":"annotation name"
         },
         "dashboard":"DashboardModel"
      }
      """.stripMargin)

    uri = s"http://${GrafanaBackend.host}:${GrafanaBackend.port}/annotations" // The annotation route.
    responseFuture = Http().singleRequest(HttpRequest(HttpMethods.POST, uri = uri, entity = content))
    responseFuture
      .onComplete {
        case Success(response) => println(response)
        case Failure(failure) => failure.printStackTrace()
      }
  }
}
