package no.nav.pto.veilarbportefolje.auth;

import io.vavr.Tuple;
import no.nav.common.abac.Pep;
import no.nav.common.abac.domain.AbacPersonId;
import no.nav.common.abac.domain.request.ActionId;
import no.nav.common.auth.subject.SsoToken;
import no.nav.common.auth.subject.SubjectHandler;
import no.nav.pto.veilarbportefolje.domene.Bruker;
import no.nav.pto.veilarbportefolje.domene.VeilederId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
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
        String veilederToken = getInnloggetBrukerToken();
        AuthUtils.test("oppfølgingsbruker", getInnloggetVeilederIdent(), veilarbPep.harVeilederTilgangTilModia(veilederToken));
    }

    @Cacheable(TILGANG_TIL_ENHET)
    public void tilgangTilEnhet(String enhet) {
        String veilederId = getInnloggetVeilederIdent().getVeilederId();
        AuthUtils.test("tilgang til enhet", Tuple.of(enhet, veilederId), harVeilederTilgangTilEnhet(veilederId, enhet));
    }

    public boolean harVeilederTilgangTilEnhet(String veilederId, String enhet) {
        return veilarbPep.harVeilederTilgangTilEnhet(veilederId, enhet);
    }

    public void tilgangTilBruker(String fnr) {
        AuthUtils.test("tilgangTilBruker", fnr, veilarbPep.harTilgangTilPerson(getInnloggetBrukerToken(), ActionId.READ, AbacPersonId.fnr(fnr)));
    }

    public List<Bruker> sensurerBrukere(List<Bruker> brukere) {
        return brukere.stream()
                .map(this::fjernKonfidensiellInfoDersomIkkeTilgang)
                .collect(toList());
    }

    private Bruker fjernKonfidensiellInfoDersomIkkeTilgang(Bruker bruker) {
        if(!bruker.erKonfidensiell()) {
            return bruker;
        }

        String diskresjonskode = bruker.getDiskresjonskode();
        String veilederIdent = getInnloggetVeilederIdent().getVeilederId();

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

    public static String getInnloggetBrukerToken() {
        return SubjectHandler
                .getSsoToken()
                .map(SsoToken::getToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token is missing"));
    }

    public static VeilederId getInnloggetVeilederIdent() {
        return SubjectHandler
                .getIdent()
                .map(VeilederId::of)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Id is missing from subject"));
    }
}
