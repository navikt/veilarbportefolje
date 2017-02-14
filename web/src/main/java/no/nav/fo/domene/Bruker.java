package no.nav.fo.domene;


import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.solr.common.SolrDocument;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

@Data
@Accessors(chain = true)
public class Bruker {
    String fnr;
    String fornavn;
    String etternavn;
    String veilederId;
    List<String> sikkerhetstiltak;
    String diskresjonskode;
    Boolean egenAnsatt;
    Boolean erDoed;

    public static Bruker of(SolrDocument document) {
        return new Bruker()
                .setFnr((String) document.get("fnr"))
                .setFornavn((String) document.get("fornavn"))
                .setEtternavn((String) document.get("etternavn"))
                .setVeilederId((String) document.get("veileder_id"))
                .setDiskresjonskode((String) document.get("diskresjonskode"))
                .setEgenAnsatt( (Boolean) document.get("egen_ansatt"))
                .setErDoed( (Boolean) document.get("er_doed"))
                .setSikkerhetstiltak(getSikkerhetstiltak(document));
    }

    private static List<String> getSikkerhetstiltak(SolrDocument document) {
        String kode = (String) document.get("sikkerhetstiltak");
        if (kode == null) {
            return emptyList();
        } else {
            return singletonList(kode);
        }
    }
}
