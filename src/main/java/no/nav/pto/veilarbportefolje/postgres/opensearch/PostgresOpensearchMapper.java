package no.nav.pto.veilarbportefolje.postgres.opensearch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.opensearch.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.postgres.opensearch.utils.AktivitetEntity;
import no.nav.pto.veilarbportefolje.postgres.opensearch.utils.PostgresAktivitetMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostgresOpensearchMapper {
    private final AktoerDataOpensearchMapper aktoerDataOpensearchMapper;
    private final AktivitetOpensearchService aktivitetOpensearchService;

    public List<OppfolgingsBruker> flettInnPostgresData(List<OppfolgingsBruker> brukere, boolean mapAktiviteter, boolean medDiffLogging) {
        List<AktorId> aktoerIder = brukere.stream().map(OppfolgingsBruker::getAktoer_id).map(AktorId::of).toList();

        HashMap<AktorId, PostgresAktorIdEntity> aktorIdData = aktoerDataOpensearchMapper.hentAktoerData(aktoerIder);
        Map<AktorId, List<AktivitetEntity>> aktiveAktiviter = aktivitetOpensearchService.hentAktivitetData(aktoerIder);

        brukere.forEach(bruker -> {
                    Optional.ofNullable(aktorIdData.get(AktorId.of(bruker.getAktoer_id())))
                            .ifPresentOrElse(
                                    postgresAktorIdData -> flettInnAktoerData(postgresAktorIdData, bruker, medDiffLogging),
                                    () -> log.warn("Fant ikke aktoer i aktoer basert postgres: {}", bruker.getAktoer_id())
                            );
                    if (mapAktiviteter) {
                        Optional.ofNullable(aktiveAktiviter.get(AktorId.of(bruker.getAktoer_id())))
                                .ifPresentOrElse(
                                        aktivitetsListe -> {
                                            PostgresAktivitetEntity aktivitetData = PostgresAktivitetMapper.build(aktivitetsListe);
                                            flettInnAktivitetData(aktivitetData, bruker, medDiffLogging);
                                        },
                                        () -> flettInnAktivitetData(new PostgresAktivitetEntity(), bruker, medDiffLogging)
                                );
                    }
                }
        );

        return brukere;
    }

    private void flettInnAktivitetData(PostgresAktivitetEntity aktivitetData, OppfolgingsBruker bruker, boolean medDiffLogging) {
        if (medDiffLogging) {
            loggDiff(aktivitetData, bruker);
        }
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
    }

    private void loggDiff(PostgresAktivitetEntity postgresEntity, OppfolgingsBruker bruker) {
        if (isDifferent(bruker.getAktiviteter(), postgresEntity.getAktiviteter())) {
            log.warn("postgres Opensearch: getAktiviteter feil, på bruker: {}", bruker.getAktoer_id());
        }
        if (isDifferent(bruker.getTiltak(), postgresEntity.getTiltak())) {
            log.warn("postgres Opensearch: getTiltak feil, på bruker: {}", bruker.getAktoer_id());
        }
        if (isDifferentDate(bruker.getNyesteutlopteaktivitet(), postgresEntity.getNyesteUtlopteAktivitet())) {
            log.warn("postgres Opensearch: NyesteUtlopteAktivitet feil, på bruker: {}", bruker.getAktoer_id());
        }
        if (isDifferentDate(bruker.getAktivitet_start(), postgresEntity.getAktivitetStart())) {
            log.warn("postgres Opensearch: feil aktivitet_start, på bruker: {}", bruker.getAktoer_id());
        }
        if (isDifferentDate(bruker.getNeste_aktivitet_start(), postgresEntity.getNesteAktivitetStart())) {
            log.warn("postgres Opensearch: feil neste_aktivitet_start, på bruker: {}", bruker.getAktoer_id());
        }
        if (isDifferentDate(bruker.getForrige_aktivitet_start(), postgresEntity.getForrigeAktivitetStart())) {
            log.warn("postgres Opensearch: feil forrige_aktivitet_start, på bruker: {}", bruker.getAktoer_id());
        }
        if (isDifferentDate(bruker.getAktivitet_mote_utlopsdato(), postgresEntity.getAktivitetMoteUtlopsdato())) {
            log.warn("postgres Opensearch: feil mote_utlopsdato, på bruker: {}", bruker.getAktoer_id());
        }
        if (isDifferentDate(bruker.getAktivitet_mote_startdato(), postgresEntity.getAktivitetMoteStartdato())) {
            log.warn("postgres Opensearch: feil mote_startdato, på bruker: {}", bruker.getAktoer_id());
        }
        if (isDifferentDate(bruker.getAktivitet_stilling_utlopsdato(), postgresEntity.getAktivitetStillingUtlopsdato())) {
            log.warn("postgres Opensearch: feil stilling_utlopsdato, på bruker: {}", bruker.getAktoer_id());
        }
        if (isDifferentDate(bruker.getAktivitet_egen_utlopsdato(), postgresEntity.getAktivitetEgenUtlopsdato())) {
            log.warn("postgres Opensearch: feil egen_utlopsdato, på bruker: {}", bruker.getAktoer_id());
        }
        if (isDifferentDate(bruker.getAktivitet_behandling_utlopsdato(), postgresEntity.getAktivitetBehandlingUtlopsdato())) {
            log.warn("postgres Opensearch: feil behandling_utlopsdato, på bruker: {}", bruker.getAktoer_id());
        }
        if (isDifferentDate(bruker.getAktivitet_ijobb_utlopsdato(), postgresEntity.getAktivitetIjobbUtlopsdato())) {
            log.warn("postgres Opensearch: feil ijobb_utlopsdato, på bruker: {}", bruker.getAktoer_id());
        }
        if (isDifferentDate(bruker.getAktivitet_sokeavtale_utlopsdato(), postgresEntity.getAktivitetSokeavtaleUtlopsdato())) {
            log.warn("postgres Opensearch: feil sokeavtale_utlopsdato, på bruker: {}", bruker.getAktoer_id());
        }
        if (isDifferentDate(bruker.getAktivitet_tiltak_utlopsdato(), postgresEntity.getAktivitetTiltakUtlopsdato())) {
            log.warn("postgres Opensearch: feil tiltak_utlopsdato, på bruker: {}", bruker.getAktoer_id());
        }
        if (isDifferentDate(bruker.getAktivitet_utdanningaktivitet_utlopsdato(), postgresEntity.getAktivitetUtdanningaktivitetUtlopsdato())) {
            log.warn("postgres Opensearch: feil utdanningaktivitet_utlopsdato, på bruker: {}", bruker.getAktoer_id());
        }
        if (isDifferentDate(bruker.getAktivitet_gruppeaktivitet_utlopsdato(), postgresEntity.getAktivitetGruppeaktivitetUtlopsdato())) {
            log.warn("postgres Opensearch: feil gruppeaktivitet_utlopsdato, på bruker: {}", bruker.getAktoer_id());
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
            log.warn("postgres Opensearch: isOppfolging feil bruker: {}", bruker.getAktoer_id());
        }
        if (bruker.isNy_for_veileder() != postgresAktorIdEntity.getNyForVeileder()) {
            log.warn("postgres Opensearch: isNy_for_veileder feil bruker: {}", bruker.getAktoer_id());
        }
        if ((bruker.getManuell_bruker() != null && bruker.getManuell_bruker().equals("MANUELL")) != postgresAktorIdEntity.getManuellBruker()) {
            log.warn("postgres Opensearch: getManuell_bruker feil bruker: {}", bruker.getAktoer_id());
        }
        if (isDifferent(bruker.getOppfolging_startdato(), postgresAktorIdEntity.getOppfolgingStartdato())) {
            log.warn("postgres Opensearch: getOppfolging_startdato feil bruker: {}", bruker.getAktoer_id());
        }
        if (isDifferent(bruker.getVenterpasvarfrabruker(), postgresAktorIdEntity.getVenterpasvarfrabruker())) {
            log.info("postgres Opensearch: Venterpasvarfrabruker feil bruker: {}", bruker.getAktoer_id());
        }
        if (isDifferent(bruker.getVenterpasvarfranav(), postgresAktorIdEntity.getVenterpasvarfranav())) {
            log.info("postgres Opensearch: getVenterpasvarfranav feil bruker: {}", bruker.getAktoer_id());
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
