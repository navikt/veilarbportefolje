package no.nav.fo.util;

import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.aktivitet.AktivitetDAO;
import no.nav.fo.domene.*;
import no.nav.fo.domene.aktivitet.*;
import no.nav.fo.filmottak.tiltak.TiltakHandler;
import no.nav.fo.service.AktoerService;
import org.apache.solr.common.SolrInputDocument;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static no.nav.fo.domene.aktivitet.AktivitetData.aktivitetTyperFraAktivitetsplanList;
import static no.nav.fo.util.DateUtils.getSolrMaxAsIsoUtc;
import static no.nav.fo.util.MetricsUtils.timed;

@Slf4j
public class AktivitetUtils {

    private static final String DATO_FORMAT = "yyyy-MM-dd";

    public static List<AktivitetBrukerOppdatering> konverterTilBrukerOppdatering(List<AktoerAktiviteter> aktoerAktiviteter,
                                                                                 AktoerService aktoerService) {
        return aktoerAktiviteter
                .stream()
                .map(aktoerAktivitet -> {
                    AktoerId aktoerId = AktoerId.of(aktoerAktivitet.getAktoerid());
                    Try<PersonId> personid = aktoerService.hentPersonidFraAktoerid(aktoerId)
                            .onFailure((e) -> log.warn("Kunne ikke hente personid for aktoerid {}", aktoerAktivitet.getAktoerid(), e));

                    return personid.isSuccess() && personid.get() != null ?
                            konverterTilBrukerOppdatering(aktoerAktivitet.getAktiviteter(), aktoerId, personid.get()) :
                            null;
                })
                .filter(Objects::nonNull)
                .collect(toList());
    }


    private static AktivitetBrukerOppdatering konverterTilBrukerOppdatering(List<AktivitetDTO> aktiviteter,
                                                                            AktoerId aktoerId,
                                                                            PersonId personId) {

        Set<AktivitetStatus> aktiveAktiviteter = lagAktivitetSet(aktiviteter, LocalDate.now(), aktoerId, personId);
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


    public static List<AktivitetBrukerOppdatering> hentAktivitetBrukerOppdateringer(List<AktoerId> aktoerIds, AktoerService aktoerService, AktivitetDAO aktivitetDAO) {
        List<AktoerAktiviteter> aktiviteter = aktivitetDAO.getAktiviteterForListOfAktoerid(aktoerIds.stream().map(AktoerId::toString).collect(toList()));
        return konverterTilBrukerOppdatering(aktiviteter, aktoerService);
    }

    static boolean erAktivitetIPeriode(AktivitetDTO aktivitet, LocalDate today) {
        if (aktivitet.getTilDato() == null) {
            return true; // Aktivitet er aktiv dersom tildato ikke er satt
        }
        LocalDate tilDato = aktivitet.getTilDato().toLocalDateTime().toLocalDate();

        return today.isBefore(tilDato.plusDays(1));
    }

    static AktivitetDTO finnNyesteUtlopteAktivAktivitet(List<AktivitetDTO> aktiviteter, LocalDate today) {
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

    static Set<AktivitetStatus> lagAktivitetSet(List<AktivitetDTO> aktiviteter, LocalDate today, AktoerId aktoerId, PersonId personId) {
        Set<AktivitetStatus> aktiveAktiviteter = new HashSet<>();

        aktivitetTyperFraAktivitetsplanList
                .stream()
                .map(Objects::toString)
                .forEach(aktivitetsype -> {

                    List<AktivitetDTO> aktiviteterMedAktivtStatus = aktiviteter
                            .stream()
                            .filter(aktivitet -> aktivitetsype.equals(aktivitet.getAktivitetType()))
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

                    boolean aktivitetErIkkeFullfort = !aktiviteterMedAktivtStatus.isEmpty();

                    aktiveAktiviteter.add(
                            AktivitetStatus.of(
                                    personId,
                                    aktoerId,
                                    aktivitetsype,
                                    aktivitetErIkkeFullfort,
                                    datoForNesteUtlop
                            )
                    );
                });

        return aktiveAktiviteter;
    }

    public static void applyAktivitetStatuser(List<SolrInputDocument> dokumenter, AktivitetDAO aktivitetDAO) {
        io.vavr.collection.List.ofAll(dokumenter)
                .sliding(1000, 1000)
                .forEach((dokumenterBatch) -> {
                    timed("indeksering.applyaktiviteter1000", () -> {
                        List<PersonId> personIds = dokumenterBatch.toJavaList().stream()
                                .map((dokument) -> PersonId.of((String) dokument.get("person_id").getValue())).collect(toList());

                        Map<PersonId, Set<AktivitetStatus>> aktivitetStatuser = aktivitetDAO.getAktivitetstatusForBrukere(personIds);

                        dokumenterBatch.forEach((dokument) -> {
                            PersonId personId = PersonId.of((String) dokument.get("person_id").getValue());
                            applyAktivitetstatusToDocument(dokument, aktivitetStatuser.get(personId));
                        });
                    },
                            (timer, success) -> timer.addTagToReport("batch", dokumenter.size() > 1 ? "true" : "false"));
                });
    }

    public static void applyAktivitetstatusToDocument(SolrInputDocument document, Set<AktivitetStatus> aktivitetStatuser) {
        if (aktivitetStatuser == null) {
            return;
        }
        List<String> aktiveAktiviteter = aktivitetStatuser
                .stream()
                .filter(AktivitetStatus::isAktiv)
                .map(AktivitetStatus::getAktivitetType)
                .collect(toList());

        Map<String, String> eksisterendeAktiviteterTilUtlopsdato = aktivitetStatuser
                .stream()
                .collect(toMap(status -> status.getAktivitetType().toLowerCase(),
                        AktivitetUtils::statusToIsoUtcString,
        (v1, v2) -> v2));

        Map<String, String> alleAktiviteterTilUtlopsdato = leggTilSolrMaxOmAktivitetIkkeEksisterer(eksisterendeAktiviteterTilUtlopsdato);

        alleAktiviteterTilUtlopsdato.forEach((key, value) -> document.addField(addPrefixForAktivitetUtlopsdato(key), value));

        document.addField("aktiviteter", aktiveAktiviteter);
    }


    private static Map<String, String> leggTilSolrMaxOmAktivitetIkkeEksisterer(Map<String, String> aktiviteter) {
        AktivitetData.aktivitetTyperList.stream().map(Enum::name).forEach(aktivitet -> {
            if(!aktiviteter.containsKey(aktivitet)) {
                aktiviteter.put(aktivitet, getSolrMaxAsIsoUtc());
            }
        });
        return aktiviteter;
    }

    private static String statusToIsoUtcString(AktivitetStatus status) {
        return Optional.ofNullable(status).map(AktivitetStatus::getNesteUtlop).map(DateUtils::toIsoUTC).orElse(DateUtils.getSolrMaxAsIsoUtc());
    }

    public static String addPrefixForAktivitetUtlopsdato(String aktivitet) {
        return Optional.ofNullable(aktivitet).map( s -> "aktivitet_"+s+"_utlopsdato").orElse(null);
    }

    public static Object applyTiltak(List<SolrInputDocument> dokumenter, AktivitetDAO aktivitetDAO) {
        io.vavr.collection.List.ofAll(dokumenter)
                .sliding(1000,1000)
                .forEach((dokumenterSubSet) -> {
                    List<SolrInputDocument> dokumenterSubSetListe = dokumenterSubSet.toJavaList();
                    List<Fnr> fnrs = dokumenterSubSetListe.stream()
                            .map((dokument) -> Fnr.of((String) dokument.get("fnr").getValue()))
                            .collect(toList());
                    Map<Fnr, Set<Brukertiltak>> tiltak = filtrerBrukertiltak(aktivitetDAO.hentBrukertiltak(fnrs));
                    dokumenterSubSetListe.forEach(document -> {
                        Fnr fnr = Fnr.of((String) document.get("fnr").getValue());
                        Optional<Set<Brukertiltak>> brukertiltak = Optional.ofNullable(tiltak.get(fnr));
                        if(brukertiltak.isPresent()) {
                            List<String> tiltakliste = brukertiltak.get().stream().map(Brukertiltak::getTiltak).collect(toList());
                            document.addField("tiltak", tiltakliste);
                        }
                });
        });
        return null;
    }

    private static Map<Fnr, Set<Brukertiltak>> filtrerBrukertiltak(List<Brukertiltak> brukertiltak) {
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
}
