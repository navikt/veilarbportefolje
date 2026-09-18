package no.nav.pto.veilarbportefolje.util;

import no.nav.pto.veilarbportefolje.domene.Sorteringsfelt;
import no.nav.pto.veilarbportefolje.domene.Sorteringsrekkefolge;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static java.lang.String.format;

public class ValideringsRegler {
    public static void sjekkEnhet(String enhet) {
        test("enhet", enhet, enhet.matches("\\d{4}"));
    }

    public static void sjekkVeilederIdent(String veilederIdent, boolean optional) {

        test("veilederident", veilederIdent, optional || veilederIdent.matches("[A-Z]\\d{6}"));
    }

    public static Sorteringsfelt sjekkSorteringsfelt(String sorteringsFelt) {
        try {
            return Sorteringsfelt.toSorteringsfelt(sorteringsFelt);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, format("%s er ikke et gyldig sorteringsfelt", sorteringsFelt));
        }
    }

    public static Sorteringsrekkefolge sjekkSorteringsrekkefolge(String sorteringsRekkefolge) {
        try {
            return Sorteringsrekkefolge.toSorteringsrekkefolge(sorteringsRekkefolge);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, format("%s er ikke en gyldig sorteringsrekkefølge", sorteringsRekkefolge));
        }
    }

    private static void test(String navn, Object data, boolean matches) {
        if (!matches) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, format("sjekk av %s feilet, %s", navn, data));
        }
    }
}
