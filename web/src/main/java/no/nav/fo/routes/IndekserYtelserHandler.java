package no.nav.fo.routes;

import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.YtelseMapping;
import no.nav.fo.exception.FantIkkePersonIdException;
import no.nav.fo.exception.FantIngenYtelseMappingException;
import no.nav.fo.exception.YtelseManglerTOMDatoException;
import no.nav.fo.service.SolrService;
import no.nav.melding.virksomhet.loependeytelser.v1.Dagpengetellere;
import no.nav.melding.virksomhet.loependeytelser.v1.LoependeVedtak;
import no.nav.melding.virksomhet.loependeytelser.v1.LoependeYtelser;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Collections;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;


public class IndekserYtelserHandler {

    public static final String DAGPENGER = "DAGP";

    @Inject
    SolrService solr;

    @Inject
    BrukerRepository brukerRepository;

    public void indekser(LoependeYtelser ytelser) {
        ytelser
                .getLoependeVedtakListe()
                .stream()
                .filter(this::brukerFinnesISolrIndeks)
                .map(this::lagSolrDocument)
                .map(Collections::singletonList)
                .forEach(solr::addDocuments);
    }

    private boolean brukerFinnesISolrIndeks(LoependeVedtak loependeVedtak) {
        return solr.hentBruker(loependeVedtak.getPersonident()).isPresent();
    }

    private SolrInputDocument lagSolrDocument(LoependeVedtak loependeVedtak) {
        SolrInputDocument dokument = new SolrInputDocument();

        YtelseMapping ytelseMapping = YtelseMapping.of(loependeVedtak).orElseThrow(() -> new FantIngenYtelseMappingException(loependeVedtak));
        LocalDateTime utlopsdato = utlopsdato(loependeVedtak);

        String personId = brukerRepository.retrievePersonidFromFnr(loependeVedtak.getPersonident())
                .map(BigDecimal::intValue)
                .map(x -> Integer.toString(x))
                .orElseThrow(() -> new FantIkkePersonIdException(loependeVedtak.getPersonident()));

        dokument.put("person_id", new SolrInputField(personId));
        dokument.put("fnr", new SolrInputField(loependeVedtak.getPersonident()));
        dokument.put("ytelse", new SolrInputField(ytelseMapping.toString()));
        dokument.put("utlopsdato", new SolrInputField(utlopsdato.format(ISO_INSTANT)));
        dokument.put("utlopsdato_mnd", new SolrInputField(String.valueOf(utlopsdato.getMonthValue())));

        return dokument;
    }

    private LocalDateTime utlopsdato(LoependeVedtak loependeVedtak) {
        if (loependeVedtak.getVedtaksperiode().getTom() != null) {
            return loependeVedtak.getVedtaksperiode().getTom().toGregorianCalendar().toZonedDateTime().toLocalDateTime();
        }

        if (!DAGPENGER.equals(loependeVedtak.getSakstypeKode())) {
            throw new YtelseManglerTOMDatoException(loependeVedtak);
        }

        return utlopsdatoUtregning(LocalDateTime.now(), loependeVedtak.getDagpengetellere());
    }

    static LocalDateTime utlopsdatoUtregning(LocalDateTime now, Dagpengetellere dagpengetellere) {
        LocalDateTime utlopsdato = now
                .minusDays(1)
                .plusWeeks(dagpengetellere.getAntallUkerIgjen().intValue())
                .plusDays(dagpengetellere.getAntallDagerIgjen().intValue());

        while (utlopsdato.getDayOfWeek() == DayOfWeek.SATURDAY || utlopsdato.getDayOfWeek() == DayOfWeek.SUNDAY) {
            utlopsdato = utlopsdato.plusDays(1);
        }

        return utlopsdato;
    }
}
