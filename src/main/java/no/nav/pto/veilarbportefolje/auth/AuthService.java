package no.nav.pto.veilarbportefolje.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.vavr.Tuple;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.abac.Pep;
import no.nav.common.abac.domain.request.ActionId;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.NavIdent;
import no.nav.poao_tilgang.client.Decision;
import no.nav.poao_tilgang.client.TilgangClient;
import no.nav.pto.veilarbportefolje.config.EnvironmentProperties;
import no.nav.pto.veilarbportefolje.domene.Bruker;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;
import static no.nav.common.client.utils.CacheUtils.tryCacheFirst;
import static no.nav.pto.veilarbportefolje.auth.AuthUtils.*;

@Slf4j
@Service
public class AuthService {

    private final AuthContextHolder authContextHolder;
    private final AzureAdOnBehalfOfTokenClient aadOboTokenClient;
    private final EnvironmentProperties environmentProperties;
    private final TilgangClient tilgangClient;
    private final ModiaPep modiaPep;
    private final Pep veilarbPep;
    private final Cache<VeilederPaEnhet, Boolean > harVeilederTilgangTilEnhetCache;
    private final UnleashService unleashService;

    @Autowired
    public AuthService(Pep veilarbPep,
                       TilgangClient tilgangClient,
                       ModiaPep modiaPep,
                       AuthContextHolder authContextHolder,
                       AzureAdOnBehalfOfTokenClient aadOboTokenClient,
                       EnvironmentProperties environmentProperties,
                       UnleashService unleashService) {
        this.authContextHolder = authContextHolder;
        this.aadOboTokenClient = aadOboTokenClient;
        this.environmentProperties = environmentProperties;
        this.tilgangClient = tilgangClient;
        this.modiaPep =  modiaPep;
        this.veilarbPep = veilarbPep;
        this.harVeilederTilgangTilEnhetCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(6000)
                .build();
        this.unleashService = unleashService;
    }

    public void tilgangTilOppfolging() {
        boolean harTilgang = modiaPep.harVeilederTilgangTilModia(getInnloggetBrukerToken());

        if (unleashService.isEnabled("veilarbportefolje.poao-tilgang.sammenligne")) {
            try {
                Decision decisionPoaoTilgang = tilgangClient.harVeilederTilgangTilModia(getInnloggetVeilederIdent().getValue());
                boolean harTilgangPoaoTilgang = Decision.Type.PERMIT.equals(decisionPoaoTilgang.getType());
                if (harTilgang != harTilgangPoaoTilgang) {
                    log.info("Forskjellig resultat fra poao-tilgang og abac-modia");
                }
            } catch (Exception e) {
                log.error("Kall til poao-tilgang feilet", e);
            }
        }

        AuthUtils.test("oppfølgingsbruker", getInnloggetVeilederIdent(), harTilgang);
    }

    public void tilgangTilEnhet(String enhet) {
        String veilederId = getInnloggetVeilederIdent().toString();
        AuthUtils.test("tilgang til enhet", Tuple.of(enhet, veilederId), harVeilederTilgangTilEnhet(veilederId, enhet));
    }

    public boolean harVeilederTilgangTilEnhet(String veilederId, String enhet) {
        return tryCacheFirst(harVeilederTilgangTilEnhetCache, new VeilederPaEnhet(veilederId,enhet),
                () -> veilarbPep.harVeilederTilgangTilEnhet(NavIdent.of(veilederId), EnhetId.of(enhet)));
    }

    public void tilgangTilBruker(String fnr) {
        AuthUtils.test("tilgangTilBruker", fnr, veilarbPep.harTilgangTilPerson(getInnloggetBrukerToken(), ActionId.READ, Fnr.of(fnr)));
    }

    public List<Bruker> sensurerBrukere(List<Bruker> brukere) {
        String veilederIdent = getInnloggetVeilederIdent().toString();
        return brukere.stream()
                .map(bruker -> fjernKonfidensiellInfoDersomIkkeTilgang(bruker, veilederIdent))
                .collect(toList());
    }

    public Bruker fjernKonfidensiellInfoDersomIkkeTilgang(Bruker bruker, String veilederIdent) {
        if(!bruker.erKonfidensiell()) {
            return bruker;
        }

        String diskresjonskode = bruker.getDiskresjonskode();

        if("6".equals(diskresjonskode) && !veilarbPep.harVeilederTilgangTilKode6(NavIdent.of(veilederIdent))) {
            return AuthUtils.fjernKonfidensiellInfo(bruker);
        }
        if("7".equals(diskresjonskode) && !veilarbPep.harVeilederTilgangTilKode7(NavIdent.of(veilederIdent))) {
            return AuthUtils.fjernKonfidensiellInfo(bruker);
        }
        if(bruker.isEgenAnsatt() && !veilarbPep.harVeilederTilgangTilEgenAnsatt(NavIdent.of(veilederIdent))) {
            return AuthUtils.fjernKonfidensiellInfo(bruker);
        }
        return bruker;

    }

    public Skjermettilgang hentVeilederTilgangTilSkjermet(){
        String veilederId = getInnloggetVeilederIdent().toString();
        boolean tilgangTilKode6 = veilarbPep.harVeilederTilgangTilKode6(NavIdent.of(veilederId));
        boolean tilgangTilKode7 = veilarbPep.harVeilederTilgangTilKode7(NavIdent.of(veilederId));
        boolean tilgangEgenAnsatt = veilarbPep.harVeilederTilgangTilEgenAnsatt(NavIdent.of(veilederId));

        return new Skjermettilgang(tilgangTilKode6, tilgangTilKode7, tilgangEgenAnsatt);
    }

    public String getOboOrOpenAmToken(DownstreamApi receivingApp){
        return getContextAwareUserToken(receivingApp, authContextHolder, aadOboTokenClient, environmentProperties);
    }
    @Data
    @Accessors(chain = true)
    class VeilederPaEnhet {
        private final String veilederId;
        private final String enhetId;
    }

}
