package no.nav.pto.veilarbportefolje.database;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetDAO;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetDTO;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetUtils;
import no.nav.pto.veilarbportefolje.arenaaktiviteter.GruppeAktivitetRepository;
import no.nav.pto.veilarbportefolje.arenaaktiviteter.TiltakRepositoryV2;
import no.nav.pto.veilarbportefolje.arenaaktiviteter.arenaDTO.GruppeAktivitetSchedueldDTO;
import no.nav.pto.veilarbportefolje.domene.Brukerdata;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.service.BrukerService;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDate;
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

    public void oppdaterAktivitetBrukerData(AktorId aktorId) {
        if (aktorId == null) {
            return;
        }
        brukerService.hentPersonidFraAktoerid(aktorId)
                .onSuccess(personId -> oppdaterAktivitetBrukerData(aktorId, personId))
                .onFailure(error -> log.error("Kunne ikke hente personId pa bruker: {}", aktorId));
    }

    public void oppdaterAktivitetBrukerData(AktorId aktorId, PersonId personId) {
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

    public List<AktorId> hentBrukerSomMaOppdaters() {
        Set<AktorId> rs = new HashSet<>();
        rs.addAll(tiltakRepositoryV2.hentBrukereMedUtlopteTiltak());
        rs.addAll(aktivitetDAO.hentBrukereMedUtlopteAktiviteter());
        rs.addAll(brukerDataRepository.hentBrukereMedUtlopteAktivitetStartDato());

        return new ArrayList<>(rs);
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
        List<Timestamp> sluttdatoer = tiltakRepositoryV2.hentStartDatoer(personId).stream()
                .filter(Objects::nonNull).collect(toList());
        List<Timestamp> aktiviteter = aktivitetDAO.getAktiviteterForAktoerid(aktorId).getAktiviteter().stream()
                .filter(AktivitetUtils::harIkkeStatusFullfort)
                .map(AktivitetDTO::getTilDato)
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
                .map(AktivitetDTO::getFraDato)
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
