package no.nav.fo.filmottak.ytelser;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.database.PersistentOppdatering;
import no.nav.fo.domene.*;
import no.nav.fo.exception.FantIngenYtelseMappingException;
import no.nav.fo.util.MetricsUtils;
import no.nav.melding.virksomhet.loependeytelser.v1.LoependeVedtak;
import no.nav.melding.virksomhet.loependeytelser.v1.LoependeYtelser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.time.LocalDateTime.now;
import static java.util.stream.Collectors.*;
import static no.nav.fo.domene.Utlopsdato.utlopsdato;
import static no.nav.fo.util.MetricsUtils.timed;
import static no.nav.fo.util.StreamUtils.batchProcess;


public class IndekserYtelserHandler {
    static Logger logger = LoggerFactory.getLogger(IndekserYtelserHandler.class);
    static final Map<YtelseMapping, BiConsumer<LoependeVedtak, BrukerinformasjonFraFil>> ytelsesSpesifikeFelter = new HashMap<>();

    static {
        ytelsesSpesifikeFelter.put(YtelseMapping.TILTAKSPENGER, IndekserYtelserHandler::settUtlopsdatoOgMndFasett);
        ytelsesSpesifikeFelter.put(YtelseMapping.AAP_UNNTAK, IndekserYtelserHandler::settUtlopsdatoOgMndFasett);
        ytelsesSpesifikeFelter.put(YtelseMapping.DAGPENGER_OVRIGE, IndekserYtelserHandler::settDagpUke);
        ytelsesSpesifikeFelter.put(YtelseMapping.ORDINARE_DAGPENGER, IndekserYtelserHandler::settDagpUke);
        ytelsesSpesifikeFelter.put(YtelseMapping.AAP_MAXTID, (vedtak, brukerinfo) -> {
            settUtlopsdatoOgMndFasett(vedtak, brukerinfo);
            settAapMaxtidUke(vedtak, brukerinfo);
        });
        ytelsesSpesifikeFelter.put(YtelseMapping.DAGPENGER_MED_PERMITTERING, (vedtak, brukerinfo) -> {
            settPermittertDagpUke(vedtak, brukerinfo);
            settDagpUke(vedtak, brukerinfo);
        });
    }

    @Inject
    private PersistentOppdatering persistentOppdatering;

    @Inject
    private BrukerRepository brukerRepository;

    public synchronized void indekser(LoependeYtelser ytelser) {
        logger.info("Sletter ytelsesdata fra DB");
        MetricsUtils.timed("GR199.slettytelser", () -> {
            brukerRepository.slettYtelsesdata();
            return null;
        });

        batchProcess(10000, ytelser.getLoependeVedtakListe(), (vedtakListe) -> {
            LocalDateTime now = now();

            Map<String, Optional<String>> brukererIDB = brukererIDB(vedtakListe);

            Map<Boolean, List<Try<BrukerinformasjonFraFil>>> alleOppdateringer = timed("GR199.lagoppdatering", () -> {
                        return vedtakListe
                                .stream()
                                .map((vedtak) -> brukererIDB.get(vedtak.getPersonident()).map((personId) -> Tuple.of(personId, vedtak)))
                                .filter(Optional::isPresent)
                                .map(Optional::get)
                                .map(this.lagBrukeroppdatering(now))
                                .collect(partitioningBy(Try::isSuccess));
                    }
            );

            alleOppdateringer
                    .get(false)
                    .forEach((e) -> logger.warn("Feil ved generering av brukeroppdatering: ", e.getCause()));

            List<BrukerOppdatering> dokumenter = alleOppdateringer
                    .get(true)
                    .stream()
                    .map(Try::get)
                    .collect(toList());

            logger.info("Brukeroppdateringer laget. {} vellykkede, {} feilet", alleOppdateringer.get(true).size(), alleOppdateringer.get(false).size());
            timed("GR199.lagreOppdateringer", () -> {
                persistentOppdatering.lagreBrukeroppdateringerIDB(dokumenter);
                return null;
            });
        });
        logger.info("Lagring av ytelser ferdig!");
    }

    private Map<String, Optional<String>> brukererIDB(Collection<LoependeVedtak> vedtaks) {
        Supplier<Map<String, Optional<String>>> personIdSupplier = () -> brukerRepository
                .retrievePersonidFromFnrs(vedtaks
                        .stream()
                        .map(LoependeVedtak::getPersonident)
                        .collect(toSet())
                );

        return timed("GR199.brukersjekk", personIdSupplier);
    }

    private Function<Tuple2<String, LoependeVedtak>, Try<BrukerinformasjonFraFil>> lagBrukeroppdatering(LocalDateTime now) {
        return (Tuple2<String, LoependeVedtak> loependeVedtak) -> Try.of(() -> {
            String personId = loependeVedtak._1;
            LoependeVedtak vedtak = loependeVedtak._2;
            BrukerinformasjonFraFil brukerinfo = new BrukerinformasjonFraFil(personId);

            YtelseMapping ytelseMapping = YtelseMapping.of(vedtak).orElseThrow(() -> new FantIngenYtelseMappingException(vedtak));
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
}
