package no.nav.pto.veilarbportefolje.api;

import io.vavr.Tuple;
import io.vavr.control.Validation;
import no.nav.common.auth.SubjectHandler;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.domene.VeilederId;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteService;
import no.nav.pto.veilarbportefolje.abac.PepClient;

import javax.ws.rs.ForbiddenException;
import java.util.ArrayList;
import java.util.List;

import static io.vavr.control.Validation.invalid;
import static io.vavr.control.Validation.valid;
import static java.lang.String.format;
import static no.nav.pto.veilarbportefolje.util.RestUtils.getSsoToken;

public class TilgangsRegler {

    public static void tilgangTilOppfolging(PepClient pep) {
        String veilederId = SubjectHandler.getIdent().orElseThrow(IllegalStateException::new);
        test("oppfølgingsbruker", veilederId, pep.isSubjectMemberOfModiaOppfolging(veilederId, getSsoToken()));
    }

    public static void tilgangTilEnhet(PepClient pep, String enhet) {
        String veilederId = SubjectHandler.getIdent().orElseThrow(IllegalStateException::new);
        tilgangTilEnhet(pep, enhet, veilederId);
    }

    private static void tilgangTilEnhet(PepClient pep, String enhet, String ident) {
        test("tilgang til enhet", Tuple.of(enhet, ident), pep.tilgangTilEnhet(ident, enhet));
    }

    public static void tilgangTilBruker(PepClient pep, String fnr) {
        test("tilgangTilBruker", fnr, pep.tilgangTilBruker(fnr));
    }

    static void test(String navn, Object data, boolean matches) {
        if (!matches) {
            throw new ForbiddenException(format("sjekk av %s feilet, %s", navn, data));
        }
    }

    public static Validation<String, List<Fnr>> erVeilederForBrukere(ArbeidslisteService arbeidslisteService, List<Fnr> fnrs) {
        List<Fnr> validerteFnrs = new ArrayList<>(fnrs.size());
        fnrs.forEach(fnr -> {
            if(erVeilederForBruker(arbeidslisteService, fnr.toString()).isValid()) {
                validerteFnrs.add(fnr);
            }
        });

        return validerteFnrs.size() == fnrs.size() ? valid(validerteFnrs) : invalid(format("Veileder har ikke tilgang til alle brukerene i listen: %s", fnrs));

    }

    public static Validation<String, Fnr> erVeilederForBruker(ArbeidslisteService arbeidslisteService, String fnr) {
        VeilederId veilederId = SubjectHandler.getIdent().map(VeilederId::new).orElseThrow(IllegalStateException::new);

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
}
