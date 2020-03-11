package no.nav.pto.veilarbportefolje.api;

import io.vavr.collection.Seq;
import io.vavr.control.Validation;
import no.nav.pto.veilarbportefolje.arbeidsliste.Arbeidsliste;
import no.nav.pto.veilarbportefolje.domene.Filtervalg;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteDTO;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteRequest;

import javax.ws.rs.BadRequestException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static io.vavr.control.Validation.invalid;
import static io.vavr.control.Validation.valid;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static no.nav.apiapp.util.StringUtils.nullOrEmpty;

public class ValideringsRegler {
    private static List<String> sortDirs = asList("ikke_satt", "ascending", "descending");
    public static List<String> sortFields = asList(
            "ikke_satt",
            "valgteaktiviteter",
            "etternavn",
            "fodselsnummer",
            "utlopsdato",
            "dagputlopuke",
            "permutlopuke",
            "aapmaxtiduke",
            "aapunntakukerigjen",
            "arbeidslistefrist",
            "arbeidsliste_overskrift",
            "venterpasvarfranav",
            "venterpasvarfrabruker",
            "utlopteaktiviteter",
            "aktivitet_start",
            "neste_aktivitet_start",
            "forrige_aktivitet_start",
            "iavtaltaktivitet",
            "aaprettighetsperiode",
            "moterMedNAVIdag",
            "oppfolging_startdato",
            "veileder_id",
            "vedtakstatus",
            "vedtak_status_endret",
            "arbeidslisteikon"
            );

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
            throw new BadRequestException(format("sjekk av %s feilet, %s", navn, data));
        }
    }

    public static Validation<Seq<String>, ArbeidslisteDTO> validerArbeidsliste(ArbeidslisteRequest arbeidsliste, boolean redigering) {
        return
                Validation
                        .combine(
                                validerFnr(arbeidsliste.getFnr()),
                                valid(arbeidsliste.getOverskrift()),
                                validateKommentar(arbeidsliste.getKommentar()),
                                validateFrist(arbeidsliste.getFrist(), redigering),
                                validateKategori(arbeidsliste.getKategori())
                        )
                        .ap(ArbeidslisteDTO::of);
    }

    private static Validation<String, Timestamp> validateFrist(String frist, boolean redigering) {
        if (nullOrEmpty(frist)) {
            return valid(null);
        }
        Timestamp dato = Timestamp.from(Instant.parse(frist));
        if (redigering) {
            return valid(dato);
        }
        return isBeforeToday(dato) ? invalid("Fristen kan ikke settes til den tidligere dato") : valid(dato);
    }

    private static boolean isBeforeToday(Timestamp timestamp) {
        return timestamp.toLocalDateTime().toLocalDate().isBefore(LocalDate.now());
    }

    private static Validation<String, Arbeidsliste.Kategori> validateKategori (String kategori) {
        try {
            return valid(Arbeidsliste.Kategori.valueOf(kategori));
        } catch (Exception e) {
            return invalid(format("%s er ikke et gyldig kategori", kategori));
        }
    }

    private static Validation<String, String> validateKommentar(String kommentar) {
        return valid(kommentar);
    }

    public static Validation<String, Fnr> validerFnr(String fnr) {
        if (fnr != null && fnr.matches("\\d{11}")) {
            return valid(new Fnr(fnr));
        }
        return invalid(format("%s er ikke et gyldig fnr", fnr));
    }

    public static Validation<List<Fnr>, List<Fnr>> validerFnrs(List<Fnr> fnrs) {
        List<Fnr> validerteFnrs = new ArrayList<>();

        fnrs.forEach((fnr) -> {
            if (validerFnr(fnr.toString()).isValid()) {
                validerteFnrs.add(fnr);
            }
        });

        return validerteFnrs.size() == fnrs.size() ? valid(validerteFnrs) : invalid(fnrs);
    }
}
