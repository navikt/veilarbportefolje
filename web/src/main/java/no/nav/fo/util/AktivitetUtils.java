package no.nav.fo.util;

import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.AktivitetData;
import org.apache.solr.common.SolrInputDocument;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.*;

public class AktivitetUtils {
    public static Boolean erBrukersAktivitetAktiv(List<String> aktivitetStatusListe, List<String> fullførsteStatuser) {
        return !aktivitetStatusListe
                .stream()
                .filter( status -> !fullførsteStatuser.contains(status))
                .collect(Collectors.toList())
                .isEmpty();
    }

    public static void applyAktivitetStatuser(List<SolrInputDocument> dokumenter, BrukerRepository brukerRepository, Set<String> aktivitettyperSet) {
        for(SolrInputDocument document : dokumenter) {
            String personid = (String) document.get("person_id").getValue();
            Map<String, Boolean> statusMap = brukerRepository.getAktivitetStatusMap(personid, AktivitetData.aktivitettyperSet);
            AktivitetData.aktivitettyperSet.forEach( (type) -> document.addField(type, statusMap.get(type)));
        }
    }

    public static void applyAktivitetStatuser(SolrInputDocument dokument, BrukerRepository brukerRepository, Set<String> aktivitettyperSet) {
        applyAktivitetStatuser(singletonList(dokument), brukerRepository, aktivitettyperSet);
    }
}
