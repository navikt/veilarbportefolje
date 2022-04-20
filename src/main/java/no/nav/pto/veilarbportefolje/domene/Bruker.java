package no.nav.pto.veilarbportefolje.domene;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.arbeidsliste.Arbeidsliste;
import no.nav.pto.veilarbportefolje.opensearch.domene.Endring;
import no.nav.pto.veilarbportefolje.opensearch.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.util.OppfolgingUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static no.nav.pto.veilarbportefolje.domene.AktivitetFiltervalg.JA;
import static no.nav.pto.veilarbportefolje.util.DateUtils.dateToTimestamp;
import static no.nav.pto.veilarbportefolje.util.DateUtils.isFarInTheFutureDate;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toLocalDateTimeOrNull;
import static no.nav.pto.veilarbportefolje.util.OppfolgingUtils.vurderingsBehov;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Slf4j
@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class Bruker {
    String fnr;
    String aktoerid;
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
    Integer dagputlopUke;
    Integer permutlopUke;
    Integer aapmaxtidUke;
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
    LocalDateTime nesteUtlopsdatoAlleAktiviteter;
    List<String> brukertiltak;
    Map<String, Timestamp> aktiviteter = new HashMap<>();
    Map<String, Timestamp> alleAktiviteter = new HashMap<>();
    LocalDateTime moteStartTid;
    LocalDateTime moteSluttTid;
    LocalDateTime alleMoterStartTid;
    LocalDateTime alleMoterSluttTid;
    boolean erSykmeldtMedArbeidsgiver;
    String vedtakStatus;
    String ansvarligVeilederForVedtak;
    LocalDateTime vedtakStatusEndret;
    boolean trengerRevurdering;
    String sisteEndringKategori;
    LocalDateTime sisteEndringTidspunkt;
    String sisteEndringAktivitetId;

    public static Bruker of(OppfolgingsBruker bruker, boolean ufordelt, boolean erVedtakstottePilotPa) {

        String formidlingsgruppekode = bruker.getFormidlingsgruppekode();
        String kvalifiseringsgruppekode = bruker.getKvalifiseringsgruppekode();
        String sikkerhetstiltak = bruker.getSikkerhetstiltak();
        String profileringResultat = bruker.getProfilering_resultat();
        String diskresjonskode = bruker.getDiskresjonskode();
        LocalDateTime oppfolgingStartDato = toLocalDateTimeOrNull(bruker.getOppfolging_startdato());
        boolean trengerVurdering = bruker.isTrenger_vurdering();

        return new Bruker()
                .setNyForEnhet(ufordelt)
                .setFnr(bruker.getFnr())
                .setAktoerid(bruker.getAktoer_id())
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
                .setDagputlopUke(bruker.getDagputlopuke())
                .setPermutlopUke(bruker.getPermutlopuke())
                .setAapmaxtidUke(bruker.getAapmaxtiduke())
                .setAapUnntakUkerIgjen(bruker.getAapunntakukerigjen())
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
                .setAlleMoterStartTid(toLocalDateTimeOrNull(bruker.getAlle_aktiviteter_mote_startdato()))
                .setAlleMoterSluttTid(toLocalDateTimeOrNull(bruker.getAlle_aktiviteter_mote_utlopsdato()))
                .setVedtakStatus(bruker.getVedtak_status())
                .setVedtakStatusEndret(toLocalDateTimeOrNull(bruker.getVedtak_status_endret()))
                .setAnsvarligVeilederForVedtak(bruker.getAnsvarlig_veileder_for_vedtak())
                .setOppfolgingStartdato(oppfolgingStartDato)
                .setTrengerRevurdering(trengerRevurdering(bruker, erVedtakstottePilotPa))
                .addAvtaltAktivitetUtlopsdato("tiltak", dateToTimestamp(bruker.getAktivitet_tiltak_utlopsdato()))
                .addAvtaltAktivitetUtlopsdato("behandling", dateToTimestamp(bruker.getAktivitet_behandling_utlopsdato()))
                .addAvtaltAktivitetUtlopsdato("sokeavtale", dateToTimestamp(bruker.getAktivitet_sokeavtale_utlopsdato()))
                .addAvtaltAktivitetUtlopsdato("stilling", dateToTimestamp(bruker.getAktivitet_stilling_utlopsdato()))
                .addAvtaltAktivitetUtlopsdato("ijobb", dateToTimestamp(bruker.getAktivitet_ijobb_utlopsdato()))
                .addAvtaltAktivitetUtlopsdato("egen", dateToTimestamp(bruker.getAktivitet_egen_utlopsdato()))
                .addAvtaltAktivitetUtlopsdato("gruppeaktivitet", dateToTimestamp(bruker.getAktivitet_gruppeaktivitet_utlopsdato()))
                .addAvtaltAktivitetUtlopsdato("mote", dateToTimestamp(bruker.getAktivitet_mote_utlopsdato()))
                .addAvtaltAktivitetUtlopsdato("utdanningaktivitet", dateToTimestamp(bruker.getAktivitet_utdanningaktivitet_utlopsdato()))
                .addAlleAktiviteterUtlopsdato("behandling", dateToTimestamp(bruker.getAlle_aktiviteter_behandling_utlopsdato()))
                .addAlleAktiviteterUtlopsdato("sokeavtale", dateToTimestamp(bruker.getAlle_aktiviteter_sokeavtale_utlopsdato()))
                .addAlleAktiviteterUtlopsdato("stilling", dateToTimestamp(bruker.getAlle_aktiviteter_stilling_utlopsdato()))
                .addAlleAktiviteterUtlopsdato("ijobb", dateToTimestamp(bruker.getAlle_aktiviteter_ijobb_utlopsdato()))
                .addAlleAktiviteterUtlopsdato("egen", dateToTimestamp(bruker.getAlle_aktiviteter_egen_utlopsdato()))
                .addAlleAktiviteterUtlopsdato("mote", dateToTimestamp(bruker.getAlle_aktiviteter_mote_utlopsdato()));
    }

    public void kalkulerNesteUtlopsdatoAvValgtAktivitetFornklet(List<String> aktiviteterForenklet) {
        if (aktiviteterForenklet == null) {
            return;
        }
        aktiviteterForenklet.forEach(navnPaaAktivitet -> nesteUtlopsdatoAktivitet = nesteUtlopsdatoAktivitet(aktiviteter.get(navnPaaAktivitet.toLowerCase()), nesteUtlopsdatoAktivitet));
    }

    public void leggTilUtlopsdatoForAktiviteter(List<String> aktiviteterForenklet) {
        if (aktiviteterForenklet == null) {
            return;
        }
        aktiviteterForenklet.forEach(navnPaaAktivitet -> nesteUtlopsdatoAlleAktiviteter = nesteUtlopsdatoAktivitet(alleAktiviteter.get(navnPaaAktivitet.toLowerCase()), nesteUtlopsdatoAlleAktiviteter));
    }

    public void kalkulerNesteUtlopsdatoAvValgtAktivitetAvansert(Map<String, AktivitetFiltervalg> aktiviteterAvansert) {
        if (aktiviteterAvansert == null) {
            return;
        }
        aktiviteterAvansert.forEach((navnPaaAktivitet, valg) -> {
            if (JA.equals(valg)) {
                nesteUtlopsdatoAktivitet = nesteUtlopsdatoAktivitet(aktiviteter.get(navnPaaAktivitet.toLowerCase()), nesteUtlopsdatoAktivitet);
            }
        });
    }

    public boolean erKonfidensiell() {
        return (isNotEmpty(this.diskresjonskode)) || (this.egenAnsatt);
    }

    public void kalkulerSisteEndring(Map<String, Endring> siste_endringer, List<String> kategorier) {
        if (siste_endringer == null) {
            return;
        }
        kategorier.forEach(kategori -> {
            if (erNyesteKategori(siste_endringer, kategori)) {
                Endring endring = siste_endringer.get(kategori);
                sisteEndringKategori = kategori;
                sisteEndringTidspunkt = toLocalDateTimeOrNull(endring.getTidspunkt());
                sisteEndringAktivitetId = endring.getAktivtetId();
            }
        });
    }

    private boolean erNyesteKategori(Map<String, Endring> siste_endringer, String kategori) {
        if (siste_endringer.get(kategori) == null) {
            return false;
        }
        LocalDateTime tidspunkt = toLocalDateTimeOrNull(siste_endringer.get(kategori).getTidspunkt());
        return sisteEndringTidspunkt == null || (tidspunkt != null && tidspunkt.isAfter(sisteEndringTidspunkt));
    }

    private Bruker addAvtaltAktivitetUtlopsdato(String type, Timestamp utlopsdato) {
        if (Objects.isNull(utlopsdato) || isFarInTheFutureDate(utlopsdato)) {
            return this;
        }
        aktiviteter.put(type, utlopsdato);
        return this;
    }

    private Bruker addAlleAktiviteterUtlopsdato(String type, Timestamp utlopsdato) {
        if (Objects.isNull(utlopsdato) || isFarInTheFutureDate(utlopsdato)) {
            return this;
        }
        alleAktiviteter.put(type, utlopsdato);
        return this;
    }

    private static boolean trengerRevurdering(OppfolgingsBruker oppfolgingsBruker, boolean erVedtakstottePilotPa) {
        if (erVedtakstottePilotPa) {
            return oppfolgingsBruker.isTrenger_revurdering();
        }
        return false;
    }

    private LocalDateTime nesteUtlopsdatoAktivitet(Timestamp aktivitetUlopsdato, LocalDateTime comp) {
        if (aktivitetUlopsdato == null) {
            return null;
        }
        if (comp == null) {
            return aktivitetUlopsdato.toLocalDateTime();
        } else if (comp.isAfter(aktivitetUlopsdato.toLocalDateTime())) {
            return aktivitetUlopsdato.toLocalDateTime();
        }
        return comp;
    }
}
