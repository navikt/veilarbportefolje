package no.nav.fo.domene;


import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.solr.common.SolrDocument;

import java.util.List;

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
    boolean sperretAnsatt;

    public static Bruker of(SolrDocument document) {
        return new Bruker()
                .setFodselsnr((String) document.get("fodselsnr"))
                .setFornavn((String) document.get("fornavn"))
                .setVeilderId((String) document.get("veileder_id"))
                .setSperretAnsatt(Boolean.parseBoolean((String) document.get("sperret_ansatt")))
                .setSikkerhetstiltak(singletonList((String) document.get("sikkerhetstiltak_type_kode")));
    }
}
