package no.nav.pto.veilarbportefolje.arenafiler.gr199.ytelser;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.database.PersistentOppdatering;
import no.nav.pto.veilarbportefolje.domene.*;
import no.nav.melding.virksomhet.loependeytelser.v1.LoependeVedtak;
import no.nav.melding.virksomhet.loependeytelser.v1.LoependeYtelser;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static java.time.LocalDateTime.now;
import static java.util.stream.Collectors.*;
import static no.nav.pto.veilarbportefolje.domene.Utlopsdato.utlopsdato;
import static no.nav.pto.veilarbportefolje.util.StreamUtils.batchProcess;

@Slf4j
public class IndekserYtelserHandler {
    static final Map<YtelseMapping, BiConsumer<LoependeVedtak, BrukerinformasjonFraFil>> ytelsesSpesifikeFelter = new HashMap<>();

    static {
        ytelsesSpesifikeFelter.put(YtelseMapping.TILTAKSPENGER, IndekserYtelserHandler::settUtlopsdatoOgMndFasett);
        ytelsesSpesifikeFelter.put(YtelseMapping.DAGPENGER_MED_PERMITTERING_FISKEINDUSTRI, IndekserYtelserHandler::settDagpUke);
        ytelsesSpesifikeFelter.put(YtelseMapping.LONNSGARANTIMIDLER_DAGPENGER, IndekserYtelserHandler::settDagpUke);
        ytelsesSpesifikeFelter.put(YtelseMapping.ORDINARE_DAGPENGER, IndekserYtelserHandler::settDagpUke);
        ytelsesSpesifikeFelter.put(YtelseMapping.DAGPENGER_OVRIGE, IndekserYtelserHandler::settDagpUke);
        ytelsesSpesifikeFelter.put(YtelseMapping.AAP_MAXTID, exec(
                IndekserYtelserHandler::settUtlopsdatoOgMndFasett,
                IndekserYtelserHandler::settAapMaxtidUke
        ));
        ytelsesSpesifikeFelter.put(YtelseMapping.AAP_UNNTAK, exec(
                IndekserYtelserHandler::settUtlopsdatoOgMndFasett,
                IndekserYtelserHandler::settAapUnntakDagerIgjen
        ));
        ytelsesSpesifikeFelter.put(YtelseMapping.DAGPENGER_MED_PERMITTERING, exec(
                IndekserYtelserHandler::settPermittertDagpUke,
                IndekserYtelserHandler::settDagpUke
        ));
    }

    @Inject
    private PersistentOppdatering persistentOppdatering;

    @Inject
    private BrukerRepository brukerRepository;

    public synchronized void lagreYtelser(LoependeYtelser ytelser) {
        log.info("Sletter ytelsesdata fra DB");
        brukerRepository.slettYtelsesdata();

        batchProcess(10000, ytelser.getLoependeVedtakListe(), (vedtakListe) -> {
            LocalDateTime now = now();

            Map<String, Optional<String>> brukererIDB = brukererIDB(vedtakListe);

            Map<Boolean, List<Try<BrukerinformasjonFraFil>>> alleOppdateringer = vedtakListe
                    .stream()
                    .map((vedtak) -> brukererIDB.get(vedtak.getPersonident()).map((personId) -> Tuple.of(personId, vedtak)))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(this.lagBrukeroppdatering(now))
                    .collect(partitioningBy(Try::isSuccess));

            alleOppdateringer
                    .get(false)
                    .forEach((e) -> log.warn("Feil ved generering av brukeroppdatering: ", e.getCause()));

            List<BrukerOppdatering> dokumenter = alleOppdateringer
                    .get(true)
                    .stream()
                    .map(Try::get)
                    .collect(toList());

            log.info("Brukeroppdateringer laget. {} vellykkede, {} feilet", alleOppdateringer.get(true).size(), alleOppdateringer.get(false).size());
            persistentOppdatering.lagreBrukeroppdateringerIDB(dokumenter);
        });
        log.info("Lagring av ytelser ferdig!");
    }

    private Map<String, Optional<String>> brukererIDB(Collection<LoependeVedtak> loependeVedtak) {
        Set<String> personIdenter = loependeVedtak
                .stream()
                .map(LoependeVedtak::getPersonident)
                .collect(toSet());

        return brukerRepository.retrievePersonidFromFnrs(personIdenter);
    }

    private Function<Tuple2<String, LoependeVedtak>, Try<BrukerinformasjonFraFil>> lagBrukeroppdatering(LocalDateTime now) {
        return (Tuple2<String, LoependeVedtak> loependeVedtak) -> Try.of(() -> {
            String personId = loependeVedtak._1;
            LoependeVedtak vedtak = loependeVedtak._2;
            BrukerinformasjonFraFil brukerinfo = new BrukerinformasjonFraFil(personId);

            YtelseMapping ytelseMapping = YtelseMapping.of(vedtak).orElseThrow(() -> new RuntimeException(vedtak.toString()));
            brukerinfo.setYtelse(ytelseMapping);

            ytelsesSpesifikeFelter.get(ytelseMapping).accept(vedtak, brukerinfo);

            return brukerinfo;
        });
    }

    private static void settUtlopsdatoOgMndFasett(LoependeVedtak vedtak, BrukerinformasjonFraFil brukerinfo) {
        LocalDateTime utlopsdato = utlopsdato(vedtak);
        brukerinfo.setUtlopsdato(utlopsdato);
        ManedFasettMapping.finnManed(now(), utlopsdato).ifPresent(brukerinfo::setUtlopsdatoFasett);
    }

    private static void settAapMaxtidUke(LoependeVedtak vedtak, BrukerinformasjonFraFil brukerinfo) {
        int antallUkerIgjen = vedtak.getAaptellere().getAntallUkerIgjen().intValue();
        brukerinfo.setAapmaxtidUke(antallUkerIgjen);
        AAPMaxtidUkeFasettMapping.finnUkemapping(antallUkerIgjen).ifPresent(brukerinfo::setAapmaxtidUkeFasett);
    }

    private static void settAapUnntakDagerIgjen(LoependeVedtak vedtak, BrukerinformasjonFraFil brukerinfo) {
        int antallDagerIgjen = vedtak.getAaptellere().getAntallDagerIgjenUnntak().intValue();
        brukerinfo.setAapUnntakDagerIgjen(antallDagerIgjen);
        AAPUnntakUkerIgjenFasettMapping.finnUkeMapping(antallDagerIgjen).ifPresent(brukerinfo::setAapUnntakUkerIgjenFasett);
    }

    private static void settDagpUke(LoependeVedtak vedtak, BrukerinformasjonFraFil brukerinfo) {
        int antallUkerIgjen = vedtak.getDagpengetellere().getAntallUkerIgjen().intValue();
        brukerinfo.setDagputlopUke(antallUkerIgjen);
        DagpengerUkeFasettMapping.finnUkemapping(antallUkerIgjen).ifPresent(brukerinfo::setDagputlopUkeFasett);
    }

    private static void settPermittertDagpUke(LoependeVedtak vedtak, BrukerinformasjonFraFil brukerinfo) {
        int antallUkerIgjen = vedtak.getDagpengetellere().getAntallUkerIgjenUnderPermittering().intValue();
        brukerinfo.setPermutlopUke(antallUkerIgjen);
        DagpengerUkeFasettMapping.finnUkemapping(antallUkerIgjen).ifPresent(brukerinfo::setPermutlopUkeFasett);
    }

    @SafeVarargs
    private static <S, T> BiConsumer<S, T> exec(BiConsumer<S, T>... consumers) {
        return (s, t) -> {
            for (BiConsumer<S, T> consumer : consumers) {
                consumer.accept(s, t);
            }
        };
    }
}
