package no.nav.pto.veilarbportefolje.domene;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.Profileringsresultat;
import no.nav.pto.veilarbportefolje.hendelsesfilter.Hendelse;
import no.nav.pto.veilarbportefolje.opensearch.domene.Endring;
import no.nav.pto.veilarbportefolje.opensearch.domene.PortefoljebrukerOpensearchModell;
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
public class PortefoljebrukerFrontendModell {
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
    LocalDateTime tildeltTidspunkt;
    boolean nyForEnhet;
    VurderingsBehov vurderingsBehov;
    boolean trengerOppfolgingsvedtak;
    Profileringsresultat profileringResultat;
    String innsatsgruppe;
    boolean erDoed;
    String foedeland;
    YtelseMapping ytelse;
    LocalDateTime utlopsdato;
    Integer dagputlopUke;
    Integer permutlopUke;
    Integer aapmaxtidUke;
    Integer aapUnntakUkerIgjen;
    LocalDate aapordinerutlopsdato;
    AapKelvinForBruker aapKelvin;
    TiltakspengerForBruker tiltakspenger;
    LocalDateTime venterPaSvarFraNAV;
    LocalDateTime venterPaSvarFraBruker;
    LocalDateTime nyesteUtlopteAktivitet;
    LocalDateTime aktivitetStart;
    LocalDateTime nesteAktivitetStart;
    LocalDateTime forrigeAktivitetStart;
    LocalDateTime oppfolgingStartdato;
    LocalDateTime nesteUtlopsdatoAktivitet;
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

    LocalDate nesteSvarfristCvStillingFraNav;

    Avvik14aVedtak avvik14aVedtak;
    List<BarnUnder18AarData> barnUnder18AarData;

    EnsligeForsorgereOvergangsstonadFrontend ensligeForsorgereOvergangsstonad;

    LocalDate utdanningOgSituasjonSistEndret;

    HuskelappForBruker huskelapp;
    String fargekategori;
    String fargekategoriEnhetId;

    TiltakshendelseForBruker tiltakshendelse;
    GjeldendeVedtak14a gjeldendeVedtak14a;
    Hendelse.HendelseInnhold utgattVarsel;

    public static PortefoljebrukerFrontendModell of(PortefoljebrukerOpensearchModell brukerOpensearchModell, boolean ufordelt) {

        String formidlingsgruppekode = brukerOpensearchModell.getFormidlingsgruppekode();
        String kvalifiseringsgruppekode = brukerOpensearchModell.getKvalifiseringsgruppekode();
        String sikkerhetstiltak = brukerOpensearchModell.getSikkerhetstiltak();
        Profileringsresultat profileringResultat = brukerOpensearchModell.getProfilering_resultat();
        String diskresjonskode = brukerOpensearchModell.getDiskresjonskode();
        LocalDateTime oppfolgingStartDato = toLocalDateTimeOrNull(brukerOpensearchModell.getOppfolging_startdato());

        VurderingsBehov vurderingsBehov = brukerOpensearchModell.isTrenger_vurdering() ? vurderingsBehov(kvalifiseringsgruppekode, profileringResultat) : null;
        boolean trengerOppfolgingsvedtak = brukerOpensearchModell.getGjeldendeVedtak14a() == null;

        boolean harUtenlandskAdresse = brukerOpensearchModell.getUtenlandskAdresse() != null;

        return new PortefoljebrukerFrontendModell()
                .setNyForEnhet(ufordelt)
                .setFnr(brukerOpensearchModell.getFnr())
                .setAktoerid(brukerOpensearchModell.getAktoer_id())
                .setNyForVeileder(brukerOpensearchModell.isNy_for_veileder())
                .setTildeltTidspunkt(brukerOpensearchModell.getTildelt_tidspunkt())
                .setVurderingsBehov(vurderingsBehov)
                .setTrengerOppfolgingsvedtak(trengerOppfolgingsvedtak)
                .setProfileringResultat(profileringResultat)
                .setErSykmeldtMedArbeidsgiver(OppfolgingUtils.erSykmeldtMedArbeidsgiver(formidlingsgruppekode, kvalifiseringsgruppekode)) // Etiketten sykemeldt ska vises oavsett om brukeren har ett påbegynnt vedtak eller ej
                .setInnsatsgruppe(INNSATSGRUPPEKODER.contains(kvalifiseringsgruppekode) ? kvalifiseringsgruppekode : null)
                .setFornavn(brukerOpensearchModell.getFornavn())
                .setEtternavn(brukerOpensearchModell.getEtternavn())
                .setVeilederId(brukerOpensearchModell.getVeileder_id())
                .setDiskresjonskode((Adressebeskyttelse.FORTROLIG.diskresjonskode.equals(diskresjonskode) || Adressebeskyttelse.STRENGT_FORTROLIG.diskresjonskode.equals(diskresjonskode)) ? diskresjonskode : null)
                .setEgenAnsatt(brukerOpensearchModell.isEgen_ansatt())
                .setSkjermetTil(brukerOpensearchModell.getSkjermet_til())
                .setErDoed(brukerOpensearchModell.isEr_doed())
                .setSikkerhetstiltak(sikkerhetstiltak == null ? new ArrayList<>() : Collections.singletonList(sikkerhetstiltak)) //TODO: Hvorfor er dette en liste?
                .setFoedeland(brukerOpensearchModell.getFoedelandFulltNavn())
                .setYtelse(YtelseMapping.of(brukerOpensearchModell.getYtelse()))
                .setUtlopsdato(toLocalDateTimeOrNull(brukerOpensearchModell.getUtlopsdato()))
                .setDagputlopUke(brukerOpensearchModell.getDagputlopuke())
                .setPermutlopUke(brukerOpensearchModell.getPermutlopuke())
                .setAapmaxtidUke(brukerOpensearchModell.getAapmaxtiduke())
                .setAapUnntakUkerIgjen(brukerOpensearchModell.getAapunntakukerigjen())
                .setAapordinerutlopsdato(brukerOpensearchModell.getAapordinerutlopsdato())
                .setAapKelvin(AapKelvinForBruker.of(
                        brukerOpensearchModell.getAap_kelvin_tom_vedtaksdato(),
                        brukerOpensearchModell.getAap_kelvin_rettighetstype()))
                .setTiltakspenger(TiltakspengerForBruker.of(
                        brukerOpensearchModell.getTiltakspenger_vedtaksdato_tom(),
                        brukerOpensearchModell.getTiltakspenger_rettighet()))
                .setVenterPaSvarFraNAV(toLocalDateTimeOrNull(brukerOpensearchModell.getVenterpasvarfranav()))
                .setVenterPaSvarFraBruker(toLocalDateTimeOrNull(brukerOpensearchModell.getVenterpasvarfrabruker()))
                .setNyesteUtlopteAktivitet(toLocalDateTimeOrNull(brukerOpensearchModell.getNyesteutlopteaktivitet()))
                .setAktivitetStart(toLocalDateTimeOrNull(brukerOpensearchModell.getAktivitet_start()))
                .setNesteAktivitetStart(toLocalDateTimeOrNull(brukerOpensearchModell.getNeste_aktivitet_start()))
                .setForrigeAktivitetStart(toLocalDateTimeOrNull(brukerOpensearchModell.getForrige_aktivitet_start()))
                .setMoteStartTid(toLocalDateTimeOrNull(brukerOpensearchModell.getAktivitet_mote_startdato()))
                .setAlleMoterStartTid(toLocalDateTimeOrNull(brukerOpensearchModell.getAlle_aktiviteter_mote_startdato()))
                .setAlleMoterSluttTid(toLocalDateTimeOrNull(brukerOpensearchModell.getAlle_aktiviteter_mote_utlopsdato()))
                .setNesteSvarfristCvStillingFraNav(brukerOpensearchModell.getNeste_svarfrist_stilling_fra_nav())
                .setUtkast14a(Utkast14a.of(
                        brukerOpensearchModell.getUtkast_14a_status(),
                        toLocalDateTimeOrNull(brukerOpensearchModell.getUtkast_14a_status_endret()),
                        brukerOpensearchModell.getUtkast_14a_ansvarlig_veileder()))
                .setOppfolgingStartdato(oppfolgingStartDato)
                .addAvtaltAktivitetUtlopsdato("tiltak", dateToTimestamp(brukerOpensearchModell.getAktivitet_tiltak_utlopsdato()))
                .addAvtaltAktivitetUtlopsdato("behandling", dateToTimestamp(brukerOpensearchModell.getAktivitet_behandling_utlopsdato()))
                .addAvtaltAktivitetUtlopsdato("sokeavtale", dateToTimestamp(brukerOpensearchModell.getAktivitet_sokeavtale_utlopsdato()))
                .addAvtaltAktivitetUtlopsdato("stilling", dateToTimestamp(brukerOpensearchModell.getAktivitet_stilling_utlopsdato()))
                .addAvtaltAktivitetUtlopsdato("ijobb", dateToTimestamp(brukerOpensearchModell.getAktivitet_ijobb_utlopsdato()))
                .addAvtaltAktivitetUtlopsdato("egen", dateToTimestamp(brukerOpensearchModell.getAktivitet_egen_utlopsdato()))
                .addAvtaltAktivitetUtlopsdato("gruppeaktivitet", dateToTimestamp(brukerOpensearchModell.getAktivitet_gruppeaktivitet_utlopsdato()))
                .addAvtaltAktivitetUtlopsdato("mote", dateToTimestamp(brukerOpensearchModell.getAktivitet_mote_utlopsdato()))
                .addAvtaltAktivitetUtlopsdato("utdanningaktivitet", dateToTimestamp(brukerOpensearchModell.getAktivitet_utdanningaktivitet_utlopsdato()))
                .setTolkebehov(Tolkebehov.of(brukerOpensearchModell.getTalespraaktolk(), brukerOpensearchModell.getTegnspraaktolk(), brukerOpensearchModell.getTolkBehovSistOppdatert()))
                .setHovedStatsborgerskap(brukerOpensearchModell.getHovedStatsborgerskap())
                .setBostedBydel(brukerOpensearchModell.getBydelsnummer())
                .setBostedKommune(brukerOpensearchModell.getKommunenummer())
                .setHarUtelandsAddresse(harUtenlandskAdresse)
                .setHarUkjentBosted(brukerOpensearchModell.isHarUkjentBosted())
                .setBostedSistOppdatert(brukerOpensearchModell.getBostedSistOppdatert())
                .setAvvik14aVedtak(brukerOpensearchModell.getAvvik14aVedtak())
                .setBarnUnder18AarData(brukerOpensearchModell.getBarn_under_18_aar())
                .setEnsligeForsorgereOvergangsstonad(EnsligeForsorgereOvergangsstonadFrontend.of(brukerOpensearchModell.getEnslige_forsorgere_overgangsstonad()))
                .setUtdanningOgSituasjonSistEndret(brukerOpensearchModell.getUtdanning_og_situasjon_sist_endret())
                .setHuskelapp(brukerOpensearchModell.getHuskelapp())
                .setFargekategori(brukerOpensearchModell.getFargekategori())
                .setFargekategoriEnhetId(brukerOpensearchModell.getFargekategori_enhetId())
                .setTiltakshendelse(TiltakshendelseForBruker.of(brukerOpensearchModell.getTiltakshendelse()))
                .setGjeldendeVedtak14a(brukerOpensearchModell.getGjeldendeVedtak14a())
                .setUtgattVarsel(brukerOpensearchModell.getUtgatt_varsel())
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

    private PortefoljebrukerFrontendModell addAvtaltAktivitetUtlopsdato(String type, Timestamp utlopsdato) {
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
