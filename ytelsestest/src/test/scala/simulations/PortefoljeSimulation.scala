package simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import no.nav.sbl.gatling.login.OpenIdConnectLogin
import org.slf4j.LoggerFactory

import scala.concurrent.duration._

class PortefoljeSimulation extends Simulation {
    private val logger = LoggerFactory.getLogger(PortefoljeSimulation.this.getClass)
    private val brukernavn = csv(System.getProperty("BRUKERE", "brukere_t4.csv")).circular

    private val numUsersOversikt = Integer.getInteger("NUM_USERS", 1).toInt
    private val numUsersRest = Integer.getInteger("USERS_PER_SEC", 1).toInt
    private val rampTime = java.lang.Long.getLong("RAMPTIME", 1L).toLong
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
        .contentTypeHeader("application/json")
        .userAgentHeader("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:30.0) Gecko/20100101 Firefox/30.0")
        .disableWarmUp
        .silentResources

    private val portefoljeScenario = scenario("simulations.PortefoljeSimulation")
        .feed(brukernavn)
        .exec(addCookie(Cookie("ID_token", session => openIdConnectLogin.getIssoToken(session("username").as[String], password))))
        .exec(addCookie(Cookie("refresh_token", session => openIdConnectLogin.getRefreshToken(session("username").as[String], password))))
        .exec(
            http("get veilarbportefoljeflatefs")
                .get("/veilarbportefoljeflatefs/")
                .check(status.is(200))
        )

    setUp(
        portefoljeScenario.inject(constantUsersPerSec(numUsersOversikt) during duration.minutes)
    )
        .protocols(httpProtocol)
        .assertions(global.successfulRequests.percent.gte(99))
}
