package no.nav.pto.veilarbportefolje.arenapakafka.ytelser;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.ArenaHendelseRepository;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.*;
import no.nav.pto.veilarbportefolje.database.BrukerDataService;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.ArenaAktivitetUtils.*;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class YtelsesService {
    @NonNull
    @Qualifier("systemClient")
    private final AktorClient aktorClient;
    private final BrukerDataService brukerDataService;
    private final YtelsesRepository ytelsesRepository;
    private final ArenaHendelseRepository arenaHendelseRepository;
    private final ElasticIndexer elasticIndexer;

    public void behandleKafkaRecord(ConsumerRecord<String, YtelsesDTO> kafkaMelding, TypeKafkaYtelse ytsele) {
        YtelsesDTO melding = kafkaMelding.value();
        log.info(
                "Behandler kafka-melding med key: {} og offset: {}, og partition: {} på topic {}",
                kafkaMelding.key(),
                kafkaMelding.offset(),
                kafkaMelding.partition(),
                kafkaMelding.topic()
        );

        behandleKafkaMelding(melding, ytsele);
    }

    public void behandleKafkaMelding(YtelsesDTO kafkaMelding, TypeKafkaYtelse ytsele) {
        YtelsesInnhold innhold = getInnhold(kafkaMelding);
        if (innhold == null || erGammelMelding(kafkaMelding, innhold)) {
            return;
        }

        AktorId aktorId = getAktorId(aktorClient, innhold.getFnr());
        if (skalSlettesGoldenGate(kafkaMelding)) {
            log.info("Sletter ytelse: {}, pa aktorId: {}", innhold.getVedtakId(), aktorId);
            ytelsesRepository.slettYtelse(innhold.getVedtakId());
        } else {
            log.info("Lagrer ytelse: {}, pa aktorId: {}", innhold.getVedtakId(), aktorId);
            ytelsesRepository.upsertYtelse(aktorId, ytsele, innhold);
        }
        Optional<YtelseDAO> lopendeYtelse = finnLopendeYtelse(aktorId);
        log.info("AktoerId: {} har en løpende ytelse med saksId: {}", aktorId, lopendeYtelse.map(YtelseDAO::getSaksId).orElse("ingen løpende vedtak"));
        brukerDataService.oppdaterYtelser(aktorId, PersonId.of(innhold.getPersonId()), lopendeYtelse);
        arenaHendelseRepository.upsertYtelsesHendelse(innhold.getVedtakId(), innhold.getHendelseId());

        elasticIndexer.indekser(aktorId);
    }

    public Optional<YtelseDAO> finnLopendeYtelse(AktorId aktorId) {
        LocalDate iDag = LocalDate.now();
        List<YtelseDAO> aktiveYtelser = ytelsesRepository.getYtelser(aktorId).stream()
                .filter(Objects::nonNull)
                .filter(ytelse -> harUtlopsDatoIFremtiden(ytelse, iDag))
                .collect(Collectors.toList());

        if (aktiveYtelser.isEmpty()) {
            return Optional.empty();
        }

        YtelseDAO tidligsteYtelse = aktiveYtelser.stream()
                .min(Comparator.comparing(YtelseDAO::getStartDato))
                .orElse(null);

        if (erTomEllerHarStartDatoIFremtiden(tidligsteYtelse, iDag)) {
            return Optional.empty();
        }
        if (TypeKafkaYtelse.DAGPENGER.equals(tidligsteYtelse.getType())) {
            // Dagpenger skal aldri ha en utløpsdato
            // Hvis det finnes en utløpsdato er det mest sannynlig et annet dagpenge vedtak som skal ta over for det løpende vedatekt, eller en bug
            return Optional.of(tidligsteYtelse.setUtlopsDato(null));
        }

        if(tidligsteYtelse.getUtlopsDato() == null){
            return Optional.of(tidligsteYtelse);
        }
        return finnVedtakMedSisteUtlopsDatoPaSak(aktiveYtelser, tidligsteYtelse);
    }

    private Optional<YtelseDAO> finnVedtakMedSisteUtlopsDatoPaSak(List<YtelseDAO> ytelser, YtelseDAO tidligsteYtelse) {
        return ytelser.stream()
                .filter(ytelseDOA -> ytelseDOA.getSaksId() != null && ytelseDOA.getSaksId().equals(tidligsteYtelse.getSaksId()))
                .filter(ytelseDOA -> ytelseDOA.getUtlopsDato() != null)
                .max(Comparator.comparing(YtelseDAO::getUtlopsDato));
    }

    private boolean erTomEllerHarStartDatoIFremtiden(YtelseDAO ytelse, LocalDate iDag) {
        // startDato er en 'fra og med' dato.
        return ytelse == null || ytelse.getStartDato().toLocalDateTime().toLocalDate().isAfter(iDag);
    }

    private boolean harUtlopsDatoIFremtiden(YtelseDAO ytelse, LocalDate iDag) {
        // Utløpsdato == null betyr en "uendlig" ytelse.
        // Utløpsdato er en 'til og med' dato.
        return ytelse.getUtlopsDato() == null
                || ytelse.getUtlopsDato().toLocalDateTime().toLocalDate().isAfter(iDag.minusDays(1));
    }

    private boolean erGammelMelding(YtelsesDTO kafkaMelding, YtelsesInnhold innhold) {
        Long hendelseIDB = arenaHendelseRepository.retrieveYtelsesHendelse(innhold.getVedtakId());

        if (erGammelHendelseBasertPaOperasjon(hendelseIDB, innhold.getHendelseId(), kafkaMelding.getOperationType())) {
            log.info("Fikk tilsendt gammel ytelses-melding, vedtak: {}, personId: {}", innhold.getVedtakId(), innhold.getPersonId());
            return true;
        }
        return false;
    }
}
