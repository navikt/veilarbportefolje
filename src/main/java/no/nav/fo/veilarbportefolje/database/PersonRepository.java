package no.nav.fo.veilarbportefolje.database;

import lombok.SneakyThrows;
import no.nav.fo.veilarbportefolje.domene.Fnr;
import no.nav.fo.veilarbportefolje.domene.Personinfo;
import no.nav.fo.veilarbportefolje.util.sql.SqlUtils;
import no.nav.fo.veilarbportefolje.util.sql.where.WhereClause;

import javax.sql.DataSource;

import java.sql.ResultSet;
import java.util.Objects;
import java.util.Optional;

public class PersonRepository {
    private static final String SIKKERHETSTILTAK_TYPE_KODE = "SIKKERHETSTILTAK_TYPE_KODE";
    private static final String SPERRET_ANSATT = "SPERRET_ANSATT";
    private static final String FODSELSNR = "FODSELSNR";
    private static final String J = "J";

    private DataSource ds;

    public PersonRepository(DataSource ds) {
        this.ds = ds;
    }

    public Optional<Personinfo> hentPersoninfoForFnr(Fnr fnr) {
        return Optional.ofNullable(SqlUtils.select(ds,"OPPFOLGINGSBRUKER", PersonRepository::mapper)
                .column(SIKKERHETSTILTAK_TYPE_KODE)
                .column(SPERRET_ANSATT)
                .where(WhereClause.equals(FODSELSNR, fnr.toString()))
                .execute());
    }

    @SneakyThrows
    private static Personinfo mapper(ResultSet rs) {
        return new Personinfo()
                .withSikkerhetstiltak(rs.getString(SIKKERHETSTILTAK_TYPE_KODE))
                .setEgenAnsatt(parseJorN(rs.getString(SPERRET_ANSATT)));
    }

    private static boolean parseJorN(String s) {
        if(Objects.isNull(s)) {
            return false;
        }
        return s.equals(J);
    }

}
