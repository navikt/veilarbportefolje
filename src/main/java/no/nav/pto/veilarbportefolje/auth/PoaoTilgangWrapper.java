package no.nav.pto.veilarbportefolje.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.rest.client.RestClient;
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.utils.EnvironmentUtils;
import no.nav.poao_tilgang.client.*;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static no.nav.common.utils.UrlUtils.createDevInternalIngressUrl;
import static no.nav.common.utils.UrlUtils.createProdInternalIngressUrl;

public class PoaoTilgangWrapper {
    private final PoaoTilgangClient poaoTilgangClient;
    private final AuthContextHolder authContextHolder;

    private final Cache<PolicyInput, Decision> policyInputToDecisionCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(30))
            .build();
    private final Cache<UUID, List<AdGruppe>> navAnsattIdToAzureAdGrupperCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(30))
            .build();
    private final Cache<String, Boolean> norskIdentToErSkjermetCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(30))
            .build();

    public PoaoTilgangWrapper(AuthContextHolder authContextHolder, AzureAdMachineToMachineTokenClient tokenClient) {
        boolean isProduction = EnvironmentUtils.isProduction().orElseThrow();

        String url = isProduction ?
                createProdInternalIngressUrl("poao-tilgang") :
                createDevInternalIngressUrl("poao-tilgang");

        String tokenScope = String.format("api://%s-gcp.poao.poao-tilgang/.default", isProduction ? "prod" : "dev");

        this.authContextHolder = authContextHolder;

        this.poaoTilgangClient = new PoaoTilgangCachedClient(
                new PoaoTilgangHttpClient(
                        url,
                        () -> tokenClient.createMachineToMachineToken(tokenScope),
                        RestClient.baseClient()),
                policyInputToDecisionCache,
                navAnsattIdToAzureAdGrupperCache,
                norskIdentToErSkjermetCache
        );
    }

    public Decision harVeilederTilgangTilModia() {
        return poaoTilgangClient.evaluatePolicy(new NavAnsattTilgangTilModiaPolicyInput(
                AuthUtils.hentInnloggetVeilederUUID(authContextHolder))
        ).getOrThrow();
    }

    public Decision harVeilederTilgangTilEnhet(EnhetId enhetId) {
        return poaoTilgangClient.evaluatePolicy(new NavAnsattTilgangTilNavEnhetPolicyInput(
                AuthUtils.hentInnloggetVeilederUUID(authContextHolder),
                enhetId.get())
        ).getOrThrow();
    }

    public Decision harTilgangTilPerson(Fnr fnr) {
        return poaoTilgangClient.evaluatePolicy(new NavAnsattTilgangTilEksternBrukerPolicyInput(
                AuthUtils.hentInnloggetVeilederUUID(authContextHolder),
                TilgangType.LESE,
                fnr.get())
        ).getOrThrow();
    }

    public Decision harVeilederTilgangTilKode6() {
        return poaoTilgangClient.evaluatePolicy(new NavAnsattBehandleStrengtFortroligBrukerePolicyInput(
                AuthUtils.hentInnloggetVeilederUUID(authContextHolder))
        ).getOrThrow();
    }

    public Decision harVeilederTilgangTilKode7() {
        return poaoTilgangClient.evaluatePolicy(new NavAnsattBehandleFortroligBrukerePolicyInput(
                AuthUtils.hentInnloggetVeilederUUID(authContextHolder))
        ).getOrThrow();
    }

    public Boolean harVeilederTilgangTilEgenAnsatt() {
        return poaoTilgangClient.erSkjermetPerson(AuthUtils.hentInnloggetVeilederUUID(authContextHolder).toString()).getOrThrow();
    }
}
