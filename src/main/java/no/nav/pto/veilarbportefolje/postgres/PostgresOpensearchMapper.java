package no.nav.pto.veilarbportefolje.postgres;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.opensearch.domene.Endring;
import no.nav.pto.veilarbportefolje.opensearch.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.oppfolging.SkjermingService;
import no.nav.pto.veilarbportefolje.postgres.utils.AktivitetEntity;
import no.nav.pto.veilarbportefolje.postgres.utils.AvtaltAktivitetEntity;
import no.nav.pto.veilarbportefolje.postgres.utils.PostgresAktorIdEntity;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringService;
import no.nav.pto.veilarbportefolje.vedtakstotte.KafkaVedtakStatusEndring;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static no.nav.pto.veilarbportefolje.config.FeatureToggle.brukNOMSkjerming;
import static no.nav.pto.veilarbportefolje.postgres.PostgresAktivitetMapper.kalkulerAvtalteAktivitetInformasjon;
import static no.nav.pto.veilarbportefolje.postgres.PostgresAktivitetMapper.kalkulerGenerellAktivitetInformasjon;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostgresOpensearchMapper {
    private final AktoerDataOpensearchMapper aktoerDataOpensearchMapper;
    private final AktivitetOpensearchService aktivitetOpensearchService;
    private final SkjermingService skjermingService;
    private final SisteEndringService sisteEndringService;
    private final UnleashService unleashService;

    @Deprecated
    public List<OppfolgingsBruker> flettInnPostgresData(List<OppfolgingsBruker> brukere) {
        List<AktorId> aktoerIder = brukere.stream().map(OppfolgingsBruker::getAktoer_id).map(AktorId::of).toList();
        List<String> fnrs = brukere.stream().map(OppfolgingsBruker::getFnr).collect(Collectors.toList());
        Set<Fnr> skjermetPersonerNOM = skjermingService.hentSkjermetPersoner(fnrs);
        HashMap<AktorId, PostgresAktorIdEntity> aktorIdData = aktoerDataOpensearchMapper.hentAktoerData(aktoerIder);

        brukere.forEach(bruker -> {
                    AktorId aktorId = AktorId.of(bruker.getAktoer_id());
                    boolean erSkjermet_NOM = skjermetPersonerNOM.contains(Fnr.of(bruker.getFnr()));

                    Optional.ofNullable(aktorIdData.get(aktorId))
                            .ifPresentOrElse(
                                    postgresAktorIdData -> flettInnBrukerData(postgresAktorIdData, bruker, erSkjermet_NOM),
                                    () -> log.warn("Fant ikke aktoer i aktoer basert postgres: {}", bruker.getAktoer_id())
                            );
                }
        );
        return brukere;
    }

    public List<OppfolgingsBruker> flettInnAktivitetsData(List<OppfolgingsBruker> brukere) {
        List<AktorId> aktoerIder = brukere.stream().map(OppfolgingsBruker::getAktoer_id).map(AktorId::of).toList();
        Map<AktorId, List<AktivitetEntityDto>> avtalteAktiviterMap = aktivitetOpensearchService.hentAvtaltAktivitetData(aktoerIder);
        Map<AktorId, List<AktivitetEntityDto>> ikkeAvtalteAktiviterMap = aktivitetOpensearchService.hentIkkeAvtaltAktivitetData(aktoerIder);

        brukere.forEach(bruker -> {
                    AktorId aktorId = AktorId.of(bruker.getAktoer_id());

                    List<AktivitetEntityDto> avtalteAktiviteter = avtalteAktiviterMap.get(aktorId) != null ? avtalteAktiviterMap.get(aktorId) : new ArrayList<>();
                    List<AktivitetEntityDto> ikkeAvtalteAktiviteter = ikkeAvtalteAktiviterMap.get(aktorId) != null ? ikkeAvtalteAktiviterMap.get(aktorId) : new ArrayList<>();

                    AvtaltAktivitetEntity avtaltAktivitetData = kalkulerAvtalteAktivitetInformasjon(avtalteAktiviteter);
                    AktivitetEntity alleAktiviteterData = kalkulerGenerellAktivitetInformasjon(
                            Stream.concat(avtalteAktiviteter.stream(), ikkeAvtalteAktiviteter.stream()).toList()
                    );
                    flettInnAvtaltAktivitetData(avtaltAktivitetData, bruker);
                    flettInnAktivitetData(alleAktiviteterData, bruker);
                }
        );

        return brukere;
    }

    public Map<AktorId, Map<String, Endring>> hentPostgresSisteEndringerData(List<OppfolgingsBruker> brukere) {
        List<AktorId> aktoerIder = brukere.stream().map(OppfolgingsBruker::getAktoer_id).map(AktorId::of).toList();
        return sisteEndringService.hentSisteEndringerFraPostgres(aktoerIder);
    }

    private void flettInnAktivitetData(AktivitetEntity aktivitetData, OppfolgingsBruker bruker) {
        bruker.setAlle_aktiviteter_mote_startdato(aktivitetData.getAktivitetMoteStartdato());
        bruker.setAlle_aktiviteter_mote_utlopsdato(aktivitetData.getAktivitetMoteUtlopsdato());
        bruker.setAlle_aktiviteter_stilling_utlopsdato(aktivitetData.getAktivitetStillingUtlopsdato());
        bruker.setAlle_aktiviteter_egen_utlopsdato(aktivitetData.getAktivitetEgenUtlopsdato());
        bruker.setAlle_aktiviteter_behandling_utlopsdato(aktivitetData.getAktivitetBehandlingUtlopsdato());
        bruker.setAlle_aktiviteter_ijobb_utlopsdato(aktivitetData.getAktivitetIjobbUtlopsdato());
        bruker.setAlle_aktiviteter_sokeavtale_utlopsdato(aktivitetData.getAktivitetSokeavtaleUtlopsdato());
        bruker.setAlleAktiviteter(aktivitetData.getAlleAktiviteter());
        // NOTE: tiltak, gruppeaktiviteter, og utdanningsaktiviteter blir håndtert av copy_to feltet i opensearch
        // Dette gjøres da disse aktivitetene ikke kan være satt til "ikke avtalt"
    }

    private void flettInnAvtaltAktivitetData(AvtaltAktivitetEntity aktivitetData, OppfolgingsBruker bruker) {
        bruker.setNyesteutlopteaktivitet(aktivitetData.getNyesteUtlopteAktivitet());
        bruker.setAktivitet_start(aktivitetData.getAktivitetStart());
        bruker.setNeste_aktivitet_start(aktivitetData.getNesteAktivitetStart());
        bruker.setForrige_aktivitet_start(aktivitetData.getForrigeAktivitetStart());
        bruker.setAktivitet_mote_utlopsdato(aktivitetData.getAktivitetMoteUtlopsdato());
        bruker.setAktivitet_mote_startdato(aktivitetData.getAktivitetMoteStartdato());
        bruker.setAktivitet_stilling_utlopsdato(aktivitetData.getAktivitetStillingUtlopsdato());
        bruker.setAktivitet_egen_utlopsdato(aktivitetData.getAktivitetEgenUtlopsdato());
        bruker.setAktivitet_behandling_utlopsdato(aktivitetData.getAktivitetBehandlingUtlopsdato());
        bruker.setAktivitet_ijobb_utlopsdato(aktivitetData.getAktivitetIjobbUtlopsdato());
        bruker.setAktivitet_sokeavtale_utlopsdato(aktivitetData.getAktivitetSokeavtaleUtlopsdato());
        bruker.setAktivitet_tiltak_utlopsdato(aktivitetData.getAktivitetTiltakUtlopsdato());
        bruker.setAktivitet_utdanningaktivitet_utlopsdato(aktivitetData.getAktivitetUtdanningaktivitetUtlopsdato());
        bruker.setAktivitet_gruppeaktivitet_utlopsdato(aktivitetData.getAktivitetGruppeaktivitetUtlopsdato());

        bruker.setAktiviteter(aktivitetData.getAktiviteter());
        bruker.setTiltak(aktivitetData.getTiltak());
    }

    @Deprecated
    private void flettInnBrukerData(PostgresAktorIdEntity dataPaAktorId, OppfolgingsBruker bruker, boolean erSkjermet_NOM) {
        bruker.setOppfolging(dataPaAktorId.getOppfolging());
        bruker.setNy_for_veileder(dataPaAktorId.getNyForVeileder());
        bruker.setManuell_bruker(dataPaAktorId.getManuellBruker() ? "MANUELL" : null);
        bruker.setVeileder_id(dataPaAktorId.getVeileder());
        bruker.setOppfolging_startdato(dataPaAktorId.getOppfolgingStartdato());

        bruker.setVenterpasvarfranav(dataPaAktorId.getVenterpasvarfranav());
        bruker.setVenterpasvarfrabruker(dataPaAktorId.getVenterpasvarfrabruker());

        if (brukNOMSkjerming(unleashService)) {
            bruker.setEgen_ansatt(erSkjermet_NOM);
        }

        bruker.setCv_eksistere(dataPaAktorId.getCvEksistere());
        bruker.setHar_delt_cv(dataPaAktorId.getHarDeltCv());

        bruker.setBrukers_situasjon(dataPaAktorId.getBrukersSituasjon());
        bruker.setProfilering_resultat(dataPaAktorId.getProfileringResultat());
        bruker.setUtdanning(dataPaAktorId.getUtdanning());
        bruker.setUtdanning_bestatt(dataPaAktorId.getUtdanningBestatt());
        bruker.setUtdanning_godkjent(dataPaAktorId.getUtdanningGodkjent());
        bruker.setArbeidsliste_aktiv(dataPaAktorId.isArbeidslisteAktiv());

        bruker.setYtelse(dataPaAktorId.getYtelse());
        bruker.setUtlopsdato(dataPaAktorId.getYtelseUtlopsdato());
        bruker.setDagputlopuke(dataPaAktorId.getDagputlopuke());
        bruker.setPermutlopuke(dataPaAktorId.getPermutlopuke());
        bruker.setAapmaxtiduke(dataPaAktorId.getAapmaxtiduke());
        bruker.setAapunntakukerigjen(dataPaAktorId.getAapunntakukerigjen());

        bruker.setVedtak_status(Optional.ofNullable(dataPaAktorId.getVedtak14AStatus())
                .map(KafkaVedtakStatusEndring.VedtakStatusEndring::valueOf)
                .map(KafkaVedtakStatusEndring::vedtakStatusTilTekst)
                .orElse(null)
        );
        bruker.setVedtak_status_endret(dataPaAktorId.getVedtak14AStatusEndret());
        bruker.setAnsvarlig_veileder_for_vedtak(dataPaAktorId.getAnsvarligVeilederFor14AVedtak());

        if (dataPaAktorId.isArbeidslisteAktiv()) {
            bruker.setArbeidsliste_sist_endret_av_veilederid(dataPaAktorId.getArbeidslisteSistEndretAvVeilederid());
            bruker.setArbeidsliste_endringstidspunkt(dataPaAktorId.getArbeidslisteEndringstidspunkt());
            bruker.setArbeidsliste_frist(dataPaAktorId.getArbeidslisteFrist());
            bruker.setArbeidsliste_kategori(dataPaAktorId.getArbeidslisteKategori());
            bruker.setArbeidsliste_tittel_sortering(dataPaAktorId.getArbeidslisteTittelSortering());
            bruker.setArbeidsliste_tittel_lengde(dataPaAktorId.getArbeidslisteTittelLengde());
        }
    }

    public void loggDiff(OppfolgingsBruker oppfolgingsBrukerOracle, OppfolgingsBruker oppfolgingsBrukerPostgres) {
        if (!oppfolgingsBrukerOracle.equals(oppfolgingsBrukerPostgres)) {
            log.info("fant en diff på bruker: {}", oppfolgingsBrukerOracle.getAktoer_id());
        }
    }
}
