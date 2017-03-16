package no.nav.fo.routes;

import no.nav.fo.domene.YtelseMapping;
import no.nav.fo.exception.FantIngenYtelseMappingException;
import no.nav.fo.exception.YtelseManglerTOMDatoException;
import no.nav.fo.service.SolrService;
import no.nav.melding.virksomhet.loependeytelser.v1.Dagpengetellere;
import no.nav.melding.virksomhet.loependeytelser.v1.LoependeVedtak;
import no.nav.melding.virksomhet.loependeytelser.v1.LoependeYtelser;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;

import javax.inject.Inject;
import java.time.DayOfWeek;
import java.time.LocalDate;


public class IndekserHandler {

    public static final String DAGPENGER = "DAGP";

    @Inject
    SolrService solr;

    public void indekser(LoependeYtelser ytelser) {
        ytelser
                .getLoependeVedtakListe()
                .stream()
                .filter(this::brukerFinnesISolrIndeks)
                .map(this::lagSolrDocument)
                .forEach(this::lagreSolrDokument);
    }

    private boolean brukerFinnesISolrIndeks(LoependeVedtak loependeVedtak) {
        return solr.hentBruker(loependeVedtak.getPersonident()).isPresent();
    }

    private SolrInputDocument lagSolrDocument(LoependeVedtak loependeVedtak) {
        SolrInputDocument dokument = new SolrInputDocument();

        YtelseMapping ytelseMapping = YtelseMapping.of(loependeVedtak).orElseThrow(() -> new FantIngenYtelseMappingException(loependeVedtak));
        LocalDate utlopsdato = utlopsdato(loependeVedtak);

        dokument.put("ytelse", new SolrInputField(ytelseMapping.toString()));
        dokument.put("utlopsdato", new SolrInputField(utlopsdato.toString()));
        dokument.put("utlopsdato_mnd", new SolrInputField(String.valueOf(utlopsdato.getMonthValue())));


        return dokument;
    }

    private LocalDate utlopsdato(LoependeVedtak loependeVedtak) {
        if (loependeVedtak.getVedtaksperiode().getTom() != null) {
            return loependeVedtak.getVedtaksperiode().getTom().toGregorianCalendar().toZonedDateTime().toLocalDate();
        }

        if (!DAGPENGER.equals(loependeVedtak.getSakstypeKode())) {
            throw new YtelseManglerTOMDatoException(loependeVedtak);
        }

        return utlopsdatoUtregning(LocalDate.now(), loependeVedtak.getDagpengetellere());
    }

    static LocalDate utlopsdatoUtregning(LocalDate now, Dagpengetellere dagpengetellere) {
        LocalDate utlopsdato = now
                .minusDays(1)
                .plusWeeks(dagpengetellere.getAntallUkerIgjen().intValue())
                .plusDays(dagpengetellere.getAntallDagerIgjen().intValue());

        while (utlopsdato.getDayOfWeek() == DayOfWeek.SATURDAY || utlopsdato.getDayOfWeek() == DayOfWeek.SUNDAY) {
            utlopsdato = utlopsdato.plusDays(1);
        }

        return utlopsdato;
    }

    private void lagreSolrDokument(SolrInputDocument dokument) {
//        return (Consumer<LoependeVedtak>) loependeVedtak -> {
//            loependeVedtak.get
//        }
    }
}
