package no.nav.fo.routes;

import javaslang.Tuple;
import javaslang.Tuple2;
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

import javax.inject.Inject;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static java.time.LocalDateTime.now;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static java.util.stream.Collectors.toList;
import static no.nav.fo.domene.Utlopsdato.utlopsdato;
import static no.nav.fo.domene.Utlopsdato.utlopsdatoUtregning;
import static no.nav.fo.domene.YtelseMapping.AAP_MAXTID;


public class IndekserYtelserHandler {

    @Inject
    private SolrService solr;

    @Inject
    private BrukerRepository brukerRepository;

    public void indekser(LoependeYtelser ytelser) {
        LocalDateTime now = now();

        List<SolrInputDocument> dokumenter = ytelser.getLoependeVedtakListe()
                .stream()
                .map(this::brukerFinnesISolrIndeks)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(this.lagSolrDocument(now)).collect(toList());

        solr.addDocuments(dokumenter);
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
        };
    }
}
