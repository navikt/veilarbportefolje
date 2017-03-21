package no.nav.fo.consumer;

import javaslang.Tuple;
import javaslang.Tuple2;
import javaslang.control.Try;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.KvartalMapping;
import no.nav.fo.domene.ManedMapping;
import no.nav.fo.domene.YtelseMapping;
import no.nav.fo.exception.FantIngenYtelseMappingException;
import no.nav.fo.service.SolrService;
import no.nav.melding.virksomhet.loependeytelser.v1.LoependeVedtak;
import no.nav.melding.virksomhet.loependeytelser.v1.LoependeYtelser;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.time.LocalDateTime.now;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static java.util.stream.Collectors.*;
import static no.nav.fo.domene.Utlopsdato.utlopsdato;
import static no.nav.fo.domene.Utlopsdato.utlopsdatoUtregning;
import static no.nav.fo.domene.YtelseMapping.AAP_MAXTID;
import static no.nav.fo.util.StreamUtils.batchProcess;
import static no.nav.fo.util.MetricsUtils.timed;


public class IndekserYtelserHandler {
    static Logger logger = LoggerFactory.getLogger(IndekserYtelserHandler.class);

    @Inject
    private SolrService solr;

    @Inject
    private BrukerRepository brukerRepository;

    public synchronized void indekser(LoependeYtelser ytelser) {
        batchProcess(10000, ytelser.getLoependeVedtakListe(), (vedtaks) -> {
            LocalDateTime now = now();

            Map<String, Optional<String>> brukererIDB = brukererIDB(vedtaks);

            Map<Boolean, List<Try<SolrInputDocument>>> alleDokumenter = timed("GR199.lagsolrdocument", () -> vedtaks
                    .stream()
                    .map((vedtak) -> brukererIDB.get(vedtak.getPersonident()).map((fnr) -> Tuple.of(fnr, vedtak)))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(this.lagSolrDocument(now))
                    .collect(partitioningBy(Try::isSuccess))
            );

            alleDokumenter
                    .get(false)
                    .forEach((e) -> logger.error("Feil ved generering av solr-dokument: ", e.getCause()));

            List<SolrInputDocument> dokumenter = alleDokumenter
                    .get(true)
                    .stream()
                    .map(Try::get)
                    .collect(toList());

            logger.info("Solr-dokumenter laget. {} vellykkede, {} feilet", alleDokumenter.get(true).size(), alleDokumenter.get(false).size());
            timed("GR199.addDocuments", () -> solr.addDocuments(dokumenter));
        });
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

    private Function<Tuple2<String, LoependeVedtak>, Try<SolrInputDocument>> lagSolrDocument(LocalDateTime now) {
        return (Tuple2<String, LoependeVedtak> loependeVedtak) -> Try.of(() -> {
            SolrInputDocument dokument = new SolrInputDocument();

            String personId = loependeVedtak._1;
            LoependeVedtak vedtak = loependeVedtak._2;

            YtelseMapping ytelseMapping = YtelseMapping.of(vedtak).orElseThrow(() -> new FantIngenYtelseMappingException(vedtak));

            LocalDateTime utlopsdato = utlopsdato(now, vedtak);

            dokument.put("person_id", new SolrInputField(personId));
            dokument.put("fnr", new SolrInputField(vedtak.getPersonident()));
            dokument.put("ytelse", new SolrInputField(ytelseMapping.toString()));
            dokument.put("utlopsdato", new SolrInputField(utlopsdato.atZone(ZoneId.of("Europe/Oslo")).format(ISO_INSTANT)));

            ManedMapping.finnManed(now, utlopsdato).ifPresent((mndMapping) -> {
                dokument.put("utlopsdato_mnd_fasett", new SolrInputField(mndMapping.toString()));
            });

            if (AAP_MAXTID.sjekk.test(vedtak)) {
                LocalDateTime maxtid = utlopsdatoUtregning(now, vedtak.getAaptellere());
                dokument.put("aap_maxtid", new SolrInputField(maxtid.toString()));

                KvartalMapping.finnKvartal(now, maxtid).ifPresent((kvartalMapping -> {
                    dokument.put("aap_maxtid_fasettert", new SolrInputField(kvartalMapping.toString()));
                }));
            }

            return dokument;
        });
    }
}
