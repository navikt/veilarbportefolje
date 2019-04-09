package no.nav.fo.veilarbportefolje.domene;

import io.vavr.control.Option;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbportefolje.indeksering.domene.OppfolgingsBruker;
import no.nav.fo.veilarbportefolje.util.OppfolgingUtils;
import org.apache.solr.common.SolrDocument;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static no.nav.common.utils.CollectionUtils.toList;
import static no.nav.fo.veilarbportefolje.util.DateUtils.*;
import static no.nav.fo.veilarbportefolje.util.OppfolgingUtils.vurderingsBehov;
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
    VurderingsBehov vurderingsBehov;
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
    boolean erSykmeldtMedArbeidsgiver;

    @SuppressWarnings("unchecked")
    public static Bruker of(SolrDocument document) {
        String formidlingsgruppekode = (String) document.get("formidlingsgruppekode");
        String kvalifiseringsgruppekode = (String) document.get("kvalifiseringsgruppekode");

        return new Bruker()
                .setFnr((String) document.get("fnr"))
                .setNyForEnhet(isNyForEnhet(document))
                .setNyForVeileder(isNyForVeileder(document))
                .setTrengerVurdering(defaultBool(document, "trenger_vurdering", false))
                .setErSykmeldtMedArbeidsgiver(OppfolgingUtils.erSykmeldtMedArbeidsgiver(formidlingsgruppekode, kvalifiseringsgruppekode))
                .setVurderingsBehov(vurderingsBehov(formidlingsgruppekode, kvalifiseringsgruppekode))
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

    public static Bruker of(OppfolgingsBruker bruker) {

        String formidlingsgruppekode = bruker.getFormidlingsgruppekode();
        String kvalifiseringsgruppekode = bruker.getKvalifiseringsgruppekode();
        String sikkerhetstiltak = bruker.getSikkerhetstiltak();
        String diskresjonskode = bruker.getDiskresjonskode();

        return new Bruker()
                .setFnr(bruker.getFnr())
                .setNyForEnhet(bruker.isNy_for_enhet())
                .setNyForVeileder(bruker.isNy_for_veileder())
                .setTrengerVurdering(bruker.isTrenger_vurdering())
                .setErSykmeldtMedArbeidsgiver(OppfolgingUtils.erSykmeldtMedArbeidsgiver(formidlingsgruppekode, kvalifiseringsgruppekode))
                .setVurderingsBehov(vurderingsBehov(formidlingsgruppekode, kvalifiseringsgruppekode))
                .setFornavn(bruker.getFornavn())
                .setEtternavn(bruker.getEtternavn())
                .setVeilederId(bruker.getVeileder_id())
                .setDiskresjonskode(("7".equals(diskresjonskode) || "6".equals(diskresjonskode)) ? diskresjonskode : null)
                .setEgenAnsatt(bruker.isEgen_ansatt())
                .setErDoed(bruker.isEr_doed())
                .setSikkerhetstiltak(sikkerhetstiltak == null ? new ArrayList<>() : Collections.singletonList(sikkerhetstiltak)) //TODO: Hvorfor er dette en liste?
                .setFodselsdagIMnd(bruker.getFodselsdag_i_mnd())
                .setFodselsdato(toLocalDateTimeOrNull(bruker.getFodselsdato()))
                .setKjonn(bruker.getKjonn())
                .setYtelse(YtelseMapping.of(bruker.getYtelse()))
                .setUtlopsdato(toLocalDateTimeOrNull(bruker.getUtlopsdato()))
                .setUtlopsdatoFasett(ManedFasettMapping.of(bruker.getUtlopsdatofasett()))
                .setDagputlopUke(bruker.getDagputlopuke())
                .setDagputlopUkeFasett(DagpengerUkeFasettMapping.of(bruker.getDagputlopukefasett()))
                .setPermutlopUke(bruker.getPermutlopuke())
                .setPermutlopUkeFasett(DagpengerUkeFasettMapping.of(bruker.getPermutlopukefasett()))
                .setAapmaxtidUke(bruker.getAapmaxtiduke())
                .setAapmaxtidUkeFasett(AAPMaxtidUkeFasettMapping.of(bruker.getAapmaxtidukefasett()))
                .setAapUnntakUkerIgjen(bruker.getAapunntakukerigjen())
                .setAapUnntakUkerIgjenFasett(AAPUnntakUkerIgjenFasettMapping.of(bruker.getAapunntakukerigjenfasett()))
                .setArbeidsliste(Arbeidsliste.of(bruker))
                .setVenterPaSvarFraNAV(toLocalDateTimeOrNull(bruker.getVenterpasvarfranav()))
                .setVenterPaSvarFraBruker(toLocalDateTimeOrNull(bruker.getVenterpasvarfrabruker()))
                .setNyesteUtlopteAktivitet(toLocalDateTimeOrNull(bruker.getNyesteutlopteaktivitet()))
                .setAktivitetStart(toLocalDateTimeOrNull(bruker.getAktivitet_start()))
                .setNesteAktivitetStart(toLocalDateTimeOrNull(bruker.getNeste_aktivitet_start()))
                .setForrigeAktivitetStart(toLocalDateTimeOrNull(bruker.getForrige_aktivitet_start()))
                .setBrukertiltak(toList(bruker.getTiltak()))
                .setManuellBrukerStatus(bruker.getManuell_bruker())
                .addAktivitetUtlopsdato("tiltak", dateToTimestamp(bruker.getAktivitet_tiltak_utlopsdato()))
                .addAktivitetUtlopsdato("behandling", dateToTimestamp(bruker.getAktivitet_behandling_utlopsdato()))
                .addAktivitetUtlopsdato("sokeavtale", dateToTimestamp(bruker.getAktivitet_sokeavtale_utlopsdato()))
                .addAktivitetUtlopsdato("stilling", dateToTimestamp(bruker.getAktivitet_stilling_utlopsdato()))
                .addAktivitetUtlopsdato("ijobb", dateToTimestamp(bruker.getAktivitet_ijobb_utlopsdato()))
                .addAktivitetUtlopsdato("egen", dateToTimestamp(bruker.getAktivitet_egen_utlopsdato()))
                .addAktivitetUtlopsdato("gruppeaktivitet", dateToTimestamp(bruker.getAktivitet_gruppeaktivitet_utlopsdato()))
                .addAktivitetUtlopsdato("mote", dateToTimestamp(bruker.getAktivitet_mote_utlopsdato()))
                .addAktivitetUtlopsdato("utdanningaktivitet", dateToTimestamp(bruker.getAktivitet_utdanningaktivitet_utlopsdato()));

    }

    private static LocalDateTime toLocalDateTimeOrNull(String date) {
        if (date == null) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.parse(date), ZoneId.systemDefault());
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
        return Option.of((Boolean) document.get(field))
                .getOrElse(defaultValue);
    }

    private Bruker addAktivitetUtlopsdato(String type, Timestamp utlopsdato) {
        if (Objects.isNull(utlopsdato) || isRandomFutureDate(utlopsdato)) {
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
