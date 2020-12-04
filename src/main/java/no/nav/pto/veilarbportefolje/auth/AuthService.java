package no.nav.pto.veilarbportefolje.auth;

import io.vavr.Tuple;
import no.nav.common.abac.Pep;
import no.nav.common.abac.domain.AbacPersonId;
import no.nav.common.abac.domain.request.ActionId;
import no.nav.pto.veilarbportefolje.domene.Bruker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import static java.util.stream.Collectors.toList;
import static no.nav.pto.veilarbportefolje.config.CacheConfig.TILGANG_TIL_ENHET;

@Service
public class AuthService {

    private final Pep veilarbPep;

    @Autowired
    public AuthService(Pep veilarbPep) {
        this.veilarbPep = veilarbPep;
    }

    public void tilgangTilOppfolging() {
        AuthUtils.test("oppfølgingsbruker", AuthUtils.getInnloggetVeilederIdent(), veilarbPep.harVeilederTilgangTilModia(AuthUtils.getInnloggetBrukerToken()));
    }

    public void tilgangTilEnhet(String enhet) {
        String veilederId = AuthUtils.getInnloggetVeilederIdent().toString();
        AuthUtils.test("tilgang til enhet", Tuple.of(enhet, veilederId), harVeilederTilgangTilEnhet(veilederId, enhet));
    }

    @Cacheable(TILGANG_TIL_ENHET)
    public boolean harVeilederTilgangTilEnhet(String veilederId, String enhet) {
        return veilarbPep.harVeilederTilgangTilEnhet(veilederId, enhet);
    }

    //TODO ER DETTA RIKTIGT ??
    public void tilgangTilBruker(String fnr) {
        AuthUtils.test("tilgangTilBruker", fnr, veilarbPep.harTilgangTilPerson(AuthUtils.getInnloggetBrukerToken(), ActionId.READ, AbacPersonId.fnr(fnr)));
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


        if("6".equals(diskresjonskode) && !veilarbPep.harVeilederTilgangTilKode6(veilederIdent)) {
            return AuthUtils.fjernKonfidensiellInfo(bruker);
        }
        if("7".equals(diskresjonskode) && !veilarbPep.harVeilederTilgangTilKode7(veilederIdent)) {
            return AuthUtils.fjernKonfidensiellInfo(bruker);
        }
        if(bruker.isEgenAnsatt() && !veilarbPep.harVeilederTilgangTilEgenAnsatt(veilederIdent)) {
            return AuthUtils.fjernKonfidensiellInfo(bruker);
        }
        return bruker;

    }

}
