package no.nav.pto.veilarbportefolje.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.vavr.Tuple;
import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.common.abac.Pep;
import no.nav.common.abac.domain.request.ActionId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.NavIdent;
import no.nav.pto.veilarbportefolje.domene.Bruker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;
import static no.nav.common.client.utils.CacheUtils.tryCacheFirst;

@Service
public class AuthService {

    private final Pep veilarbPep;
    private final Cache<VeilederPaEnhet, Boolean > harVeilederTilgangTilEnhetCache;

    @Autowired
    public AuthService(Pep veilarbPep) {
        this.veilarbPep = veilarbPep;
        this.harVeilederTilgangTilEnhetCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(6000)
                .build();
    }

    public void tilgangTilOppfolging() {
        AuthUtils.test("oppfølgingsbruker", AuthUtils.getInnloggetVeilederIdent(), veilarbPep.harVeilederTilgangTilModia(AuthUtils.getInnloggetBrukerToken()));
    }

    public void tilgangTilEnhet(String enhet) {
        String veilederId = AuthUtils.getInnloggetVeilederIdent().toString();
        AuthUtils.test("tilgang til enhet", Tuple.of(enhet, veilederId), harVeilederTilgangTilEnhet(veilederId, enhet));
    }

    public boolean harVeilederTilgangTilEnhet(String veilederId, String enhet) {
        return tryCacheFirst(harVeilederTilgangTilEnhetCache, new VeilederPaEnhet(veilederId,enhet),
                () -> veilarbPep.harVeilederTilgangTilEnhet(NavIdent.of(veilederId), EnhetId.of(enhet)));
    }

    public void tilgangTilBruker(String fnr) {
        AuthUtils.test("tilgangTilBruker", fnr, veilarbPep.harTilgangTilPerson(AuthUtils.getInnloggetBrukerToken(), ActionId.READ, Fnr.of(fnr)));
    }

    public List<Bruker> sensurerBrukere(List<Bruker> brukere) {
        String veilederIdent = AuthUtils.getInnloggetVeilederIdent().toString();
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

    @Data
    @Accessors(chain = true)
    class VeilederPaEnhet {
        private final String veilederId;
        private final String enhetId;
    }

}
