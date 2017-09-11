package no.nav.fo.util;

import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.AktivitetStatus;
import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.PersonId;
import no.nav.fo.domene.aktivitet.AktivitetBrukerOppdatering;
import no.nav.fo.domene.aktivitet.AktivitetDTO;
import no.nav.fo.domene.aktivitet.AktivitetFullfortStatuser;
import no.nav.fo.domene.aktivitet.AktoerAktiviteter;
import no.nav.fo.service.AktoerService;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;

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

    public static List<AktivitetBrukerOppdatering> konverterTilBrukerOppdatering(List<AktoerAktiviteter> aktoerAktiviteter, AktoerService aktoerService) {
        return aktoerAktiviteter
                .stream()
                .map(aktoerAktivitet -> {
                    AktoerId aktoerId = new AktoerId(aktoerAktivitet.getAktoerid());
                    Try<PersonId> personid = getPersonId(aktoerId, aktoerService)
                            .onFailure((e) -> log.warn("Kunne ikke hente personid for aktoerid {}", aktoerAktivitet.getAktoerid(), e));

                    return personid.isSuccess() && personid.get() != null ?
                            konverterTilBrukerOppdatering(aktoerAktivitet.getAktiviteter(), aktoerId, personid.get()) :
                            null;
                })
                .filter(Objects::nonNull)
                .collect(toList());
    }


    public static AktivitetBrukerOppdatering konverterTilBrukerOppdatering(List<AktivitetDTO> aktiviteter, AktoerId aktoerId, PersonId personId) {

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

    public static Boolean erBrukersAktivitetAktiv(List<String> aktivitetStatusListe) {
        return aktivitetStatusListe
                .stream()
                .filter(status -> !AktivitetFullfortStatuser.contains(status))
                .anyMatch(match -> true);
    }

    public static boolean erBrukerIAktivAktivitet(List<AktivitetDTO> aktiviteter, LocalDate today) {
        return aktiviteter
                .stream()
                .filter(AktivitetUtils::harIkkeStatusFullfort)
                .filter(aktivitet -> erAktivitetIPeriode(aktivitet, today))
                .anyMatch(match -> true);

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
                .sorted(Comparator.comparing(AktivitetDTO::getTilDato))
                .findFirst()
                .orElse(null);
    }

    public static Set<AktivitetStatus> lagAktivitetSet(List<AktivitetDTO> aktiviteter, LocalDate today, AktoerId aktoerId, PersonId personId) {
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
                                .map((dokument) -> new PersonId((String) dokument.get("person_id").getValue())).collect(toList());

                        Map<PersonId, Set<AktivitetStatus>> aktivitetStatuser = brukerRepository.getAktivitetstatusForBrukere(personIds);

                        dokumenterBatch.forEach((dokument) -> {
                            PersonId personId = new PersonId((String) dokument.get("person_id").getValue());
                            applyAktivitetstatusToDocument(dokument, aktivitetStatuser.get(personId));
                        });
                    },
                            (timer, success) -> timer.addTagToReport("size", Objects.toString(dokumenter.size())));
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

    @Value("${arena.aktivitet.datofilter}")
    private static String datoFilter;

    public static Object applyTiltak(List<SolrInputDocument> dokumenter, BrukerRepository brukerRepository) {
        Timestamp arenaAktivitetFilterDato = parseDato(System.getProperty("arena.aktivitet.datofilter"));
        dokumenter.forEach(document -> {
            String fnr = (String) document.get("fnr").getValue();
            List<String> tiltakListe = brukerRepository.hentBrukertiltak(fnr).stream()
                .filter(tiltak -> etterFilterDato((Timestamp) tiltak.get("tildato"), arenaAktivitetFilterDato))
                .map(tiltak -> (String) tiltak.get("tiltak"))
                .collect(toList());
            if(!tiltakListe.isEmpty()) {
                document.addField("tiltak", tiltakListe);
            }
        });
        return null;
    }

    private static final String DATO_FORMAT = "yyyy-MM-dd";

    private static Timestamp parseDato(String konfigurertDato) {
        try {
            Date parse = new SimpleDateFormat(DATO_FORMAT).parse(konfigurertDato);
            return new Timestamp(parse.getTime());
        } catch (Exception e) {
            logger.warn("Kunne ikke parse dato [{}] med datoformat [{}].", konfigurertDato, DATO_FORMAT);
            return null;
        }
    }

    private static boolean etterFilterDato(Timestamp tilDato, Timestamp arenaAktivitetFilterDato) {
        return tilDato == null || arenaAktivitetFilterDato == null || arenaAktivitetFilterDato.before(tilDato);
    }

    static Try<PersonId> getPersonId(AktoerId aktoerid, AktoerService aktoerService) {
        return aktoerService
                .hentPersonidFraAktoerid(aktoerid);
    }

    static boolean harIkkeStatusFullfort(AktivitetDTO aktivitetDTO) {
        return !AktivitetFullfortStatuser.contains(aktivitetDTO.getStatus());
    }
}
