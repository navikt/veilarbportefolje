package no.nav.fo.domene;


import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.solr.common.SolrDocument;
import org.joda.time.LocalDate;

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
    int fodselsdag_i_mnd;
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
                .setFodselsdag_i_mnd((int) document.get("fodselsdag_i_mnd"))
                .setFodselsdato((String) document.get("fodselsdato"))
                .setKjonn((String) document.get("fodselsdato"))
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
        return document.get("formidlingsgruppekode").equals("ISERV") && !erNyBruker(document);
    }
}
