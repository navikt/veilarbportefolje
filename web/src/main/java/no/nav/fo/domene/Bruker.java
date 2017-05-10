package no.nav.fo.domene;

import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.solr.common.SolrDocument;

import java.time.*;
import java.util.Date;
import java.util.List;

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Data
@Accessors(chain = true)
public class Bruker {
    String fnr;
    String fornavn;
    String etternavn;
    String veilederId;
    List<String> sikkerhetstiltak;
    String diskresjonskode;
    boolean egenAnsatt;
    boolean erDoed;
    int fodselsdagIMnd;
    LocalDateTime fodselsdato;
    String kjonn;
    YtelseMapping ytelse;
    LocalDateTime utlopsdato;
    ManedMapping utlopsdatoFasett;
    LocalDateTime aapMaxtid;
    KvartalMapping aapMaxtidFasett;


    public static Bruker of(SolrDocument document) {
        return new Bruker()
                .setFnr((String) document.get("fnr"))
                .setFornavn((String) document.get("fornavn"))
                .setEtternavn((String) document.get("etternavn"))
                .setVeilederId((String) document.get("veileder_id"))
                .setDiskresjonskode(getDiskresjonskode(document))
                .setEgenAnsatt((Boolean) document.get("egen_ansatt"))
                .setErDoed((Boolean) document.get("er_doed"))
                .setSikkerhetstiltak(getSikkerhetstiltak(document))
                .setFodselsdagIMnd((int) document.get("fodselsdag_i_mnd"))
                .setFodselsdato(dato((Date)document.get("fodselsdato")))
                .setKjonn((String) document.get("kjonn"))
                .setYtelse(YtelseMapping.of((String) document.get("ytelse")))
                .setUtlopsdato(dato((Date) document.get("utlopsdato")))
                .setUtlopsdatoFasett(ManedMapping.of((String) document.get("utlopsdato_mnd_fasett")))
                .setAapMaxtid(dato((Date) document.get("aap_maxtid")))
                .setAapMaxtidFasett(KvartalMapping.of((String) document.get("aap_maxtid_fasett")));
    }

    static LocalDateTime dato(Date dato) {
        if (dato == null) {
            return null;
        }
        return LocalDateTime.ofInstant(dato.toInstant(), ZoneId.systemDefault());
    }


    private static String getDiskresjonskode(SolrDocument document) {
        String diskresjonskode = (String) document.get("diskresjonskode");

        if ("6".equals(diskresjonskode) || "7".equals(diskresjonskode)) {
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

    public boolean erKonfidensiell() {
        return (isNotEmpty(this.diskresjonskode)) || (this.egenAnsatt);

    }
}
