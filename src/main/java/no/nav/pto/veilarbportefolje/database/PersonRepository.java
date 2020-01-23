package no.nav.pto.veilarbportefolje.database;

import lombok.SneakyThrows;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.domene.Personinfo;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.util.Objects;
import java.util.Optional;

public class PersonRepository {
    private static final String SIKKERHETSTILTAK_TYPE_KODE = "SIKKERHETSTILTAK_TYPE_KODE";
    private static final String SPERRET_ANSATT = "SPERRET_ANSATT";
    private static final String FODSELSNR = "FODSELSNR";
    private static final String J = "J";

    private JdbcTemplate ds;

    public PersonRepository(JdbcTemplate ds) {
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
