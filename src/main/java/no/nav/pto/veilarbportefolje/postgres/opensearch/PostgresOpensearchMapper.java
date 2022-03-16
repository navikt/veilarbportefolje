package no.nav.pto.veilarbportefolje.postgres.opensearch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.opensearch.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.postgres.opensearch.utils.AktivitetEntityDto;
import no.nav.pto.veilarbportefolje.postgres.opensearch.utils.AvtaltAktivitetEntity;
import no.nav.pto.veilarbportefolje.postgres.opensearch.utils.IkkeAvtaltAktivitetEntity;
import no.nav.pto.veilarbportefolje.postgres.opensearch.utils.PostgresAktorIdEntity;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static no.nav.pto.veilarbportefolje.config.FeatureToggle.brukAvCvdataPaPostgres;
import static no.nav.pto.veilarbportefolje.config.FeatureToggle.brukAvOppfolgingsdataPaPostgres;
import static no.nav.pto.veilarbportefolje.postgres.opensearch.PostgresAktivitetMapper.kalkulerAvtalteAktivitetInformasjon;
import static no.nav.pto.veilarbportefolje.postgres.opensearch.PostgresAktivitetMapper.kalkulerGenerellAktivitetInformasjon;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostgresOpensearchMapper {
    private final AktoerDataOpensearchMapper aktoerDataOpensearchMapper;
    private final AktivitetOpensearchService aktivitetOpensearchService;
    private final UnleashService unleashService;

    public List<OppfolgingsBruker> flettInnPostgresData(List<OppfolgingsBruker> brukere, boolean medDiffLogging) {
        List<AktorId> aktoerIder = brukere.stream().map(OppfolgingsBruker::getAktoer_id).map(AktorId::of).toList();

        HashMap<AktorId, PostgresAktorIdEntity> aktorIdData = aktoerDataOpensearchMapper.hentAktoerData(aktoerIder);
        Map<AktorId, List<AktivitetEntityDto>> avtalteAktiviterMap = aktivitetOpensearchService.hentAvtaltAktivitetData(aktoerIder);
        Map<AktorId, List<AktivitetEntityDto>> ikkeAvtalteAktiviterMap = aktivitetOpensearchService.hentIkkeAvtaltAktivitetData(aktoerIder);

        brukere.forEach(bruker -> {
                    AktorId aktorId = AktorId.of(bruker.getAktoer_id());
                    Optional.ofNullable(aktorIdData.get(aktorId))
                            .ifPresentOrElse(
                                    postgresAktorIdData -> flettInnAktoerData(postgresAktorIdData, bruker, medDiffLogging),
                                    () -> log.warn("Fant ikke aktoer i aktoer basert postgres: {}", bruker.getAktoer_id())
                            );
                    List<AktivitetEntityDto> avtalteAktiviteter = avtalteAktiviterMap.get(aktorId) != null ? avtalteAktiviterMap.get(aktorId) : new ArrayList<>();
                    List<AktivitetEntityDto> ikkeAvtalteAktiviteter = ikkeAvtalteAktiviterMap.get(aktorId) != null ? ikkeAvtalteAktiviterMap.get(aktorId) : new ArrayList<>();

                    AvtaltAktivitetEntity avtaltAktivitetData = kalkulerAvtalteAktivitetInformasjon(avtalteAktiviteter);
                    IkkeAvtaltAktivitetEntity ikkeAvtaltAktivitetData = kalkulerGenerellAktivitetInformasjon(
                            Stream.concat(avtalteAktiviteter.stream(), ikkeAvtalteAktiviteter.stream()).toList()
                    );

                    flettInnAvtaltAktivitetData(avtaltAktivitetData, bruker);
                    flettInnIkkeAvtaltAktivitetData(ikkeAvtaltAktivitetData, bruker);
                }
        );

        return brukere;
    }

    private void flettInnIkkeAvtaltAktivitetData(IkkeAvtaltAktivitetEntity aktivitetData, OppfolgingsBruker bruker) {
        bruker.setAlle_aktiviteter_mote_startdato(aktivitetData.getAktivitetMoteStartdato());
        bruker.setAlle_aktiviteter_mote_utlopsdato(aktivitetData.getAktivitetMoteUtlopsdato());
        bruker.setAlle_aktiviteter_stilling_utlopsdato(aktivitetData.getAktivitetStillingUtlopsdato());
        bruker.setAlle_aktiviteter_egen_utlopsdato(aktivitetData.getAktivitetEgenUtlopsdato());
        bruker.setAlle_aktiviteter_behandling_utlopsdato(aktivitetData.getAktivitetBehandlingUtlopsdato());
        bruker.setAlle_aktiviteter_ijobb_utlopsdato(aktivitetData.getAktivitetIjobbUtlopsdato());
        bruker.setAlle_aktiviteter_sokeavtale_utlopsdato(aktivitetData.getAktivitetSokeavtaleUtlopsdato());
        bruker.setAlleAktiviteter(aktivitetData.getAlleAktiviteter());
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

    private void flettInnAktoerData(PostgresAktorIdEntity dataPaAktorId, OppfolgingsBruker bruker, boolean medDiffLogging) {
        if (medDiffLogging) {
            loggDiff(dataPaAktorId, bruker);
        }
        if (brukAvOppfolgingsdataPaPostgres(unleashService)) {
            bruker.setOppfolging(dataPaAktorId.getOppfolging());
            bruker.setNy_for_veileder(dataPaAktorId.getNyForVeileder());
            bruker.setManuell_bruker(dataPaAktorId.getManuellBruker() ? "MANUELL" : null);
            bruker.setVeileder_id(dataPaAktorId.getVeileder());
            bruker.setOppfolging_startdato(dataPaAktorId.getOppfolgingStartdato());
        }
        if (brukAvCvdataPaPostgres(unleashService)) {
            bruker.setCv_eksistere(dataPaAktorId.getCvEksistere());
            bruker.setHar_delt_cv(dataPaAktorId.getHarDeltCv());
        }

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

        if (dataPaAktorId.isArbeidslisteAktiv()) {
            bruker.setArbeidsliste_sist_endret_av_veilederid(dataPaAktorId.getArbeidslisteSistEndretAvVeilederid());
            bruker.setArbeidsliste_endringstidspunkt(dataPaAktorId.getArbeidslisteEndringstidspunkt());
            bruker.setArbeidsliste_frist(dataPaAktorId.getArbeidslisteFrist());
            bruker.setArbeidsliste_kategori(dataPaAktorId.getArbeidslisteKategori());
            bruker.setArbeidsliste_tittel_sortering(dataPaAktorId.getArbeidslisteTittelSortering());
            bruker.setArbeidsliste_tittel_lengde(dataPaAktorId.getArbeidslisteTittelLengde());
        }
    }

    private void loggDiff(PostgresAktorIdEntity postgresAktorIdEntity, OppfolgingsBruker bruker) {
        if (bruker.isHar_delt_cv() != postgresAktorIdEntity.getHarDeltCv()) {
            log.info("postgres Opensearch: isHar_delt_cv feil bruker: {}", bruker.getAktoer_id());
        }
        if (bruker.isCv_eksistere() != postgresAktorIdEntity.getCvEksistere()) {
            log.info("postgres Opensearch: isCv_eksistere feil bruker: {}", bruker.getAktoer_id());
        }
        if (bruker.isOppfolging() != postgresAktorIdEntity.getOppfolging()) {
            log.info("postgres Opensearch: isOppfolging feil bruker: {}", bruker.getAktoer_id());
        }
        if (bruker.isNy_for_veileder() != postgresAktorIdEntity.getNyForVeileder()) {
            log.info("postgres Opensearch: isNy_for_veileder feil bruker: {}", bruker.getAktoer_id());
        }
        if ((bruker.getManuell_bruker() != null && bruker.getManuell_bruker().equals("MANUELL")) != postgresAktorIdEntity.getManuellBruker()) {
            log.info("postgres Opensearch: getManuell_bruker feil bruker: {}", bruker.getAktoer_id());
        }
        if (isDifferent(bruker.getOppfolging_startdato(), postgresAktorIdEntity.getOppfolgingStartdato())) {
            log.info("postgres Opensearch: getOppfolging_startdato feil bruker: {}", bruker.getAktoer_id());
        }
        if (isDifferent(bruker.getVenterpasvarfrabruker(), postgresAktorIdEntity.getVenterpasvarfrabruker())) {
            log.info("postgres Opensearch: Venterpasvarfrabruker feil bruker: {}", bruker.getAktoer_id());
        }
        if (isDifferent(bruker.getVenterpasvarfranav(), postgresAktorIdEntity.getVenterpasvarfranav())) {
            log.info("postgres Opensearch: getVenterpasvarfranav feil bruker: {}", bruker.getAktoer_id());
        }

        // Arbeidslista
        if (isDifferent(bruker.isArbeidsliste_aktiv(), postgresAktorIdEntity.isArbeidslisteAktiv())) {
            log.info("postgres Opensearch: arbeidslisteAktiv feil på bruker {}", bruker.getAktoer_id());
        }
        if (bruker.isArbeidsliste_aktiv()) {
            if (isDifferent(bruker.getArbeidsliste_sist_endret_av_veilederid(), postgresAktorIdEntity.getArbeidslisteSistEndretAvVeilederid())) {
                log.info("postgres Opensearch: arbeidslisteSistEndretAvVeilederid feil på bruker {}", bruker.getAktoer_id());
            }
            if (isDifferentDate(bruker.getArbeidsliste_endringstidspunkt(), postgresAktorIdEntity.getArbeidslisteEndringstidspunkt())) {
                log.info("postgres Opensearch: arbeidslisteEndringstidspunkt feil på bruker {}", bruker.getAktoer_id());
            }
            if (isDifferent(bruker.getArbeidsliste_frist(), postgresAktorIdEntity.getArbeidslisteFrist())) {
                log.info("postgres Opensearch: arbeidslisteFrist feil på bruker {}", bruker.getAktoer_id());
            }
            if (isDifferent(bruker.getArbeidsliste_kategori(), postgresAktorIdEntity.getArbeidslisteKategori())) {
                log.info("postgres Opensearch: arbeidslisteKategori feil på bruker {}", bruker.getAktoer_id());
            }
            if (isDifferent(bruker.getArbeidsliste_tittel_sortering(), postgresAktorIdEntity.getArbeidslisteTittelSortering())) {
                log.info("postgres Opensearch: arbeidslisteTittelSortering feil på bruker {}", bruker.getAktoer_id());
            }
            if (isDifferent(bruker.getArbeidsliste_tittel_lengde(), postgresAktorIdEntity.getArbeidslisteTittelLengde())) {
                log.info("postgres Opensearch: arbeidslisteTittelLengde feil på bruker {}", bruker.getAktoer_id());
            }
        }
    }

    private boolean isDifferent(Object o, Object other) {
        if (o == null && other == null) {
            return false;
        } else if (o == null || other == null) {
            return true;
        }
        return !o.equals(other);
    }

    private boolean isDifferentDate(String o, String other) {
        if (o == null && other == null) {
            return false;
        } else if (o == null || other == null) {
            return true;
        }
        return !o.substring(0, 10).equals(other.substring(0, 10));
    }

}
