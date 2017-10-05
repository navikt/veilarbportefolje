package simulations

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import no.nav.sbl.gatling.login.OpenIdConnectLogin
import org.slf4j.LoggerFactory
import utils.{Helpers, RequestFilter, RequestTildelingVeileder, AktivitetRequestFilter}
import java.util.concurrent.TimeUnit

import io.gatling.core.feeder.RecordSeqFeederBuilder

import scala.concurrent.duration._
import scala.util.Random

class PortefoljeSimulation extends Simulation {
  private val logger = LoggerFactory.getLogger(PortefoljeSimulation.this.getClass)
  private val brukernavn = csv(System.getProperty("BRUKERE", "brukere_t3.csv")).circular

  private val usersPerSecEnhet = Integer.getInteger("USERS_PER_SEC", 1).toInt
  private val duration = Integer.getInteger("DURATION", 1).toInt
  private val baseUrl = System.getProperty("BASEURL", "https://app-t3.adeo.no")
  private val loginUrl = System.getProperty("LOGINURL", "https://isso-t.adeo.no")
  private val password = System.getProperty("USER_PASSWORD", "Teflon3970")
  private val oidcPassword = System.getProperty("OIDC_PASSWORD", "0987654321")
  private val enheter = System.getProperty("ENHETER", "1001").split(",")

  private val appnavn = "veilarbpersonflatefs"
  private val openIdConnectLogin = new OpenIdConnectLogin("OIDC", oidcPassword, loginUrl, baseUrl, appnavn)
  private val enhetsFeeder = RecordSeqFeederBuilder(enheter.map(enhet => Map("enhet" -> enhet.trim))).circular

  private val veilederForTildeling1 = System.getProperty("VEIL_1", "X905111")
  private val veilederForTildeling2 = System.getProperty("VEIL_2", "X905112")
  private val brukerForTildeling = System.getProperty("BRUKER_TIL_VEILEDER", "27078236871")

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
    exec(addCookie(Cookie("ID_token", session => openIdConnectLogin.getIssoToken(session("username").as[String], password))))
      .exec(addCookie(Cookie("refresh_token", session => openIdConnectLogin.getRefreshToken(session("username").as[String], password))))
  }

  private val loginScenario = scenario("Logger inn")
    .feed(brukernavn)
    .exec(login)

  private val portefoljeScenario = scenario("Portefolje: Enhet")
    .feed(brukernavn)
    .feed(enhetsFeeder)
    .exec(addCookie(Cookie("ID_token", session => openIdConnectLogin.getIssoToken(session("username").as[String], password))))
    .exec(addCookie(Cookie("refresh_token", session => openIdConnectLogin.getRefreshToken(session("username").as[String], password))))
    .pause("50", "600", TimeUnit.MILLISECONDS)
    .exec(Helpers.httpGetSuccess("tekster portefolje", "/veilarbportefoljeflatefs/tjenester/tekster"))
    .exec(Helpers.httpGetSuccess("enheter", "/veilarbveileder/tjenester/veileder/enheter"))
    .exec(Helpers.httpGetSuccess("veileder", "/veilarbveileder/tjenester/veileder/me"))
    .exec(Helpers.httpGetSuccess("statustall", session => s"/veilarbportefolje/tjenester/enhet/${session("enhet").as[String]}/statustall"))
    .exec(
      Helpers.httpPostPaginering("portefoljefilter nye brukere", session => s"/veilarbportefolje/tjenester/enhet/${session("enhet").as[String]}/portefolje")
        .body(Helpers.toBody(RequestFilter()))
        .check(status.is(200))
    )
    .pause("50", "600", TimeUnit.MILLISECONDS)
    .exec(Helpers.httpGetSuccess("enheter", "/veilarbveileder/tjenester/veileder/enheter"))
    .exec(Helpers.httpGetSuccess("veileder", "/veilarbveileder/tjenester/veileder/me"))
    .pause("3", "15", TimeUnit.SECONDS)
    .exec(
      Helpers.httpPostPaginering("portefoljefilter alder", session => s"/veilarbportefolje/tjenester/enhet/${session("enhet").as[String]}/portefolje")
        .body(Helpers.toBody(RequestFilter(alder = List("20-24", "30-39"))))
        .check(status.is(200))
    )
    .pause("1", "10", TimeUnit.SECONDS)
    .exec(
      Helpers.httpPostPaginering("portefoljefilter alder og kjoenn", session => s"/veilarbportefolje/tjenester/enhet/${session("enhet").as[String]}/portefolje")
        .body(Helpers.toBody(RequestFilter(alder = List("25-29", "30-39"), kjonn = List("M"))))
        .check(status.is(200))
    )
    .pause("1", "10", TimeUnit.SECONDS)
    .exec(
      Helpers.httpPostPaginering("portefoljefilter kjoenn og foedselsdag", session => s"/veilarbportefolje/tjenester/enhet/${session("enhet").as[String]}/portefolje")
        .body(Helpers.toBody(RequestFilter(
          kjonn = List("M"),
          fodselsdagIMnd = List("1", "2")
        )))
        .check(status.is(200))
    )
    .pause("1", "10", TimeUnit.SECONDS)
    .exec(
      Helpers.httpPostPaginering("portefoljefilter alder, kjoenn og foedselsdag", session => s"/veilarbportefolje/tjenester/enhet/${session("enhet").as[String]}/portefolje")
        .body(Helpers.toBody(RequestFilter(
          alder = List("20-24", "30-39"),
          kjonn = List("M"),
          fodselsdagIMnd = List("1", "2")
        )))
        .check(status.is(200))
    )
    .pause("1", "10", TimeUnit.SECONDS)
    .exec(
      Helpers.httpPostPaginering("portefoljefilter tiltak", session => s"/veilarbportefolje/tjenester/enhet/${session("enhet").as[String]}/portefolje")
        .body(Helpers.toBody(RequestFilter(
          aktiviteter = AktivitetRequestFilter(TILTAK = "JA")
        )))
        .check(status.is(200))
    )
    .exec(
      Helpers.httpPostPaginering("tildele veileder", session => s"/veilarbsituasjon/api/tilordneveileder/")
        .body(Helpers.toBody(List(RequestTildelingVeileder(
          brukerFnr = brukerForTildeling,
          fraVeilederId = null,
          tilVeilederId = veilederForTildeling1
        ))))
        .check(status.is(200))
    )
    .pause("1", "6", TimeUnit.SECONDS)
    .exec(
      Helpers.httpPostPaginering("tildele veileder", session => s"/veilarbsituasjon/api/tilordneveileder/")
        .body(Helpers.toBody(List(RequestTildelingVeileder(
          brukerFnr = brukerForTildeling,
          fraVeilederId = veilederForTildeling1,
          tilVeilederId = veilederForTildeling2
        ))))
        .check(status.is(200))
    )
    .pause("1", "6", TimeUnit.SECONDS)
    .exec(
      Helpers.httpPostPaginering("tildele veileder", session => s"/veilarbsituasjon/api/tilordneveileder/")
        .body(Helpers.toBody(List(RequestTildelingVeileder(
          brukerFnr = brukerForTildeling,
          fraVeilederId = veilederForTildeling2,
          tilVeilederId = veilederForTildeling1
        ))))
        .check(status.is(200))
    )
    .pause("1", "6", TimeUnit.SECONDS)
    .exec(Helpers.httpGetSuccess("veilederoversikt", session => s"/veilarbportefolje/tjenester/enhet/${session("enhet").as[String]}/portefoljestorrelser"))

    .pause("1", "6", TimeUnit.SECONDS)
    .exec(
      Helpers.httpPostPaginering("veileders portefolje", session => s"/veilarbportefolje/tjenester/veileder/${session("username").as[String]}/portefolje?enhet=${session("enhet").as[String]}", "0")
        .body(Helpers.toBody(RequestFilter(brukerstatus = null)))
        .check(status.is(200))
    )

  setUp(
    // LoginScenario kjøres for at innloggingsrutinen skal gå seg "varm" slik at denne feilen skal forsvinne:
    // WARNING: Cookie rejected [amlbcookie="01", version:0, domain:test.local, path:/, expiry:null] Illegal 'domain' attribute "test.local". Domain of origin: "isso-t.adeo.no"
    loginScenario.inject(constantUsersPerSec(10) during (140 seconds)),
    portefoljeScenario.inject(nothingFor(140 seconds), constantUsersPerSec(usersPerSecEnhet) during duration.minutes)
  )
    .protocols(httpProtocol)
    .assertions(global.successfulRequests.percent.gte(99))
}
