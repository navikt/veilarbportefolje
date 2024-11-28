package no.nav.pto.veilarbportefolje.util;

import io.vavr.collection.Seq;
import io.vavr.control.Validation;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.arbeidsliste.Arbeidsliste;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteDTO;
import no.nav.pto.veilarbportefolje.arbeidsliste.v1.ArbeidslisteRequest;
import no.nav.pto.veilarbportefolje.arbeidsliste.v2.ArbeidslisteV2Request;
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
import static no.nav.pto.veilarbportefolje.domene.Sorteringsfelt.*;

public class ValideringsRegler {
    private static List<String> sortDirs = asList("ikke_satt", "ascending", "descending");
    public static List<String> sortFields = asList(
           IKKE_SATT.sorteringsverdi,
           VALGTE_AKTIVITETER.sorteringsverdi,
           ETTERNAVN.sorteringsverdi,
           FODSELSNUMMER.sorteringsverdi,
           UTLOPSDATO.sorteringsverdi,
           DAGPENGER_UTLOP_UKE.sorteringsverdi,
           DAGPENGER_PERM_UTLOP_UKE.sorteringsverdi,
           AAP_TYPE.sorteringsverdi,
           AAP_VURDERINGSFRIST.sorteringsverdi,
           AAP_MAXTID_UKE.sorteringsverdi,
           AAP_UNNTAK_UKER_IGJEN.sorteringsverdi,
           ARBEIDSLISTE_FRIST.sorteringsverdi,
           VENTER_PA_SVAR_FRA_NAV.sorteringsverdi,
           VENTER_PA_SVAR_FRA_BRUKER.sorteringsverdi,
           UTLOPTE_AKTIVITETER.sorteringsverdi,
           STARTDATO_FOR_AVTALT_AKTIVITET.sorteringsverdi,
           NESTE_STARTDATO_FOR_AVTALT_AKTIVITET.sorteringsverdi,
           FORRIGE_DATO_FOR_AVTALT_AKTIVITET.sorteringsverdi,
           I_AVTALT_AKTIVITET.sorteringsverdi,
           AAP_RETTIGHETSPERIODE.sorteringsverdi,
           MOTER_MED_NAV_IDAG.sorteringsverdi,
           MOTESTATUS.sorteringsverdi,
           OPPFOLGING_STARTET.sorteringsverdi,
           VEILEDER_IDENT.sorteringsverdi,
           GJELDENDE_VEDTAK_14A_INNSATSGRUPPE.sorteringsverdi,
           GJELDENDE_VEDTAK_14A_HOVEDMAL.sorteringsverdi,
           GJELDENDE_VEDTAK_14A_VEDTAKSDATO.sorteringsverdi,
           UTKAST_14A_STATUS.sorteringsverdi,
           UTKAST_14A_ANSVARLIG_VEILEDER.sorteringsverdi,
           UTKAST_14A_STATUS_ENDRET.sorteringsverdi,
           ARBEIDSLISTE_KATEGORI.sorteringsverdi,
           ARBEIDSLISTE_OVERSKRIFT.sorteringsverdi,
           SISTE_ENDRING_DATO.sorteringsverdi,
           FODELAND.sorteringsverdi,
           STATSBORGERSKAP.sorteringsverdi,
           STATSBORGERSKAP_GYLDIG_FRA.sorteringsverdi,
           TOLKESPRAK.sorteringsverdi,
           TOLKEBEHOV_SIST_OPPDATERT.sorteringsverdi,
           BOSTED_KOMMUNE.sorteringsverdi,
           BOSTED_BYDEL.sorteringsverdi,
           BOSTED_SIST_OPPDATERT.sorteringsverdi,
           CV_SVARFRIST.sorteringsverdi,
           ENSLIGE_FORSORGERE_UTLOP_YTELSE.sorteringsverdi,
           ENSLIGE_FORSORGERE_VEDTAKSPERIODETYPE.sorteringsverdi,
           ENSLIGE_FORSORGERE_AKTIVITETSPLIKT.sorteringsverdi,
           ENSLIGE_FORSORGERE_OM_BARNET.sorteringsverdi,
           BARN_UNDER_18_AR.sorteringsverdi,
           BRUKERS_SITUASJON_SIST_ENDRET.sorteringsverdi,
           HUSKELAPP_FRIST.sorteringsverdi,
           HUSKELAPP_KOMMENTAR.sorteringsverdi,
           HUSKELAPP.sorteringsverdi,
           FARGEKATEGORI.sorteringsverdi,
           UTDANNING_OG_SITUASJON_SIST_ENDRET.sorteringsverdi,
           TILTAKSHENDELSE_DATO_OPPRETTET.sorteringsverdi,
           TILTAKSHENDELSE_TEKST.sorteringsverdi
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
