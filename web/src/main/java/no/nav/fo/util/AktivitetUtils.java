package no.nav.fo.util;

import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.Aktivitet.AktivitetBrukerOppdatering;
import no.nav.fo.domene.Aktivitet.AktivitetDTO;
import no.nav.fo.domene.Aktivitet.AktivitetData;
import no.nav.fo.domene.Aktivitet.AktivitetFullfortStatuser;
import no.nav.fo.exception.FantIkkePersonIdException;
import no.nav.fo.service.AktoerService;
import org.apache.solr.common.SolrInputDocument;
import org.joda.time.LocalDate;

import java.sql.Timestamp;
import java.util.*;

import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;
import static no.nav.fo.domene.Aktivitet.AktivitetData.aktivitetTyperList;

public class AktivitetUtils {

    public static AktivitetBrukerOppdatering hentAktivitetBrukerOppdatering(String aktoerid, AktoerService aktoerService, BrukerRepository brukerRepository) {
        String personid = aktoerService.hentPersonidFraAktoerid(aktoerid).orElseThrow(() -> new FantIkkePersonIdException("Fant ikke personid for aktor id: " + aktoerid));

        List<AktivitetDTO> aktiviteter = brukerRepository.getAktiviteterForAktoerid(aktoerid);

        Map<String, Boolean> aktivitetTypeTilStatus = lagAktivitetTypeTilStatusMap(aktiviteter);
        Boolean erIAvtaltIAvtaltAktivitet = erBrukerIAktivAktivitet(aktiviteter, LocalDate.now());
        Optional<AktivitetDTO> nyesteUtlopteAktivitet = Optional.ofNullable(finnNyesteUtlopteAktivAktivitet(aktiviteter, LocalDate.now()));

        return new AktivitetBrukerOppdatering(personid, aktoerid)
                .setAktivitetStatus(aktivitetTypeTilStatus)
                .setIAvtaltAktivitet(erIAvtaltIAvtaltAktivitet)
                .setNyesteUtlopteAktivitet(nyesteUtlopteAktivitet.map(AktivitetDTO::getTilDato).orElse(null));
    }

    public static Boolean erBrukersAktivitetAktiv(List<String> aktivitetStatusListe) {
        return aktivitetStatusListe
                .stream()
                .filter(status -> !AktivitetFullfortStatuser.contains(status))
                .anyMatch( match -> true);
    }

    public static boolean erBrukerIAktivAktivitet(List<AktivitetDTO> aktiviteter, LocalDate today) {
        return aktiviteter
                .stream()
                .filter( aktivitet -> !AktivitetFullfortStatuser.contains(aktivitet.getStatus()))
                .filter( aktivitet -> erAktivitetIPeriode(aktivitet, today))
                .anyMatch( match -> true);

    }

    public static boolean erAktivitetIPeriode(AktivitetDTO aktivitet, LocalDate today) {
        LocalDate fraDato = LocalDate.fromDateFields(aktivitet.getFraDato());
        LocalDate tilDato = LocalDate.fromDateFields(aktivitet.getTilDato());

        return fraDato.isBefore(today.plusDays(1)) && tilDato.plusDays(1).isAfter(today);
    }

    public static AktivitetDTO finnNyesteUtlopteAktivAktivitet(List<AktivitetDTO> aktiviteter, LocalDate today) {
        return aktiviteter
                .stream()
                .filter(aktivitet -> !AktivitetFullfortStatuser.contains(aktivitet.getStatus()))
                .filter(aktivitet -> LocalDate.fromDateFields(aktivitet.getTilDato()).isBefore(today))
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

    public static void applyAktivitetStatuser(List<SolrInputDocument> dokumenter, BrukerRepository brukerRepository) {
        for(SolrInputDocument document : dokumenter) {
            String personid = (String) document.get("person_id").getValue();
            Map<String, Timestamp> statusMap = brukerRepository.getAktivitetStatusMap(personid);
            AktivitetData.aktivitetTyperList.forEach( (type) -> document.addField(type.toString(), statusMap.get(type.toString())));
        }
    }

    public static void applyAktivitetStatuser(SolrInputDocument dokument, BrukerRepository brukerRepository) {
        applyAktivitetStatuser(singletonList(dokument), brukerRepository);
    }
}
