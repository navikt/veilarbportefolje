package no.nav.pto.veilarbportefolje.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.poao_tilgang.client.Decision;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.TimeUnit;

import static no.nav.common.client.utils.CacheUtils.tryCacheFirst;
import static no.nav.pto.veilarbportefolje.auth.AuthUtils.getInnloggetBrukerToken;
import static no.nav.pto.veilarbportefolje.auth.AuthUtils.getInnloggetVeilederIdent;

@Service
@Slf4j
public class AuthService {
    private final AzureAdOnBehalfOfTokenClient aadOboTokenClient;
    private final AzureAdMachineToMachineTokenClient aadM2MTokenClient;
    private final PoaoTilgangWrapper poaoTilgangWrapper;
    private final Cache<VeilederPaEnhet, Boolean> harVeilederTilgangTilEnhetCache;

    @Autowired
    public AuthService(
            AzureAdOnBehalfOfTokenClient aadOboTokenClient,
            AzureAdMachineToMachineTokenClient aadM2MTokenClient,
            PoaoTilgangWrapper poaoTilgangWrapper
    ) {
        this.aadOboTokenClient = aadOboTokenClient;
        this.aadM2MTokenClient = aadM2MTokenClient;
        this.poaoTilgangWrapper = poaoTilgangWrapper;
        this.harVeilederTilgangTilEnhetCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(6000)
                .build();
    }

    public void innloggetVeilederHarTilgangTilOppfolging() {
        Decision decisionPoaoTilgang = poaoTilgangWrapper.harVeilederTilgangTilModia();

        if (decisionPoaoTilgang.isDeny()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    public void innloggetVeilederHarTilgangTilEnhet(String enhet) {
        String veilederId = getInnloggetVeilederIdent().toString();

        if (!harVeilederTilgangTilEnhet(veilederId, enhet)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    public boolean harVeilederTilgangTilEnhet(String veilederId, String enhet) {
        return tryCacheFirst(
                harVeilederTilgangTilEnhetCache,
                new VeilederPaEnhet(veilederId, enhet),
                poaoTilgangWrapper.harVeilederTilgangTilEnhet(EnhetId.of(enhet))::isPermit
        );
    }

    public void innloggetVeilederHarTilgangTilBruker(String fnr) {
        Decision decisionPoaoTilgang = poaoTilgangWrapper.harTilgangTilPerson(Fnr.of(fnr));

        if (decisionPoaoTilgang.isDeny()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    public boolean harVeilederTilgangTilKode6() {
        Decision decision = poaoTilgangWrapper.harVeilederTilgangTilKode6();
        return decision.isPermit();
    }

    public boolean harVeilederTilgangTilKode7() {
        Decision decision = poaoTilgangWrapper.harVeilederTilgangTilKode7();
        return decision.isPermit();
    }

    public boolean harVeilederTilgangTilEgenAnsatt() {
        return poaoTilgangWrapper.harVeilederTilgangTilEgenAnsatt().isPermit();
    }

    public BrukerinnsynTilganger hentVeilederBrukerInnsynTilganger() {
        boolean tilgangTilAdressebeskyttelseStrengtFortrolig = harVeilederTilgangTilKode6();
        boolean tilgangTilAdressebeskyttelseFortrolig = harVeilederTilgangTilKode7();
        boolean tilgangEgenAnsatt = harVeilederTilgangTilEgenAnsatt();

        return new BrukerinnsynTilganger(tilgangTilAdressebeskyttelseStrengtFortrolig, tilgangTilAdressebeskyttelseFortrolig, tilgangEgenAnsatt);
    }

    public String getOboToken(String tokenScope) {
        return aadOboTokenClient.exchangeOnBehalfOfToken(tokenScope, getInnloggetBrukerToken());
    }

    public String getM2MToken(String tokenScope) {
        return aadM2MTokenClient.createMachineToMachineToken(tokenScope);
    }

    @Data
    @Accessors(chain = true)
    static class VeilederPaEnhet {
        private final String veilederId;
        private final String enhetId;
    }

}
