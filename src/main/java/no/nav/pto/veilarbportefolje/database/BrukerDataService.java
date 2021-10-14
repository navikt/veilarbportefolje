package no.nav.pto.veilarbportefolje.database;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.aktiviteter.*;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.GruppeAktivitetRepository;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.GruppeAktivitetRepositoryV2;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.TiltakRepositoryV2;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.GruppeAktivitetSchedueldDTO;
import no.nav.pto.veilarbportefolje.arenapakafka.ytelser.YtelseDAO;
import no.nav.pto.veilarbportefolje.domene.Brukerdata;
import no.nav.pto.veilarbportefolje.domene.YtelseMapping;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrukerDataService {
    private final AktivitetDAO aktivitetDAO;
    private final TiltakRepositoryV2 tiltakRepositoryV2;
    private final GruppeAktivitetRepository gruppeAktivitetRepository;
    private final GruppeAktivitetRepositoryV2 gruppeAktivitetRepositoryV2;
    private final BrukerDataRepository brukerDataRepository;

    //POSTGRES
    private final AktiviteterRepositoryV2 aktiviteterRepositoryV2;
    private final AktivitetStatusRepositoryV2 aktivitetStatusRepositoryV2;

    public void oppdaterAktivitetBrukerDataPostgres(AktorId aktorId) {
        oppdaterAktivitetBrukerData(aktorId, null, true);
    }

    public void oppdaterAktivitetBrukerData(AktorId aktorId, PersonId personId) {
        oppdaterAktivitetBrukerData(aktorId, personId, false);
    }

    public void oppdaterAktivitetBrukerData(AktorId aktorId, PersonId personId, boolean postgres) {
        if (aktorId == null || (personId == null && !postgres)) {
            log.error("PersonId null pa bruker: {}", aktorId);
            return;
        }
        log.info("Oppdaterer brukerdata for aktor: {}, personId: {}", aktorId, personId);
        Brukerdata brukerAktivitetTilstand = new Brukerdata();
        LocalDate idag = LocalDate.now();

        List<Timestamp> sluttdatoer = postgres ? hentAlleSluttdatoerPostgres(aktorId) : hentAlleSluttdatoer(aktorId, personId);
        List<Timestamp> startDatoer = postgres ? hentAlleStartdatoerPostgres(aktorId) : hentAlleStartdatoer(aktorId, personId);

        Timestamp nyesteUtlopteDato = finnNyesteUtlopteAktivAktivitet(sluttdatoer, idag);
        Timestamp forigeAktivitetStart = finnForrigeAktivitetStartDatoer(startDatoer, idag);

        List<Timestamp> startDatoerEtterDagensDato = finnDatoerEtterDagensDato(startDatoer, idag);
        Timestamp aktivitetStart = (startDatoerEtterDagensDato.isEmpty()) ? null : startDatoerEtterDagensDato.get(0);
        Timestamp nesteAktivitetStart = (startDatoerEtterDagensDato.size() < 2) ? null : startDatoerEtterDagensDato.get(1);

        brukerAktivitetTilstand
                .setAktoerid(aktorId.get())
                .setPersonid(personId == null ? null : personId.getValue())
                .setNyesteUtlopteAktivitet(nyesteUtlopteDato)
                .setForrigeAktivitetStart(forigeAktivitetStart)
                .setAktivitetStart(aktivitetStart)
                .setNesteAktivitetStart(nesteAktivitetStart);
        if (postgres) {
            aktivitetStatusRepositoryV2.upsertAktivitetStatus(brukerAktivitetTilstand);
        } else {
            brukerDataRepository.upsertAktivitetData(brukerAktivitetTilstand);
        }
    }

    public void oppdaterYtelser(AktorId aktorId, PersonId personId, Optional<YtelseDAO> innhold) {
        Brukerdata ytelsesTilstand = new Brukerdata()
                .setAktoerid(aktorId.get())
                .setPersonid(personId.getValue());
        if (innhold.isEmpty()) {
            brukerDataRepository.upsertYtelser(ytelsesTilstand);
            return;
        }

        switch (innhold.get().getType()) {
            case DAGPENGER:
                leggTilYtelsesData(ytelsesTilstand, innhold.get());
                leggTilRelevantDagpengeData(ytelsesTilstand, innhold.get());
                break;
            case AAP:
                leggTilYtelsesData(ytelsesTilstand, innhold.get());
                leggTilRelevantAAPData(ytelsesTilstand, innhold.get());
                break;
            case TILTAKSPENGER:
                leggTilYtelsesData(ytelsesTilstand, innhold.get());
                break;
        }

        brukerDataRepository.upsertYtelser(ytelsesTilstand);
    }

    private void leggTilYtelsesData(Brukerdata ytelsesTilstand, YtelseDAO innhold) {
        YtelseMapping ytelseMapping = YtelseMapping.of(innhold)
                .orElseThrow(() -> new RuntimeException(innhold.toString()));
        LocalDateTime utlopsDato = Optional.ofNullable(innhold.getUtlopsDato())
                .map(Timestamp::toLocalDateTime)
                .orElse(null);

        ytelsesTilstand.setYtelse(ytelseMapping).setUtlopsdato(utlopsDato);
    }

    private void leggTilRelevantDagpengeData(Brukerdata ytelsesTilstand, YtelseDAO innhold) {
        ytelsesTilstand
                .setDagputlopUke(innhold.getAntallUkerIgjen())
                .setPermutlopUke(innhold.getAntallUkerIgjenPermittert());
    }

    private void leggTilRelevantAAPData(Brukerdata ytelsesTilstand, YtelseDAO innhold) {
        ytelsesTilstand
                .setAapmaxtidUke(innhold.getAntallUkerIgjen())
                .setAapUnntakDagerIgjen(innhold.getAntallDagerIgjenUnntak());
    }

    public static Timestamp finnNyesteUtlopteAktivAktivitet(List<Timestamp> aktiviteter, LocalDate today) {
        return aktiviteter
                .stream()
                .filter(aktivitet -> aktivitet.toLocalDateTime().toLocalDate().isBefore(today))
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    private static Timestamp finnForrigeAktivitetStartDatoer(List<Timestamp> startDatoer, LocalDate today) {
        return startDatoer
                .stream()
                .filter(aktivitet -> aktivitet.toLocalDateTime().toLocalDate().isBefore(today))
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    private static List<Timestamp> finnDatoerEtterDagensDato(List<Timestamp> aktiviteter, LocalDate today) {
        return aktiviteter
                .stream()
                .filter(aktivitet -> !aktivitet.toLocalDateTime().toLocalDate().isBefore(today))
                .sorted()
                .collect(Collectors.toList());
    }

    // TODO: vurder aa merge de to metodene under
    private List<Timestamp> hentAlleStartdatoer(AktorId aktorId, PersonId personId) {
        List<Timestamp> startDatoer = tiltakRepositoryV2.hentStartDatoer(personId).stream()
                .filter(Objects::nonNull).collect(toList());
        List<Timestamp> aktiviteter = aktivitetDAO.getAvtalteAktiviteterForAktoerid(aktorId).getAktiviteter().stream()
                .filter(AktivitetUtils::harIkkeStatusFullfort)
                .map(AktivitetDTO::getFraDato)
                .filter(Objects::nonNull).collect(toList());
        List<Timestamp> gruppeAktiviteter = gruppeAktivitetRepository.hentAktiveAktivteter(aktorId).stream()
                .map(GruppeAktivitetSchedueldDTO::getAktivitetperiodeFra)
                .filter(Objects::nonNull).collect(toList());

        startDatoer.addAll(aktiviteter);
        startDatoer.addAll(gruppeAktiviteter);
        startDatoer.sort(Comparator.naturalOrder());
        return startDatoer;
    }


    private List<Timestamp> hentAlleSluttdatoer(AktorId aktorId, PersonId personId) {
        List<Timestamp> sluttdatoer = tiltakRepositoryV2.hentSluttdatoer(personId).stream()
                .filter(Objects::nonNull).collect(toList());
        List<Timestamp> aktiviteter = aktivitetDAO.getAvtalteAktiviteterForAktoerid(aktorId).getAktiviteter().stream()
                .filter(AktivitetUtils::harIkkeStatusFullfort)
                .map(AktivitetDTO::getTilDato)
                .filter(Objects::nonNull).collect(toList());
        List<Timestamp> gruppeAktiviteter = gruppeAktivitetRepository.hentAktiveAktivteter(aktorId).stream()
                .map(GruppeAktivitetSchedueldDTO::getAktivitetperiodeTil)
                .filter(Objects::nonNull).collect(toList());

        sluttdatoer.addAll(aktiviteter);
        sluttdatoer.addAll(gruppeAktiviteter);
        sluttdatoer.sort(Comparator.naturalOrder());
        return sluttdatoer;
    }


    private List<Timestamp> hentAlleStartdatoerPostgres(AktorId aktorId) {
        List<Timestamp> startDatoer = aktiviteterRepositoryV2.getAvtalteAktiviteterForAktoerid(aktorId).getAktiviteter().stream()
                .filter(AktivitetUtils::harIkkeStatusFullfort)
                .map(AktivitetDTO::getFraDato)
                .filter(Objects::nonNull)
                .collect(toList());
        List<Timestamp> gruppeAktiviteter = gruppeAktivitetRepositoryV2.hentAktiveAktivteter(aktorId).stream()
                .map(GruppeAktivitetSchedueldDTO::getAktivitetperiodeFra)
                .filter(Objects::nonNull).collect(toList());
        // TODO: legg til tiltak
        //startDatoer.addAll(tiltak);
        startDatoer.addAll(gruppeAktiviteter);
        startDatoer.sort(Comparator.naturalOrder());
        return startDatoer;

    }

    private List<Timestamp> hentAlleSluttdatoerPostgres(AktorId aktorId) {
        List<Timestamp> sluttdatoer = aktiviteterRepositoryV2.getAvtalteAktiviteterForAktoerid(aktorId).getAktiviteter().stream()
                .filter(AktivitetUtils::harIkkeStatusFullfort)
                .map(AktivitetDTO::getTilDato)
                .filter(Objects::nonNull)
                .collect(toList());
        List<Timestamp> gruppeAktiviteter = gruppeAktivitetRepositoryV2.hentAktiveAktivteter(aktorId).stream()
                .map(GruppeAktivitetSchedueldDTO::getAktivitetperiodeTil)
                .filter(Objects::nonNull).collect(toList());

        // TODO: legg til tiltak
        // sluttdatoer.addAll(tiltak);
        sluttdatoer.addAll(gruppeAktiviteter);
        sluttdatoer.sort(Comparator.naturalOrder());
        return sluttdatoer;
    }
}
