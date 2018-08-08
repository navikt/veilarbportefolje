package no.nav.fo.domene;

import io.vavr.control.Option;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.solr.common.SolrDocument;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static no.nav.fo.util.DateUtils.*;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Slf4j
@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class Bruker {
    String fnr;
    String fornavn;
    String etternavn;
    String veilederId;
    List<String> sikkerhetstiltak;
    String diskresjonskode;
    boolean egenAnsatt;
    boolean nyForVeileder;
    boolean nyForEnhet;
    boolean trengerVurdering;
    boolean erDoed;
    String manuellBrukerStatus;
    int fodselsdagIMnd;
    LocalDateTime fodselsdato;
    String kjonn;
    YtelseMapping ytelse;
    LocalDateTime utlopsdato;
    ManedFasettMapping utlopsdatoFasett;
    Integer dagputlopUke;
    DagpengerUkeFasettMapping dagputlopUkeFasett;
    Integer permutlopUke;
    DagpengerUkeFasettMapping permutlopUkeFasett;
    Integer aapmaxtidUke;
    AAPMaxtidUkeFasettMapping aapmaxtidUkeFasett;
    AAPUnntakUkerIgjenFasettMapping aapUnntakUkerIgjenFasett;
    Integer aapUnntakUkerIgjen;
    Arbeidsliste arbeidsliste;
    LocalDateTime venterPaSvarFraNAV;
    LocalDateTime venterPaSvarFraBruker;
    LocalDateTime nyesteUtlopteAktivitet;
    LocalDateTime aktivitetStart;
    LocalDateTime nesteAktivitetStart;
    LocalDateTime forrigeAktivitetStart;
    List<String> brukertiltak;
    Map<String, Timestamp> aktiviteter = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static Bruker of(SolrDocument document) {
        return new Bruker()
                .setFnr((String) document.get("fnr"))
                .setNyForEnhet(isNyForEnhet(document))
                .setNyForVeileder(isNyForVeileder(document))
                .setTrengerVurdering(defaultBool(document, "trenger_vurdering", false))
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
                .setUtlopsdatoFasett(ManedFasettMapping.of((String) document.get("utlopsdatofasett")))
                .setDagputlopUke((Integer) document.get("dagputlopuke"))
                .setDagputlopUkeFasett(DagpengerUkeFasettMapping.of((String) document.get("dagputlopukefasett")))
                .setPermutlopUke((Integer) document.get("permutlopuke"))
                .setPermutlopUkeFasett(DagpengerUkeFasettMapping.of((String) document.get("permutlopukefasett")))
                .setAapmaxtidUke((Integer) document.get("aapmaxtiduke"))
                .setAapmaxtidUkeFasett(AAPMaxtidUkeFasettMapping.of((String) document.get("aapmaxtidukefasett")))
                .setAapUnntakUkerIgjen((Integer) document.get("aapunntakukerigjen"))
                .setAapUnntakUkerIgjenFasett(AAPUnntakUkerIgjenFasettMapping.of((String) document.get("aapunntakukerigjenfasett")))
                .setArbeidsliste(Arbeidsliste.of(document))
                .setVenterPaSvarFraNAV(toLocalDateTime((Date) document.get("venterpasvarfranav")))
                .setVenterPaSvarFraBruker(toLocalDateTime((Date) document.get("venterpasvarfrabruker")))
                .setNyesteUtlopteAktivitet(toLocalDateTime((Date) document.get("nyesteutlopteaktivitet")))
                .setAktivitetStart(toLocalDateTime((Date) document.get("aktivitet_start")))
                .setNesteAktivitetStart(toLocalDateTime((Date) document.get("neste_aktivitet_start")))
                .setForrigeAktivitetStart(toLocalDateTime((Date) document.get("forrige_aktivitet_start")))
                .setBrukertiltak(getBrukertiltak(document))
                .setManuellBrukerStatus((String) document.get("manuell_bruker"))
                .addAktivitetUtlopsdato("tiltak", dateToTimestamp((Date) document.get("aktivitet_tiltak_utlopsdato")))
                .addAktivitetUtlopsdato("behandling", dateToTimestamp((Date) document.get("aktivitet_behandling_utlopsdato")))
                .addAktivitetUtlopsdato("sokeavtale", dateToTimestamp((Date) document.get("aktivitet_sokeavtale_utlopsdato")))
                .addAktivitetUtlopsdato("stilling", dateToTimestamp((Date) document.get("aktivitet_stilling_utlopsdato")))
                .addAktivitetUtlopsdato("ijobb", dateToTimestamp((Date) document.get("aktivitet_ijobb_utlopsdato")))
                .addAktivitetUtlopsdato("samtalereferat", dateToTimestamp((Date) document.get("aktivitet_samtalereferat_utlopsdato")))
                .addAktivitetUtlopsdato("egen", dateToTimestamp((Date) document.get("aktivitet_egen_utlopsdato")))
                .addAktivitetUtlopsdato("gruppeaktivitet", dateToTimestamp((Date) document.get("aktivitet_gruppeaktivitet_utlopsdato")))
                .addAktivitetUtlopsdato("mote", dateToTimestamp((Date) document.get("aktivitet_mote_utlopsdato")))
                .addAktivitetUtlopsdato("utdanningaktivitet", dateToTimestamp((Date) document.get("aktivitet_utdanningaktivitet_utlopsdato")));
    }

    private static boolean isNyForEnhet(SolrDocument document) {
        return Optional. //FO-610 rydde
                ofNullable((Boolean) document.get("har_veileder_fra_enhet"))
                .map(a -> !a)
                .orElse(
                        Optional
                                .ofNullable((Boolean) document.get("ny_for_enhet"))
                                .orElse(false));

    }

    private static boolean isNyForVeileder(SolrDocument document) {
        return defaultBool(document, "ny_for_veileder", false);
    }

    private static boolean defaultBool(SolrDocument document, String field, boolean defaultValue) {
        return Option.of(((Boolean) document.get(field)))
                .getOrElse(defaultValue);
    }

    private Bruker addAktivitetUtlopsdato(String type, Timestamp utlopsdato) {
        if(Objects.isNull(utlopsdato) || isRandomFutureDate(utlopsdato)) {
            return this;
        }
        aktiviteter.put(type, utlopsdato);
        return this;
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
        List<String> tiltak = (List<String>) document.get("tiltak");

        if (Objects.isNull(tiltak)) {
            return emptyList();
        } else {
            return tiltak;
        }
    }

    public boolean erKonfidensiell() {
        return (isNotEmpty(this.diskresjonskode)) || (this.egenAnsatt);
    }
}
