package no.nav.fo.util;

import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.PersonId;
import no.nav.fo.domene.aktivitet.AktivitetBrukerOppdatering;
import no.nav.fo.domene.aktivitet.AktivitetDTO;
import no.nav.fo.domene.aktivitet.AktivitetFullfortStatuser;
import no.nav.fo.domene.aktivitet.AktoerAktiviteter;
import no.nav.fo.service.AktoerService;
import org.apache.solr.common.SolrInputDocument;

import java.time.LocalDate;
import java.util.*;

import static java.util.stream.Collectors.toList;
import static no.nav.fo.domene.aktivitet.AktivitetData.aktivitetTyperList;

@Slf4j
public class AktivitetUtils {

    public static List<AktivitetBrukerOppdatering> konverterTilBrukerOppdatering(List<AktoerAktiviteter> aktoerAktiviteter, AktoerService aktoerService) {
        return aktoerAktiviteter
                .stream()
                .map(aktoerAktivitet -> {
                    Try<PersonId> personid = getPersonId(new AktoerId(aktoerAktivitet.getAktoerid()), aktoerService)
                            .onFailure((e) -> log.warn("Kunne ikke hente personid for aktoerid {}", aktoerAktivitet.getAktoerid(), e));

                    return personid.isSuccess() && personid.get() != null ?
                            konverterTilBrukerOppdatering(aktoerAktivitet.getAktiviteter(), aktoerAktivitet.getAktoerid(), personid.get().personId) :
                            null;
                })
                .filter(Objects::nonNull)
                .collect(toList());
    }


    public static AktivitetBrukerOppdatering konverterTilBrukerOppdatering(List<AktivitetDTO> aktiviteter, String aktoerid, String personid) {

        Map<String, Boolean> aktivitetTypeTilStatus = lagAktivitetTypeTilStatusMap(aktiviteter);
        Boolean erIAvtaltIAvtaltAktivitet = erBrukerIAktivAktivitet(aktiviteter, LocalDate.now());
        Optional<AktivitetDTO> nyesteUtlopteAktivitet = Optional.ofNullable(finnNyesteUtlopteAktivAktivitet(aktiviteter, LocalDate.now()));

        return new AktivitetBrukerOppdatering(personid, aktoerid)
                .setAktivitetStatus(aktivitetTypeTilStatus)
                .setIAvtaltAktivitet(erIAvtaltIAvtaltAktivitet)
                .setNyesteUtlopteAktivitet(nyesteUtlopteAktivitet.map(AktivitetDTO::getTilDato).orElse(null));
    }


    public static AktivitetBrukerOppdatering hentAktivitetBrukerOppdatering(String aktoerid, AktoerService aktoerService, BrukerRepository brukerRepository) {
        String personid = getPersonId(new AktoerId(aktoerid), aktoerService)
                .onFailure((e) -> log.warn("Kunne ikke hente personid for aktoerid {}", aktoerid, e))
                .get().personId;

        List<AktivitetDTO> aktiviteter = brukerRepository.getAktiviteterForAktoerid(aktoerid);

        return konverterTilBrukerOppdatering(aktiviteter, aktoerid, personid);
    }

    public static Boolean erBrukersAktivitetAktiv(List<String> aktivitetStatusListe) {
        return aktivitetStatusListe
                .stream()
                .filter(status -> !AktivitetFullfortStatuser.contains(status))
                .anyMatch(match -> true);
    }

    public static boolean erBrukerIAktivAktivitet(List<AktivitetDTO> aktiviteter, LocalDate today) {
        return aktiviteter
                .stream()
                .filter(aktivitet -> !AktivitetFullfortStatuser.contains(aktivitet.getStatus()))
                .filter(aktivitet -> erAktivitetIPeriode(aktivitet, today))
                .anyMatch(match -> true);

    }

    public static boolean erAktivitetIPeriode(AktivitetDTO aktivitet, LocalDate today) {
        if(aktivitet.getTilDato() == null) {
            return true; // Aktivitet er aktiv dersom tildato ikke er satt
        }
        LocalDate tilDato = aktivitet.getTilDato().toLocalDateTime().toLocalDate();

        return today.isBefore(tilDato.plusDays(1));
    }

    public static AktivitetDTO finnNyesteUtlopteAktivAktivitet(List<AktivitetDTO> aktiviteter, LocalDate today) {
        return aktiviteter
                .stream()
                .filter(aktivitet -> !AktivitetFullfortStatuser.contains(aktivitet.getStatus()))
                .filter(aktivitet -> Objects.nonNull(aktivitet.getTilDato()))
                .filter(aktivitet -> aktivitet.getTilDato().toLocalDateTime().toLocalDate().isBefore(today))
                .sorted(Comparator.comparing(AktivitetDTO::getTilDato))
                .findFirst()
                .orElse(null);
    }

    public static Map<String, Boolean> lagAktivitetTypeTilStatusMap(List<AktivitetDTO> aktivitetStatus) {
        Map<String, Boolean> aktivitetTypeTilStatus = new HashMap<>();

        aktivitetTyperList.forEach(aktivitetsype -> {
            List<String> statuser = aktivitetStatus
                    .stream()
                    .filter(aktivitet -> aktivitetsype.toString().equals(aktivitet.getAktivitetType()))
                    .map(AktivitetDTO::getStatus)
                    .collect(toList());

            aktivitetTypeTilStatus.put(aktivitetsype.toString(), erBrukersAktivitetAktiv(statuser));
        });

        return aktivitetTypeTilStatus;
    }

    public static Object applyTiltak(List<SolrInputDocument> dokumenter, BrukerRepository brukerRepository) {
        dokumenter.stream().forEach(document -> {
            String personid = (String) document.get("person_id").getValue();
            List<String> tiltak = brukerRepository.getTiltak(personid);
            if (!tiltak.isEmpty()) {
                document.addField("tiltak", tiltak);
            }
        });
        return null;
    }

    static Try<PersonId> getPersonId(AktoerId aktoerid, AktoerService aktoerService) {
        return aktoerService
                .hentPersonidFraAktoerid(aktoerid);
    }
}
