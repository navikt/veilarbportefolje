package no.nav.pto.veilarbportefolje.domene;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.arbeidsliste.Arbeidsliste;
import no.nav.pto.veilarbportefolje.opensearch.domene.Endring;
import no.nav.pto.veilarbportefolje.opensearch.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar.BarnUnder18AarData;
import no.nav.pto.veilarbportefolje.persononinfo.domene.Adressebeskyttelse;
import no.nav.pto.veilarbportefolje.siste14aVedtak.Avvik14aVedtak;
import no.nav.pto.veilarbportefolje.huskelapp.domain.Huskelapp;
import no.nav.pto.veilarbportefolje.util.OppfolgingUtils;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static no.nav.pto.veilarbportefolje.domene.AktivitetFiltervalg.JA;
import static no.nav.pto.veilarbportefolje.util.DateUtils.*;
import static no.nav.pto.veilarbportefolje.util.OppfolgingUtils.INNSATSGRUPPEKODER;
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
    String sikkerhetstiltak_gyldig_fra;
    String sikkerhetstiltak_gyldig_til;
    String sikkerhetstiltak_beskrivelse;
    String diskresjonskode;
    boolean egenAnsatt;
    LocalDateTime skjermetTil;
    boolean nyForVeileder;
    boolean nyForEnhet;
    boolean trengerVurdering;
    VurderingsBehov vurderingsBehov;
    String innsatsgruppe;
    boolean erDoed;
    String manuellBrukerStatus;
    int fodselsdagIMnd;
    LocalDateTime fodselsdato;
    String foedeland;
    String kjonn;
    YtelseMapping ytelse;
    LocalDateTime utlopsdato;
    Integer dagputlopUke;
    Integer permutlopUke;
    Integer aapmaxtidUke;
    Integer aapUnntakUkerIgjen;
    LocalDate aapordinerutlopsdato;
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
    String utkast14aStatus;
    String utkast14aAnsvarligVeileder;
    LocalDateTime utkast14aStatusEndret;
    boolean trengerRevurdering;
    String sisteEndringKategori;
    LocalDateTime sisteEndringTidspunkt;
    String sisteEndringAktivitetId;
    String talespraaktolk;
    String tegnspraaktolk;
    LocalDate tolkBehovSistOppdatert;
    String landgruppe;
    Statsborgerskap hovedStatsborgerskap;
    boolean harFlereStatsborgerskap;
    String bostedKommune;
    String bostedBydel;
    LocalDate bostedSistOppdatert;
    boolean harUtelandsAddresse;
    boolean harUkjentBosted;

    String nesteCvKanDelesStatus;
    LocalDate nesteSvarfristCvStillingFraNav;

    Avvik14aVedtak avvik14aVedtak;
    List<BarnUnder18AarData> barnUnder18AarData;

    EnsligeForsorgereOvergangsstonad ensligeForsorgereOvergangsstonad;

    LocalDate brukersSituasjonSistEndret;

    HuskelappForBruker huskelapp;


    public static Bruker of(OppfolgingsBruker bruker, boolean ufordelt, boolean erVedtakstottePilotPa) {

        String formidlingsgruppekode = bruker.getFormidlingsgruppekode();
        String kvalifiseringsgruppekode = bruker.getKvalifiseringsgruppekode();
        String sikkerhetstiltak = bruker.getSikkerhetstiltak();
        String profileringResultat = bruker.getProfilering_resultat();
        String diskresjonskode = bruker.getDiskresjonskode();
        LocalDateTime oppfolgingStartDato = toLocalDateTimeOrNull(bruker.getOppfolging_startdato());
        boolean trengerVurdering = bruker.isTrenger_vurdering();
        boolean harUtenlandskAdresse = bruker.getUtenlandskAdresse() != null;

        return new Bruker()
                .setNyForEnhet(ufordelt)
                .setFnr(bruker.getFnr())
                .setAktoerid(bruker.getAktoer_id())
                .setNyForVeileder(bruker.isNy_for_veileder())
                .setTrengerVurdering(trengerVurdering)
                .setErSykmeldtMedArbeidsgiver(OppfolgingUtils.erSykmeldtMedArbeidsgiver(formidlingsgruppekode, kvalifiseringsgruppekode)) // Etiketten sykemeldt ska vises oavsett om brukeren har ett påbegynnt vedtak eller ej
                .setVurderingsBehov(trengerVurdering ? vurderingsBehov(formidlingsgruppekode, kvalifiseringsgruppekode, profileringResultat, erVedtakstottePilotPa) : null)
                .setInnsatsgruppe(INNSATSGRUPPEKODER.contains(kvalifiseringsgruppekode) ? kvalifiseringsgruppekode : null)
                .setFornavn(bruker.getFornavn())
                .setEtternavn(bruker.getEtternavn())
                .setVeilederId(bruker.getVeileder_id())
                .setDiskresjonskode((Adressebeskyttelse.FORTROLIG.diskresjonskode.equals(diskresjonskode) || Adressebeskyttelse.STRENGT_FORTROLIG.diskresjonskode.equals(diskresjonskode)) ? diskresjonskode : null)
                .setEgenAnsatt(bruker.isEgen_ansatt())
                .setSkjermetTil(bruker.getSkjermet_til())
                .setErDoed(bruker.isEr_doed())
                .setSikkerhetstiltak(sikkerhetstiltak == null ? new ArrayList<>() : Collections.singletonList(sikkerhetstiltak)) //TODO: Hvorfor er dette en liste?
                .setSikkerhetstiltak_gyldig_fra(bruker.getSikkerhetstiltak_gyldig_fra())
                .setSikkerhetstiltak_gyldig_til(bruker.getSikkerhetstiltak_gyldig_til())
                .setSikkerhetstiltak_beskrivelse(bruker.getSikkerhetstiltak_beskrivelse())
                .setFodselsdagIMnd(bruker.getFodselsdag_i_mnd())
                .setFodselsdato(toLocalDateTimeOrNull(bruker.getFodselsdato()))
                .setFoedeland(bruker.getFoedelandFulltNavn())
                .setKjonn(bruker.getKjonn())
                .setYtelse(YtelseMapping.of(bruker.getYtelse()))
                .setUtlopsdato(toLocalDateTimeOrNull(bruker.getUtlopsdato()))
                .setDagputlopUke(bruker.getDagputlopuke())
                .setPermutlopUke(bruker.getPermutlopuke())
                .setAapmaxtidUke(bruker.getAapmaxtiduke())
                .setAapUnntakUkerIgjen(bruker.getAapunntakukerigjen())
                .setAapordinerutlopsdato(bruker.getAapordinerutlopsdato())
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
                .setNesteCvKanDelesStatus(bruker.getNeste_cv_kan_deles_status())
                .setNesteSvarfristCvStillingFraNav(bruker.getNeste_svarfrist_stilling_fra_nav())
                .setUtkast14aStatus(bruker.getUtkast_14a_status())
                .setUtkast14aStatusEndret(toLocalDateTimeOrNull(bruker.getUtkast_14a_status_endret()))
                .setUtkast14aAnsvarligVeileder(bruker.getUtkast_14a_ansvarlig_veileder())
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
                .addAlleAktiviteterUtlopsdato("mote", dateToTimestamp(bruker.getAlle_aktiviteter_mote_utlopsdato()))
                .setTegnspraaktolk(bruker.getTegnspraaktolk())
                .setTalespraaktolk(bruker.getTalespraaktolk())
                .setTolkBehovSistOppdatert(bruker.getTolkBehovSistOppdatert())
                .setHarFlereStatsborgerskap(bruker.isHarFlereStatsborgerskap())
                .setHovedStatsborgerskap(bruker.getHovedStatsborgerskap())
                .setLandgruppe(bruker.getLandgruppe())
                .setBostedBydel(bruker.getBydelsnummer())
                .setBostedKommune(bruker.getKommunenummer())
                .setHarUtelandsAddresse(harUtenlandskAdresse)
                .setHarUkjentBosted(bruker.isHarUkjentBosted())
                .setBostedSistOppdatert(bruker.getBostedSistOppdatert())
                .setAvvik14aVedtak(bruker.getAvvik14aVedtak())
                .setBarnUnder18AarData(bruker.getBarn_under_18_aar())
                .setEnsligeForsorgereOvergangsstonad(bruker.getEnslige_forsorgere_overgangsstonad())
                .setBrukersSituasjonSistEndret(bruker.getBrukers_situasjon_sist_endret())
                .setHuskelapp(bruker.getHuskelapp());
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
