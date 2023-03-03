package no.nav.pto.veilarbportefolje.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.vavr.Tuple;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.abac.Pep;
import no.nav.common.abac.domain.request.ActionId;
import no.nav.common.metrics.Event;
import no.nav.common.metrics.MetricsClient;
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.NavIdent;
import no.nav.poao_tilgang.client.Decision;
import no.nav.pto.veilarbportefolje.config.FeatureToggle;
import no.nav.pto.veilarbportefolje.domene.Bruker;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.persononinfo.domene.Adressebeskyttelse;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;
import static no.nav.common.client.utils.CacheUtils.tryCacheFirst;
import static no.nav.pto.veilarbportefolje.auth.AuthUtils.getInnloggetBrukerToken;
import static no.nav.pto.veilarbportefolje.auth.AuthUtils.getInnloggetVeilederIdent;
import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@Service
@Slf4j
public class AuthService {
    private final AzureAdOnBehalfOfTokenClient aadOboTokenClient;
    private final PoaoTilgangWrapper poaoTilgangWrapper;
    private final Pep veilarbPep;
    private final Cache<VeilederPaEnhet, Boolean> harVeilederTilgangTilEnhetCache;
    private final UnleashService unleashService;
    private final MetricsClient metricsClient;

    @Autowired
    public AuthService(Pep veilarbPep, PoaoTilgangWrapper poaoTilgangWrapper, AzureAdOnBehalfOfTokenClient aadOboTokenClient, UnleashService unleashService, MetricsClient metricsClient) {
        this.aadOboTokenClient = aadOboTokenClient;
        this.poaoTilgangWrapper = poaoTilgangWrapper;
        this.veilarbPep = veilarbPep;
        this.unleashService = unleashService;
        this.metricsClient = metricsClient;
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
            poaoTilgangWrapper.harVeilederTilgangTilEnhet(EnhetId.of(enhet));
        }
        return abacResponse;
    }

    public void tilgangTilBruker(String fnr) {
        boolean abacResponse = veilarbPep.harTilgangTilPerson(getInnloggetBrukerToken(), ActionId.READ, Fnr.of(fnr));
        if (FeatureToggle.brukPoaoTilgang(unleashService)) {
            poaoTilgangWrapper.harTilgangTilPerson(Fnr.of(fnr));
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

        if (Adressebeskyttelse.STRENGT_FORTROLIG.diskresjonskode.equals(diskresjonskode) && !harVeilederTilgangTilKode6(NavIdent.of(veilederIdent))) {
            return AuthUtils.fjernKonfidensiellInfo(bruker);
        }
        if (Adressebeskyttelse.FORTROLIG.diskresjonskode.equals(diskresjonskode) && !harVeilederTilgangTilKode7(NavIdent.of(veilederIdent))) {
            return AuthUtils.fjernKonfidensiellInfo(bruker);
        }
        if (bruker.isEgenAnsatt() && !harVeilederTilgangTilEgenAnsatt(NavIdent.of(veilederIdent))) {
            return AuthUtils.fjernKonfidensiellInfo(bruker);
        }
        return bruker;
    }

    public boolean harVeilederTilgangTilKode6(NavIdent veilederIdent) {
        boolean abacResponse = veilarbPep.harVeilederTilgangTilKode6(veilederIdent);
        if (FeatureToggle.brukPoaoTilgang(unleashService)) {
            Decision decision = poaoTilgangWrapper.harVeilederTilgangTilKode6();
            if (decision.isPermit() != abacResponse) {
                metricsClient.report(new Event("poao-tilgang-diff").addTagToReport("method", "harVeilederTilgangTilKode6"));
            }
        }
        return abacResponse;
    }

    public boolean harVeilederTilgangTilKode7(NavIdent veilederIdent) {
        boolean abacResponse = veilarbPep.harVeilederTilgangTilKode7(veilederIdent);
        if (FeatureToggle.brukPoaoTilgang(unleashService)) {
            Decision decision = poaoTilgangWrapper.harVeilederTilgangTilKode7();
            if (decision.isPermit() != abacResponse) {
                metricsClient.report(new Event("poao-tilgang-diff").addTagToReport("method", "harVeilederTilgangTilKode7"));
            }
        }
        return abacResponse;
    }

    public boolean harVeilederTilgangTilEgenAnsatt(NavIdent veilederIdent) {
        boolean abacResponse = veilarbPep.harVeilederTilgangTilEgenAnsatt(veilederIdent);
        if (FeatureToggle.brukPoaoTilgang(unleashService)) {
            Decision decision = poaoTilgangWrapper.harVeilederTilgangTilEgenAnsatt();
            if (decision.isPermit() != abacResponse) {
                secureLog.warn("Diff between abac and poao-tilgang for veileder: " + veilederIdent + ". Poao-tilgang decision is: " + decision.isPermit());
            }
        }
        return abacResponse;
    }

    public BrukerInnsynTilganger hentVeilederBrukerInnsynTilganger() {
        String veilederId = getInnloggetVeilederIdent().toString();
        boolean tilgangTilAdressebeskyttelseStrengtFortrolig = harVeilederTilgangTilKode6(NavIdent.of(veilederId));
        boolean tilgangTilAdressebeskyttelseFortrolig = harVeilederTilgangTilKode7(NavIdent.of(veilederId));
        boolean tilgangEgenAnsatt = harVeilederTilgangTilEgenAnsatt(NavIdent.of(veilederId));

        return new BrukerInnsynTilganger(tilgangTilAdressebeskyttelseStrengtFortrolig, tilgangTilAdressebeskyttelseFortrolig, tilgangEgenAnsatt);
    }

    public String getOboToken(String tokenScope) {
        return aadOboTokenClient.exchangeOnBehalfOfToken(tokenScope, getInnloggetBrukerToken());
    }

    @Data
    @Accessors(chain = true)
    static class VeilederPaEnhet {
        private final String veilederId;
        private final String enhetId;
    }

}
