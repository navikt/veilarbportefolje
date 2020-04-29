package no.nav.pto.veilarbportefolje.util;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.feed.oppfolging.OppfolgingUtils;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import org.apache.commons.lang3.text.WordUtils;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import static no.nav.pto.veilarbportefolje.util.DateUtils.getFarInTheFutureDate;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toIsoUTC;
import static no.nav.pto.veilarbportefolje.feed.oppfolging.OppfolgingUtils.isNyForEnhet;

@Slf4j
public class DbUtils {

    @SneakyThrows
    public static OppfolgingsBruker mapTilOppfolgingsBruker(ResultSet rs, boolean vedtakstotteFeatureErPa) {
        String formidlingsgruppekode = rs.getString("formidlingsgruppekode");
        String kvalifiseringsgruppekode = rs.getString("kvalifiseringsgruppekode");
        String brukersSituasjon = rs.getString("BRUKERS_SITUASJON");

        String fornavn = kapitaliser(rs.getString("fornavn"));
        String etternavn = kapitaliser(rs.getString("etternavn"));

        boolean trengerVurdering = OppfolgingUtils.trengerVurdering(formidlingsgruppekode, kvalifiseringsgruppekode);
        boolean erSykmeldtMedArbeidsgiver = OppfolgingUtils.erSykmeldtMedArbeidsgiver(formidlingsgruppekode, kvalifiseringsgruppekode);

        OppfolgingsBruker bruker = new OppfolgingsBruker()
                .setPerson_id(numberToString(rs.getBigDecimal("person_id")))
                .setOppfolging_startdato(toIsoUTC(rs.getTimestamp("oppfolging_startdato")))
                .setAktoer_id(rs.getString("aktoerid"))
                .setFnr(rs.getString("fodselsnr"))
                .setFornavn(fornavn)
                .setEtternavn(etternavn)
                .setFullt_navn(String.format("%s, %s", etternavn, fornavn))
                .setEnhet_id(rs.getString("nav_kontor"))
                .setFormidlingsgruppekode(formidlingsgruppekode)
                .setIserv_fra_dato(toIsoUTC(rs.getTimestamp("iserv_fra_dato")))
                .setKvalifiseringsgruppekode(kvalifiseringsgruppekode)
                .setRettighetsgruppekode(rs.getString("rettighetsgruppekode"))
                .setHovedmaalkode(rs.getString("hovedmaalkode"))
                .setSikkerhetstiltak(rs.getString("sikkerhetstiltak_type_kode"))
                .setDiskresjonskode(rs.getString("fr_kode"))
                .setEgen_ansatt(parseJaNei(rs.getString("sperret_ansatt"), "sperret_ansatt"))
                .setEr_doed(parseJaNei(rs.getString("er_doed"), "er_doed"))
                .setDoed_fra_dato(toIsoUTC(rs.getTimestamp("doed_fra_dato")))
                .setVeileder_id(rs.getString("veilederident"))
                .setFodselsdag_i_mnd(Integer.parseInt(FodselsnummerUtils.lagFodselsdagIMnd(rs.getString("fodselsnr"))))
                .setFodselsdato(FodselsnummerUtils.lagFodselsdato(rs.getString("fodselsnr")))
                .setKjonn(FodselsnummerUtils.lagKjonn(rs.getString("fodselsnr")))
                .setYtelse(rs.getString("ytelse"))
                .setUtlopsdato(toIsoUTC(rs.getTimestamp("utlopsdato")))
                .setUtlopsdatofasett(rs.getString("utlopsdatofasett"))
                .setDagputlopuke(parseInt(rs.getString("dagputlopuke")))
                .setDagputlopukefasett(rs.getString("dagputlopukefasett"))
                .setPermutlopuke(parseInt(rs.getString("permutlopuke")))
                .setAapmaxtiduke(parseInt(rs.getString("aapmaxtiduke")))
                .setAapmaxtidukefasett(rs.getString("aapmaxtidukefasett"))
                .setAapunntakukerigjen(konverterDagerTilUker(rs.getString("aapunntakdagerigjen")))
                .setAapunntakukerigjenfasett(rs.getString("aapunntakukerigjenfasett"))
                .setOppfolging(parseJaNei(rs.getString("OPPFOLGING"), "OPPFOLGING"))
                .setNy_for_veileder(parseJaNei(rs.getString("NY_FOR_VEILEDER"), "NY_FOR_VEILEDER"))
                .setNy_for_enhet(isNyForEnhet(rs.getString("veilederident")))
                .setTrenger_vurdering(trengerVurdering)
                .setVenterpasvarfrabruker(toIsoUTC(rs.getTimestamp("venterpasvarfrabruker")))
                .setVenterpasvarfranav(toIsoUTC(rs.getTimestamp("venterpasvarfranav")))
                .setNyesteutlopteaktivitet(toIsoUTC(rs.getTimestamp("nyesteutlopteaktivitet")))
                .setAktivitet_start(toIsoUTC(rs.getTimestamp("aktivitet_start")))
                .setNeste_aktivitet_start(toIsoUTC(rs.getTimestamp("neste_aktivitet_start")))
                .setForrige_aktivitet_start(toIsoUTC(rs.getTimestamp("forrige_aktivitet_start")))
                .setManuell_bruker(identifiserManuellEllerKRRBruker(rs.getString("RESERVERTIKRR"), rs.getString("MANUELL")))
                .setBrukers_situasjon(brukersSituasjon)
                .setEr_sykmeldt_med_arbeidsgiver(erSykmeldtMedArbeidsgiver);

        boolean brukerHarArbeidsliste = parseJaNei(rs.getString("ARBEIDSLISTE_AKTIV"), "ARBEIDSLISTE_AKTIV");

        if (brukerHarArbeidsliste) {
            bruker
                    .setArbeidsliste_aktiv(true)
                    .setArbeidsliste_sist_endret_av_veilederid(rs.getString("ARBEIDSLISTE_ENDRET_AV"))
                    .setArbeidsliste_endringstidspunkt(toIsoUTC(rs.getTimestamp("ARBEIDSLISTE_ENDRET_TID")))
                    .setArbeidsliste_kommentar(rs.getString("ARBEIDSLISTE_KOMMENTAR"))
                    .setArbeidsliste_overskrift(rs.getString("ARBEIDSLISTE_OVERSKRIFT"))
                    .setArbeidsliste_kategori(rs.getString("ARBEIDSLISTE_KATEGORI"))
                    .setArbeidsliste_frist(Optional.ofNullable(toIsoUTC(rs.getTimestamp("ARBEIDSLISTE_FRIST"))).orElse(getFarInTheFutureDate()));
        }

        if(vedtakstotteFeatureErPa) {
            String vedtakstatus = rs.getString("VEDTAKSTATUS");
            bruker
                    .setTrenger_vurdering(trengerVurdering && vedtakstatus == null)
                    .setEr_sykmeldt_med_arbeidsgiver(erSykmeldtMedArbeidsgiver && vedtakstatus == null)
                    .setVedtak_status(vedtakstatus)
                    .setVedtak_status_endret(toIsoUTC(rs.getTimestamp("VEDTAK_STATUS_ENDRET_TIDSPUNKT")))
                    .setTrenger_revurdering(OppfolgingUtils.trengerRevurderingVedtakstotte(formidlingsgruppekode, kvalifiseringsgruppekode, vedtakstatus));
        }

        return bruker;
    }

    public static Integer konverterDagerTilUker(String antallDagerFraDB) {
        Integer antallDager = parseInt(antallDagerFraDB);
        return antallDager == null ? 0 : (antallDager / 5);
    }

    static String kapitaliser(String s) {
        return WordUtils.capitalizeFully(s, ' ', '\'', '-');
    }

    public static String identifiserManuellEllerKRRBruker(String krrJaNei, String manuellJaNei) {
        if ("J".equals(krrJaNei)) {
            return "KRR";
        } else if ("J".equals(manuellJaNei)) {
            return "MANUELL";
        }
        return null;
    }

    public static boolean parseJaNei(Object janei, String name) {
        boolean defaultValue = false;
        if (janei == null) {
            log.debug(String.format("%s er ikke satt i databasen, defaulter til %b", name, defaultValue));
            return defaultValue;
        }

        switch (janei.toString()) {
            case "J":
                return true;
            case "N":
                return false;
            default:
                throw new IllegalArgumentException(String.format("Kunne ikke parse verdi %s fra database til boolean", janei));
        }
    }

    static Integer parseInt(String integer) {
        if (integer == null) {
            return null;
        }
        return Integer.parseInt(integer);
    }

    public static Boolean parse0OR1(String value) {
        if (value == null) {
            return null;
        }
        return "1".equals(value);
    }

    public static String boolTo0OR1(boolean bool) {
        return bool ? "1" : "0";
    }

    public static String numberToString(BigDecimal bd) {
        return String.valueOf(bd.intValue());
    }

    public static <S> Set<S> toSet(S s) {
        Set<S> set = new HashSet<>();
        set.add(s);
        return set;
    }


    public static <T> Predicate<T> not(Predicate<T> predicate) {
        return (T t) -> !predicate.test(t);
    }

    public static String dbTimerNavn(String sql) {
        return (sql + ".db").replaceAll("[^\\w]", "-");
    }
}
