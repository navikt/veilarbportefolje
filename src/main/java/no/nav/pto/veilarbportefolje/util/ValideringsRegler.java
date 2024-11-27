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
           IKKE_SATT.toString(),
           VALGTE_AKTIVITETER.toString(),
           ETTERNAVN.toString(),
           FODSELSNUMMER.toString(),
           UTLOPSDATO.toString(),
           DAGPENGER_UTLOP_UKE.toString(),
           DAGPENGER_PERM_UTLOP_UKE.toString(),
           AAP_TYPE.toString(),
           AAP_VURDERINGSFRIST.toString(),
           AAP_MAXTID_UKE.toString(),
           AAP_UNNTAK_UKER_IGJEN.toString(),
           ARBEIDSLISTE_FRIST.toString(),
           VENTER_PA_SVAR_FRA_NAV.toString(),
           VENTER_PA_SVAR_FRA_BRUKER.toString(),
           UTLOPTE_AKTIVITETER.toString(),
           STARTDATO_FOR_AVTALT_AKTIVITET.toString(),
           NESTE_STARTDATO_FOR_AVTALT_AKTIVITET.toString(),
           FORRIGE_DATO_FOR_AVTALT_AKTIVITET.toString(),
           I_AVTALT_AKTIVITET.toString(),
           AAP_RETTIGHETSPERIODE.toString(),
           MOTER_MED_NAV_IDAG.toString(),
           MOTESTATUS.toString(),
           OPPFOLGING_STARTET.toString(),
           VEILEDER_IDENT.toString(),
           GJELDENDE_VEDTAK_14A_INNSATSGRUPPE.toString(),
           GJELDENDE_VEDTAK_14A_HOVEDMAL.toString(),
           GJELDENDE_VEDTAK_14A_VEDTAKSDATO.toString(),
           UTKAST_14A_STATUS.toString(),
           UTKAST_14A_ANSVARLIG_VEILEDER.toString(),
           UTKAST_14A_STATUS_ENDRET.toString(),
           ARBEIDSLISTE_KATEGORI.toString(),
           ARBEIDSLISTE_OVERSKRIFT.toString(),
           SISTE_ENDRING_DATO.toString(),
           FODELAND.toString(),
           STATSBORGERSKAP.toString(),
           STATSBORGERSKAP_GYLDIG_FRA.toString(),
           TOLKESPRAK.toString(),
           TOLKEBEHOV_SIST_OPPDATERT.toString(),
           BOSTED_KOMMUNE.toString(),
           BOSTED_BYDEL.toString(),
           BOSTED_SIST_OPPDATERT.toString(),
           CV_SVARFRIST.toString(),
           ENSLIGE_FORSORGERE_UTLOP_YTELSE.toString(),
           ENSLIGE_FORSORGERE_VEDTAKSPERIODETYPE.toString(),
           ENSLIGE_FORSORGERE_AKTIVITETSPLIKT.toString(),
           ENSLIGE_FORSORGERE_OM_BARNET.toString(),
           BARN_UNDER_18_AR.toString(),
           BRUKERS_SITUASJON_SIST_ENDRET.toString(),
           HUSKELAPP_FRIST.toString(),
           HUSKELAPP_KOMMENTAR.toString(),
           HUSKELAPP.toString(),
           FARGEKATEGORI.toString(),
           UTDANNING_OG_SITUASJON_SIST_ENDRET.toString(),
           TILTAKSHENDELSE_DATO_OPPRETTET.toString(),
           TILTAKSHENDELSE_TEKST.toString()
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
