package no.nav.pto.veilarbportefolje.util;

import io.vavr.collection.Seq;
import io.vavr.control.Validation;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.arbeidsliste.Arbeidsliste;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteDTO;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteRequest;
import no.nav.pto.veilarbportefolje.domene.Filtervalg;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
import static no.nav.common.utils.StringUtils.nullOrEmpty;

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
            "aap_type",
            "aap_vurderingsfrist",
            "aapmaxtiduke",
            "aapunntakukerigjen",
            "arbeidslistefrist",
            "venterpasvarfranav",
            "venterpasvarfrabruker",
            "utlopteaktiviteter",
            "aktivitet_start",
            "neste_aktivitet_start",
            "forrige_aktivitet_start",
            "iaktivitet",
            "iavtaltaktivitet",
            "aaprettighetsperiode",
            "moterMedNAVIdag",
            "motestatus",
            "oppfolging_startdato",
            "veileder_id",
            "utkast_14a_status",
            "utkast_14a_ansvarlig_veileder",
            "utkast_14a_status_endret",
            "arbeidslistekategori",
            "arbeidsliste_overskrift",
            "siste_endring_tidspunkt",
            "fodeland",
            "statsborgerskap",
            "statsborgerskap_gyldig_fra",
            "tolkespraak",
            "tolkebehov_sistoppdatert",
            "kommunenummer",
            "bydelsnummer",
            "bostedSistOppdatert",
            "neste_svarfrist_stilling_fra_nav",
            "enslige_forsorgere_utlop_ytelse",
            "enslige_forsorgere_vedtaksperiodetype",
            "enslige_forsorgere_aktivitetsplikt",
            "enslige_forsorgere_om_barnet",
            "barn_under_18_aar"
    );

    public static void sjekkEnhet(String enhet) {
        test("enhet", enhet, enhet.matches("\\d{4}"));
    }


    public static void sjekkVeilederIdent(String veilederIdent, boolean optional) {

        test("veilederident", veilederIdent, optional || veilederIdent.matches("[A-Z]\\d{6}"));
    }

    public static void sjekkFiltervalg(Filtervalg filtervalg) {
        test("filtervalg", filtervalg, filtervalg::valider);
    }

    public static void sjekkSortering(String sortDirection, String sortField) {
        test("sortDirection", sortDirection, sortDirs.contains(sortDirection));
        test("sortField", sortField, sortFields.contains(sortField));
    }

    private static void test(String navn, Object data, Supplier<Boolean> matches) {
        test(navn, data, matches.get());
    }

    private static void test(String navn, Object data, boolean matches) {
        if (!matches) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, format("sjekk av %s feilet, %s", navn, data));
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

    private static Validation<String, Arbeidsliste.Kategori> validateKategori(String kategori) {
        try {
            return valid(Arbeidsliste.Kategori.valueOf(kategori));
        } catch (Exception e) {
            return invalid(format("%s er ikke en gyldig kategori", kategori));
        }
    }

    private static Validation<String, String> validateKommentar(String kommentar) {
        return valid(kommentar);
    }

    public static Validation<String, Fnr> validerFnr(String fnr) {
        if (fnr != null && fnr.matches("\\d{11}")) {
            return valid(Fnr.ofValidFnr(fnr));
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
