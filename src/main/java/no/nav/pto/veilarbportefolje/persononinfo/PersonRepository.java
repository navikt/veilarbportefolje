package no.nav.pto.veilarbportefolje.persononinfo;

import lombok.SneakyThrows;
import no.nav.pto.veilarbportefolje.domene.value.Fnr;
import no.nav.pto.veilarbportefolje.domene.Personinfo;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.util.Objects;
import java.util.Optional;

@Repository
public class PersonRepository {
    private static final String SIKKERHETSTILTAK_TYPE_KODE = "SIKKERHETSTILTAK_TYPE_KODE";
    private static final String SPERRET_ANSATT = "SPERRET_ANSATT";
    private static final String FODSELSNR = "FODSELSNR";
    private static final String J = "J";

    private final JdbcTemplate db;

    @Autowired
    public PersonRepository(JdbcTemplate db) {
        this.db = db;
    }

    public Optional<Personinfo> hentPersoninfoForFnr(Fnr fnr) {
        return Optional.ofNullable(SqlUtils.select(db,"OPPFOLGINGSBRUKER", PersonRepository::mapper)
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
