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
  private val appnavn = "veilarbpersonflatefs"
  private val enhetsFeeder = enheter.map(enhet => Map("enhet" -> enhet.trim)).circular
  private val veilederForTildeling1 = System.getProperty("VEIL_1", "X905111")
  private val veilederForTildeling2 = System.getProperty("VEIL_2", "X905112")
  private val brukerForTildeling = System.getProperty("BRUKER_TIL_VEILEDER", "!!CHANGE ME!!")

  val mapper = new ObjectMapper() with ScalaObjectMapper

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

  private def rapporterMedNavn(navn: String): String = {
    if (rapporterSolrSamlet) "portefolje-solrsporring" else navn
  }

  private val portefoljeScenario = scenario("Portefolje: Enhet")
    .feed(brukernavn)
    .feed(enhetsFeeder)
    .exec(login)
    .pause(500 milliseconds)
    .exec(Helpers.httpGetSuccess("tekster portefolje", "/veilarbportefoljeflatefs/tjenester/tekster"))
    .exec(Helpers.httpGetSuccess("enheter", "/veilarbveileder/api/veileder/enheter"))
    .exec(Helpers.httpGetSuccess("veileder", "/veilarbveileder/api/veileder/me"))
    .exec(Helpers.httpGetSuccess("statustall", session => s"/veilarbportefolje/api/enhet/${session("enhet").as[String]}/statustall"))
    .exec(
      Helpers.httpPostPaginering(rapporterMedNavn("portefoljefilter nye brukere"), session => s"/veilarbportefolje/api/enhet/${session("enhet").as[String]}/portefolje")
        .body(Helpers.toBody(RequestFilter()))
        .check(status.is(200))
    )
    .pause(1 second)
    .exec(Helpers.httpGetSuccess("enheter", "/veilarbveileder/api/veileder/enheter"))
    .exec(Helpers.httpGetSuccess("veileder", "/veilarbveileder/api/veileder/me"))
    .pause(1 second, 3 seconds)
    .exec(
      Helpers.httpPostPaginering(rapporterMedNavn("portefoljefilter alder"), session => s"/veilarbportefolje/api/enhet/${session("enhet").as[String]}/portefolje")
        .body(Helpers.toBody(RequestFilter(alder = List("20-24", "30-39"))))
        .check(status.is(200))
    )
    .pause(1 second, 3 seconds)
    .exec(
      Helpers.httpPostPaginering(rapporterMedNavn("portefoljefilter alder, kjoenn og foedselsdag"), session => s"/veilarbportefolje/api/enhet/${session("enhet").as[String]}/portefolje")
        .body(Helpers.toBody(RequestFilter(
          alder = List("20-24", "30-39"),
          kjonn = List("M"),
          fodselsdagIMnd = List("1", "2")
        )))
        .check(status.is(200))
    )
    .pause(1 second, 3 seconds)
    .exec(
      Helpers.httpPostPaginering(rapporterMedNavn("portefoljefilter tiltak"), session => s"/veilarbportefolje/api/enhet/${session("enhet").as[String]}/portefolje")
        .body(Helpers.toBody(RequestFilter(
          aktiviteter = AktivitetRequestFilter(TILTAK = "JA")
        )))
        .check(status.is(200))
    )
    .exec(
      Helpers.httpPostPaginering("tildele veileder", session => s"/veilarboppfolging/api/tilordneveileder/")
        .body(Helpers.toBody(List(RequestTildelingVeileder(
          brukerFnr = brukerForTildeling,
          fraVeilederId = null,
          tilVeilederId = veilederForTildeling1
        ))))
        .check(status.is(200))
    )
    .pause(1 second, 3 seconds)
    .exec(
      Helpers.httpPostPaginering("tildele veileder", session => s"/veilarboppfolging/api/tilordneveileder/")
        .body(Helpers.toBody(List(RequestTildelingVeileder(
          brukerFnr = brukerForTildeling,
          fraVeilederId = veilederForTildeling1,
          tilVeilederId = veilederForTildeling2
        ))))
        .check(status.is(200))
    )
    .pause(1 second, 3 seconds)
    .exec(
      Helpers.httpPostPaginering("tildele veileder", session => s"/veilarboppfolging/api/tilordneveileder/")
        .body(Helpers.toBody(List(RequestTildelingVeileder(
          brukerFnr = brukerForTildeling,
          fraVeilederId = veilederForTildeling2,
          tilVeilederId = veilederForTildeling1
        ))))
        .check(status.is(200))
    )
    .pause(1 second, 3 seconds)
    .exec(Helpers.httpGetSuccess("veilederoversikt", session => s"/veilarbportefolje/api/enhet/${session("enhet").as[String]}/portefoljestorrelser"))

    .pause(1 second, 3 seconds)
    .exec(
      Helpers.httpPostPaginering(rapporterMedNavn("veileders portefolje"), session => s"/veilarbportefolje/api/veileder/${session("username").as[String]}/portefolje?enhet=${session("enhet").as[String]}", "0")
        .body(Helpers.toBody(RequestFilter(brukerstatus = null)))
        .check(status.is(200))
    )

  setUp(
    portefoljeScenario.inject(rampUsers(600) over (600 seconds), rampUsers(1800) over (600 seconds), constantUsersPerSec(usersPerSecEnhet) during duration.minutes)
  )
    .protocols(httpProtocol)
    .assertions(global.successfulRequests.percent.gte(99))
}
