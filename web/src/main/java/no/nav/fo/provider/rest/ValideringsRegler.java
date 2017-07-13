package no.nav.fo.provider.rest;

import io.vavr.collection.Seq;
import io.vavr.control.Validation;
import no.nav.fo.domene.Filtervalg;
import no.nav.fo.domene.Fnr;
import no.nav.fo.domene.VeilederId;
import no.nav.fo.exception.RestValideringException;
import no.nav.fo.provider.rest.arbeidsliste.ArbeidslisteData;
import no.nav.fo.provider.rest.arbeidsliste.ArbeidslisteRequest;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

import static io.vavr.control.Validation.invalid;
import static io.vavr.control.Validation.valid;
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
        test("fnr", fnr, fnr.matches("\\d{11}"));
    }

    private static void test(String navn, Object data, Supplier<Boolean> matches) {
        test(navn, data, matches.get());
    }

    private static void test(String navn, Object data, boolean matches) {
        if (!matches) {
            throw new RestValideringException(format("sjekk av %s feilet, %s", navn, data));
        }
    }

    static Validation<Seq<String>, ArbeidslisteData> validerArbeidsliste(ArbeidslisteRequest arbeidsliste) {
        return
                Validation
                        .combine(
                                validerFnr(arbeidsliste.getFnr()),
                                validateVeilderId(arbeidsliste.getVeilederId()),
                                validateKommentar(arbeidsliste.getKommentar()),
                                validateFrist(arbeidsliste.getFrist())
                        )
                        .ap(ArbeidslisteData::of);
    }

    private static Validation<String, Timestamp> validateFrist(String frist) {
        Timestamp timestamp = Timestamp.from(Instant.parse(frist));
        return valid(timestamp);
    }

    private static Validation<String, String> validateKommentar(String kommentar) {
        return valid(kommentar);
    }

    private static Validation<String, VeilederId> validateVeilderId(String veilederId) {
        return valid(new VeilederId(veilederId));
    }

    public static Validation<String, Fnr> validerFnr(String fnr) {
        if (fnr != null && fnr.matches("\\d{11}")) {
            return valid(new Fnr(fnr));
        }
        return invalid(format("%s er ikke et gyldig fnr", fnr));
    }
}
