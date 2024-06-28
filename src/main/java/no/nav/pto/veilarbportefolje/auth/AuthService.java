package no.nav.pto.veilarbportefolje.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.vavr.Tuple;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.poao_tilgang.client.Decision;
import no.nav.pto.veilarbportefolje.domene.Bruker;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar.BarnUnder18AarData;
import no.nav.pto.veilarbportefolje.persononinfo.domene.Adressebeskyttelse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;
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
        VeilederId veilederId = getInnloggetVeilederIdent();
        Decision decisionPoaoTilgang = poaoTilgangWrapper.harVeilederTilgangTilModia();
        boolean harTilgang = Decision.Type.PERMIT.equals(decisionPoaoTilgang.getType());
        AuthUtils.test("oppfølgingsbruker", veilederId, harTilgang);
    }

    public void innloggetVeilederHarTilgangTilEnhet(String enhet) {
        String veilederId = getInnloggetVeilederIdent().toString();
        AuthUtils.test("tilgang til enhet", Tuple.of(enhet, veilederId), harVeilederTilgangTilEnhet(veilederId, enhet));
    }

    public boolean harVeilederTilgangTilEnhet(String veilederId, String enhet) {
        //Først catcher veiledertilgang til enheten så returnerer responsen, da unngår vi veiledertilgang sjekk hver gang når det skjer endring
        return tryCacheFirst(
                    harVeilederTilgangTilEnhetCache,
                    new VeilederPaEnhet(veilederId, enhet),
                    poaoTilgangWrapper.harVeilederTilgangTilEnhet(EnhetId.of(enhet))::isPermit
        );
    }

    public void innloggetVeilederHarTilgangTilBruker(String fnr) {
        boolean response = poaoTilgangWrapper.harTilgangTilPerson(Fnr.of(fnr)).isPermit();
        AuthUtils.test("tilgangTilBruker", fnr, response);
    }

    public List<Bruker> sensurerBrukere(List<Bruker> brukere) {
        return brukere.stream()
                .map(this::fjernKonfidensiellInfoDersomIkkeTilgang)
                .collect(toList());
    }

    public Bruker fjernKonfidensiellInfoDersomIkkeTilgang(Bruker bruker) {
        if (bruker.getBarnUnder18AarData() != null) {
            bruker.setBarnUnder18AarData(
                    bruker.getBarnUnder18AarData().stream().filter(
                            this::harVeilederTilgangTilBarn
                    ).toList()
            );
        }

        if (!bruker.erKonfidensiell()) {
            return bruker;
        }

        String diskresjonskode = bruker.getDiskresjonskode();

        if (Adressebeskyttelse.STRENGT_FORTROLIG.diskresjonskode.equals(diskresjonskode) && !harVeilederTilgangTilKode6()) {
            return AuthUtils.fjernKonfidensiellInfo(bruker);
        }
        if (Adressebeskyttelse.FORTROLIG.diskresjonskode.equals(diskresjonskode) && !harVeilederTilgangTilKode7()) {
            return AuthUtils.fjernKonfidensiellInfo(bruker);
        }
        if (bruker.isEgenAnsatt() && !harVeilederTilgangTilEgenAnsatt()) {
            return AuthUtils.fjernKonfidensiellInfo(bruker);
        }
        return bruker;
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

    public boolean harVeilederTilgangTilBarn(BarnUnder18AarData barn) {
        if (barn.getDiskresjonskode() != null && (barn.getDiskresjonskode().equals(Adressebeskyttelse.STRENGT_FORTROLIG.diskresjonskode)
                || barn.getDiskresjonskode().equals(Adressebeskyttelse.STRENGT_FORTROLIG_UTLAND.diskresjonskode))) {
            return harVeilederTilgangTilKode6();
        }
        if (barn.getDiskresjonskode() != null && barn.getDiskresjonskode().equals(Adressebeskyttelse.FORTROLIG.diskresjonskode)) {
            return harVeilederTilgangTilKode7();
        }
        return true;
    }

    @Data
    @Accessors(chain = true)
    static class VeilederPaEnhet {
        private final String veilederId;
        private final String enhetId;
    }

}
