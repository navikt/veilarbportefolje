package no.nav.pto.veilarbportefolje.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.rest.client.RestClient;
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.poao_tilgang.client.*;
import no.nav.poao_tilgang.client.api.NetworkApiException;
import no.nav.pto.veilarbportefolje.config.EnvironmentProperties;
import no.nav.poao_tilgang.api.dto.response.TilgangsattributterResponse;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
public class PoaoTilgangWrapper {
    // Endringer i denne PR-en (se plan.md): (1) retryOnConnectionFailure(false) i
    // constructoren under, (2) evaluer()-metoden nederst som legger Prometheus-metrikk
    // rundt alle policy-kallene. Begge er observability/robusthet - ingen endring i
    // avgjørelseslogikken selv.
    private final PoaoTilgangClient poaoTilgangClient;
    private final AuthContextHolder authContextHolder;
    private final MeterRegistry meterRegistry;

    private final Cache<PolicyInput, Decision> policyInputToDecisionCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(30))
            .build();
    private final Cache<UUID, List<AdGruppe>> navAnsattIdToAzureAdGrupperCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(30))
            .build();
    private final Cache<String, Boolean> norskIdentToErSkjermetCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(30))
            .build();
    private final Cache<String, TilgangsattributterResponse> tilgangsAttributterCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(30))
            .build();


    public PoaoTilgangWrapper(AuthContextHolder authContextHolder, AzureAdMachineToMachineTokenClient tokenClient, EnvironmentProperties environmentProperties, MeterRegistry meterRegistry) {
        String url = environmentProperties.getPoaoTilgangUrl();
        String tokenScope = environmentProperties.getPoaoTilgangScope();

        this.authContextHolder = authContextHolder;
        this.meterRegistry = meterRegistry;

        this.poaoTilgangClient = new PoaoTilgangCachedClient(
                new PoaoTilgangHttpClient(
                        url,
                        () -> tokenClient.createMachineToMachineToken(tokenScope),
                        RestClient.baseClientBuilder()
                                .readTimeout(1, TimeUnit.SECONDS)
                                .writeTimeout(1, TimeUnit.SECONDS)
                                .callTimeout(1, TimeUnit.SECONDS)
                                // Uten denne blir OkHttps standardverdi (true) brukt, som lar et
                                // enkeltkall som feiler mot én backend-tilkobling bli automatisk
                                // forsøkt på nytt mot en annen tilkobling - og dermed stable opp
                                // flere fulle callTimeout-forsøk (bekreftet mistenkt årsak til de
                                // sjeldne 5000ms+-tilfellene i produksjonslogg, se plan.md punkt 0).
                                // Med false blir den konfigurerte 1-sekunds callTimeout en reell,
                                // forutsigbar øvre grense per kall.
                                .retryOnConnectionFailure(false)
                                .build()),
                policyInputToDecisionCache,
                navAnsattIdToAzureAdGrupperCache,
                norskIdentToErSkjermetCache,
                tilgangsAttributterCache
        );
    }

    public Decision harVeilederTilgangTilModia() {
        return evaluer("modia", () -> poaoTilgangClient.evaluatePolicy(new NavAnsattTilgangTilModiaPolicyInput(
                AuthUtils.hentInnloggetVeilederUUID(authContextHolder))
        ).getOrThrow());
    }

    public Decision harVeilederTilgangTilEnhet(EnhetId enhetId) {
        return evaluer("enhet", () -> poaoTilgangClient.evaluatePolicy(new NavAnsattTilgangTilNavEnhetPolicyInput(
                AuthUtils.hentInnloggetVeilederUUID(authContextHolder),
                enhetId.get())
        ).getOrThrow());
    }

    public Decision harTilgangTilPerson(Fnr fnr) {
        return evaluer("person", () -> poaoTilgangClient.evaluatePolicy(new NavAnsattTilgangTilEksternBrukerPolicyInput(
                AuthUtils.hentInnloggetVeilederUUID(authContextHolder),
                TilgangType.LESE,
                fnr.get())
        ).getOrThrow());
    }

    public Decision harVeilederTilgangTilKode6() {
        return evaluer("kode6", () -> poaoTilgangClient.evaluatePolicy(new NavAnsattBehandleStrengtFortroligBrukerePolicyInput(
                AuthUtils.hentInnloggetVeilederUUID(authContextHolder))
        ).getOrThrow());
    }

    public Decision harVeilederTilgangTilKode7() {
        return evaluer("kode7", () -> poaoTilgangClient.evaluatePolicy(new NavAnsattBehandleFortroligBrukerePolicyInput(
                AuthUtils.hentInnloggetVeilederUUID(authContextHolder))
        ).getOrThrow());
    }

    public Decision harVeilederTilgangTilEgenAnsatt() {
        return evaluer("egen_ansatt", () -> poaoTilgangClient.evaluatePolicy(new NavAnsattBehandleSkjermedePersonerPolicyInput(
                AuthUtils.hentInnloggetVeilederUUID(authContextHolder))
        ).getOrThrow());
    }

    /**
     * Kjører et poao-tilgang-kall og registrerer utfallet (suksess/permit/deny/feil) som
     * Prometheus-metrikk, tagget med policy-type. Endrer ikke selve avgjørelsen eller
     * exception-typen som kastes videre - kun observability (grønn sone, plan.md punkt 4).
     * <p>
     * {@code NetworkApiException} er (litt overraskende) en sjekket exception i
     * poao-tilgang-klienten, men kastes i praksis "usjekket" gjennom Kotlin-koden i
     * {@code .getOrThrow()} - Java-kompilatoren kan derfor ikke bevise statisk at den kan
     * forekomme her, og et eksplisitt {@code catch (NetworkApiException e)} nekter å
     * kompilere. Vi fanger derfor bredt ({@code Throwable}) og bruker
     * {@link SneakyThrows} til å kaste akkurat samme exception-objekt videre
     * uendret (samme type og stacktrace), slik at {@code GlobalExceptionHandler} og
     * annen feilhåndtering nedstrøms fortsatt ser den opprinnelige exception-typen.
     */
    @SneakyThrows
    private Decision evaluer(String policyNavn, Supplier<Decision> kall) {
        try {
            Decision decision = kall.get();
            Counter.builder("poaotilgang_kall_utfall")
                    .tag("policy", policyNavn)
                    .tag("utfall", decision.isPermit() ? "permit" : "deny")
                    .register(meterRegistry)
                    .increment();
            return decision;
        } catch (Throwable t) {
            String utfall = (t instanceof NetworkApiException) ? "timeout_eller_nettverksfeil" : "annen_feil";
            // Logges her - IKKE i GlobalExceptionHandler - fordi policyNavn kun er kjent på dette
            // stadiet. NetworkApiException bærer selv ingen policy-kontekst når den propagerer
            // videre, så et forsøk på å logge policyNavn nedstrøms i GlobalExceptionHandler ville
            // enten mangle informasjonen eller måtte late som den fantes. Ingen fnr/PII i denne
            // logglinjen - kun policy-type og utfall (samme tagger som Counter under).
            log.warn("poao-tilgang-kall feilet: policy={}, utfall={}", policyNavn, utfall, t);
            Counter.builder("poaotilgang_kall_utfall")
                    .tag("policy", policyNavn)
                    .tag("utfall", utfall)
                    .register(meterRegistry)
                    .increment();
            throw t;
        }
    }
}
