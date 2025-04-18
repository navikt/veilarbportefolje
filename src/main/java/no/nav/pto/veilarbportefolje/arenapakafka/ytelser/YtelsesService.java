package no.nav.pto.veilarbportefolje.arenapakafka.ytelser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.ArenaHendelseRepository;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.YtelsesDTO;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.YtelsesInnhold;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.domene.Brukerdata;
import no.nav.pto.veilarbportefolje.domene.YtelseMapping;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static no.nav.common.utils.EnvironmentUtils.isDevelopment;
import static no.nav.pto.veilarbportefolje.arenapakafka.ArenaUtils.*;
import static no.nav.pto.veilarbportefolje.arenapakafka.ytelser.TypeKafkaYtelse.AAP;
import static no.nav.pto.veilarbportefolje.arenapakafka.ytelser.TypeKafkaYtelse.DAGPENGER;
import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@Slf4j
@Service
@RequiredArgsConstructor
public class YtelsesService {
    private final AktorClient aktorClient;
    private final YtelsesRepositoryV2 ytelsesRepositoryV2;
    private final ArenaHendelseRepository arenaHendelseRepository;
    private final YtelsesStatusRepositoryV2 ytelsesStatusRepositoryV2;
    private final OpensearchIndexer opensearchIndexer;

    public void behandleKafkaRecord(ConsumerRecord<String, YtelsesDTO> kafkaMelding, TypeKafkaYtelse ytelse) {
        YtelsesDTO melding = kafkaMelding.value();
        secureLog.info(
                "Behandler kafka-melding med key: {} og offset: {}, og partition: {} på topic {}",
                kafkaMelding.key(),
                kafkaMelding.offset(),
                kafkaMelding.partition(),
                kafkaMelding.topic()
        );
        behandleKafkaMelding(melding, ytelse);
    }

    public void behandleKafkaMelding(YtelsesDTO kafkaMelding, TypeKafkaYtelse ytelse) {
        YtelsesInnhold innhold = getInnhold(kafkaMelding);
        if (innhold == null || erGammelMelding(kafkaMelding, innhold)) {
            return;
        }

        AktorId aktorId = getAktorId(aktorClient, innhold.getFnr());

        if(isDevelopment().orElse(false) && Objects.equals(aktorId.get(), "-1")) {
            // TODO: Dette er en midlertidig fiks siden vi har et tilfelle der vi fyller YTELSESVEDTAK DB-tabellen med
            // masse "-1" verdier i AKTORID-kolonna. Grunnen til at dette skjer er fordi `getAktorId(...)`-metoden
            // returnerer "-1" dersom den får inn en verdi som ikke kvalifiserer som et gyldig fødselsnummer OG
            // vi er i dev-miljøet (i prod kastes exception).
            // Inntil videre ignorerer vi disse meldingene i dette scenariet til vi vet hva vi ønsker å gjøre med
            // disse fnr-ene som ikke er gyldige fødselsnummer.
            // 2024-11-07, Sondre

            secureLog.warn("(Hendelse i dev-miljø) Fikk verdi \"-1\" når vi forsøkte å hente AktørID for fnr {}. Ignorerer melding.", innhold.getFnr());
            return;
        }

        if (skalSlettesGoldenGate(kafkaMelding)) {
            secureLog.info("Postgres: Sletter ytelse: {}, pa aktorId: {}", innhold.getVedtakId(), aktorId);
            ytelsesRepositoryV2.slettYtelse(innhold.getVedtakId());
            oppdaterYtelsesInformasjonMedUnntaksLogikkForSletting(aktorId, innhold);
        } else {
            secureLog.info("Postgres: Lagrer ytelse: {}, pa aktorId: {}", innhold.getVedtakId(), aktorId);
            ytelsesRepositoryV2.upsert(aktorId, ytelse, innhold);
            oppdaterYtelsesInformasjon(aktorId);
        }

        arenaHendelseRepository.upsertYtelsesHendelse(innhold.getVedtakId(), innhold.getHendelseId());
        opensearchIndexer.indekser(aktorId);
    }

    public Optional<YtelseDAO> oppdaterYtelsesInformasjon(AktorId aktorId) {
        Optional<YtelseDAO> lopendeYtelse = finnLopendeYtelse(aktorId);
        secureLog.info("Postgres: AktoerId: {} har en løpende ytelse med saksId: {}", aktorId, lopendeYtelse.map(YtelseDAO::getSaksId).orElse("ingen løpende vedtak"));
        oppdaterAktivYtelse(aktorId, lopendeYtelse.orElse(null));

        return lopendeYtelse;
    }

    /**
     * NB: I tilfeller der arena sletter et løpende vedtak.
     * Det må da sjekkes om det finnes andre vedtak i samme sak, hvis dette er tilfellet så skal ytelsen på brukeren fortsette.
     * Dette gjelder uavhengig av startdatoen på vedtaket som tar over som løpende.
     * Det neste løpende vedtaket kan med andre ord ha en startdato satt i fremtiden.
     */
    public Optional<YtelseDAO> oppdaterYtelsesInformasjonMedUnntaksLogikkForSletting(AktorId aktorId, YtelsesInnhold innhold) {
        LocalDate iDag = LocalDate.now();

        Timestamp startDato = Timestamp.valueOf(innhold.getFraOgMedDato().getLocalDateTime());
        Timestamp utlopsDato = innhold.getTilOgMedDato() == null ? null : Timestamp.valueOf(innhold.getTilOgMedDato().getLocalDateTime());

        boolean erLopendeVedtak = harPassertStartdato(startDato, iDag) && harFremtidigUtlopsdato(utlopsDato, iDag);

        if (erLopendeVedtak) {
            Optional<YtelseDAO> sisteYtelsePaSakId = finnSisteYtelsePaSakIdSomIkkeErUtlopt(aktorId, innhold.getSaksId());
            if (sisteYtelsePaSakId.isPresent()) {
                secureLog.info("AktoerId: {} har en løpende ytelse med saksId: {}", aktorId, sisteYtelsePaSakId.map(YtelseDAO::getSaksId).orElse("ingen løpende vedtak"));
                oppdaterAktivYtelse(aktorId, sisteYtelsePaSakId.get());

                return sisteYtelsePaSakId;
            }
        }

        return oppdaterYtelsesInformasjon(aktorId);
    }

    public Optional<YtelseDAO> finnLopendeYtelse(AktorId aktorId) {
        LocalDate iDag = LocalDate.now();
        List<YtelseDAO> aktiveOgFremtidigeYtelser = ytelsesRepositoryV2.getYtelser(aktorId).stream()
                .filter(Objects::nonNull)
                .filter(ytelse -> harFremtidigUtlopsdato(ytelse.getUtlopsDato(), iDag))
                .collect(Collectors.toList());

        if (aktiveOgFremtidigeYtelser.isEmpty()) {
            return Optional.empty();
        }

        YtelseDAO tidligsteYtelse = aktiveOgFremtidigeYtelser.stream()
                .min(Comparator.comparing(YtelseDAO::getStartDato)).get();

        if (!harPassertStartdato(tidligsteYtelse.getStartDato(), iDag)) {
            return Optional.empty();
        }
        if (DAGPENGER.equals(tidligsteYtelse.getType())) {
            // Dagpenger skal aldri ha en utløpsdato
            // Hvis det finnes en utløpsdato er det mest sannynlig et annet dagpengevedtak som skal ta over for det løpende vedtaket, eller en bug
            return Optional.of(tidligsteYtelse.setUtlopsDato(null));
        }
        if (tidligsteYtelse.getUtlopsDato() == null) {
            return Optional.of(tidligsteYtelse);
        }
        return finnVedtakMedSisteUtlopsDatoPaSak(aktiveOgFremtidigeYtelser, tidligsteYtelse);
    }

    public Optional<YtelseDAO> finnSisteYtelsePaSakIdSomIkkeErUtlopt(AktorId aktorId, String sakID) {
        LocalDate iDag = LocalDate.now();
        List<YtelseDAO> aktiveYtelserPaSakID = ytelsesRepositoryV2.getYtelser(aktorId).stream()
                .filter(Objects::nonNull)
                .filter(ytelse -> sakID.equals(ytelse.getSaksId()))
                .filter(ytelse -> harFremtidigUtlopsdato(ytelse.getUtlopsDato(), iDag))
                .collect(Collectors.toList());

        if (aktiveYtelserPaSakID.isEmpty()) {
            return Optional.empty();
        }
        Optional<YtelseDAO> ytelseMedSluttDatoEllerNull = aktiveYtelserPaSakID.stream()
                .filter(ytelseDAO -> ytelseDAO.getUtlopsDato() != null)
                .max(Comparator.comparing(YtelseDAO::getUtlopsDato));

        return Optional.of(ytelseMedSluttDatoEllerNull.orElse(aktiveYtelserPaSakID.get(0)));
    }

    public void oppdaterBrukereMedYtelserSomStarterIDag() {
        List<AktorId> brukere = ytelsesRepositoryV2.hentBrukereMedYtelserSomStarterIDag();
        secureLog.info("(Postgres) Oppdaterer ytelser for: " + brukere.size() + " antall brukere");

        brukere.forEach(aktorId -> {
            secureLog.info("(Postgres) Oppdaterer ytelse for aktorId: " + aktorId);

            oppdaterYtelsesInformasjon(aktorId);
        });

        secureLog.info("(Postgres) Oppdatering av ytelser fullført");
    }

    private Optional<YtelseDAO> finnVedtakMedSisteUtlopsDatoPaSak(List<YtelseDAO> ytelser, YtelseDAO tidligsteYtelse) {
        return ytelser.stream()
                .filter(ytelsesDAO -> tidligsteYtelse.getSaksId().equals(ytelsesDAO.getSaksId()))
                .filter(ytelsesDAO -> ytelsesDAO.getUtlopsDato() != null)
                .max(Comparator.comparing(YtelseDAO::getUtlopsDato));
    }

    private boolean harPassertStartdato(Timestamp startDato, LocalDate iDag) {
        // startDato er en "fra og med"-dato.
        return startDato.toLocalDateTime().toLocalDate().isBefore(iDag.plusDays(1));
    }

    private boolean harFremtidigUtlopsdato(Timestamp utlopsDato, LocalDate iDag) {
        // Utløpsdato er en "til og med"-dato.
        return utlopsDato == null || utlopsDato.toLocalDateTime().toLocalDate().isAfter(iDag.minusDays(1));
    }

    private boolean erGammelMelding(YtelsesDTO kafkaMelding, YtelsesInnhold innhold) {
        Long hendelseIDB = arenaHendelseRepository.retrieveYtelsesHendelse(innhold.getVedtakId());

        if (erGammelHendelseBasertPaOperasjon(hendelseIDB, innhold.getHendelseId(), skalSlettesGoldenGate(kafkaMelding))) {
            secureLog.info("Fikk tilsendt gammel ytelsesmelding, vedtak: {}, personId: {}", innhold.getVedtakId(), innhold.getPersonId());
            return true;
        }
        return false;
    }

    private void oppdaterAktivYtelse(AktorId aktorId, YtelseDAO ytelse) {
        Brukerdata ytelsesTilstand = new Brukerdata()
                .setAktoerid(aktorId.get());
        if (ytelse == null) {
            ytelsesStatusRepositoryV2.slettYtelseStatus(aktorId);
            return;
        }
        ytelsesTilstand
                .setUtlopsdato(Optional.ofNullable(ytelse.getUtlopsDato())
                        .map(Timestamp::toLocalDateTime)
                        .orElse(null))
                .setEndretDato(ytelse.getEndretDato())
                .setYtelse(YtelseMapping.of(ytelse)
                        .orElseThrow(() -> new RuntimeException("Feil i ytelses mapping! Pa vedtak: " + ytelse.getSaksId())));

        if (ytelse.getType() == DAGPENGER) {
            ytelsesTilstand
                    .setDagputlopUke(ytelse.getAntallUkerIgjen())
                    .setPermutlopUke(ytelse.getAntallUkerIgjenPermittert());
        } else if (ytelse.getType() == AAP) {
            ytelsesTilstand
                    .setAapmaxtidUke(ytelse.getAntallUkerIgjen())
                    .setAapUnntakDagerIgjen(ytelse.getAntallDagerIgjenUnntak())
                    .setAapmaxtidDager(ytelse.getAntallDagerIgjen());
        }

        ytelsesStatusRepositoryV2.upsertYtelseStatus(ytelsesTilstand);
    }
}
