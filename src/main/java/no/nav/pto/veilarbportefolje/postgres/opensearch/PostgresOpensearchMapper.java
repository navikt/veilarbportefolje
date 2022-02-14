package no.nav.pto.veilarbportefolje.postgres.opensearch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.opensearch.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.postgres.opensearch.utils.AktivitetEntity;
import no.nav.pto.veilarbportefolje.postgres.opensearch.utils.PostgresAktivitetBuilder;
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
    private final AktivitetOpensearchMapper aktivitetOpensearchMapper;

    public List<OppfolgingsBruker> mapBulk(List<OppfolgingsBruker> brukere, boolean mapAktiviteter, boolean medDiffLogging) {
        List<AktorId> aktoerIder = brukere.stream().map(OppfolgingsBruker::getAktoer_id).map(AktorId::of).toList();

        HashMap<AktorId, PostgresAktorIdEntity> aktorIdEntityMap = aktoerDataOpensearchMapper.mapBulk(aktoerIder);
        Map<AktorId, List<AktivitetEntity>> aktivitetSamlingMap = aktivitetOpensearchMapper.mapBulk(aktoerIder);

        brukere.forEach(bruker -> {
                    Optional.ofNullable(aktorIdEntityMap.get(AktorId.of(bruker.getAktoer_id())))
                            .ifPresentOrElse(
                                    postgresAktorIdData -> flettInnAktoerData(postgresAktorIdData, bruker, medDiffLogging),
                                    () -> log.warn("Fant ikke aktoer i aktoer basert postgres: {}", bruker.getAktoer_id()
                                    )
                            );
                    if (mapAktiviteter) {
                        Optional.ofNullable(aktivitetSamlingMap.get(AktorId.of(bruker.getAktoer_id())))
                                .ifPresentOrElse(
                                        aktivitetsListe -> flettInnAktivitetData(aktivitetsListe, bruker, medDiffLogging),
                                        () -> log.warn("Fant ikke aktoer i aktivitets basert postgres (ingen aktivitetete på brukeren?): {}", bruker.getAktoer_id()
                                        )
                                );
                    }
                }
        );

        return brukere;
    }

    private void flettInnAktivitetData(List<AktivitetEntity> aktivitetSamling, OppfolgingsBruker bruker, boolean medDiffLogging) {
        PostgresAktivitetEntity aktivitetEntity = PostgresAktivitetBuilder.build(aktivitetSamling);
        if (medDiffLogging) {
            loggDiff(aktivitetEntity, bruker);
        }
        bruker.setNyesteutlopteaktivitet(aktivitetEntity.getNyesteUtlopteAktivitet());
        bruker.setAktivitet_start(aktivitetEntity.getAktivitetStart());
        bruker.setNeste_aktivitet_start(aktivitetEntity.getNesteAktivitetStart());
        bruker.setForrige_aktivitet_start(aktivitetEntity.getForrigeAktivitetStart());
        bruker.setAktivitet_mote_utlopsdato(aktivitetEntity.getAktivitetMoteUtlopsdato());
        bruker.setAktivitet_mote_startdato(aktivitetEntity.getAktivitetMoteStartdato());
        bruker.setAktivitet_stilling_utlopsdato(aktivitetEntity.getAktivitetStillingUtlopsdato());
        bruker.setAktivitet_egen_utlopsdato(aktivitetEntity.getAktivitetEgenUtlopsdato());
        bruker.setAktivitet_behandling_utlopsdato(aktivitetEntity.getAktivitetBehandlingUtlopsdato());
        bruker.setAktivitet_ijobb_utlopsdato(aktivitetEntity.getAktivitetIjobbUtlopsdato());
        bruker.setAktivitet_sokeavtale_utlopsdato(aktivitetEntity.getAktivitetSokeavtaleUtlopsdato());
        bruker.setAktivitet_tiltak_utlopsdato(aktivitetEntity.getAktivitetTiltakUtlopsdato());
        bruker.setAktivitet_utdanningaktivitet_utlopsdato(aktivitetEntity.getAktivitetUtdanningaktivitetUtlopsdato());
        bruker.setAktivitet_gruppeaktivitet_utlopsdato(aktivitetEntity.getAktivitetGruppeaktivitetUtlopsdato());

        bruker.setAktiviteter(aktivitetEntity.getAktiviteter());
        bruker.setTiltak(aktivitetEntity.getTiltak());
    }

    private void flettInnAktoerData(PostgresAktorIdEntity postgresAktorIdEntity, OppfolgingsBruker bruker, boolean medDiffLogging) {
        if (medDiffLogging) {
            loggDiff(postgresAktorIdEntity, bruker);
        }
        bruker.setBrukers_situasjon(postgresAktorIdEntity.getBrukersSituasjon());
        bruker.setProfilering_resultat(postgresAktorIdEntity.getProfileringResultat());
        bruker.setUtdanning(postgresAktorIdEntity.getUtdanning());
        bruker.setUtdanning_bestatt(postgresAktorIdEntity.getUtdanningBestatt());
        bruker.setUtdanning_godkjent(postgresAktorIdEntity.getUtdanningGodkjent());
    }

    private void loggDiff(PostgresAktivitetEntity postgresEntity, OppfolgingsBruker bruker) {
        if (isDifferent(bruker.getNyesteutlopteaktivitet(), postgresEntity.getNyesteUtlopteAktivitet())) {
            log.warn("postgres Opensearch: Situsjon feil bruker: {}", bruker.getAktoer_id());
        }
        if (isDifferent(bruker.getAktivitet_start(), postgresEntity.getAktivitetStart())) {
            log.warn("postgres Opensearch: feil aktivitet_start");
        }
        if (isDifferent(bruker.getNeste_aktivitet_start(), postgresEntity.getNesteAktivitetStart())) {
            log.warn("postgres Opensearch: feil neste_aktivitet_start");
        }
        if (isDifferent(bruker.getForrige_aktivitet_start(), postgresEntity.getForrigeAktivitetStart())) {
            log.warn("postgres Opensearch: feil forrige_aktivitet_start");
        }
        if (isDifferent(bruker.getAktivitet_mote_utlopsdato(), postgresEntity.getAktivitetMoteUtlopsdato())) {
            log.warn("postgres Opensearch: feil mote_utlopsdato");
        }
        if (isDifferent(bruker.getAktivitet_mote_startdato(), postgresEntity.getAktivitetMoteStartdato())) {
            log.warn("postgres Opensearch: feil mote_startdato");
        }
        if (isDifferent(bruker.getAktivitet_stilling_utlopsdato(), postgresEntity.getAktivitetStillingUtlopsdato())) {
            log.warn("postgres Opensearch: feil stilling_utlopsdato");
        }
        if (isDifferent(bruker.getAktivitet_egen_utlopsdato(), postgresEntity.getAktivitetEgenUtlopsdato())) {
            log.warn("postgres Opensearch: feil egen_utlopsdato");
        }
        if (isDifferent(bruker.getAktivitet_behandling_utlopsdato(), postgresEntity.getAktivitetBehandlingUtlopsdato())) {
            log.warn("postgres Opensearch: feil behandling_utlopsdato");
        }
        if (isDifferent(bruker.getAktivitet_ijobb_utlopsdato(), postgresEntity.getAktivitetIjobbUtlopsdato())) {
            log.warn("postgres Opensearch: feil ijobb_utlopsdato");
        }
        if (isDifferent(bruker.getAktivitet_sokeavtale_utlopsdato(), postgresEntity.getAktivitetSokeavtaleUtlopsdato())) {
            log.warn("postgres Opensearch: feil sokeavtale_utlopsdato");
        }
        if (isDifferent(bruker.getAktivitet_tiltak_utlopsdato(), postgresEntity.getAktivitetTiltakUtlopsdato())) {
            log.warn("postgres Opensearch: feil tiltak_utlopsdato");
        }
        if (isDifferent(bruker.getAktivitet_utdanningaktivitet_utlopsdato(), postgresEntity.getAktivitetUtdanningaktivitetUtlopsdato())) {
            log.warn("postgres Opensearch: feil utdanningaktivitet_utlopsdato");
        }
        if (isDifferent(bruker.getAktivitet_gruppeaktivitet_utlopsdato(), postgresEntity.getAktivitetGruppeaktivitetUtlopsdato())) {
            log.warn("postgres Opensearch: feil gruppeaktivitet_utlopsdato");
        }
    }

    private void loggDiff(PostgresAktorIdEntity postgresAktorIdEntity, OppfolgingsBruker bruker) {
        if (isDifferent(bruker.getBrukers_situasjon(), postgresAktorIdEntity.getBrukersSituasjon())) {
            log.warn("postgres Opensearch: Situsjon feil bruker: {}", bruker.getAktoer_id());
        }
        if (isDifferent(bruker.getProfilering_resultat(), postgresAktorIdEntity.getProfileringResultat())) {
            log.warn("postgres Opensearch: Profilering feil bruker: {}", bruker.getAktoer_id());
        }
        if (isDifferent(bruker.getUtdanning(), postgresAktorIdEntity.getUtdanning())) {
            log.warn("postgres Opensearch: Utdanning feil bruker: {}", bruker.getAktoer_id());
        }
        if (isDifferent(bruker.getUtdanning_bestatt(), postgresAktorIdEntity.getUtdanningBestatt())) {
            log.warn("postgres Opensearch: Utdanning bestått feil bruker: {}", bruker.getAktoer_id());
        }
        if (isDifferent(bruker.getUtdanning_godkjent(), postgresAktorIdEntity.getUtdanningGodkjent())) {
            log.warn("postgres Opensearch: Utdanning godskjent feil bruker: {}", bruker.getAktoer_id());
        }
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

}
