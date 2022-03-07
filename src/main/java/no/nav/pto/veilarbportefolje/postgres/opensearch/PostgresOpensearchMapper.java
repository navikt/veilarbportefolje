package no.nav.pto.veilarbportefolje.postgres.opensearch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.opensearch.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.postgres.opensearch.utils.AktivitetEntity;
import no.nav.pto.veilarbportefolje.postgres.opensearch.utils.PostgresAktivitetMapper;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.config.FeatureToggle.erArbeidslistaPaPostgres;
import static no.nav.pto.veilarbportefolje.config.FeatureToggle.erYtelserPaPostgres;

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
        Map<AktorId, List<AktivitetEntity>> aktiveAktiviter = aktivitetOpensearchService.hentAktivitetData(aktoerIder);

        brukere.forEach(bruker -> {
                    Optional.ofNullable(aktorIdData.get(AktorId.of(bruker.getAktoer_id())))
                            .ifPresentOrElse(
                                    postgresAktorIdData -> flettInnAktoerData(postgresAktorIdData, bruker, medDiffLogging),
                                    () -> log.warn("Fant ikke aktoer i aktoer basert postgres: {}", bruker.getAktoer_id())
                            );
                    Optional.ofNullable(aktiveAktiviter.get(AktorId.of(bruker.getAktoer_id())))
                            .ifPresentOrElse(
                                    aktivitetsListe -> {
                                        PostgresAktivitetEntity aktivitetData = PostgresAktivitetMapper.build(aktivitetsListe);
                                        flettInnAktivitetData(aktivitetData, bruker);
                                    },
                                    () -> flettInnAktivitetData(new PostgresAktivitetEntity(), bruker)
                            );
                }
        );

        return brukere;
    }

    private void flettInnAktivitetData(PostgresAktivitetEntity aktivitetData, OppfolgingsBruker bruker) {
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
        bruker.setBrukers_situasjon(dataPaAktorId.getBrukersSituasjon());
        bruker.setProfilering_resultat(dataPaAktorId.getProfileringResultat());
        bruker.setUtdanning(dataPaAktorId.getUtdanning());
        bruker.setUtdanning_bestatt(dataPaAktorId.getUtdanningBestatt());
        bruker.setUtdanning_godkjent(dataPaAktorId.getUtdanningGodkjent());

        if(erArbeidslistaPaPostgres(unleashService)){
            bruker.setArbeidsliste_aktiv(dataPaAktorId.isArbeidslisteAktiv());
            if(dataPaAktorId.isArbeidslisteAktiv()) {
                bruker.setArbeidsliste_sist_endret_av_veilederid(dataPaAktorId.getArbeidslisteSistEndretAvVeilederid());
                bruker.setArbeidsliste_endringstidspunkt(dataPaAktorId.getArbeidslisteEndringstidspunkt());
                bruker.setArbeidsliste_frist(dataPaAktorId.getArbeidslisteFrist());
                bruker.setArbeidsliste_kategori(dataPaAktorId.getArbeidslisteKategori());
                bruker.setArbeidsliste_tittel_sortering(dataPaAktorId.getArbeidslisteTittelSortering());
                bruker.setArbeidsliste_tittel_lengde(dataPaAktorId.getArbeidslisteTittelLengde());
            }
        }
        if(erYtelserPaPostgres(unleashService)){
            bruker.setYtelse(dataPaAktorId.getYtelse());
            bruker.setUtlopsdato(dataPaAktorId.getYtelseUtlopsdato());
            bruker.setDagputlopuke(dataPaAktorId.getDagputlopuke());
            bruker.setPermutlopuke(dataPaAktorId.getPermutlopuke());
            bruker.setAapmaxtiduke(dataPaAktorId.getAapmaxtiduke());
            bruker.setAapunntakukerigjen(dataPaAktorId.getAapunntakukerigjen());
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

        if (isDifferent(bruker.getYtelse(), postgresAktorIdEntity.getYtelse())) {
            log.info("postgres Opensearch: getYtelse feil bruker: {}", bruker.getAktoer_id());
        }

        // Ytelser
        if (isDifferentDate(bruker.getUtlopsdato(), postgresAktorIdEntity.getYtelseUtlopsdato())) {
            log.info("postgres Opensearch: ytelseUtlopsdato feil på bruker {}", bruker.getAktoer_id());
        }
        if (isDifferent(bruker.getDagputlopuke(), postgresAktorIdEntity.getDagputlopuke())) {
            log.info("postgres Opensearch: dagputlopuke feil på bruker {}", bruker.getAktoer_id());
        }
        if (isDifferent(bruker.getPermutlopuke(), postgresAktorIdEntity.getPermutlopuke())) {
            log.info("postgres Opensearch: permutlopuke feil på bruker {}", bruker.getAktoer_id());
        }
        if (isDifferent(bruker.getAapmaxtiduke(), postgresAktorIdEntity.getAapmaxtiduke())) {
            log.info("postgres Opensearch: aapmaxtiduke feil på bruker {}", bruker.getAktoer_id());
        }
        if (isDifferent(bruker.getAapunntakukerigjen(), postgresAktorIdEntity.getAapunntakukerigjen())) {
            log.info("postgres Opensearch: aapunntakukerigjen feil på bruker {}", bruker.getAktoer_id());
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
