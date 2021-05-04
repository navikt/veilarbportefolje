package no.nav.pto.veilarbportefolje.domene;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.arbeidsliste.Arbeidsliste;
import no.nav.pto.veilarbportefolje.elastic.domene.Endring;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.util.OppfolgingUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

import static no.nav.pto.veilarbportefolje.database.PostgresTable.BRUKER_VIEW.*;
import static no.nav.pto.veilarbportefolje.domene.AktivitetFiltervalg.JA;
import static no.nav.pto.veilarbportefolje.util.DateUtils.*;
import static no.nav.pto.veilarbportefolje.util.OppfolgingUtils.vurderingsBehov;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Slf4j
@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class Bruker {
    String fnr;
    String fornavn;
    String etternavn;
    String veilederId;
    List<String> sikkerhetstiltak;
    String diskresjonskode;
    boolean egenAnsatt;
    boolean nyForVeileder;
    boolean nyForEnhet;
    boolean trengerVurdering;
    VurderingsBehov vurderingsBehov;
    boolean erDoed;
    String manuellBrukerStatus;
    int fodselsdagIMnd;
    LocalDateTime fodselsdato;
    String kjonn;
    YtelseMapping ytelse;
    LocalDateTime utlopsdato;
    ManedFasettMapping utlopsdatoFasett;
    Integer dagputlopUke;
    DagpengerUkeFasettMapping dagputlopUkeFasett;
    Integer permutlopUke;
    DagpengerUkeFasettMapping permutlopUkeFasett;
    Integer aapmaxtidUke;
    AAPMaxtidUkeFasettMapping aapmaxtidUkeFasett;
    AAPUnntakUkerIgjenFasettMapping aapUnntakUkerIgjenFasett;
    Integer aapUnntakUkerIgjen;
    Arbeidsliste arbeidsliste;
    LocalDateTime venterPaSvarFraNAV;
    LocalDateTime venterPaSvarFraBruker;
    LocalDateTime nyesteUtlopteAktivitet;
    LocalDateTime aktivitetStart;
    LocalDateTime nesteAktivitetStart;
    LocalDateTime forrigeAktivitetStart;
    LocalDateTime oppfolgingStartdato;
    LocalDateTime nesteUtlopsdatoAktivitet;
    List<String> brukertiltak;
    Map<String, Timestamp> aktiviteter = new HashMap<>();
    LocalDateTime moteStartTid;
    LocalDateTime moteSluttTid;
    boolean erSykmeldtMedArbeidsgiver;
    String vedtakStatus;
    String ansvarligVeilederForVedtak;
    LocalDateTime vedtakStatusEndret;
    boolean trengerRevurdering;
    String sisteEndringKategori;
    LocalDateTime sisteEndringTidspunkt;
    String sisteEndringAktivitetId;

    public static Bruker of(OppfolgingsBruker bruker, boolean erVedtakstottePilotPa) {

        String formidlingsgruppekode = bruker.getFormidlingsgruppekode();
        String kvalifiseringsgruppekode = bruker.getKvalifiseringsgruppekode();
        String sikkerhetstiltak = bruker.getSikkerhetstiltak();
        String profileringResultat = bruker.getProfilering_resultat();
        String diskresjonskode = bruker.getDiskresjonskode();
        LocalDateTime oppfolgingStartDato = toLocalDateTimeOrNull(bruker.getOppfolging_startdato());
        boolean trengerVurdering = bruker.isTrenger_vurdering();

        return new Bruker()
                .setFnr(bruker.getFnr())
                .setNyForEnhet(bruker.isNy_for_enhet())
                .setNyForVeileder(bruker.isNy_for_veileder())
                .setTrengerVurdering(trengerVurdering)
                .setErSykmeldtMedArbeidsgiver(OppfolgingUtils.erSykmeldtMedArbeidsgiver(formidlingsgruppekode, kvalifiseringsgruppekode)) // Etiketten sykemeldt ska vises oavsett om brukeren har ett påbegynnt vedtak eller ej
                .setVurderingsBehov(trengerVurdering ? vurderingsBehov(formidlingsgruppekode, kvalifiseringsgruppekode, profileringResultat, erVedtakstottePilotPa) : null)
                .setFornavn(bruker.getFornavn())
                .setEtternavn(bruker.getEtternavn())
                .setVeilederId(bruker.getVeileder_id())
                .setDiskresjonskode(("7".equals(diskresjonskode) || "6".equals(diskresjonskode)) ? diskresjonskode : null)
                .setEgenAnsatt(bruker.isEgen_ansatt())
                .setErDoed(bruker.isEr_doed())
                .setSikkerhetstiltak(sikkerhetstiltak == null ? new ArrayList<>() : Collections.singletonList(sikkerhetstiltak)) //TODO: Hvorfor er dette en liste?
                .setFodselsdagIMnd(bruker.getFodselsdag_i_mnd())
                .setFodselsdato(toLocalDateTimeOrNull(bruker.getFodselsdato()))
                .setKjonn(bruker.getKjonn())
                .setYtelse(YtelseMapping.of(bruker.getYtelse()))
                .setUtlopsdato(toLocalDateTimeOrNull(bruker.getUtlopsdato()))
                .setUtlopsdatoFasett(ManedFasettMapping.of(bruker.getUtlopsdatofasett()))
                .setDagputlopUke(bruker.getDagputlopuke())
                .setDagputlopUkeFasett(DagpengerUkeFasettMapping.of(bruker.getDagputlopukefasett()))
                .setPermutlopUke(bruker.getPermutlopuke())
                .setPermutlopUkeFasett(DagpengerUkeFasettMapping.of(bruker.getPermutlopukefasett()))
                .setAapmaxtidUke(bruker.getAapmaxtiduke())
                .setAapmaxtidUkeFasett(AAPMaxtidUkeFasettMapping.of(bruker.getAapmaxtidukefasett()))
                .setAapUnntakUkerIgjen(bruker.getAapunntakukerigjen())
                .setAapUnntakUkerIgjenFasett(AAPUnntakUkerIgjenFasettMapping.of(bruker.getAapunntakukerigjenfasett()))
                .setArbeidsliste(Arbeidsliste.of(bruker))
                .setVenterPaSvarFraNAV(toLocalDateTimeOrNull(bruker.getVenterpasvarfranav()))
                .setVenterPaSvarFraBruker(toLocalDateTimeOrNull(bruker.getVenterpasvarfrabruker()))
                .setNyesteUtlopteAktivitet(toLocalDateTimeOrNull(bruker.getNyesteutlopteaktivitet()))
                .setAktivitetStart(toLocalDateTimeOrNull(bruker.getAktivitet_start()))
                .setNesteAktivitetStart(toLocalDateTimeOrNull(bruker.getNeste_aktivitet_start()))
                .setForrigeAktivitetStart(toLocalDateTimeOrNull(bruker.getForrige_aktivitet_start()))
                .setBrukertiltak(new ArrayList<>(bruker.getTiltak()))
                .setManuellBrukerStatus(bruker.getManuell_bruker())
                .setMoteStartTid(toLocalDateTimeOrNull(bruker.getAktivitet_mote_startdato()))
                .setMoteSluttTid(toLocalDateTimeOrNull(bruker.getAktivitet_mote_utlopsdato()))
                .setVedtakStatus(bruker.getVedtak_status())
                .setVedtakStatusEndret(toLocalDateTimeOrNull(bruker.getVedtak_status_endret()))
                .setAnsvarligVeilederForVedtak(bruker.getAnsvarlig_veileder_for_vedtak())
                .setOppfolgingStartdato(oppfolgingStartDato)
                .setTrengerRevurdering(trengerRevurdering(bruker, erVedtakstottePilotPa))
                .addAktivitetUtlopsdato("tiltak", dateToTimestamp(bruker.getAktivitet_tiltak_utlopsdato()))
                .addAktivitetUtlopsdato("behandling", dateToTimestamp(bruker.getAktivitet_behandling_utlopsdato()))
                .addAktivitetUtlopsdato("sokeavtale", dateToTimestamp(bruker.getAktivitet_sokeavtale_utlopsdato()))
                .addAktivitetUtlopsdato("stilling", dateToTimestamp(bruker.getAktivitet_stilling_utlopsdato()))
                .addAktivitetUtlopsdato("ijobb", dateToTimestamp(bruker.getAktivitet_ijobb_utlopsdato()))
                .addAktivitetUtlopsdato("egen", dateToTimestamp(bruker.getAktivitet_egen_utlopsdato()))
                .addAktivitetUtlopsdato("gruppeaktivitet", dateToTimestamp(bruker.getAktivitet_gruppeaktivitet_utlopsdato()))
                .addAktivitetUtlopsdato("mote", dateToTimestamp(bruker.getAktivitet_mote_utlopsdato()))
                .addAktivitetUtlopsdato("utdanningaktivitet", dateToTimestamp(bruker.getAktivitet_utdanningaktivitet_utlopsdato()));
    }

    private Bruker addAktivitetUtlopsdato(String type, Timestamp utlopsdato) {
        if (Objects.isNull(utlopsdato) || isFarInTheFutureDate(utlopsdato)) {
            return this;
        }
        aktiviteter.put(type, utlopsdato);
        return this;
    }

    public void kalkulerSisteEndring(Map<String, Endring> siste_endringer, List<String> kategorier) {
        if (siste_endringer == null) {
            return;
        }

        for (String kategori : kategorier) {
            Endring endring = siste_endringer.get(kategori);
            if (endring != null) {
                LocalDateTime tidspunkt = toLocalDateTimeOrNull(endring.getTidspunkt());
                if (tidspunkt != null && (sisteEndringTidspunkt == null || tidspunkt.isAfter(sisteEndringTidspunkt))) {
                    sisteEndringTidspunkt = tidspunkt;
                    sisteEndringKategori = kategori;
                    sisteEndringAktivitetId = siste_endringer.get(kategori).getAktivtetId();
                }
            }
        }
    }

    public void kalkulerNesteUtlopsdatoAvValgtAktivitetFornklet(List<String> aktiviteterForenklet) {
        if (aktiviteterForenklet == null) {
            return;
        }
        aktiviteterForenklet.forEach(navnPaaAktivitet -> setNesteUtlopsdatoAktivitetHvisNyest(aktiviteter.get(navnPaaAktivitet.toLowerCase())));
    }

    public void kalkulerNesteUtlopsdatoAvValgtAktivitetAvansert(Map<String, AktivitetFiltervalg> aktiviteterAvansert) {
        if (aktiviteterAvansert == null) {
            return;
        }
        aktiviteterAvansert.forEach((navnPaaAktivitet, valg) -> {
            if (JA.equals(valg)) {
                setNesteUtlopsdatoAktivitetHvisNyest(aktiviteter.get(navnPaaAktivitet.toLowerCase()));
            }
        });
    }

    public boolean erKonfidensiell() {
        return (isNotEmpty(this.diskresjonskode)) || (this.egenAnsatt);
    }

    private static boolean trengerRevurdering(OppfolgingsBruker oppfolgingsBruker, boolean erVedtakstottePilotPa) {
        if (erVedtakstottePilotPa) {
            return oppfolgingsBruker.isTrenger_revurdering();
        }
        return false;
    }

    private void setNesteUtlopsdatoAktivitetHvisNyest(Timestamp aktivitetUlopsdato) {
        if (aktivitetUlopsdato == null) {
            return;
        }
        if (nesteUtlopsdatoAktivitet == null) {
            nesteUtlopsdatoAktivitet = aktivitetUlopsdato.toLocalDateTime();
        } else if (nesteUtlopsdatoAktivitet.isAfter(aktivitetUlopsdato.toLocalDateTime())) {
            nesteUtlopsdatoAktivitet = aktivitetUlopsdato.toLocalDateTime();
        }
    }

    public Bruker fraEssensiellInfo(Map<String, Object> row) {
        String diskresjonskode = (String) row.get(DISKRESJONSKODE);
        String formidlingsgruppekode = (String) row.get(FORMIDLINGSGRUPPEKODE);

        return setNyForVeileder((boolean) row.get(NY_FOR_VEILEDER))
                .setVeilederId((String) row.get(VEILEDERID))
                .setDiskresjonskode((String) row.get(DISKRESJONSKODE))
                .setFnr((String) row.get(FODSELSNR))
                .setFornavn((String) row.get(FORNAVN))
                .setEtternavn((String) row.get(ETTERNAVN))
                .setDiskresjonskode(("7".equals(diskresjonskode) || "6".equals(diskresjonskode)) ? diskresjonskode : null)
                .setOppfolgingStartdato(toLocalDateTimeOrNull((Timestamp) row.get(STARTDATO)));

    }

    public Bruker fraBrukerView(Map<String, Object> row) {
        String diskresjonskode = (String) row.get(DISKRESJONSKODE);
        String kvalifiseringsgruppekode = (String) row.get(KVALIFISERINGSGRUPPEKODE);
        return setNyForVeileder((boolean) row.get(NY_FOR_VEILEDER))
                .setVeilederId((String) row.get(VEILEDERID))
                .setDiskresjonskode((String) row.get(DISKRESJONSKODE))
                .setFnr((String) row.get(FODSELSNR))
                .setFornavn((String) row.get(FORNAVN))
                .setEtternavn((String) row.get(ETTERNAVN))
                .setDiskresjonskode(("7".equals(diskresjonskode) || "6".equals(diskresjonskode)) ? diskresjonskode : null)
                .setOppfolgingStartdato(toLocalDateTimeOrNull((Timestamp) row.get(STARTDATO)))
                .setVenterPaSvarFraBruker(toLocalDateTimeOrNull((Timestamp) row.get(VENTER_PA_BRUKER)))
                .setVenterPaSvarFraNAV(toLocalDateTimeOrNull((Timestamp) row.get(VENTER_PA_NAV)))
                .setEgenAnsatt((boolean) row.get(SPERRET_ANSATT))
                .setErDoed((boolean) row.get(ER_DOED));

        //TODO: utledd manuell
    }
    // TODO: sjekk om disse feltene er i bruk, de kan være nødvendige for statuser eller filtere
    /*
        public static final String MANUELL = "MANUELL";
        public static final String ISERV_FRA_DATO = "ISERV_FRA_DATO";
        public static final String FORMIDLINGSGRUPPEKODE = "FORMIDLINGSGRUPPEKODE";
        public static final String KVALIFISERINGSGRUPPEKODE = "KVALIFISERINGSGRUPPEKODE";
        public static final String RETTIGHETSGRUPPEKODE = "RETTIGHETSGRUPPEKODE";
        public static final String HOVEDMAALKODE = "HOVEDMAALKODE";
        public static final String SIKKERHETSTILTAK_TYPE_KODE = "SIKKERHETSTILTAK_TYPE_KODE";
        public static final String HAR_OPPFOLGINGSSAK = "HAR_OPPFOLGINGSSAK";
     */
}
