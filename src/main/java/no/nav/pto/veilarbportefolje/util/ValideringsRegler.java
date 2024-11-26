package no.nav.pto.veilarbportefolje.util;

import io.vavr.collection.Seq;
import io.vavr.control.Validation;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.arbeidsliste.Arbeidsliste;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteDTO;
import no.nav.pto.veilarbportefolje.arbeidsliste.v1.ArbeidslisteRequest;
import no.nav.pto.veilarbportefolje.arbeidsliste.v2.ArbeidslisteV2Request;
import no.nav.pto.veilarbportefolje.domene.Filtervalg;
import no.nav.pto.veilarbportefolje.domene.Sorteringsfelt;
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
           Sorteringsfelt.IKKE_SATT.name(),
           Sorteringsfelt.VALGTE_AKTIVITETER.name(),
           Sorteringsfelt.ETTERNAVN.name(),
           Sorteringsfelt.FODSELSNUMMER.name(),
           Sorteringsfelt.UTLOPSDATO.name(),
           Sorteringsfelt.DAGPENGER_UTLOP_UKE.name(),
           Sorteringsfelt.DAGPENGER_PERM_UTLOP_UKE.name(),
           Sorteringsfelt.AAP_TYPE.name(),
           Sorteringsfelt.AAP_VURDERINGSFRIST.name(),
           Sorteringsfelt.AAP_MAXTID_UKE.name(),
           Sorteringsfelt.AAP_UNNTAK_UKER_IGJEN.name(),
           Sorteringsfelt.ARBEIDSLISTE_FRIST.name(),
           Sorteringsfelt.VENTER_PA_SVAR_FRA_NAV.name(),
           Sorteringsfelt.VENTER_PA_SVAR_FRA_BRUKER.name(),
           Sorteringsfelt.UTLOPTE_AKTIVITETER.name(),
           Sorteringsfelt.STARTDATO_FOR_AVTALT_AKTIVITET.name(),
           Sorteringsfelt.NESTE_STARTDATO_FOR_AVTALT_AKTIVITET.name(),
           Sorteringsfelt.FORRIGE_DATO_FOR_AVTALT_AKTIVITET.name(),
           Sorteringsfelt.I_AVTALT_AKTIVITET.name(),
           Sorteringsfelt.AAP_RETTIGHETSPERIODE.name(),
           Sorteringsfelt.MOTER_MED_NAV_IDAG.name(),
           Sorteringsfelt.MOTESTATUS.name(),
           Sorteringsfelt.OPPFOLGING_STARTET.name(),
           Sorteringsfelt.VEILEDER_IDENT.name(),
           Sorteringsfelt.GJELDENDE_VEDTAK_14A_INNSATSGRUPPE.name(),
           Sorteringsfelt.GJELDENDE_VEDTAK_14A_HOVEDMAL.name(),
           Sorteringsfelt.GJELDENDE_VEDTAK_14A_VEDTAKSDATO.name(),
           Sorteringsfelt.UTKAST_14A_STATUS.name(),
           Sorteringsfelt.UTKAST_14A_ANSVARLIG_VEILEDER.name(),
           Sorteringsfelt.UTKAST_14A_STATUS_ENDRET.name(),
           Sorteringsfelt.ARBEIDSLISTE_KATEGORI.name(),
           Sorteringsfelt.ARBEIDSLISTE_OVERSKRIFT.name(),
           Sorteringsfelt.SISTE_ENDRING_DATO.name(),
           Sorteringsfelt.FODELAND.name(),
           Sorteringsfelt.STATSBORGERSKAP.name(),
           Sorteringsfelt.STATSBORGERSKAP_GYLDIG_FRA.name(),
           Sorteringsfelt.TOLKESPRAK.name(),
           Sorteringsfelt.TOLKEBEHOV_SIST_OPPDATERT.name(),
           Sorteringsfelt.BOSTED_KOMMUNE.name(),
           Sorteringsfelt.BOSTED_BYDEL.name(),
           Sorteringsfelt.BOSTED_SIST_OPPDATERT.name(),
           Sorteringsfelt.CV_SVARFRIST.name(),
           Sorteringsfelt.ENSLIGE_FORSORGERE_UTLOP_YTELSE.name(),
           Sorteringsfelt.ENSLIGE_FORSORGERE_VEDTAKSPERIODETYPE.name(),
           Sorteringsfelt.ENSLIGE_FORSORGERE_AKTIVITETSPLIKT.name(),
           Sorteringsfelt.ENSLIGE_FORSORGERE_OM_BARNET.name(),
           Sorteringsfelt.BARN_UNDER_18_AR.name(),
           Sorteringsfelt.BRUKERS_SITUASJON_SIST_ENDRET.name(),
           Sorteringsfelt.HUSKELAPP_FRIST.name(),
           Sorteringsfelt.HUSKELAPP_KOMMENTAR.name(),
           Sorteringsfelt.HUSKELAPP.name(),
           Sorteringsfelt.FARGEKATEGORI.name(),
           Sorteringsfelt.UTDANNING_OG_SITUASJON_SIST_ENDRET.name(),
           Sorteringsfelt.TILTAKSHENDELSE_DATO_OPPRETTET.name(),
           Sorteringsfelt.TILTAKSHENDELSE_TEKST.name()
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
