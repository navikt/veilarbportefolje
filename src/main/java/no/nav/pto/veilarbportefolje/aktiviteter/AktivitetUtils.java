package no.nav.pto.veilarbportefolje.aktiviteter;

import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.arenafiler.gr202.tiltak.Brukertiltak;
import no.nav.pto.veilarbportefolje.arenafiler.gr202.tiltak.TiltakHandler;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.service.BrukerService;
import no.nav.pto.veilarbportefolje.util.DateUtils;
import no.nav.pto.veilarbportefolje.util.DbUtils;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static no.nav.pto.veilarbportefolje.aktiviteter.AktivitetData.aktivitetTyperFraAktivitetsplanList;
import static no.nav.pto.veilarbportefolje.aktiviteter.AktivitetData.aktivitetTyperFraKafka;

@Slf4j
public class AktivitetUtils {

    private static final String DATO_FORMAT = "yyyy-MM-dd";

    public static AktivitetBrukerOppdatering konverterTilBrukerOppdatering(AktoerAktiviteter aktoerAktiviteter,
                                                                           BrukerService brukerService,
                                                                           boolean erGR202PaKafka) {
        AktorId aktoerId = AktorId.of(aktoerAktiviteter.getAktoerid());

        Try<PersonId> personid = brukerService.hentPersonidFraAktoerid(aktoerId)
                .onFailure((e) -> log.warn("Kunne ikke hente personid for aktoerid {}", aktoerId.toString(), e));

        return personid
                .map(personId -> konverterTilBrukerOppdatering(aktoerAktiviteter.getAktiviteter(), aktoerId, personid.get(), erGR202PaKafka))
                .getOrNull();
    }


    private static AktivitetBrukerOppdatering konverterTilBrukerOppdatering(List<AktivitetDTO> aktiviteter,
                                                                            AktorId aktoerId,
                                                                            PersonId personId,
                                                                            boolean erGR202PaKafka) {

        Set<AktivitetStatus> aktiveAktiviteter = lagAktivitetSet(aktiviteter, LocalDate.now(), aktoerId, personId, erGR202PaKafka);
        Optional<AktivitetDTO> nyesteUtlopteAktivitet = Optional.ofNullable(finnNyesteUtlopteAktivAktivitet(aktiviteter, LocalDate.now()));

        List<AktivitetDTO> aktiveAktivitetDTOList = aktiviteter
                .stream()
                .filter(AktivitetUtils::harIkkeStatusFullfort)
                .filter(aktivitet -> Objects.nonNull(aktivitet.getFraDato()))
                .collect(toList());

        Set<LocalDate> startDatoAktiviteter = finnStartDatoerEtterDagensDato(aktiveAktivitetDTOList);

        Iterator<LocalDate> iterator = startDatoAktiviteter.iterator();
        Optional<LocalDate> aktivitetStart = Try.of(() -> Optional.of(iterator.next())).getOrElse(Optional::empty);
        Optional<LocalDate> nesteAktivitetStart = Try.of(() -> Optional.of(iterator.next())).getOrElse(Optional::empty);

        Optional<LocalDate> forrigeAktivtetStart = finnForrigeAktivitetStartDatoer(aktiveAktivitetDTOList);


        return new AktivitetBrukerOppdatering(personId.toString(), aktoerId.toString())
                .setAktiviteter(aktiveAktiviteter)
                .setNyesteUtlopteAktivitet(nyesteUtlopteAktivitet.map(AktivitetDTO::getTilDato).orElse(null))
                .setAktivitetStart(aktivitetStart.map(date -> Timestamp.valueOf(date.atStartOfDay())).orElse(null))
                .setNesteAktivitetStart(nesteAktivitetStart.map(date -> Timestamp.valueOf(date.atStartOfDay())).orElse(null))
                .setForrigeAktivitetStart(forrigeAktivtetStart.map(date -> Timestamp.valueOf(date.atStartOfDay())).orElse(null));
    }


    public static AktivitetBrukerOppdatering hentAktivitetBrukerOppdateringer(AktorId aktoerId, BrukerService brukerService, AktivitetDAO aktivitetDAO, boolean erGR202PaKafka) {
        AktoerAktiviteter aktiviteter = aktivitetDAO.getAktiviteterForAktoerid(aktoerId);
        return konverterTilBrukerOppdatering(aktiviteter, brukerService, erGR202PaKafka);
    }

    public static boolean erAktivitetIPeriode(AktivitetDTO aktivitet, LocalDate today) {
        if (aktivitet.getTilDato() == null) {
            return true; // Aktivitet er aktiv dersom tildato ikke er satt
        }
        LocalDate tilDato = aktivitet.getTilDato().toLocalDateTime().toLocalDate();

        return today.isBefore(tilDato.plusDays(1));
    }

    public static AktivitetDTO finnNyesteUtlopteAktivAktivitet(List<AktivitetDTO> aktiviteter, LocalDate today) {
        return aktiviteter
                .stream()
                .filter(AktivitetUtils::harIkkeStatusFullfort)
                .filter(aktivitet -> Objects.nonNull(aktivitet.getTilDato()))
                .filter(aktivitet -> aktivitet.getTilDato().toLocalDateTime().toLocalDate().isBefore(today))
                .sorted(Comparator.comparing(AktivitetDTO::getTilDato).reversed())
                .findFirst()
                .orElse(null);
    }

    private static Set<LocalDate> finnStartDatoerEtterDagensDato(List<AktivitetDTO> aktiviteter) {
        return aktiviteter
                .stream()
                .map(aktivitet -> aktivitet.getFraDato().toLocalDateTime().toLocalDate())
                .filter(data -> !data.isBefore(LocalDate.now()))
                .sorted()
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Optional<LocalDate> finnForrigeAktivitetStartDatoer(List<AktivitetDTO> aktiveAktivitetDTOList) {
        return aktiveAktivitetDTOList
                .stream()
                .map(aktivitet -> aktivitet.getFraDato().toLocalDateTime().toLocalDate())
                .filter(data -> data.isBefore(LocalDate.now()))
                .sorted(Comparator.reverseOrder())
                .findFirst();
    }

    public static Set<AktivitetStatus> lagAktivitetSet(List<AktivitetDTO> aktiviteter, LocalDate today, AktorId aktoerId, PersonId personId, boolean erGR202PaKafka) {
        Set<AktivitetStatus> aktiveAktiviteter = new HashSet<>();

        kafkaAktiviteter(erGR202PaKafka)
                .forEach(aktivitetstype -> {

                    List<AktivitetDTO> aktiviteterMedAktivtStatus = aktiviteter
                            .stream()
                            .filter(aktivitet -> aktivitetstype.equals(aktivitet.getAktivitetType()))
                            .filter(AktivitetUtils::harIkkeStatusFullfort)
                            .collect(toList());

                    Timestamp datoForNesteUtlop = aktiviteterMedAktivtStatus
                            .stream()
                            .filter(aktivitet -> erAktivitetIPeriode(aktivitet, today))
                            .map(AktivitetDTO::getTilDato)
                            .filter(Objects::nonNull)
                            .sorted()
                            .findFirst()
                            .orElse(null);

                    Timestamp datoForNesteStart = aktiviteterMedAktivtStatus
                            .stream()
                            .filter(aktivitet -> erAktivitetIPeriode(aktivitet, today))
                            .map(AktivitetDTO::getFraDato)
                            .filter(Objects::nonNull)
                            .sorted()
                            .findFirst()
                            .orElse(null);


                    boolean aktivitetErIkkeFullfort = !aktiviteterMedAktivtStatus.isEmpty();

                    aktiveAktiviteter.add(
                            new AktivitetStatus()
                                    .setPersonid(personId)
                                    .setAktoerid(aktoerId)
                                    .setAktivitetType(aktivitetstype)
                                    .setAktiv(aktivitetErIkkeFullfort)
                                    .setNesteStart(datoForNesteStart)
                                    .setNesteUtlop(datoForNesteUtlop));

                });

        return aktiveAktiviteter;
    }

    public static String statusToIsoUtcString(AktivitetStatus status) {
        return Optional.ofNullable(status).map(AktivitetStatus::getNesteUtlop).map(DateUtils::toIsoUTC).orElse(DateUtils.getFarInTheFutureDate());
    }

    public static String startDatoToIsoUtcString(AktivitetStatus status) {
        return Optional.ofNullable(status).map(AktivitetStatus::getNesteStart).map(DateUtils::toIsoUTC).orElse(null);
    }

    public static Map<Fnr, Set<Brukertiltak>> filtrerBrukertiltak(List<Brukertiltak> brukertiltak) {
        return brukertiltak
                .stream()
                .filter(tiltak -> etterFilterDato(tiltak.getTildato()))
                .collect(toMap(Brukertiltak::getFnr, DbUtils::toSet,
                        (oldValue, newValue) -> {
                            oldValue.addAll(newValue);
                            return oldValue;
                        }
                ));
    }


    public static boolean etterFilterDato(Timestamp tilDato) {
        Timestamp datofilter = TiltakHandler.getDatoFilter();
        return tilDato == null || datofilter == null || datofilter.before(tilDato);
    }

    public static Timestamp parseDato(String konfigurertDato) {
        try {
            Date parse = new SimpleDateFormat(DATO_FORMAT).parse(konfigurertDato);
            return new Timestamp(parse.getTime());
        } catch (Exception e) {
            log.warn("Kunne ikke parse dato [{}] med datoformat [{}].", konfigurertDato, DATO_FORMAT);
            return null;
        }
    }

    private static boolean harIkkeStatusFullfort(AktivitetDTO aktivitetDTO) {
        return !AktivitetIkkeAktivStatuser.contains(aktivitetDTO.getStatus());
    }

    private static Stream<String> kafkaAktiviteter(boolean erGR202PaKafka){
        if(erGR202PaKafka){
            return aktivitetTyperFraKafka.stream().map(Objects::toString);
        }
        return aktivitetTyperFraAktivitetsplanList.stream().map(Objects::toString);
    }
}
