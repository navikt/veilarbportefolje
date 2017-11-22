package no.nav.fo.database;

import lombok.SneakyThrows;
import no.nav.fo.domene.Fnr;
import no.nav.fo.domene.Personinfo;
import no.nav.fo.util.DateUtils;
import no.nav.fo.util.FodselsnummerUtils;
import no.nav.fo.util.sql.SqlUtils;
import no.nav.fo.util.sql.where.WhereClause;

import javax.sql.DataSource;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

import static no.nav.fo.database.BrukerRepository.OPPFOLGINGSBRUKER;

public class PersonRepository {
    static final String SIKKERHETSTILTAK_TYPE_KODE = "SIKKERHETSTILTAK_TYPE_KODE";
    static final String SPERRET_ANSATT = "SPERRET_ANSATT";
    static final String FODSELSNR = "FODSELSNR";
    static final String FR_KODE = "FR_KODE";
    static final String ETTERNAVN = "ETTERNAVN";
    static final String FORNAVN = "FORNAVN";
    static final String DOED_FRA_DATO = "DOED_FRA_DATO";

    private static final String J = "J";

    private DataSource ds;

    public PersonRepository(DataSource ds) {
        this.ds = ds;
    }

    public Optional<Personinfo> hentPersoninfoForFnr(Fnr fnr) {
        return Optional.ofNullable(SqlUtils.select(ds,OPPFOLGINGSBRUKER, PersonRepository::mapper)
                .column(SIKKERHETSTILTAK_TYPE_KODE)
                .column(SPERRET_ANSATT)
                .column(FR_KODE)
                .column(ETTERNAVN)
                .column(FORNAVN)
                .column(FODSELSNR)
                .column(DOED_FRA_DATO)
                .where(WhereClause.equals(FODSELSNR, fnr.toString()))
                .execute());
    }

    @SneakyThrows
    private static Personinfo mapper(ResultSet rs) {
        String fornavn = rs.getString(FORNAVN);
        String etternavn = rs.getString(ETTERNAVN);
        String fodselsnummer = rs.getString(FODSELSNR);
        String fodselsdatoString = FodselsnummerUtils.lagFodselsdato(fodselsnummer);
        LocalDate fodselsdato = DateUtils.timestampFromISO8601(fodselsdatoString).toLocalDateTime().toLocalDate();
        LocalDate dodsdato = Optional.ofNullable(rs.getTimestamp(DOED_FRA_DATO))
                .map(Timestamp::toLocalDateTime)
                .map(LocalDateTime::toLocalDate)
                .orElse(null);

        return new Personinfo()
                .withSikkerhetstiltak(rs.getString(SIKKERHETSTILTAK_TYPE_KODE))
                .setEgenAnsatt(parseJorN(rs.getString(SPERRET_ANSATT)))
                .withDiskresjonskodeOnly6And7(rs.getString(FR_KODE))
                .setFornavn(fornavn)
                .setEtternavn(etternavn)
                .setSammensattNavn(etternavn + " " + fornavn)
                .setFodselsnummer(rs.getString(FODSELSNR))
                .setKjonn(FodselsnummerUtils.lagKjonn(fodselsnummer))
                .setFodselsdato(fodselsdato)
                .setDodsdato(dodsdato);
    }

    private static boolean parseJorN(String s) {
        if(Objects.isNull(s)) {
            return false;
        }
        return s.equals(J);
    }

}
