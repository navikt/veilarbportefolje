package no.nav.pto.veilarbportefolje.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.poao_tilgang.client.Decision;
import no.nav.pto.veilarbportefolje.domene.frontendmodell.PortefoljebrukerFrontendModell;
import no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar.BarnUnder18AarData;
import no.nav.pto.veilarbportefolje.persononinfo.domene.Adressebeskyttelse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static java.util.stream.Collectors.toList;
import static no.nav.common.client.utils.CacheUtils.tryCacheFirst;
import static no.nav.pto.veilarbportefolje.auth.AuthUtils.getInnloggetBrukerToken;
import static no.nav.pto.veilarbportefolje.auth.AuthUtils.getInnloggetVeilederIdent;
import static org.apache.logging.log4j.util.Lazy.lazy;

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


    public List<PortefoljebrukerFrontendModell> sensurerBrukere(List<PortefoljebrukerFrontendModell> brukere) {
        // Memoisering: harVeilederTilgangTilKode6()/Kode7()/EgenAnsatt()
        // avhenger kun av innlogget veileder, ikke av hvilken bruker som sjekkes - så vi
        // beregner dem lat (maks én gang hver, kun hvis faktisk brukt) her, i stedet for at
        // fjernKonfidensiellInfoDersomIkkeTilgang kaller poaoTilgangWrapper på nytt per bruker.
        // Verste-fall gikk fra N (antall skjermede brukere i lista) × 1s til maks 3 × 1s,
        // uten å endre selve autorisasjonssemantikken. Se AuthServiceMemoiseringTest.
        BooleanSupplier harVeilederTilgangTilKode6 = lazy(this::harVeilederTilgangTilKode6)::get;
        BooleanSupplier harVeilederTilgangTilKode7 = lazy(this::harVeilederTilgangTilKode7)::get;
        BooleanSupplier harVeilederTilgangTilEgenAnsatt = lazy(this::harVeilederTilgangTilEgenAnsatt)::get;
        return brukere.stream()
                .map(bruker -> fjernKonfidensiellInfoDersomIkkeTilgang(bruker,  harVeilederTilgangTilKode6, harVeilederTilgangTilKode7, harVeilederTilgangTilEgenAnsatt))
                .collect(toList());
    }

    public PortefoljebrukerFrontendModell fjernKonfidensiellInfoDersomIkkeTilgang(PortefoljebrukerFrontendModell bruker, BooleanSupplier harVeilederTilgangTilKode6, BooleanSupplier harVeilederTilgangTilKode7, BooleanSupplier harVeilederTilgangTilEgenAnsatt) {
        if (bruker.getBarnUnder18AarData() != null) {
            bruker.setBarnUnder18AarData(
                    bruker.getBarnUnder18AarData().stream().filter(
                            barn -> harVeilederTilgangTilBarn(barn, harVeilederTilgangTilKode6, harVeilederTilgangTilKode7)
                    ).toList()
            );
        }

        String diskresjonskode = bruker.getEtiketter().getDiskresjonskodeFortrolig();

        boolean erKonfidensiell = StringUtils.isNotEmpty(diskresjonskode) || bruker.getEgenAnsatt();
        if (!erKonfidensiell) {
            return bruker;
        }

        if (Adressebeskyttelse.STRENGT_FORTROLIG.diskresjonskode.equals(diskresjonskode) && !harVeilederTilgangTilKode6.getAsBoolean()) {
            return AuthUtils.fjernKonfidensiellInfo(bruker);
        }
        if (Adressebeskyttelse.FORTROLIG.diskresjonskode.equals(diskresjonskode) && !harVeilederTilgangTilKode7.getAsBoolean()) {
            return AuthUtils.fjernKonfidensiellInfo(bruker);
        }
        if (bruker.getEgenAnsatt() && !harVeilederTilgangTilEgenAnsatt.getAsBoolean()) {
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

    // NB: ingen kjente kallere igjen etter memoiserings-refaktoren - all
    // produksjonskode bruker nå 3-parameters-varianten under. Vurder å fjerne denne, eller la
    // den delegere til 3-parameters-varianten for å unngå duplisert logikk.
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

    public boolean harVeilederTilgangTilBarn(BarnUnder18AarData barn, BooleanSupplier harVeilederTilgangTilKode6, BooleanSupplier harVeilederTilgangTilKode7) {
        if (barn.getDiskresjonskode() != null && (barn.getDiskresjonskode().equals(Adressebeskyttelse.STRENGT_FORTROLIG.diskresjonskode) ||
                barn.getDiskresjonskode().equals(Adressebeskyttelse.STRENGT_FORTROLIG_UTLAND.diskresjonskode))) {
            return harVeilederTilgangTilKode6.getAsBoolean();
        }
        if (barn.getDiskresjonskode() != null && barn.getDiskresjonskode().equals(Adressebeskyttelse.FORTROLIG.diskresjonskode)) {
            return harVeilederTilgangTilKode7.getAsBoolean();
        }
        return true;
    }

    @Accessors(chain = true)
    record VeilederPaEnhet(String veilederId, String enhetId) {
    }

}
