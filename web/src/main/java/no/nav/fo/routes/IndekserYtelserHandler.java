package no.nav.fo.routes;

import javaslang.Tuple;
import javaslang.Tuple2;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.KvartalMapping;
import no.nav.fo.domene.ManedMapping;
import no.nav.fo.domene.Utlopsdato;
import no.nav.fo.domene.YtelseMapping;
import no.nav.fo.exception.FantIngenYtelseMappingException;
import no.nav.fo.exception.YtelseManglerTOMDatoException;
import no.nav.fo.service.SolrService;
import no.nav.melding.virksomhet.loependeytelser.v1.AAPtellere;
import no.nav.melding.virksomhet.loependeytelser.v1.Dagpengetellere;
import no.nav.melding.virksomhet.loependeytelser.v1.LoependeVedtak;
import no.nav.melding.virksomhet.loependeytelser.v1.LoependeYtelser;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;

import static java.time.LocalDateTime.now;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static no.nav.fo.domene.Utlopsdato.utlopsdato;
import static no.nav.fo.domene.Utlopsdato.utlopsdatoUtregning;


public class IndekserYtelserHandler {

    @Inject
    private SolrService solr;

    @Inject
    private BrukerRepository brukerRepository;

    public void indekser(LoependeYtelser ytelser) {
        LocalDateTime now = now();

        ytelser.getLoependeVedtakListe()
                .stream()
                .map(this::brukerFinnesISolrIndeks)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(this.lagSolrDocument(now))
                .map(Collections::singletonList)
                .forEach(solr::addDocuments);
    }


    private Optional<Tuple2<String, LoependeVedtak>> brukerFinnesISolrIndeks(LoependeVedtak loependeVedtak) {
        return brukerRepository.retrievePersonidFromFnr(loependeVedtak.getPersonident())
                .map(BigDecimal::intValue)
                .map(x -> Integer.toString(x))
                .map((id) -> Tuple.of(id, loependeVedtak));
    }

    private Function<Tuple2<String, LoependeVedtak>, SolrInputDocument> lagSolrDocument(LocalDateTime now) {
        return (Tuple2<String, LoependeVedtak> loependeVedtak) -> {
            SolrInputDocument dokument = new SolrInputDocument();

            String personId = loependeVedtak._1;
            LoependeVedtak vedtak = loependeVedtak._2;

            YtelseMapping ytelseMapping = YtelseMapping.of(vedtak).orElseThrow(() -> new FantIngenYtelseMappingException(vedtak));

            LocalDateTime utlopsdato = utlopsdato(now, vedtak).atOffset(ZoneOffset.of(""));

            dokument.put("person_id", new SolrInputField(personId));
            dokument.put("fnr", new SolrInputField(vedtak.getPersonident()));
            dokument.put("ytelse", new SolrInputField(ytelseMapping.toString()));
            dokument.put("utlopsdato", new SolrInputField(utlopsdato.format(ISO_INSTANT)));

            ManedMapping.finnManed(now, utlopsdato).ifPresent((mndMapping) -> {
                dokument.put("utlopsdato_mnd_fasett", new SolrInputField(mndMapping.toString()));
            });

            if ("AAP".equals(vedtak.getSakstypeKode())) {
                LocalDateTime maxtid = utlopsdatoUtregning(now, vedtak.getAaptellere());
                dokument.put("aap_maxtid", new SolrInputField(maxtid.toString()));

                KvartalMapping.finnKvartal(now, maxtid).ifPresent((kvartalMapping -> {
                    dokument.put("aap_maxtid_fasettert", new SolrInputField(kvartalMapping.toString()));
                }));
            }

            return dokument;
        };
    }


}
