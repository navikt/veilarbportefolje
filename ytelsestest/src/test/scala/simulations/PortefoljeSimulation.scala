package simulations

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import no.nav.sbl.gatling.login.LoginHelper
import org.slf4j.LoggerFactory
import utils.{Helpers, RequestFilter, RequestTildelingVeileder, AktivitetRequestFilter}
import java.util.concurrent.TimeUnit

import scala.concurrent.duration._
import scala.util.Random

class PortefoljeSimulation extends Simulation {
  private val logger = LoggerFactory.getLogger(PortefoljeSimulation.this.getClass)
  private val brukernavn = csv(System.getProperty("BRUKERE", "brukere_t3.csv")).circular

  private val usersPerSecEnhet = Integer.getInteger("USERS_PER_SEC", 1).toInt
  private val duration = Integer.getInteger("DURATION", 1).toInt
  private val baseUrl = System.getProperty("BASEURL", "https://app-t3.adeo.no")
  private val loginUrl = System.getProperty("LOGINURL", "https://isso-t.adeo.no")
  private val veilederPassword = System.getProperty("USER_PASSWORD", "!!CHANGE ME!!")
  private val oidcUser = System.getProperty("OIDC_USER", "veilarblogin-t3")
  private val enheter = System.getProperty("ENHETER", "1001").split(",")
  private val rapporterSolrSamlet = System.getProperty("RAPPORTER_SOLR_SAMLET", "true").toBoolean;
  private val rampUp = System.getProperty("RAMP_UP", "false").toBoolean;
  private val appnavn = "veilarbpersonflatefs"
  private val enhetsFeeder = enheter.map(enhet => Map("enhet" -> enhet.trim)).circular
  private val veilederForTildeling1 = System.getProperty("VEIL_1", "X905111")
  private val veilederForTildeling2 = System.getProperty("VEIL_2", "X905112")
  private val brukereForTildeling = System.getProperty("BRUKERE_TIL_VEILEDER", "!!CHANGE ME!!")
  private val portefoljeVersion2 = System.getProperty("PORTEFOLJE_V2", "false").toBoolean

  val mapper = new ObjectMapper() with ScalaObjectMapper

  private val portefoljeApiUrl = if (portefoljeVersion2) "api/v2" else "api"

  private val httpProtocol = http
    .baseURL(baseUrl)
    .inferHtmlResources()
    .acceptHeader("image/png,image/*;q=0.8,*/*;q=0.5")
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("nb-no,nb;q=0.9,no-no;q=0.8,no;q=0.6,nn-no;q=0.5,nn;q=0.4,en-us;q=0.3,en;q=0.1")
    .contentTypeHeader(HttpHeaderValues.ApplicationJson)
    .userAgentHeader("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:30.0) Gecko/20100101 Firefox/30.0")
    .disableWarmUp
    .silentResources
    .extraInfoExtractor { extraInfo => List(Helpers.getInfo(extraInfo)) }

  private def login() = {
    exec(session => session.set("veilederPassword", veilederPassword))
      .exec(session => session.set("veilederUsername", session("username").as[String]))
      .exec(LoginHelper.loginOidc(loginUrl, oidcUser, baseUrl))
  }

  private def veilederInfo() = {
    exec(Helpers.httpGetSuccessWithResonse("enheter", "/veilarbveileder/api/veileder/enheter", "enhetliste"))
      .exec(Helpers.httpGetSuccessWithResonse("veileder", "/veilarbveileder/api/veileder/me", "fornavn"))

  }

  private def rapporterMedNavn(navn: String): String = {
    if (rapporterSolrSamlet) "portefolje-solrsporring" else navn
  }

  private def veilederTildelinger(fra: String, til: String): Array[RequestTildelingVeileder] = {
    brukereForTildeling.split(",").map(fnr => RequestTildelingVeileder(fnr, fra, til))
  }

  private val portefoljeScenario = scenario("Portefolje: Enhet")
    .feed(brukernavn)
    .feed(enhetsFeeder)
    .exec(login)
    .pause(500 milliseconds)
    .exec(Helpers.httpGetSuccessWithResonse("tekster portefolje", "/veilarbportefoljeflatefs/api/tekster", "filtrering"))
    .exec(veilederInfo)
    .exec(Helpers.httpGetSuccessWithResonse("statustall", session => s"/veilarbportefolje/${portefoljeApiUrl}/enhet/${session("enhet").as[String]}/statustall", "nyeBrukere"))
    .exec(
      Helpers.httpPostPaginering(rapporterMedNavn("portefoljefilter nye brukere"), session => s"/veilarbportefolje/${portefoljeApiUrl}/enhet/${session("enhet").as[String]}/portefolje")
        .body(Helpers.toBody(RequestFilter()))
        .check(status.is(200))
        .check(regex("antallTotalt").exists)
      // .check(bodyString.saveAs("resp"))
    )
    //.exec(Helpers.printSavedVariableToConsole("resp"))
    .pause(1 second)
    .exec(veilederInfo)
    .pause(1 second, 3 seconds)
    .exec(
      Helpers.httpPostPaginering(rapporterMedNavn("portefoljefilter alder"), session => s"/veilarbportefolje/${portefoljeApiUrl}/enhet/${session("enhet").as[String]}/portefolje")
        .body(Helpers.toBody(RequestFilter(alder = List("20-24", "30-39"))))
        .check(status.is(200))
        .check(regex("antallTotalt").exists)
    )
    .pause(1 second, 3 seconds)
    .exec(
      Helpers.httpPostPaginering(rapporterMedNavn("portefoljefilter alder, kjoenn og foedselsdag"), session => s"/veilarbportefolje/${portefoljeApiUrl}/enhet/${session("enhet").as[String]}/portefolje")
        .body(Helpers.toBody(RequestFilter(
          alder = List("20-24", "30-39"),
          kjonn = List("M"),
          fodselsdagIMnd = List("1", "2")
        )))
        .check(status.is(200))
        .check(regex("antallTotalt").exists)
    )
    .pause(1 second, 3 seconds)
    .exec(
      Helpers.httpPostPaginering(rapporterMedNavn("portefoljefilter tiltak"), session => s"/veilarbportefolje/${portefoljeApiUrl}/enhet/${session("enhet").as[String]}/portefolje")
        .body(Helpers.toBody(RequestFilter(
          aktiviteter = AktivitetRequestFilter(TILTAK = "JA")
        )))
        .check(status.is(200))
        .check(regex("antallTotalt").exists)
    )
    .exec(
        Helpers.httpPostPaginering("tildele veileder", session => s"/veilarboppfolging/api/tilordneveileder/")
          .body(Helpers.toBody(veilederTildelinger(fra = null, til = veilederForTildeling1)))
          .check(status.is(200))
          .check(regex("resultat").exists)
    )
    .pause(1 second, 3 seconds)
    .exec(
      Helpers.httpPostPaginering("tildele veileder", session => s"/veilarboppfolging/api/tilordneveileder/")
        .body(Helpers.toBody(veilederTildelinger(fra = veilederForTildeling1, til = veilederForTildeling2)))
        .check(status.is(200))
        .check(regex("resultat").exists)
    )
    .pause(1 second, 3 seconds)
    .exec(
      Helpers.httpPostPaginering("tildele veileder", session => s"/veilarboppfolging/api/tilordneveileder/")
        .body(Helpers.toBody(veilederTildelinger(fra = veilederForTildeling2, til = veilederForTildeling1)))
        .check(status.is(200))
        .check(regex("resultat").exists)
    )
    .pause(1 second, 3 seconds)
    .exec(Helpers.httpGetSuccessWithResonse("veilederoversikt", session => s"/veilarbportefolje/${portefoljeApiUrl}/enhet/${session("enhet").as[String]}/portefoljestorrelser", "facetResults"))

    .pause(1 second, 3 seconds)
    .exec(
       Helpers.httpPostPaginering(rapporterMedNavn("veileders portefolje"), session => s"/veilarbportefolje/${portefoljeApiUrl}/veileder/${session("username").as[String]}/portefolje?enhet=${session("enhet").as[String]}", "0")
         .body(Helpers.toBody(RequestFilter(brukerstatus = null)))
         .check(status.is(200))
         .check(regex("antallTotalt").exists)
    )


    setUp (
      if (rampUp) portefoljeScenario.inject(rampUsers(600) over (600 seconds), rampUsers(1800) over (600 seconds), constantUsersPerSec(usersPerSecEnhet) during duration.minutes)
      else portefoljeScenario.inject(constantUsersPerSec(usersPerSecEnhet) during duration.minutes)
    )
      .protocols(httpProtocol)
      .assertions(global.successfulRequests.percent.gte(99))
}
