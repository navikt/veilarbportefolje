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
    int fodselsdagIMnd;
    String fodselsdato;
    String kjonn;
    boolean erInaktiv;

    public static Bruker of(SolrDocument document) {
        return new Bruker()
                .setFnr((String) document.get("fnr"))
                .setFornavn((String) document.get("fornavn"))
                .setEtternavn((String) document.get("etternavn"))
                .setVeilederId((String) document.get("veileder_id"))
                .setDiskresjonskode(getDiskresjonskode(document))
                .setEgenAnsatt( (Boolean) document.get("egen_ansatt"))
                .setErDoed( (Boolean) document.get("er_doed"))
                .setSikkerhetstiltak(getSikkerhetstiltak(document))
                .setFodselsdagIMnd((int) document.get("fodselsdag_i_mnd"))
                .setFodselsdato(document.get("fodselsdato").toString())
                .setKjonn((String) document.get("kjonn"))
                .setErInaktiv(erInaktivEllerDoed(document));
    }

    private static String getDiskresjonskode(SolrDocument document) {
        String kode6 = "6";
        String kode7 = "7";

        String diskresjonskode = (String) document.get("diskresjonskode");
        if (kode6.equals(diskresjonskode) || kode7.equals(diskresjonskode)) {
            return diskresjonskode;
        }
        return null;
    }

    private static List<String> getSikkerhetstiltak(SolrDocument document) {
        String kode = (String) document.get("sikkerhetstiltak");
        if (kode == null) {
            return emptyList();
        } else {
            return singletonList(kode);
        }
    }

    private static boolean erNyBruker(SolrDocument document) {
        return document.get("veileder_id") == null;
    }

    private static boolean erInaktivEllerDoed(SolrDocument document) {
        return "ISERV".equals(document.get("formidlingsgruppekode")) && !erNyBruker(document);
    }
}
