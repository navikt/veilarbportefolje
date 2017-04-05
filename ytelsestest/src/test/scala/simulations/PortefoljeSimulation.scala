package simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import no.nav.sbl.gatling.login.OpenIdConnectLogin
import org.slf4j.LoggerFactory
import utils.{Helpers, RequestFilter}

import scala.concurrent.duration._

class PortefoljeSimulation extends Simulation {
    private val logger = LoggerFactory.getLogger(PortefoljeSimulation.this.getClass)
    private val brukernavn = csv(System.getProperty("BRUKERE", "brukere_t4.csv")).circular

    private val usersPerSecEnhet = Integer.getInteger("USERS_PER_SEC_ENHET", 1).toInt
    private val duration = Integer.getInteger("DURATION", 1).toInt
    private val baseUrl = System.getProperty("BASEURL", "https://app-q4.adeo.no")
    private val loginUrl = System.getProperty("LOGINURL", "https://isso-q.adeo.no")
    private val password = System.getProperty("USER_PASSWORD", "changeme")
    private val oidcPassword = System.getProperty("OIDC_PASSWORD", "changeme")

    private val appnavn = "veilarbpersonflatefs"
    private val openIdConnectLogin = new OpenIdConnectLogin("OIDC", oidcPassword, loginUrl, baseUrl, appnavn)

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

    private val portefoljeScenario = scenario("Portefolje: Enhet")
        .feed(brukernavn)
        .exec(addCookie(Cookie("ID_token", session => openIdConnectLogin.getIssoToken(session("username").as[String], password))))
        .exec(addCookie(Cookie("refresh_token", session => openIdConnectLogin.getRefreshToken(session("username").as[String], password))))
        .exec(Helpers.httpGetSuccess("forrside portefolje", "/veilarbportefoljeflatefs"))
        .pause(100.milliseconds)
        .exec(Helpers.httpGetSuccess("tekster portefolje", "/veilarbportefoljeflatefs/tjenester/tekster"))
        .exec(Helpers.httpGetSuccess("enheter", "/veilarbveileder/tjenester/veileder/enheter"))
        .exec(Helpers.httpGetSuccess("veileder", "/veilarbveileder/tjenester/veileder/me"))
        .exec(Helpers.httpGetSuccess("statustall", "/veilarbportefolje/tjenester/enhet/0713/statustall"))
        .exec(
            Helpers.httpPostPaginering("portefoljefilter", "/veilarbportefolje/tjenester/enhet/0713/portefolje")
                .body(Helpers.toBody(RequestFilter()))
                .check(status.is(200))
        )
        .pause(100.milliseconds)
        .exec(Helpers.httpGetSuccess("enheter", "/veilarbveileder/tjenester/veileder/enheter"))
        .exec(Helpers.httpGetSuccess("veileder", "/veilarbveileder/tjenester/veileder/me"))
        .pause(7.seconds)
        .exec(
            http("portefoljefilter")
                .post("/veilarbportefolje/tjenester/enhet/0713/portefolje")
                .body(Helpers.toBody(RequestFilter(alder = List("20-24", "30-39"))))
                .check(status.is(200))
        )
        .pause(5.seconds)
        .exec(
            http("portefoljefilter")
                .post("/veilarbportefolje/tjenester/enhet/0713/portefolje")
                .body(Helpers.toBody(RequestFilter(alder = List("25-29", "30-39"), kjonn = List("M"))))
                .check(status.is(200))
        )
        .pause(4.seconds)
        .exec(
            http("portefoljefilter")
                .post("/veilarbportefolje/tjenester/enhet/0713/portefolje")
                .body(Helpers.toBody(RequestFilter(
                    kjonn = List("M"),
                    fodselsdagIMnd = List("1", "2")
                )))
                .check(status.is(200))
        )
        .pause(7.seconds)
        .exec(
            http("portefoljefilter")
                .post("/veilarbportefolje/tjenester/enhet/0713/portefolje")
                .body(Helpers.toBody(RequestFilter(
                    alder = List("20-24", "30-39"),
                    kjonn = List("M"),
                    fodselsdagIMnd = List("1", "2")
                )))
                .check(status.is(200))
        )
        .pause(2.seconds)
        .exec(Helpers.httpGetSuccess("veilederoversikt", "/veilarbportefolje/tjenester/enhet/0713/portefoljestorrelser"))
        .pause(2.seconds)
        .exec(Helpers.httpGetSuccess("forrside personflate", "/veilarbpersonflatefs"))
        .pause(100.milliseconds)
        .exec(Helpers.httpGetSuccess("tekster personflate", "/veilarbpersonfs/tjenester/tekster?lang=nb"))
        .exec(Helpers.httpGetSuccess("enheter", "/veilarbveileder/tjenester/veileder/enheter"))
        .exec(Helpers.httpGetSuccess("veileder", "/veilarbveileder/tjenester/veileder/me"))
        .exec(Helpers.httpGetSuccess("person", "/veilarbperson/tjenester/person/***REMOVED***"))
        .pause(3.seconds)
        .exec(Helpers.httpGetSuccess("oppfoelginsstatus", "/veilarbsituasjon/api/person/***REMOVED***/oppfoelgingsstatus"))
        .pause(200.milliseconds)
        .exec(Helpers.httpGetSuccess("ytelser", "/veilarbsituasjon/api/person/***REMOVED***/ytelser"))
        .exec(Helpers.httpGetSuccess("veilederinfo", "/veilarbsituasjon/api/person/***REMOVED***/veileder"))

    setUp(
        portefoljeScenario.inject(constantUsersPerSec(usersPerSecEnhet) during duration.minutes)
    )
        .protocols(httpProtocol)
        .assertions(global.successfulRequests.percent.gte(99))
}
