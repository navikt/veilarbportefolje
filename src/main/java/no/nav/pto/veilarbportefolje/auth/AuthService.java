package no.nav.pto.veilarbportefolje.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.vavr.Tuple;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.abac.Pep;
import no.nav.common.abac.domain.request.ActionId;
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.NavIdent;
import no.nav.poao_tilgang.client.Decision;
import no.nav.pto.veilarbportefolje.config.FeatureToggle;
import no.nav.pto.veilarbportefolje.domene.Bruker;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;
import static no.nav.common.client.utils.CacheUtils.tryCacheFirst;
import static no.nav.pto.veilarbportefolje.auth.AuthUtils.*;

@Service
@Slf4j
public class AuthService {
    private final AzureAdOnBehalfOfTokenClient aadOboTokenClient;
    private final PoaoTilgangWrapper poaoTilgangWrapper;
    private final Pep veilarbPep;
    private final Cache<VeilederPaEnhet, Boolean> harVeilederTilgangTilEnhetCache;

    private final UnleashService unleashService;

    @Autowired
    public AuthService(Pep veilarbPep, PoaoTilgangWrapper poaoTilgangWrapper, AzureAdOnBehalfOfTokenClient aadOboTokenClient, UnleashService unleashService) {
        this.aadOboTokenClient = aadOboTokenClient;
        this.poaoTilgangWrapper = poaoTilgangWrapper;
        this.veilarbPep = veilarbPep;
        this.unleashService = unleashService;
        this.harVeilederTilgangTilEnhetCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(6000)
                .build();
    }

    public void tilgangTilOppfolging() {
        VeilederId veilederId = getInnloggetVeilederIdent();
        Decision decisionPoaoTilgang = poaoTilgangWrapper.harVeilederTilgangTilModia();
        boolean harTilgang = Decision.Type.PERMIT.equals(decisionPoaoTilgang.getType());
        AuthUtils.test("oppfølgingsbruker", veilederId, harTilgang);
    }

    public void tilgangTilEnhet(String enhet) {
        String veilederId = getInnloggetVeilederIdent().toString();
        AuthUtils.test("tilgang til enhet", Tuple.of(enhet, veilederId), harVeilederTilgangTilEnhet(veilederId, enhet));
    }

    public boolean harVeilederTilgangTilEnhet(String veilederId, String enhet) {
        Boolean abacResponse = tryCacheFirst(harVeilederTilgangTilEnhetCache, new VeilederPaEnhet(veilederId, enhet),
                () -> veilarbPep.harVeilederTilgangTilEnhet(NavIdent.of(veilederId), EnhetId.of(enhet)));

        if (FeatureToggle.brukPoaoTilgang(unleashService)) {
            Decision decision = poaoTilgangWrapper.harVeilederTilgangTilEnhet(EnhetId.of(enhet));
        }
        return abacResponse;
    }

    public void tilgangTilBruker(String fnr) {
        boolean abacResponse = veilarbPep.harTilgangTilPerson(getInnloggetBrukerToken(), ActionId.READ, Fnr.of(fnr));
        if (FeatureToggle.brukPoaoTilgang(unleashService)) {
            Decision decision = poaoTilgangWrapper.harTilgangTilPerson(Fnr.of(fnr));
        }
        AuthUtils.test("tilgangTilBruker", fnr, abacResponse);
    }

    public List<Bruker> sensurerBrukere(List<Bruker> brukere) {
        String veilederIdent = getInnloggetVeilederIdent().toString();
        return brukere.stream()
                .map(bruker -> fjernKonfidensiellInfoDersomIkkeTilgang(bruker, veilederIdent))
                .collect(toList());
    }

    public Bruker fjernKonfidensiellInfoDersomIkkeTilgang(Bruker bruker, String veilederIdent) {
        if (!bruker.erKonfidensiell()) {
            return bruker;
        }

        String diskresjonskode = bruker.getDiskresjonskode();

        if ("6".equals(diskresjonskode) && !harVeilederTilgangTilKode6(NavIdent.of(veilederIdent))) {
            return AuthUtils.fjernKonfidensiellInfo(bruker);
        }
        if ("7".equals(diskresjonskode) && !harVeilederTilgangTilKode7(NavIdent.of(veilederIdent))) {
            return AuthUtils.fjernKonfidensiellInfo(bruker);
        }
        if (bruker.isEgenAnsatt() && !harVeilederTilgangTilEgenAnsatt(NavIdent.of(veilederIdent))) {
            return AuthUtils.fjernKonfidensiellInfo(bruker);
        }
        return bruker;
    }

    private boolean harVeilederTilgangTilKode6(NavIdent veilederIdent) {
        boolean abacResponse = veilarbPep.harVeilederTilgangTilKode6(veilederIdent);
        if (FeatureToggle.brukPoaoTilgang(unleashService)) {
            Decision decision = poaoTilgangWrapper.harVeilederTilgangTilKode6();
            if (decision.isPermit() != abacResponse) {
                log.warn("Diff mellom ABAC og poao-tilgang: harVeilederTilgangTilKode6");
            }
        }
        return abacResponse;
    }

    private boolean harVeilederTilgangTilKode7(NavIdent veilederIdent) {
        boolean abacResponse = veilarbPep.harVeilederTilgangTilKode7(veilederIdent);
        if (FeatureToggle.brukPoaoTilgang(unleashService)) {
            Decision decision = poaoTilgangWrapper.harVeilederTilgangTilKode7();
            if (decision.isPermit() != abacResponse) {
                log.warn("Diff mellom ABAC og poao-tilgang: harVeilederTilgangTilKode7");
            }
        }
        return abacResponse;
    }

    private boolean harVeilederTilgangTilEgenAnsatt(NavIdent veilederIdent) {
        boolean abacResponse = veilarbPep.harVeilederTilgangTilEgenAnsatt(veilederIdent);
        if (FeatureToggle.brukPoaoTilgang(unleashService)) {
            Boolean decision = poaoTilgangWrapper.harVeilederTilgangTilEgenAnsatt();
            if (decision != abacResponse) {
                log.warn("Diff mellom ABAC og poao-tilgang: harVeilederTilgangTilEgenAnsatt");
            }
        }
        return abacResponse;
    }

    public Skjermettilgang hentVeilederTilgangTilSkjermet() {
        String veilederId = getInnloggetVeilederIdent().toString();
        boolean tilgangTilKode6 = harVeilederTilgangTilKode6(NavIdent.of(veilederId));
        boolean tilgangTilKode7 = harVeilederTilgangTilKode7(NavIdent.of(veilederId));
        boolean tilgangEgenAnsatt = harVeilederTilgangTilEgenAnsatt(NavIdent.of(veilederId));

        return new Skjermettilgang(tilgangTilKode6, tilgangTilKode7, tilgangEgenAnsatt);
    }

    public String getOboToken(DownstreamApi receivingApp) {
        return getAadOboTokenForTjeneste(aadOboTokenClient, receivingApp);
    }

    @Data
    @Accessors(chain = true)
    static class VeilederPaEnhet {
        private final String veilederId;
        private final String enhetId;
    }

}
