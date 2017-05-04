package no.nav.fo.provider.rest;

import javaslang.Tuple;
import no.nav.brukerdialog.security.context.SubjectHandler;
import no.nav.fo.exception.RestTilgangException;
import no.nav.fo.service.BrukertilgangService;

import static java.lang.String.format;

class TilgangsRegler {

    static void tilgangTilEnhet(BrukertilgangService brukertilgangService, String enhet) {
        String ident = SubjectHandler.getSubjectHandler().getUid();
        tilgangTilEnhet(brukertilgangService, enhet, ident);
    }

    private static void tilgangTilEnhet(BrukertilgangService brukertilgangService, String enhet, String ident) {
        test("tilgang til enhet", Tuple.of(enhet, ident), brukertilgangService.harBrukerTilgang(ident, enhet));
    }

    private static void test(String navn, Object data, boolean matches) {
        if (!matches) {
            throw new RestTilgangException(format("sjekk av %s feilet, %s", navn, data));
        }
    }
}
