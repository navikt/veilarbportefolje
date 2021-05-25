package no.nav.pto.veilarbportefolje.oppfolgingsbruker;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.ZonedDateTime;

import static no.nav.pto.veilarbportefolje.database.PostgresTable.safeBool;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.safeNull;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toTimestamp;

@Data
@Accessors(chain = true)
public class OppfolgingsbrukerKafkaDTO {
    String aktoerid;
    String fodselsnr;
    String formidlingsgruppekode;
    ZonedDateTime iserv_fra_dato;
    String etternavn;
    String fornavn;
    String nav_kontor;
    String kvalifiseringsgruppekode;
    String rettighetsgruppekode;
    String hovedmaalkode;
    String sikkerhetstiltak_type_kode;
    String fr_kode;
    Boolean har_oppfolgingssak;
    Boolean sperret_ansatt;
    Boolean er_doed;
    ZonedDateTime doed_fra_dato;
    ZonedDateTime endret_dato;

    public String toSqlInsertString() {
        return safeNull(getAktoerid()) + ", " +
                safeNull(getFodselsnr()) + ",  " +
                safeNull(getFormidlingsgruppekode()) + ", " +
                safeNull(toTimestamp(getIserv_fra_dato())) + ", " +
                safeNull(getEtternavn()) + ", " +
                safeNull(getFornavn()) + ", " +
                safeNull(getNav_kontor()) + ",  " +
                safeNull(getKvalifiseringsgruppekode()) + ", " +
                safeNull(getRettighetsgruppekode()) + ", " +
                safeNull(getHovedmaalkode()) + ", " +
                safeNull(getSikkerhetstiltak_type_kode()) + ", " +
                safeNull(getFr_kode()) + ", " +
                safeBool(getHar_oppfolgingssak()) + ", " +
                safeBool(getSperret_ansatt()) + ",  " +
                safeBool(getEr_doed()) + ", " +
                safeNull(toTimestamp(getDoed_fra_dato())) + ", " +
                safeNull(toTimestamp(getEndret_dato()));

    }

    public String toSqlUpdateString() {
        return safeNull(getFodselsnr()) + ", " +
                safeNull(getFormidlingsgruppekode()) + ", " +
                safeNull(toTimestamp(getIserv_fra_dato())) + ", " +
                safeNull(getEtternavn()) + ", " +
                safeNull(getFornavn()) + ", " +
                safeNull(getNav_kontor()) + ", " +
                safeNull(getKvalifiseringsgruppekode()) + ", " +
                safeNull(getRettighetsgruppekode()) + ", " +
                safeNull(getHovedmaalkode()) + ", " +
                safeNull(getSikkerhetstiltak_type_kode()) + ", " +
                safeNull(getFr_kode()) + ", " +
                safeBool(getHar_oppfolgingssak()) + ", " +
                safeBool(getSperret_ansatt()) + ",  " +
                safeBool(getEr_doed()) + ", " +
                safeNull(toTimestamp(getDoed_fra_dato())) + ", " +
                safeNull(toTimestamp(getEndret_dato()));
    }
}
