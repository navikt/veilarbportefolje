package no.nav.pto.veilarbportefolje.auth;

import io.vavr.Tuple;
import io.vavr.control.Validation;
import no.nav.common.abac.Pep;
import no.nav.common.abac.domain.AbacPersonId;
import no.nav.common.abac.domain.request.ActionId;
import no.nav.common.auth.subject.SsoToken;
import no.nav.common.auth.subject.SubjectHandler;
import no.nav.pto.veilarbportefolje.domene.Bruker;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.domene.VeilederId;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteService;
import no.nav.pto.veilarbportefolje.util.ValideringsRegler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.ws.rs.ForbiddenException;
import java.util.ArrayList;
import java.util.List;

import static io.vavr.control.Validation.invalid;
import static io.vavr.control.Validation.valid;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

@Service
public class AuthService {

    private final Pep veilarbPep;

    @Autowired
    public AuthService (Pep veilarbPep) {
        this.veilarbPep = veilarbPep;
    }

    public void tilgangTilOppfolging() {
        String veilederToken = getInnloggetBrukerToken();
        test("oppfølgingsbruker", getInnloggetVeilederIdent(), veilarbPep.harVeilederTilgangTilModia(veilederToken));
    }

    public void tilgangTilEnhet(String enhet) {
        String veilederId = getInnloggetVeilederIdent().getVeilederId();
        test("tilgang til enhet", Tuple.of(enhet, veilederId), harVeilederTilgangTilEnhet(veilederId, enhet));
    }

    public boolean harVeilederTilgangTilEnhet(String veilederId, String enhet) {
        return veilarbPep.harVeilederTilgangTilEnhet(veilederId, enhet);
    }

    public void tilgangTilBruker(String fnr) {
        test("tilgangTilBruker", fnr, veilarbPep.harTilgangTilPerson(getInnloggetBrukerToken(), ActionId.READ, AbacPersonId.fnr(fnr)));
    }

    static void test(String navn, Object data, boolean matches) {
        if (!matches) {
            throw new ForbiddenException(format("sjekk av %s feilet, %s", navn, data));
        }
    }

    public Validation<String, List<Fnr>> erVeilederForBrukere(ArbeidslisteService arbeidslisteService, List<Fnr> fnrs) {
        List<Fnr> validerteFnrs = new ArrayList<>(fnrs.size());
        fnrs.forEach(fnr -> {
            if(erVeilederForBruker(arbeidslisteService, fnr.toString()).isValid()) {
                validerteFnrs.add(fnr);
            }
        });

        return validerteFnrs.size() == fnrs.size() ? valid(validerteFnrs) : invalid(format("Veileder har ikke tilgang til alle brukerene i listen: %s", fnrs));

    }

    public Validation<String, Fnr> erVeilederForBruker(ArbeidslisteService arbeidslisteService, String fnr) {
        VeilederId veilederId = getInnloggetVeilederIdent();

        Boolean erVeilederForBruker =
                ValideringsRegler
                        .validerFnr(fnr)
                        .map(validFnr -> arbeidslisteService.erVeilederForBruker(validFnr, veilederId))
                        .getOrElse(false);

        if (erVeilederForBruker) {
            return valid(new Fnr(fnr));
        }
        return invalid(format("Veileder %s er ikke veileder for bruker med fnr %s", veilederId, fnr));
    }


    public  List<Bruker> sensurerBrukere(List<Bruker> brukere) {
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
            return fjernKonfidensiellInfo(bruker);
        }
        if("7".equals(diskresjonskode) && !veilarbPep.harVeilederTilgangTilKode7(veilederIdent)) {
            return fjernKonfidensiellInfo(bruker);
        }
        if(bruker.isEgenAnsatt() && !veilarbPep.harVeilederTilgangTilEgenAnsatt(veilederIdent)) {
            return fjernKonfidensiellInfo(bruker);
        }
        return bruker;

    }

    private static Bruker fjernKonfidensiellInfo(Bruker bruker) {
        return bruker.setFnr("").setEtternavn("").setFornavn("").setKjonn("").setFodselsdato(null);
    }

    public String getInnloggetBrukerToken() {
        return SubjectHandler
                .getSsoToken()
                .map(SsoToken::getToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token is missing"));
    }

    public VeilederId getInnloggetVeilederIdent() {
        return SubjectHandler
                .getIdent()
                .map(VeilederId::of)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Id is missing from subject"));
    }
}
