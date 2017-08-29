package no.nav.fo.domene;

import io.vavr.control.Try;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.solr.common.SolrDocument;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static no.nav.fo.util.DateUtils.timestampFromISO8601;
import static no.nav.fo.util.DateUtils.toLocalDateTime;
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
    Arbeidsliste arbeidsliste;
    LocalDateTime venterPaSvarFraNAV;
    LocalDateTime venterPaSvarFraBruker;
    LocalDateTime nyesteUtlopteAktivitet;
    List<String> brukertiltak;
    Map<String, Timestamp> aktiviteter;

    @SuppressWarnings("unchecked")
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
                .setFodselsdato(toLocalDateTime((Date) document.get("fodselsdato")))
                .setKjonn((String) document.get("kjonn"))
                .setYtelse(YtelseMapping.of((String) document.get("ytelse")))
                .setUtlopsdato(toLocalDateTime((Date) document.get("utlopsdato")))
                .setUtlopsdatoFasett(ManedMapping.of((String) document.get("utlopsdato_mnd_fasett")))
                .setAapMaxtid(toLocalDateTime((Date) document.get("aap_maxtid")))
                .setAapMaxtidFasett(KvartalMapping.of((String) document.get("aap_maxtid_fasett")))
                .setArbeidsliste(Arbeidsliste.of(document))
                .setVenterPaSvarFraNAV(toLocalDateTime((Date) document.get("venterpasvarfranav")))
                .setVenterPaSvarFraBruker(toLocalDateTime((Date) document.get("venterpasvarfrabruker")))
                .setNyesteUtlopteAktivitet(toLocalDateTime((Date) document.get("nyesteutlopteaktivitet")))
                .setBrukertiltak(getBrukertiltak(document))
                .setAktiviteter(lagAktiviteterMap( (List) document.get("aktiviteter"), (String) document.get("aktiviteter_utlopsdato_json")))
                ;
    }

    private static Map<String, Timestamp> lagAktiviteterMap(List<String> aktiviteter, String aktiviteterUtlopsdatoJSON) {
        if(Objects.isNull(aktiviteter)) {
            return null;
        }
        JSONObject jsonObject = Objects.nonNull(aktiviteterUtlopsdatoJSON) ? new JSONObject(aktiviteterUtlopsdatoJSON): null;
        Map<String, Timestamp> map = new HashMap<>();
        aktiviteter.forEach( aktivitet -> map.put(aktivitet,toTimestampOrNull(jsonObject,aktivitet)));
        return map;
    }

    private static Timestamp toTimestampOrNull(JSONObject jsonObject, String key) {
        Try<Timestamp> timestampTry = Try.of(() -> timestampFromISO8601((String) jsonObject.get(key)));
        return timestampTry.getOrNull();
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

    private static List<String> getBrukertiltak(SolrDocument document) {
        String tiltak = (String) document.get("tiltak");
        if (tiltak == null) {
            return emptyList();
        } else {
            return singletonList(tiltak);
        }
    }

    public boolean erKonfidensiell() {
        return (isNotEmpty(this.diskresjonskode)) || (this.egenAnsatt);

    }

    public ZonedDateTime getArbeidslisteFrist() {
        return arbeidsliste.getFrist();
    }
}
