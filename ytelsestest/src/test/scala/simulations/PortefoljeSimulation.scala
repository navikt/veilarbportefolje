package simulations

import java.util.concurrent.TimeUnit

class PortefoljeSimulation extends Simulation {
    private val logger = LoggerFactory.getLogger(PortefoljeSimulation.this.getClass)
    private val brukernavn = csv(System.getProperty("BRUKERE", "brukere_t.csv")).circular

    private val usersPerSecEnhet = Integer.getInteger("USERS_PER_SEC", 1).toInt
    private val duration = Integer.getInteger("DURATION", 1).toInt
    private val baseUrl = System.getProperty("BASEURL", "https://app-q4.adeo.no")
    private val loginUrl = System.getProperty("LOGINURL", "https://isso-q.adeo.no")
    private val password = System.getProperty("USER_PASSWORD", "changeme")
    private val oidcPassword = System.getProperty("OIDC_PASSWORD", "changeme")
    private val enheter = System.getProperty("ENHETER", "0104,0105,0315,0415,0702,0709,0713,0714,0722,0805,1002,1604,1804").split(",")

    private val appnavn = "veilarbpersonflatefs"
    private val openIdConnectLogin = new OpenIdConnectLogin("OIDC", oidcPassword, loginUrl, baseUrl, appnavn)
    private val random = new Random()
    private val enhetsFeeder = RecordSeqFeederBuilder(enheter.map(enhet => Map("enhet" -> enhet.trim))).circular

    private val veilederForTildeling1 = System.getProperty("VEIL_1", "changeme")
    private val veilederForTildeling2 = System.getProperty("VEIL_2", "changeme")
    private val brukerForTildeling = System.getProperty("BRUKER_TIL_VEILEDER", "changeme")

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
        .feed(enhetsFeeder)
        .exec(addCookie(Cookie("ID_token", session => openIdConnectLogin.getIssoToken(session("username").as[String], password))))
        .exec(addCookie(Cookie("refresh_token", session => openIdConnectLogin.getRefreshToken(session("username").as[String], password))))
        .exec(Helpers.httpGetSuccess("forrside portefolje", "/veilarbportefoljeflatefs"))
        .pause("50", "600", TimeUnit.MILLISECONDS)
        .exec(Helpers.httpGetSuccess("tekster portefolje", "/veilarbportefoljeflatefs/api/tekster"))
        .exec(Helpers.httpGetSuccess("enheter", "/veilarbveileder/api/veileder/enheter"))
        .exec(Helpers.httpGetSuccess("veileder", "/veilarbveileder/api/veileder/me"))
        .exec(Helpers.httpGetSuccess("statustall", session => s"/veilarbportefolje/api/enhet/${session("enhet").as[String]}/statustall"))
        .exec(
            Helpers.httpPostPaginering("portefoljefilter default", session => s"/veilarbportefolje/api/enhet/${session("enhet").as[String]}/portefolje")
                .body(Helpers.toBody(RequestFilter()))
                .check(status.is(200))
        )
        .pause("50", "600", TimeUnit.MILLISECONDS)
        .exec(Helpers.httpGetSuccess("enheter", "/veilarbveileder/api/veileder/enheter"))
        .exec(Helpers.httpGetSuccess("veileder", "/veilarbveileder/api/veileder/me"))
        .pause("3", "15", TimeUnit.SECONDS)
        .exec(
            Helpers.httpPostPaginering("portefoljefilter alder", session => s"/veilarbportefolje/api/enhet/${session("enhet").as[String]}/portefolje")
                .body(Helpers.toBody(RequestFilter(alder = List("20-24", "30-39"))))
                .check(status.is(200))
        )
        .pause("1", "10", TimeUnit.SECONDS)
        .exec(
            Helpers.httpPostPaginering("portefoljefilter alder og kjoenn", session => s"/veilarbportefolje/api/enhet/${session("enhet").as[String]}/portefolje")
                .body(Helpers.toBody(RequestFilter(alder = List("25-29", "30-39"), kjonn = List("M"))))
                .check(status.is(200))
        )
        .pause("1", "10", TimeUnit.SECONDS)
        .exec(
            Helpers.httpPostPaginering("portefoljefilter kjoenn og foedselsdag", session => s"/veilarbportefolje/api/enhet/${session("enhet").as[String]}/portefolje")
                .body(Helpers.toBody(RequestFilter(
                    kjonn = List("M"),
                    fodselsdagIMnd = List("1", "2")
                )))
                .check(status.is(200))
        )
        .pause("1", "10", TimeUnit.SECONDS)
        .exec(
            Helpers.httpPostPaginering("portefoljefilter alder, kjoenn og foedselsdag", session => s"/veilarbportefolje/api/enhet/${session("enhet").as[String]}/portefolje")
                .body(Helpers.toBody(RequestFilter(
                    alder = List("20-24", "30-39"),
                    kjonn = List("M"),
                    fodselsdagIMnd = List("1", "2")
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
              fraVeilederId = veilederForTildeling1,
              tilVeilederId = veilederForTildeling1
            ))))
            .check(status.is(200))
        )

        .pause("1", "6", TimeUnit.SECONDS)
        .exec(Helpers.httpGetSuccess("veilederoversikt", session => s"/veilarbportefolje/api/enhet/${session("enhet").as[String]}/portefoljestorrelser"))
        .pause("1", "6", TimeUnit.SECONDS)
        .exec(Helpers.httpGetSuccess("forrside personflate", "/veilarbpersonflatefs"))
        .pause("50", "300", TimeUnit.MILLISECONDS)
        .exec(Helpers.httpGetSuccess("tekster personflate", "/veilarbpersonfs/api/tekster?lang=nb"))
        .exec(Helpers.httpGetSuccess("enheter", "/veilarbveileder/api/veileder/enheter"))
        .exec(Helpers.httpGetSuccess("veileder", "/veilarbveileder/api/veileder/me"))
        .exec(Helpers.httpGetSuccess("person", "/veilarbperson/api/person/" + brukerForTildeling))
        .pause("1", "6", TimeUnit.SECONDS)
        .exec(Helpers.httpGetSuccess("oppfoelginsstatus", "/veilarbsituasjon/api/person/" + brukerForTildeling + "/oppfoelgingsstatus"))
        .pause("50", "300", TimeUnit.MILLISECONDS)
        .exec(Helpers.httpGetSuccess("ytelser", "/veilarbsituasjon/api/person/" + brukerForTildeling + "/ytelser"))
        .exec(Helpers.httpGetSuccess("veilederinfo", "/veilarbsituasjon/api/person/" + brukerForTildeling + "/veileder"))

    setUp(
        portefoljeScenario.inject(constantUsersPerSec(usersPerSecEnhet) during duration.minutes)
    )
        .protocols(httpProtocol)
        .assertions(global.successfulRequests.percent.gte(99))
}
