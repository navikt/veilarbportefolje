package utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import io.gatling.core.Predef._
import io.gatling.core.body.CompositeByteArrayBody
import io.gatling.core.session.Expression
import io.gatling.http.Predef._
import io.gatling.http.request.builder._
import io.gatling.http.request.ExtraInfo

object Helpers {
    val mapper = new ObjectMapper() with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)

    def httpGetSuccess(navn: String, uri: Expression[String]): HttpRequestBuilder = {
        http(navn)
            .get(uri)
            .check(status.is(200))
    }

    def httpPostPaginering(navn: String, uri: Expression[String], fra: String = "20", sortDirection: String = "ikke_satt", sortField: String = "ikke_satt"): HttpRequestBuilder = {
        http(navn)
            .post(uri)
            .queryParam("fra", fra)
            .queryParam("antall", "20")
            .queryParam("sortDirection", sortDirection)
            .queryParam("sortField", sortField)
            .check(status.is(200))
    }

    def toBody(obj: Object): CompositeByteArrayBody = {
        StringBody(mapper.writeValueAsString(obj))
    }

    def getInfo(extraInfo: ExtraInfo): String = {
        if (extraInfo.response.statusCode.get == 200) {
            "URL : " + extraInfo.request.getUrl + ", " +
              "Request : " + extraInfo.request.getStringData + ", " +
              "Response : " + extraInfo.response.body.string + ", " +
              "Session-data : " + extraInfo.session
        } else {
            ""
        }
    }

  def printToConsole(tekst: Expression[String]) = {
    exec(session => {
      println(tekst)
      session
    })
  }
}
