package no.nav.pto.veilarbportefolje.domene;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.arbeidsliste.Arbeidsliste;
import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.Profileringsresultat;
import no.nav.pto.veilarbportefolje.hendelsesfilter.Hendelse;
import no.nav.pto.veilarbportefolje.opensearch.domene.Endring;
import no.nav.pto.veilarbportefolje.opensearch.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.avvik14aVedtak.Avvik14aVedtak;
import no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.gjeldende14aVedtak.GjeldendeVedtak14a;
import no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar.BarnUnder18AarData;
import no.nav.pto.veilarbportefolje.persononinfo.domene.Adressebeskyttelse;
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
    String diskresjonskode;
    boolean egenAnsatt;
    LocalDateTime skjermetTil;
    boolean nyForVeileder;
    boolean nyForEnhet;
    VurderingsBehov vurderingsBehov;
    boolean trengerOppfolgingsvedtak;
    Profileringsresultat profileringResultat;
    String innsatsgruppe;
    boolean erDoed;
    int fodselsdagIMnd;
    String foedeland;
    String kjonn;
    YtelseMapping ytelse;
    LocalDateTime utlopsdato;
    Integer dagputlopUke;
    Integer permutlopUke;
    Integer aapmaxtidUke;
    Integer aapUnntakUkerIgjen;
    LocalDate aapordinerutlopsdato;
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
    LocalDateTime alleMoterStartTid;
    LocalDateTime alleMoterSluttTid;
    boolean erSykmeldtMedArbeidsgiver;
    Utkast14a utkast14a;
    String sisteEndringKategori;
    LocalDateTime sisteEndringTidspunkt;
    String sisteEndringAktivitetId;
    Tolkebehov tolkebehov;
    Statsborgerskap hovedStatsborgerskap;
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

    LocalDate utdanningOgSituasjonSistEndret;

    HuskelappForBruker huskelapp;
    String fargekategori;
    String fargekategoriEnhetId;

    TiltakshendelseForBruker tiltakshendelse;
    GjeldendeVedtak14a gjeldendeVedtak14a;
    Hendelse.HendelseInnhold utgattVarsel;

    public static Bruker of(OppfolgingsBruker bruker, boolean ufordelt) {

        String formidlingsgruppekode = bruker.getFormidlingsgruppekode();
        String kvalifiseringsgruppekode = bruker.getKvalifiseringsgruppekode();
        String sikkerhetstiltak = bruker.getSikkerhetstiltak();
        Profileringsresultat profileringResultat = bruker.getProfilering_resultat();
        String diskresjonskode = bruker.getDiskresjonskode();
        LocalDateTime oppfolgingStartDato = toLocalDateTimeOrNull(bruker.getOppfolging_startdato());

        VurderingsBehov vurderingsBehov = bruker.isTrenger_vurdering() ? vurderingsBehov(kvalifiseringsgruppekode, profileringResultat) : null;
        boolean trengerOppfolgingsvedtak = bruker.getGjeldendeVedtak14a() == null;

        boolean harUtenlandskAdresse = bruker.getUtenlandskAdresse() != null;

        return new Bruker()
                .setNyForEnhet(ufordelt)
                .setFnr(bruker.getFnr())
                .setAktoerid(bruker.getAktoer_id())
                .setNyForVeileder(bruker.isNy_for_veileder())
                .setVurderingsBehov(vurderingsBehov)
                .setTrengerOppfolgingsvedtak(trengerOppfolgingsvedtak)
                .setProfileringResultat(profileringResultat)
                .setErSykmeldtMedArbeidsgiver(OppfolgingUtils.erSykmeldtMedArbeidsgiver(formidlingsgruppekode, kvalifiseringsgruppekode)) // Etiketten sykemeldt ska vises oavsett om brukeren har ett påbegynnt vedtak eller ej
                .setInnsatsgruppe(INNSATSGRUPPEKODER.contains(kvalifiseringsgruppekode) ? kvalifiseringsgruppekode : null)
                .setFornavn(bruker.getFornavn())
                .setEtternavn(bruker.getEtternavn())
                .setVeilederId(bruker.getVeileder_id())
                .setDiskresjonskode((Adressebeskyttelse.FORTROLIG.diskresjonskode.equals(diskresjonskode) || Adressebeskyttelse.STRENGT_FORTROLIG.diskresjonskode.equals(diskresjonskode)) ? diskresjonskode : null)
                .setEgenAnsatt(bruker.isEgen_ansatt())
                .setSkjermetTil(bruker.getSkjermet_til())
                .setErDoed(bruker.isEr_doed())
                .setSikkerhetstiltak(sikkerhetstiltak == null ? new ArrayList<>() : Collections.singletonList(sikkerhetstiltak)) //TODO: Hvorfor er dette en liste?
                .setFodselsdagIMnd(bruker.getFodselsdag_i_mnd())
                .setFoedeland(bruker.getFoedelandFulltNavn())
                .setKjonn(bruker.getKjonn())
                .setYtelse(YtelseMapping.of(bruker.getYtelse()))
                .setUtlopsdato(toLocalDateTimeOrNull(bruker.getUtlopsdato()))
                .setDagputlopUke(bruker.getDagputlopuke())
                .setPermutlopUke(bruker.getPermutlopuke())
                .setAapmaxtidUke(bruker.getAapmaxtiduke())
                .setAapUnntakUkerIgjen(bruker.getAapunntakukerigjen())
                .setAapordinerutlopsdato(bruker.getAapordinerutlopsdato())
                .setVenterPaSvarFraNAV(toLocalDateTimeOrNull(bruker.getVenterpasvarfranav()))
                .setVenterPaSvarFraBruker(toLocalDateTimeOrNull(bruker.getVenterpasvarfrabruker()))
                .setNyesteUtlopteAktivitet(toLocalDateTimeOrNull(bruker.getNyesteutlopteaktivitet()))
                .setAktivitetStart(toLocalDateTimeOrNull(bruker.getAktivitet_start()))
                .setNesteAktivitetStart(toLocalDateTimeOrNull(bruker.getNeste_aktivitet_start()))
                .setForrigeAktivitetStart(toLocalDateTimeOrNull(bruker.getForrige_aktivitet_start()))
                .setBrukertiltak(new ArrayList<>(bruker.getTiltak()))
                .setMoteStartTid(toLocalDateTimeOrNull(bruker.getAktivitet_mote_startdato()))
                .setAlleMoterStartTid(toLocalDateTimeOrNull(bruker.getAlle_aktiviteter_mote_startdato()))
                .setAlleMoterSluttTid(toLocalDateTimeOrNull(bruker.getAlle_aktiviteter_mote_utlopsdato()))
                .setNesteCvKanDelesStatus(bruker.getNeste_cv_kan_deles_status())
                .setNesteSvarfristCvStillingFraNav(bruker.getNeste_svarfrist_stilling_fra_nav())
                .setUtkast14a(Utkast14a.of(
                        bruker.getUtkast_14a_status(),
                        toLocalDateTimeOrNull(bruker.getUtkast_14a_status_endret()),
                        bruker.getUtkast_14a_ansvarlig_veileder()))
                .setOppfolgingStartdato(oppfolgingStartDato)
                .addAvtaltAktivitetUtlopsdato("tiltak", dateToTimestamp(bruker.getAktivitet_tiltak_utlopsdato()))
                .addAvtaltAktivitetUtlopsdato("behandling", dateToTimestamp(bruker.getAktivitet_behandling_utlopsdato()))
                .addAvtaltAktivitetUtlopsdato("sokeavtale", dateToTimestamp(bruker.getAktivitet_sokeavtale_utlopsdato()))
                .addAvtaltAktivitetUtlopsdato("stilling", dateToTimestamp(bruker.getAktivitet_stilling_utlopsdato()))
                .addAvtaltAktivitetUtlopsdato("ijobb", dateToTimestamp(bruker.getAktivitet_ijobb_utlopsdato()))
                .addAvtaltAktivitetUtlopsdato("egen", dateToTimestamp(bruker.getAktivitet_egen_utlopsdato()))
                .addAvtaltAktivitetUtlopsdato("gruppeaktivitet", dateToTimestamp(bruker.getAktivitet_gruppeaktivitet_utlopsdato()))
                .addAvtaltAktivitetUtlopsdato("mote", dateToTimestamp(bruker.getAktivitet_mote_utlopsdato()))
                .addAvtaltAktivitetUtlopsdato("utdanningaktivitet", dateToTimestamp(bruker.getAktivitet_utdanningaktivitet_utlopsdato()))
                .setTolkebehov(Tolkebehov.of(bruker.getTalespraaktolk(), bruker.getTegnspraaktolk(), bruker.getTolkBehovSistOppdatert()))
                .setHovedStatsborgerskap(bruker.getHovedStatsborgerskap())
                .setBostedBydel(bruker.getBydelsnummer())
                .setBostedKommune(bruker.getKommunenummer())
                .setHarUtelandsAddresse(harUtenlandskAdresse)
                .setHarUkjentBosted(bruker.isHarUkjentBosted())
                .setBostedSistOppdatert(bruker.getBostedSistOppdatert())
                .setAvvik14aVedtak(bruker.getAvvik14aVedtak())
                .setBarnUnder18AarData(bruker.getBarn_under_18_aar())
                .setEnsligeForsorgereOvergangsstonad(bruker.getEnslige_forsorgere_overgangsstonad())
                .setUtdanningOgSituasjonSistEndret(bruker.getUtdanning_og_situasjon_sist_endret())
                .setHuskelapp(bruker.getHuskelapp())
                .setFargekategori(bruker.getFargekategori())
                .setFargekategoriEnhetId(bruker.getFargekategori_enhetId())
                .setTiltakshendelse(TiltakshendelseForBruker.of(bruker.getTiltakshendelse()))
                .setGjeldendeVedtak14a(bruker.getGjeldendeVedtak14a())
                .setUtgattVarsel(bruker.getUtgatt_varsel())
                .setNesteUtlopsdatoAktivitet(null);
    }

    public void kalkulerNesteUtlopsdatoAvValgtAktivitetFornklet(List<String> aktiviteterForenklet) {
        if (aktiviteterForenklet == null) {
            return;
        }
        aktiviteterForenklet.forEach(navnPaaAktivitet -> nesteUtlopsdatoAktivitet = nesteUtlopsdatoAktivitet(aktiviteter.get(navnPaaAktivitet.toLowerCase()), nesteUtlopsdatoAktivitet));
    }

    public void kalkulerNesteUtlopsdatoAvValgtTiltakstype() {
        nesteUtlopsdatoAktivitet = nesteUtlopsdatoAktivitet(aktiviteter.get("tiltak"), nesteUtlopsdatoAktivitet);
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

    private LocalDateTime nesteUtlopsdatoAktivitet(Timestamp aktivitetUlopsdato, LocalDateTime comp) {
        if (aktivitetUlopsdato == null) {
            return comp;
        }
        LocalDateTime aktivitetDato = aktivitetUlopsdato.toLocalDateTime();
        if (comp == null || comp.isAfter(aktivitetDato)) {
            return aktivitetDato;
        }
        return comp;
    }
}
