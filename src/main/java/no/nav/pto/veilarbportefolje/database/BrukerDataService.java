package no.nav.pto.veilarbportefolje.database;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetDAO;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetDTO;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetUtils;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.ArenaAktivitetUtils;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.GruppeAktivitetRepository;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.TiltakRepositoryV2;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.GruppeAktivitetSchedueldDTO;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.YtelsesInnhold;
import no.nav.pto.veilarbportefolje.domene.Brukerdata;
import no.nav.pto.veilarbportefolje.domene.YtelseMapping;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.service.BrukerService;
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
    private final BrukerDataRepository brukerDataRepository;
    private final BrukerService brukerService;
    private final ElasticIndexer elasticIndexer;

    public void oppdaterAktivitetBrukerDataOgHentPersonId(List<AktorId> aktorIder) {
        if (aktorIder == null) {
            return;
        }
        aktorIder.forEach(this::oppdaterAktivitetBrukerData);
    }

    public void oppdaterAktivitetBrukerData(AktorId aktorId) {
        if (aktorId == null) {
            return;
        }
        PersonId personId = brukerService.hentPersonidFraAktoerid(aktorId).toJavaOptional().orElse(null);
        if (personId == null) {
            log.info("Fant ingen personId pa aktor: {}", aktorId);
        }

        oppdaterAktivitetBrukerData(aktorId, personId);
        elasticIndexer.indekser(aktorId);
    }

    public void oppdaterAktivitetBrukerData(AktorId aktorId, PersonId personId) {
        if (personId == null || aktorId == null) {
            log.error("PersonId null pa bruker: {}", aktorId);
            return;
        }
        log.info("Oppdaterer brukerdata for aktor: {}, personId: {}", aktorId, personId);
        Brukerdata brukerAktivitetTilstand = new Brukerdata();
        LocalDate idag = LocalDate.now();

        List<Timestamp> sluttdatoer = hentAlleSluttdatoer(aktorId, personId);
        List<Timestamp> startDatoer = hentAlleStartdatoer(aktorId, personId);

        Timestamp nyesteUtlopteDato = finnNyesteUtlopteAktivAktivitet(sluttdatoer, idag);
        Timestamp forigeAktivitetStart = finnForrigeAktivitetStartDatoer(startDatoer, idag);

        List<Timestamp> startDatoerEtterDagensDato = finnDatoerEtterDagensDato(startDatoer, idag);
        Timestamp aktivitetStart = (startDatoerEtterDagensDato.isEmpty()) ? null : startDatoerEtterDagensDato.get(0);
        Timestamp nesteAktivitetStart = (startDatoerEtterDagensDato.size() < 2) ? null : startDatoerEtterDagensDato.get(1);

        brukerAktivitetTilstand
                .setAktoerid(aktorId.get())
                .setPersonid(personId.getValue())
                .setNyesteUtlopteAktivitet(nyesteUtlopteDato)
                .setForrigeAktivitetStart(forigeAktivitetStart)
                .setAktivitetStart(aktivitetStart)
                .setNesteAktivitetStart(nesteAktivitetStart);

        brukerDataRepository.upsertAktivitetData(brukerAktivitetTilstand);
    }

    public void oppdaterYtelser(AktorId aktorId, YtelsesInnhold innhold, boolean skalSlettes) {
        Brukerdata ytelsesTilstand = new Brukerdata()
                .setAktoerid(aktorId.get())
                .setPersonid(innhold.getPersonId());
        leggTilRelevantYtelsesData(ytelsesTilstand, innhold, skalSlettes);

        brukerDataRepository.upsertYtelser(ytelsesTilstand);
    }

    public List<AktorId> hentBrukerSomMaOppdaters() {
        List<AktorId> tiltak = tiltakRepositoryV2.hentBrukereMedUtlopteTiltak();
        List<AktorId> aktiviteter = aktivitetDAO.hentBrukereMedUtlopteAktiviteter();
        List<AktorId> utloptStartDato = brukerDataRepository.hentBrukereMedUtlopteAktivitetStartDato();

        Set<AktorId> rs = new HashSet<>(tiltak);
        rs.addAll(aktiviteter);
        rs.addAll(utloptStartDato);
        log.info("Fant: {} med utlopt tiltak, {} med utlopte aktiviteter, og {} med utlopt start dato.", tiltak.size(), aktiviteter.size(), utloptStartDato.size());
        return new ArrayList<>(rs);
    }


    private void leggTilRelevantYtelsesData(Brukerdata ytelsesTilstand, YtelsesInnhold innhold, boolean skalSlettes) {
        if (skalSlettes) {
            return;
        }
        YtelseMapping ytelseMapping = YtelseMapping.of(innhold)
                .orElseThrow(() -> new RuntimeException(innhold.toString()));

        LocalDateTime utlopsDato = Optional.ofNullable(innhold.getVedtaksperiode())
                .map(periode ->
                        ArenaAktivitetUtils.getLocalDateOrNull(periode.getTilogMedDato(), true)
                ).orElseThrow(() -> new RuntimeException("Ytelse mangler til og med-dato"));

        ;
        Optional<YtelsesInnhold.Dagpengetellere> dagpengetellere = Optional.ofNullable(innhold.getDagpengetellere());
        Optional<YtelsesInnhold.Aaptellere> aaptellere = Optional.ofNullable(innhold.getAaptellere());

        ytelsesTilstand
                .setYtelse(ytelseMapping)
                .setUtlopsdato(utlopsDato)
                .setDagputlopUke(dagpengetellere.map(YtelsesInnhold.Dagpengetellere::getAntallUkerIgjen).orElse(null))
                .setPermutlopUke(dagpengetellere.map(YtelsesInnhold.Dagpengetellere::getAntallUkerIgjenUnderPermittering).orElse(null))
                .setAapmaxtidUke(aaptellere.map(YtelsesInnhold.Aaptellere::getAntallUkerIgjen).orElse(null))
                .setAapUnntakDagerIgjen(aaptellere.map(YtelsesInnhold.Aaptellere::getAntallDagerIgjenUnntak).orElse(null));
    }

    private static Timestamp finnNyesteUtlopteAktivAktivitet(List<Timestamp> aktiviteter, LocalDate today) {
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
        List<Timestamp> sluttdatoer = tiltakRepositoryV2.hentStartDatoer(personId).stream()
                .filter(Objects::nonNull).collect(toList());
        List<Timestamp> aktiviteter = aktivitetDAO.getAktiviteterForAktoerid(aktorId).getAktiviteter().stream()
                .filter(AktivitetUtils::harIkkeStatusFullfort)
                .map(AktivitetDTO::getFraDato)
                .filter(Objects::nonNull).collect(toList());
        List<Timestamp> gruppeAktiviteter = gruppeAktivitetRepository.hentAktiveAktivteter(aktorId).stream()
                .map(GruppeAktivitetSchedueldDTO::getAktivitetperiodeFra)
                .filter(Objects::nonNull).collect(toList());

        sluttdatoer.addAll(aktiviteter);
        sluttdatoer.addAll(gruppeAktiviteter);
        sluttdatoer.sort(Comparator.naturalOrder());
        return sluttdatoer;
    }


    private List<Timestamp> hentAlleSluttdatoer(AktorId aktorId, PersonId personId) {
        List<Timestamp> sluttdatoer = tiltakRepositoryV2.hentSluttdatoer(personId).stream()
                .filter(Objects::nonNull).collect(toList());
        List<Timestamp> aktiviteter = aktivitetDAO.getAktiviteterForAktoerid(aktorId).getAktiviteter().stream()
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
}
