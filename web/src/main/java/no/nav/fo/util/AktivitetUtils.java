package no.nav.fo.util;

import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.*;
import no.nav.fo.domene.aktivitet.AktivitetBrukerOppdatering;
import no.nav.fo.domene.aktivitet.AktivitetDTO;
import no.nav.fo.domene.aktivitet.AktivitetFullfortStatuser;
import no.nav.fo.domene.aktivitet.AktoerAktiviteter;
import no.nav.fo.service.AktoerService;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static no.nav.fo.domene.aktivitet.AktivitetData.aktivitetTyperList;
import static no.nav.fo.util.MetricsUtils.timed;
import static org.slf4j.LoggerFactory.getLogger;

@Slf4j
public class AktivitetUtils {

    private static final Logger logger = getLogger(AktivitetUtils.class);
    private static final String DATO_FORMAT = "yyyy-MM-dd";

    public static List<AktivitetBrukerOppdatering> konverterTilBrukerOppdatering(List<AktoerAktiviteter> aktoerAktiviteter, AktoerService aktoerService) {
        return aktoerAktiviteter
                .stream()
                .map(aktoerAktivitet -> {
                    AktoerId aktoerId = AktoerId.of(aktoerAktivitet.getAktoerid());
                    Try<PersonId> personid = getPersonId(aktoerId, aktoerService)
                            .onFailure((e) -> log.warn("Kunne ikke hente personid for aktoerid {}", aktoerAktivitet.getAktoerid(), e));

                    return personid.isSuccess() && personid.get() != null ?
                            konverterTilBrukerOppdatering(aktoerAktivitet.getAktiviteter(), aktoerId, personid.get()) :
                            null;
                })
                .filter(Objects::nonNull)
                .collect(toList());
    }


    private static AktivitetBrukerOppdatering konverterTilBrukerOppdatering(List<AktivitetDTO> aktiviteter, AktoerId aktoerId, PersonId personId) {

        Set<AktivitetStatus> aktiveAktiviteter = lagAktivitetSet(aktiviteter, LocalDate.now(), aktoerId, personId);
        Boolean erIAvtaltIAvtaltAktivitet = erBrukerIAktivAktivitet(aktiviteter, LocalDate.now());
        Optional<AktivitetDTO> nyesteUtlopteAktivitet = Optional.ofNullable(finnNyesteUtlopteAktivAktivitet(aktiviteter, LocalDate.now()));

        return new AktivitetBrukerOppdatering(personId.toString(), aktoerId.toString())
                .setAktiviteter(aktiveAktiviteter)
                .setIAvtaltAktivitet(erIAvtaltIAvtaltAktivitet)
                .setNyesteUtlopteAktivitet(nyesteUtlopteAktivitet.map(AktivitetDTO::getTilDato).orElse(null));
    }


    public static AktivitetBrukerOppdatering hentAktivitetBrukerOppdatering(AktoerId aktoerid, AktoerService aktoerService, BrukerRepository brukerRepository) {
        PersonId personid = getPersonId(aktoerid, aktoerService)
                .onFailure((e) -> log.warn("Kunne ikke hente personid for aktoerid {}", aktoerid, e))
                .get();

        List<AktivitetDTO> aktiviteter = brukerRepository.getAktiviteterForAktoerid(aktoerid);

        return konverterTilBrukerOppdatering(aktiviteter, aktoerid, personid);
    }

    static boolean erBrukerIAktivAktivitet(List<AktivitetDTO> aktiviteter, LocalDate today) {
        return aktiviteter
                .stream()
                .filter(AktivitetUtils::harIkkeStatusFullfort)
                .filter(aktivitet -> erAktivitetIPeriode(aktivitet, today))
                .anyMatch(match -> true);

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
                .sorted(Comparator.comparing(AktivitetDTO::getTilDato))
                .findFirst()
                .orElse(null);
    }

    static Set<AktivitetStatus> lagAktivitetSet(List<AktivitetDTO> aktiviteter, LocalDate today, AktoerId aktoerId, PersonId personId) {
        Set<AktivitetStatus> aktiveAktiviteter = new HashSet<>();

        aktivitetTyperList
                .stream()
                .map(Objects::toString)
                .forEach(aktivitetsype -> {

                    List<AktivitetDTO> aktiviteterIPeriodeMedAktivtStatus = aktiviteter
                            .stream()
                            .filter(aktivitet -> aktivitetsype.equals(aktivitet.getAktivitetType()))
                            .filter(aktivitet -> erAktivitetIPeriode(aktivitet, today))
                            .filter(AktivitetUtils::harIkkeStatusFullfort)
                            .collect(toList());

                    Timestamp datoForNesteUtlop = aktiviteterIPeriodeMedAktivtStatus
                            .stream()
                            .map(AktivitetDTO::getTilDato)
                            .filter(Objects::nonNull)
                            .sorted()
                            .findFirst()
                            .orElse(null);

                    boolean aktivitetErIkkeFullfortEllerUtlopt = !aktiviteterIPeriodeMedAktivtStatus.isEmpty();

                    aktiveAktiviteter.add(
                            AktivitetStatus.of(
                                    personId,
                                    aktoerId,
                                    aktivitetsype,
                                    aktivitetErIkkeFullfortEllerUtlopt,
                                    datoForNesteUtlop
                            )
                    );
                });

        return aktiveAktiviteter;
    }

    public static void applyAktivitetStatuser(SolrInputDocument dokument, BrukerRepository brukerRepository) {
        applyAktivitetStatuser(singletonList(dokument), brukerRepository);
    }


    public static void applyAktivitetStatuser(List<SolrInputDocument> dokumenter, BrukerRepository brukerRepository) {
        io.vavr.collection.List.ofAll(dokumenter)
                .sliding(1000, 1000)
                .forEach((dokumenterBatch) -> {
                    timed("indeksering.applyaktiviteter1000", () -> {
                        List<PersonId> personIds = dokumenterBatch.toJavaList().stream()
                                .map((dokument) -> PersonId.of((String) dokument.get("person_id").getValue())).collect(toList());

                        Map<PersonId, Set<AktivitetStatus>> aktivitetStatuser = brukerRepository.getAktivitetstatusForBrukere(personIds);

                        dokumenterBatch.forEach((dokument) -> {
                            PersonId personId = PersonId.of((String) dokument.get("person_id").getValue());
                            applyAktivitetstatusToDocument(dokument, aktivitetStatuser.get(personId));
                        });
                    },
                            (timer, success) -> timer.addTagToReport("batch", dokumenter.size() > 1 ? "true" : "false"));
                });
    }

    private static void applyAktivitetstatusToDocument(SolrInputDocument document, Set<AktivitetStatus> aktivitetStatuser) {
        if (aktivitetStatuser == null) {
            return;
        }
        List<String> aktiveAktiviteter = aktivitetStatuser
                .stream()
                .filter(AktivitetStatus::isAktiv)
                .map(AktivitetStatus::getAktivitetType)
                .collect(toList());

        Map<String, String> aktivitTilUtlopsdato = aktivitetStatuser
                .stream()
                .filter(AktivitetStatus::isAktiv)
                .filter(aktivitetStatus -> Objects.nonNull(aktivitetStatus.getNesteUtlop()))
                .collect(toMap(AktivitetStatus::getAktivitetType,
                        aktivitetStatus -> DateUtils.iso8601FromTimestamp(aktivitetStatus.getNesteUtlop()),
                        (v1, v2) -> v2));

        String aktiviteterUtlopsdatoJSON = new JSONObject(aktivitTilUtlopsdato).toString();

        document.addField("aktiviteter", aktiveAktiviteter);
        document.addField("aktiviteter_utlopsdato_json", aktiviteterUtlopsdatoJSON);
    }

    public static Object applyTiltak(List<SolrInputDocument> dokumenter, BrukerRepository brukerRepository) {
        io.vavr.collection.List.ofAll(dokumenter)
                .sliding(1000,1000)
                .forEach((dokumenterSubSet) -> {
                    List<SolrInputDocument> dokumenterSubSetListe = dokumenterSubSet.toJavaList();
                    List<Fnr> fnrs = dokumenterSubSetListe.stream()
                            .map((dokument) -> Fnr.of((String) dokument.get("fnr").getValue()))
                            .collect(toList());
                    Map<Fnr, Set<Brukertiltak>> tiltak = filtrerBrukertiltak(brukerRepository.hentBrukertiltak(fnrs));
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

    private static boolean etterFilterDato(Timestamp tilDato) {
        Timestamp arenaAktivitetFilterDato = parseDato(System.getProperty("arena.aktivitet.datofilter"));
        return tilDato == null || arenaAktivitetFilterDato == null || arenaAktivitetFilterDato.before(tilDato);
    }

    static Timestamp parseDato(String konfigurertDato) {
        try {
            Date parse = new SimpleDateFormat(DATO_FORMAT).parse(konfigurertDato);
            return new Timestamp(parse.getTime());
        } catch (Exception e) {
            logger.warn("Kunne ikke parse dato [{}] med datoformat [{}].", konfigurertDato, DATO_FORMAT);
            return null;
        }
    }

    private static Try<PersonId> getPersonId(AktoerId aktoerid, AktoerService aktoerService) {
        return aktoerService
                .hentPersonidFraAktoerid(aktoerid);
    }

    private static boolean harIkkeStatusFullfort(AktivitetDTO aktivitetDTO) {
        return !AktivitetFullfortStatuser.contains(aktivitetDTO.getStatus());
    }
}
