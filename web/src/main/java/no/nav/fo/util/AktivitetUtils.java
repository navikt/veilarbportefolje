package no.nav.fo.util;

import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.Aktivitet.AktivitetData;
import no.nav.fo.domene.Aktivitet.AktivitetFullfortStatuser;
import org.apache.solr.common.SolrInputDocument;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Collections.*;

public class AktivitetUtils {
    public static Boolean erBrukersAktivitetAktiv(List<String> aktivitetStatusListe) {
        return !aktivitetStatusListe
                .stream()
                .filter(AktivitetFullfortStatuser::contains)
                .collect(Collectors.toList())
                .isEmpty();
    }

    public static void applyAktivitetStatuser(List<SolrInputDocument> dokumenter, BrukerRepository brukerRepository) {
        for(SolrInputDocument document : dokumenter) {
            String personid = (String) document.get("person_id").getValue();
            Map<String, Timestamp> statusMap = brukerRepository.getAktivitetStatusMap(personid);
            AktivitetData.aktivitetTyperList.forEach( (type) -> document.addField(type.toString(), statusMap.get(type.toString())));
        }
    }

    public static void applyAktivitetStatuser(SolrInputDocument dokument, BrukerRepository brukerRepository) {
        applyAktivitetStatuser(singletonList(dokument), brukerRepository);
    }
}
