package no.nav.pto.veilarbportefolje.oppfolgingsbruker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.aktorregister.IngenGjeldendeIdentException;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2;
import no.nav.pto.veilarbportefolje.vedtakstotte.KafkaVedtakStatusEndring;
import no.nav.pto.veilarbportefolje.vedtakstotte.VedtakStatusRepositoryV2;
import no.nav.pto_schema.enums.arena.Hovedmaal;
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe;
import no.nav.pto_schema.enums.arena.Rettighetsgruppe;
import no.nav.pto_schema.enums.arena.SikkerhetstiltakType;
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV2;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import static no.nav.common.utils.EnvironmentUtils.isDevelopment;

@Service
@Slf4j
@RequiredArgsConstructor
public class OppfolginsbrukerService extends KafkaCommonConsumerService<EndringPaaOppfoelgingsBrukerV2> {
    private final OppfolginsbrukerRepositoryV2 OppfolginsbrukerRepositoryV2;
    private final VedtakStatusRepositoryV2 vedtakStatusRepositoryV2;
    private final OpensearchIndexerV2 opensearchIndexerV2;
    private final AktorClient aktorClient;

    @Override
    public void behandleKafkaMeldingLogikk(EndringPaaOppfoelgingsBrukerV2 kafkaMelding) {
        AktorId aktorId = hentAktorIdMedUnntakIDev(Fnr.of(kafkaMelding.getFodselsnummer()));

        ZonedDateTime iservDato = Optional.ofNullable(kafkaMelding.getIservFraDato())
                .map(dato -> ZonedDateTime.of(dato.atStartOfDay(), ZoneId.systemDefault()))
                .orElse(null);
        ZonedDateTime dodFraDato = Optional.ofNullable(kafkaMelding.getDoedFraDato())
                .map(dato -> ZonedDateTime.of(dato.atStartOfDay(), ZoneId.systemDefault()))
                .orElse(null);

        if (aktorId == null) {
            throw new IllegalStateException("Fnr -> AktoerId var null på topic endringPaaOppfoelgingsBruker");
        }
        log.info("Fikk endring pa oppfolgingsbruker: {}, topic: aapen-fo-endringPaaOppfoelgingsBruker-v2", aktorId);
        OppfolgingsbrukerEntity oppfolgingsbruker =  new OppfolgingsbrukerEntity(
                aktorId.get(), kafkaMelding.getFodselsnummer(), kafkaMelding.getFormidlingsgruppe().name(),
                iservDato, kafkaMelding.getEtternavn(), kafkaMelding.getFornavn(),
                kafkaMelding.getOppfolgingsenhet(), Optional.ofNullable(kafkaMelding.getKvalifiseringsgruppe()).map(Kvalifiseringsgruppe::name).orElse(null),
                Optional.ofNullable(kafkaMelding.getRettighetsgruppe()).map(Rettighetsgruppe::name).orElse(null),
                Optional.ofNullable(kafkaMelding.getHovedmaal()).map(Hovedmaal::name).orElse(null),
                Optional.ofNullable(kafkaMelding.getSikkerhetstiltakType()).map(SikkerhetstiltakType::name).orElse(null), kafkaMelding.getDiskresjonskode(),
                Optional.ofNullable(kafkaMelding.getHarOppfolgingssak()).orElse(false),
                Optional.ofNullable(kafkaMelding.getSperretAnsatt()).orElse(false), Optional.ofNullable(kafkaMelding.getErDoed()).orElse(false),
                dodFraDato, kafkaMelding.getSistEndretDato());

        OppfolginsbrukerRepositoryV2.leggTilEllerEndreOppfolgingsbruker(oppfolgingsbruker);

        String vedtakStatus = vedtakStatusRepositoryV2.hentVedtak(aktorId.get())
                .map(KafkaVedtakStatusEndring::getVedtakStatusEndring)
                .map(KafkaVedtakStatusEndring.VedtakStatusEndring::toString)
                .orElse(null);
        opensearchIndexerV2.updateOppfolgingsbruker(aktorId, oppfolgingsbruker, vedtakStatus);
    }

    private AktorId hentAktorIdMedUnntakIDev(Fnr fodselsnummer) {
        try {
            return aktorClient.hentAktorId(fodselsnummer);
        } catch (IngenGjeldendeIdentException exception) {
            if (isDevelopment().orElse(false)) {
                return AktorId.of("-1");
            }
            throw exception;
        }
    }
}



