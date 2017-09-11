package no.nav.fo.provider.rest;

import io.vavr.Tuple;
import io.vavr.control.Validation;
import no.nav.brukerdialog.security.context.SubjectHandler;
import no.nav.fo.domene.Fnr;
import no.nav.fo.domene.VeilederId;
import no.nav.fo.exception.RestTilgangException;
import no.nav.fo.service.ArbeidslisteService;
import no.nav.fo.service.BrukertilgangService;
import no.nav.fo.service.PepClient;
import no.nav.fo.util.TokenUtils;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static io.vavr.control.Validation.invalid;
import static io.vavr.control.Validation.valid;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class TilgangsRegler {
    final static Pattern pattern = Pattern.compile("\\d{4}");

    static void tilgangTilOppfolging(PepClient pep) {
        SubjectHandler subjectHandler = SubjectHandler.getSubjectHandler();
        String veilederId = subjectHandler.getUid();
        String token = TokenUtils.getTokenBody(subjectHandler.getSubject());

        test("oppfølgingsbruker", veilederId, pep.isSubjectMemberOfModiaOppfolging(veilederId, token));
    }

    static void tilgangTilEnhet(BrukertilgangService brukertilgangService, String enhet) {
        String veilederId = SubjectHandler.getSubjectHandler().getUid();
        tilgangTilEnhet(brukertilgangService, enhet, veilederId);
    }

    static boolean enhetErIPilot(String enhet) {
        String enhetsliste = System.getProperty("portefolje.pilot.enhetliste", "");
        enhetsliste = pattern.matcher(enhetsliste).find() ? enhetsliste : "";

        if (isBlank(enhetsliste)) {
            return true;
        }

        List<String> pilotenheter = Arrays.asList(enhetsliste.split(","));

        return pilotenheter.isEmpty() || pilotenheter.contains(enhet);
    }


    private static void tilgangTilEnhet(BrukertilgangService brukertilgangService, String enhet, String ident) {
        test("tilgang til enhet", Tuple.of(enhet, ident), brukertilgangService.harBrukerTilgang(ident, enhet));
    }

    public static void tilgangTilBruker(PepClient pep, String fnr) {
        SubjectHandler subjectHandler = SubjectHandler.getSubjectHandler();
        String token = TokenUtils.getTokenBody(subjectHandler.getSubject());

        pep.tilgangTilBruker(token, fnr);
    }

    static void test(String navn, Object data, boolean matches) {
        if (!matches) {
            throw new RestTilgangException(format("sjekk av %s feilet, %s", navn, data));
        }
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
