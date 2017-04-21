package no.nav.fo.consumer;

import javaslang.Tuple;
import javaslang.Tuple2;
import javaslang.control.Try;
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.time.LocalDateTime.now;
import static java.util.stream.Collectors.*;
import static no.nav.fo.domene.Utlopsdato.utlopsdato;
import static no.nav.fo.domene.Utlopsdato.utlopsdatoUtregning;
import static no.nav.fo.domene.YtelseMapping.AAP_MAXTID;
import static no.nav.fo.util.MetricsUtils.timed;
import static no.nav.fo.util.StreamUtils.batchProcess;


public class IndekserYtelserHandler {
    static Logger logger = LoggerFactory.getLogger(IndekserYtelserHandler.class);

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
            timed("GR199.lagreOppdateringer", () -> { persistentOppdatering.lagreBrukeroppdateringerIDB(dokumenter); return null; });
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

            LocalDateTime utlopsdato = utlopsdato(now, vedtak);

            brukerinfo.setYtelse(ytelseMapping);
            brukerinfo.setUtlopsdato(utlopsdato);

            ManedMapping.finnManed(now, utlopsdato).ifPresent(brukerinfo::setUtlopsdatoFasett);

            if (AAP_MAXTID.sjekk.test(vedtak)) {
                LocalDateTime maxtid = utlopsdatoUtregning(now, vedtak.getAaptellere());
                brukerinfo.setAapMaxtid(maxtid);

                KvartalMapping.finnKvartal(now, maxtid).ifPresent(brukerinfo::setAapMaxtidFasett);
            }

            return brukerinfo;
        });
    }
}
