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
import no.nav.pto.veilarbportefolje.service.BrukerService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.sql.Timestamp;
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
    private final BrukerService brukerService;
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
            oppdaterYtelsesInformasjonMedUntaksLoggikForSletting(aktorId, innhold);
        } else {
            log.info("Lagrer ytelse: {}, pa aktorId: {}", innhold.getVedtakId(), aktorId);
            ytelsesRepository.upsertYtelse(aktorId, ytsele, innhold);
            oppdaterYtelsesInformasjon(aktorId, PersonId.of(innhold.getPersonId()));
        }

        arenaHendelseRepository.upsertYtelsesHendelse(innhold.getVedtakId(), innhold.getHendelseId());
        elasticIndexer.indekser(aktorId);
    }

    public Optional<YtelseDAO> oppdaterYtelsesInformasjon(AktorId aktorId, PersonId personId) {
        Optional<YtelseDAO> lopendeYtelse = finnLopendeYtelse(aktorId);
        log.info("AktoerId: {} har en løpende ytelse med saksId: {}", aktorId, lopendeYtelse.map(YtelseDAO::getSaksId).orElse("ingen løpende vedtak"));
        brukerDataService.oppdaterYtelser(aktorId, personId, lopendeYtelse);

        return lopendeYtelse;
    }

    // TODO: diskuter denne metoden. Kanskje den skal endre startdatoen på evt. vedtak som blir løpende?
    /**
     * NB: I tilfeller der arena sletter et løpende vedtak.
     * Det må da sjekkes om det finnes andre vedtak i samme sak, hvis dette er tilfellet så skal ytelsen på brukeren fortsette.
     * Dette gjelder uavhengig av start datoen på vedtaket som tar over som løpende.
     * Det neste løpende vedtaket kan med andre ord ha en start dato satt i fremtiden.
     */
    public Optional<YtelseDAO> oppdaterYtelsesInformasjonMedUntaksLoggikForSletting(AktorId aktorId, YtelsesInnhold innhold) {
        LocalDate iDag = LocalDate.now();

        Timestamp startDato = Timestamp.valueOf(innhold.getFraOgMedDato().getLocalDate());
        Timestamp utlopsDato = innhold.getTilOgMedDato() == null ? null : Timestamp.valueOf(innhold.getTilOgMedDato().getLocalDate());

        boolean erLopendeVedtak = harLøpendeStartDato(startDato, iDag) && harLøpendeUtløpsDato(utlopsDato, iDag);

        if (erLopendeVedtak) {
            Optional<YtelseDAO> sisteYtelsePaSakId = finnSisteYtelsePaSakIdSomIkkeErUtlopt(aktorId, innhold.getSaksId());
            if (sisteYtelsePaSakId.isPresent()) {
                log.info("AktoerId: {} har en løpende ytelse med saksId: {}", aktorId, sisteYtelsePaSakId.map(YtelseDAO::getSaksId).orElse("ingen løpende vedtak"));
                brukerDataService.oppdaterYtelser(aktorId, PersonId.of(innhold.getPersonId()), sisteYtelsePaSakId);

                return sisteYtelsePaSakId;
            }
        }
        return oppdaterYtelsesInformasjon(aktorId, PersonId.of(innhold.getPersonId()));
    }

    public Optional<YtelseDAO> finnLopendeYtelse(AktorId aktorId) {
        LocalDate iDag = LocalDate.now();
        List<YtelseDAO> aktiveYtelser = ytelsesRepository.getYtelser(aktorId).stream()
                .filter(Objects::nonNull)
                .filter(ytelse -> harLøpendeUtløpsDato(ytelse.getUtlopsDato(), iDag))
                .collect(Collectors.toList());

        if (aktiveYtelser.isEmpty()) {
            return Optional.empty();
        }

        YtelseDAO tidligsteYtelse = aktiveYtelser.stream()
                .min(Comparator.comparing(YtelseDAO::getStartDato)).get();

        if (!harLøpendeStartDato(tidligsteYtelse.getStartDato(), iDag)) {
            return Optional.empty();
        }
        if (TypeKafkaYtelse.DAGPENGER.equals(tidligsteYtelse.getType())) {
            // Dagpenger skal aldri ha en utløpsdato
            // Hvis det finnes en utløpsdato er det mest sannynlig et annet dagpenge vedtak som skal ta over for det løpende vedatekt, eller en bug
            return Optional.of(tidligsteYtelse.setUtlopsDato(null));
        }

        if (tidligsteYtelse.getUtlopsDato() == null) {
            return Optional.of(tidligsteYtelse);
        }
        return finnVedtakMedSisteUtlopsDatoPaSak(aktiveYtelser, tidligsteYtelse);
    }

    public Optional<YtelseDAO> finnSisteYtelsePaSakIdSomIkkeErUtlopt(AktorId aktorId, String sakID) {
        LocalDate iDag = LocalDate.now();
        List<YtelseDAO> aktiveYtelserPaSakID = ytelsesRepository.getYtelser(aktorId).stream()
                .filter(Objects::nonNull)
                .filter(ytelse -> sakID.equals(ytelse.getSaksId()))
                .filter(ytelse -> harLøpendeUtløpsDato(ytelse.getUtlopsDato(), iDag))
                .collect(Collectors.toList());

        if (aktiveYtelserPaSakID.isEmpty()) {
            return Optional.empty();
        }
        Optional<YtelseDAO> ytelseMedSluttDatoEllerNull = aktiveYtelserPaSakID.stream()
                .filter(ytelseDOA -> ytelseDOA.getUtlopsDato() != null)
                .max(Comparator.comparing(YtelseDAO::getUtlopsDato));

        return Optional.of(ytelseMedSluttDatoEllerNull
                .orElse(aktiveYtelserPaSakID.get(0)));
    }

    public void oppdaterBrukereMedYtelserSomStarterIDag() {
        List<AktorId> brukere = ytelsesRepository.hentBrukereMedYtelserSomStarterIDag();
        log.info("Oppdaterer ytelser for: " + brukere.size() + " antall brukere");

        brukere.forEach(aktorId -> {
            log.info("Oppdaterer ytelse for aktorId: " + aktorId);
            PersonId personId = brukerService.hentPersonidFraAktoerid(aktorId).toJavaOptional().orElse(null);
            if (personId == null) {
                log.warn("Avbryter ytelse oppdatering pga. manglende personId for aktorId: " + aktorId);
                return;
            }
            oppdaterYtelsesInformasjon(aktorId, personId);
            elasticIndexer.indekser(aktorId);
        });

        log.info("Oppdatering av ytelser fullført");
    }

    private Optional<YtelseDAO> finnVedtakMedSisteUtlopsDatoPaSak(List<YtelseDAO> ytelser, YtelseDAO tidligsteYtelse) {
        return ytelser.stream()
                .filter(ytelseDOA -> tidligsteYtelse.getSaksId().equals(ytelseDOA.getSaksId()))
                .filter(ytelseDOA -> ytelseDOA.getUtlopsDato() != null)
                .max(Comparator.comparing(YtelseDAO::getUtlopsDato));
    }

    private boolean harLøpendeStartDato(Timestamp startDato, LocalDate iDag) {
        // startDato er en 'fra og med' dato.
        return startDato.toLocalDateTime().toLocalDate().isBefore(iDag.plusDays(1));
    }

    private boolean harLøpendeUtløpsDato(Timestamp utlopsDato, LocalDate iDag) {
        // Utløpsdato == null betyr en "uendlig" ytelse.
        // Utløpsdato er en 'til og med' dato.
        return utlopsDato == null || utlopsDato.toLocalDateTime().toLocalDate().isAfter(iDag.minusDays(1));
    }

    private boolean erGammelMelding(YtelsesDTO kafkaMelding, YtelsesInnhold innhold) {
        Long hendelseIDB = arenaHendelseRepository.retrieveYtelsesHendelse(innhold.getVedtakId());

        if (erGammelHendelseBasertPaOperasjon(hendelseIDB, innhold.getHendelseId(), skalSlettesGoldenGate(kafkaMelding))) {
            log.info("Fikk tilsendt gammel ytelses-melding, vedtak: {}, personId: {}", innhold.getVedtakId(), innhold.getPersonId());
            return true;
        }
        return false;
    }
}
