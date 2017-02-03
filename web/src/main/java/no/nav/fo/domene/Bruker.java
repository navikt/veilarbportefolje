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
    String fodselsnr;
    String fornavn;
    String etternavn;
    String veilderId;
    List<String> sikkerhetstiltak;
    String diskresjonskode;
    Boolean egenAnsatt;

    public static Bruker of(SolrDocument document) {
        return new Bruker()
                .setFodselsnr((String) document.get("fodselsnr"))
                .setFornavn((String) document.get("fornavn"))
                .setEtternavn((String) document.get("etternavn"))
                .setVeilderId((String) document.get("veileder_id"))
                .setDiskresjonskode((String) document.get("fr_kode"))
                .setEgenAnsatt(Boolean.parseBoolean((String) document.get("sperret_ansatt")))
                .setSikkerhetstiltak(getSikkerhetstiltak(document));
    }

    private static List<String> getSikkerhetstiltak(SolrDocument document) {
        String kode = (String) document.get("sikkerhetstiltak_type_kode");
        if (kode == null) {
            return emptyList();
        } else {
            return singletonList(kode);
        }
    }
}
