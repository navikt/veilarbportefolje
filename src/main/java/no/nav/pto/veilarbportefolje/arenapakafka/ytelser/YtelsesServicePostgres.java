package no.nav.pto.veilarbportefolje.arenapakafka.ytelser;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.YtelsesDTO;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.YtelsesInnhold;
import no.nav.pto.veilarbportefolje.database.BrukerDataService;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
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

import static no.nav.pto.veilarbportefolje.arenapakafka.ArenaUtils.getAktorId;
import static no.nav.pto.veilarbportefolje.arenapakafka.ArenaUtils.getInnhold;
import static no.nav.pto.veilarbportefolje.arenapakafka.ArenaUtils.skalSlettesGoldenGate;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class YtelsesServicePostgres {
    @NonNull
    @Qualifier("systemClient")
    private final AktorClient aktorClient;
    private final BrukerDataService brukerDataService;
    private final YtelsesRepositoryV2 ytelsesRepositoryV2;


    public void behandleKafkaMeldingPostgres(YtelsesDTO kafkaMelding, TypeKafkaYtelse ytelse) {
        YtelsesInnhold innhold = getInnhold(kafkaMelding);

        AktorId aktorId = getAktorId(aktorClient, innhold.getFnr());
        if (skalSlettesGoldenGate(kafkaMelding)) {
            log.info("Postgres: Sletter ytelse: {}, pa aktorId: {}", innhold.getVedtakId(), aktorId);
            ytelsesRepositoryV2.slettYtelse(innhold.getVedtakId());
            oppdaterYtelsesInformasjonMedUnntaksLogikkForSletting(aktorId, innhold);
        } else {
            log.info("Postgres: Lagrer ytelse: {}, pa aktorId: {}", innhold.getVedtakId(), aktorId);
            ytelsesRepositoryV2.upsert(aktorId, ytelse, innhold);
            oppdaterYtelsesInformasjonPostgres(aktorId);
        }
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

        boolean erLopendeVedtak = harLopendeStartDato(startDato, iDag) && harLopendeUtlopsDato(utlopsDato, iDag);

        if (erLopendeVedtak) {
            Optional<YtelseDAO> sisteYtelsePaSakId = finnSisteYtelsePaSakIdSomIkkeErUtloptPostgres(aktorId, innhold.getSaksId());
            if (sisteYtelsePaSakId.isPresent()) {
                log.info("AktoerId: {} har en løpende ytelse med saksId: {}", aktorId, sisteYtelsePaSakId.map(YtelseDAO::getSaksId).orElse("ingen løpende vedtak"));
                brukerDataService.oppdaterYtelserPostgres(aktorId, sisteYtelsePaSakId);

                return sisteYtelsePaSakId;
            }
        }

        return oppdaterYtelsesInformasjonPostgres(aktorId);
    }

    public Optional<YtelseDAO> finnSisteYtelsePaSakIdSomIkkeErUtloptPostgres(AktorId aktorId, String sakID) {
        LocalDate iDag = LocalDate.now();

        List<YtelseDAO> aktiveYtelserPaSakID = ytelsesRepositoryV2.getYtelser(aktorId).stream()
                .filter(Objects::nonNull)
                .filter(ytelse -> sakID.equals(ytelse.getSaksId()))
                .filter(ytelse -> harLopendeUtlopsDato(ytelse.getUtlopsDato(), iDag))
                .collect(Collectors.toList());

        if (aktiveYtelserPaSakID.isEmpty()) {
            return Optional.empty();
        }

        Optional<YtelseDAO> ytelseMedSluttDatoEllerNull = aktiveYtelserPaSakID.stream()
                .filter(ytelseDAO -> ytelseDAO.getUtlopsDato() != null)
                .max(Comparator.comparing(YtelseDAO::getUtlopsDato));

        return Optional.of(ytelseMedSluttDatoEllerNull.orElse(aktiveYtelserPaSakID.get(0)));
    }

    public Optional<YtelseDAO> oppdaterYtelsesInformasjonPostgres(AktorId aktorId) {
        Optional<YtelseDAO> lopendeYtelse = finnLopendeYtelsePostgres(aktorId);
        log.info("Postgres: AktoerId: {} har en løpende ytelse med saksId: {}", aktorId, lopendeYtelse.map(YtelseDAO::getSaksId).orElse("ingen løpende vedtak"));
        brukerDataService.oppdaterYtelserPostgres(aktorId, lopendeYtelse);

        return lopendeYtelse;
    }

    public Optional<YtelseDAO> finnLopendeYtelsePostgres(AktorId aktorId) {
        LocalDate iDag = LocalDate.now();
        List<YtelseDAO> aktiveOgFremtidigeYtelser = ytelsesRepositoryV2.getYtelser(aktorId).stream()
                .filter(Objects::nonNull)
                .filter(ytelse -> harLopendeUtlopsDato(ytelse.getUtlopsDato(), iDag))
                .collect(Collectors.toList());

        if (aktiveOgFremtidigeYtelser.isEmpty()) {
            return Optional.empty();
        }

        YtelseDAO tidligsteYtelse = aktiveOgFremtidigeYtelser.stream()
                .min(Comparator.comparing(YtelseDAO::getStartDato)).get();

        if (!harLopendeStartDato(tidligsteYtelse.getStartDato(), iDag)) {
            return Optional.empty();
        }
        if (TypeKafkaYtelse.DAGPENGER.equals(tidligsteYtelse.getType())) {
            // Dagpenger skal aldri ha en utløpsdato
            // Hvis det finnes en utløpsdato er det mest sannynlig et annet dagpengevedtak som skal ta over for det løpende vedtaket, eller en bug
            return Optional.of(tidligsteYtelse.setUtlopsDato(null));
        }
        if (tidligsteYtelse.getUtlopsDato() == null) {
            return Optional.of(tidligsteYtelse);
        }

        return finnVedtakMedSisteUtlopsDatoPaSak(aktiveOgFremtidigeYtelser, tidligsteYtelse);
    }


    public void oppdaterBrukereMedYtelserSomStarterIDagPostgres() {
        List<AktorId> brukere = ytelsesRepositoryV2.hentBrukereMedYtelserSomStarterIDag();
        log.info("Oppdaterer ytelser for: " + brukere.size() + " antall brukere");

        brukere.forEach(aktorId -> {
            log.info("Oppdaterer ytelse for aktorId: " + aktorId);

            oppdaterYtelsesInformasjonPostgres(aktorId);
        });

        log.info("Oppdatering av ytelser fullført");
    }

    private Optional<YtelseDAO> finnVedtakMedSisteUtlopsDatoPaSak(List<YtelseDAO> ytelser, YtelseDAO tidligsteYtelse) {
        return ytelser.stream()
                .filter(ytelsesDAO -> tidligsteYtelse.getSaksId().equals(ytelsesDAO.getSaksId()))
                .filter(ytelsesDAO -> ytelsesDAO.getUtlopsDato() != null)
                .max(Comparator.comparing(YtelseDAO::getUtlopsDato));
    }

    private boolean harLopendeStartDato(Timestamp startDato, LocalDate iDag) {
        // startDato er en 'fra og med' dato.
        return startDato.toLocalDateTime().toLocalDate().isBefore(iDag.plusDays(1));
    }

    private boolean harLopendeUtlopsDato(Timestamp utlopsDato, LocalDate iDag) {
        // Utløpsdato er en 'til og med' dato.
        return utlopsDato == null || utlopsDato.toLocalDateTime().toLocalDate().isAfter(iDag.minusDays(1));
    }
}
