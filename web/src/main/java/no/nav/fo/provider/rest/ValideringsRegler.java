package no.nav.fo.provider.rest;

import no.nav.fo.domene.Filtervalg;
import no.nav.fo.exception.RestValideringException;

import java.util.List;
import java.util.function.Supplier;

import static java.lang.String.format;
import static java.util.Arrays.asList;

class ValideringsRegler {
    private static List<String> sortDirs = asList("ikke_satt", "ascending", "descending");
    private static List<String> sortFields = asList("ikke_satt", "etternavn", "fodselsnummer", "utlopsdato", "aapMaxtid");

    static void sjekkEnhet(String enhet) {
        test("enhet", enhet, enhet.matches("\\d{4}"));
    }


    static void sjekkVeilederIdent(String veilederIdent, boolean optional) {
        test("veilederident", veilederIdent, optional || veilederIdent.matches("[A-Z]\\d{6}"));
    }

    static void sjekkFiltervalg(Filtervalg filtervalg) {
        test("filtervalg", filtervalg, filtervalg::valider);
    }

    static void sjekkSortering(String sortDirection, String sortField) {
        test("sortDirection", sortDirection, sortDirs.contains(sortDirection));
        test("sortField", sortField, sortFields.contains(sortField));
    }

    static void harYtelsesFilter(Filtervalg filtervalg) {
        test("ytelsesfilter", filtervalg.ytelse, filtervalg.ytelse != null);
    }

    static void sjekkFnr(String fnr) {
        test("fnr", fnr, fnr.length() == 11);
    }

    private static void test(String navn, Object data, Supplier<Boolean> matches) {
        test(navn, data, matches.get());
    }

    private static void test(String navn, Object data, boolean matches) {
        if (!matches) {
            throw new RestValideringException(format("sjekk av %s feilet, %s", navn, data));
        }
    }

}
