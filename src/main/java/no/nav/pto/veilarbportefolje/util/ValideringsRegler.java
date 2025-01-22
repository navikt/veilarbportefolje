package no.nav.pto.veilarbportefolje.util;

import io.vavr.collection.Seq;
import io.vavr.control.Validation;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.arbeidsliste.Arbeidsliste;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteDTO;
import no.nav.pto.veilarbportefolje.arbeidsliste.v1.ArbeidslisteRequest;
import no.nav.pto.veilarbportefolje.arbeidsliste.v2.ArbeidslisteV2Request;
import no.nav.pto.veilarbportefolje.domene.Sorteringsfelt;
import no.nav.pto.veilarbportefolje.domene.Sorteringsrekkefolge;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static io.vavr.control.Validation.invalid;
import static io.vavr.control.Validation.valid;
import static java.lang.String.format;
import static no.nav.common.utils.StringUtils.nullOrEmpty;

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

    public static Validation<Seq<String>, ArbeidslisteDTO> validerArbeidslisteV2(ArbeidslisteV2Request arbeidsliste, boolean redigering) {

        return Validation
                .combine(
                        validerFnr(arbeidsliste.fnr().get()),
                        valid(arbeidsliste.overskrift()),
                        validateKommentar(arbeidsliste.kommentar()),
                        validateFrist(arbeidsliste.frist(), redigering),
                        validateKategori(arbeidsliste.kategori())
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
