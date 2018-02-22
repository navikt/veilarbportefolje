package no.nav.fo.provider.rest;

import io.vavr.Tuple;
import io.vavr.control.Validation;
import no.nav.apiapp.feil.IngenTilgang;
import no.nav.brukerdialog.security.context.SubjectHandler;
import no.nav.fo.domene.Fnr;
import no.nav.fo.domene.VeilederId;
import no.nav.fo.service.ArbeidslisteService;
import no.nav.fo.service.PepClient;
import no.nav.fo.util.TokenUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static io.vavr.control.Validation.invalid;
import static io.vavr.control.Validation.valid;
import static java.lang.String.format;

public class TilgangsRegler {
    final static Pattern pattern = Pattern.compile("\\d{4}");

    static void tilgangTilOppfolging(PepClient pep) {
        SubjectHandler subjectHandler = SubjectHandler.getSubjectHandler();
        String veilederId = subjectHandler.getUid();
        String token = TokenUtils.getTokenBody(subjectHandler.getSubject());

        test("oppfølgingsbruker", veilederId, pep.isSubjectMemberOfModiaOppfolging(veilederId, token));
    }

    static void tilgangTilEnhet(PepClient pep, String enhet) {
        String veilederId = SubjectHandler.getSubjectHandler().getUid();
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
            throw new IngenTilgang(format("sjekk av %s feilet, %s", navn, data));
        }
    }

    static Validation<String, List<Fnr>> erVeilederForBrukere(ArbeidslisteService arbeidslisteService, List<Fnr> fnrs) {
        List<Fnr> validerteFnrs = new ArrayList<>(fnrs.size());
        fnrs.forEach(fnr -> {
            if(erVeilederForBruker(arbeidslisteService, fnr.toString()).isValid()) {
                validerteFnrs.add(fnr);
            }
        });

        return validerteFnrs.size() == fnrs.size() ? valid(validerteFnrs) : invalid(format("Veileder har ikke tilgang til alle brukerene i listen: %s", fnrs));

    }

    static Validation<String, Fnr> erVeilederForBruker(ArbeidslisteService arbeidslisteService, String fnr) {
        SubjectHandler subjectHandler = SubjectHandler.getSubjectHandler();
        VeilederId veilederId = VeilederId.of(subjectHandler.getUid());

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
