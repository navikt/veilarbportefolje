package no.nav.pto.veilarbportefolje.domene;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.arbeidsliste.Arbeidsliste;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.feed.oppfolging.OppfolgingUtils;
import no.nav.pto.veilarbportefolje.registrering.DinSituasjonSvar;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static no.nav.common.utils.CollectionUtils.toList;
import static no.nav.pto.veilarbportefolje.util.DateUtils.dateToTimestamp;
import static no.nav.pto.veilarbportefolje.util.DateUtils.isFarInTheFutureDate;
import static no.nav.pto.veilarbportefolje.feed.oppfolging.OppfolgingUtils.vurderingsBehov;
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
    List<String> brukertiltak;
    Map<String, Timestamp> aktiviteter = new HashMap<>();
    LocalDateTime moteStartTid;
    LocalDateTime moteSluttTid;
    boolean erSykmeldtMedArbeidsgiver;
    String vedtakStatus;
    LocalDateTime vedtakStatusEndret;
    boolean trengerRevurdering;
    boolean erPermittertEtterNiendeMars;

    public static Bruker of(OppfolgingsBruker bruker) {

        String formidlingsgruppekode = bruker.getFormidlingsgruppekode();
        String kvalifiseringsgruppekode = bruker.getKvalifiseringsgruppekode();
        String sikkerhetstiltak = bruker.getSikkerhetstiltak();
        String diskresjonskode = bruker.getDiskresjonskode();
        LocalDateTime oppfolgingStartDato = toLocalDateTimeOrNull(bruker.getOppfolging_startdato());
        String brukersSitasjon = bruker.getBrukers_situasjon();


        return new Bruker()
                .setFnr(bruker.getFnr())
                .setNyForEnhet(bruker.isNy_for_enhet())
                .setNyForVeileder(bruker.isNy_for_veileder())
                .setTrengerVurdering(bruker.isTrenger_vurdering())
                .setErSykmeldtMedArbeidsgiver(OppfolgingUtils.erSykmeldtMedArbeidsgiver(formidlingsgruppekode, kvalifiseringsgruppekode)) // Etiketten sykemeldt ska vises oavsett om brukeren har ett påbegynnt vedtak eller ej
                .setVurderingsBehov(vurderingsBehov(formidlingsgruppekode, kvalifiseringsgruppekode))
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
                .setBrukertiltak(toList(bruker.getTiltak()))
                .setManuellBrukerStatus(bruker.getManuell_bruker())
                .setMoteStartTid(toLocalDateTimeOrNull(bruker.getAktivitet_mote_startdato()))
                .setMoteSluttTid(toLocalDateTimeOrNull(bruker.getAktivitet_mote_utlopsdato()))
                .setVedtakStatus(bruker.getVedtak_status())
                .setVedtakStatusEndret(toLocalDateTimeOrNull(bruker.getVedtak_status_endret()))
                .setOppfolgingStartdato(oppfolgingStartDato)
                .setTrengerRevurdering(bruker.isTrenger_revurdering())
                .setErPermittertEtterNiendeMars(erPermittertEtterNiondeMars(oppfolgingStartDato, brukersSitasjon))
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

    private static LocalDateTime toLocalDateTimeOrNull(String date) {
        if (date == null) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.parse(date), ZoneId.systemDefault());
    }


    private Bruker addAktivitetUtlopsdato(String type, Timestamp utlopsdato) {
        if (Objects.isNull(utlopsdato) || isFarInTheFutureDate(utlopsdato)) {
            return this;
        }
        aktiviteter.put(type, utlopsdato);
        return this;
    }

    private static boolean erPermittertEtterNiondeMars(LocalDateTime oppfolgingStartDato, String brukersSitasjon ) {
        if(brukersSitasjon != null && oppfolgingStartDato !=null ) {
            boolean oppfolgingStartDatoErEtterNiende = oppfolgingStartDato.isAfter(LocalDate.of(2020, 3, 10).atStartOfDay());
            boolean erPermittert = DinSituasjonSvar.valueOf(brukersSitasjon).equals(DinSituasjonSvar.ER_PERMITTERT);
            return oppfolgingStartDatoErEtterNiende && erPermittert;
        }
            return false;
    }

    public boolean erKonfidensiell() {
        return (isNotEmpty(this.diskresjonskode)) || (this.egenAnsatt);
    }
}
