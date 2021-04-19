package no.nav.pto.veilarbportefolje.postgres;

import lombok.SneakyThrows;
import no.nav.pto.veilarbportefolje.domene.Bruker;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.util.List;
import java.util.StringJoiner;

import static java.util.stream.Collectors.toList;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.BRUKER_VIEW.*;

public class PostgresQueryBuilder {
    private StringJoiner whereStatement = new StringJoiner(" AND ", "WHERE" ,";");
    private final JdbcTemplate db;

    public PostgresQueryBuilder(JdbcTemplate jdbcTemplate) {
        this.db = jdbcTemplate;
    }

    public void minOversikt(String veilederId){
        whereStatement.add(VEILEDERID + "=" + veilederId);
    }

    public List<Bruker> search(){
        return db.queryForList("SELECT * FROM " + TABLE_NAME + whereStatement.toString())
                .stream()
                .map(rs -> mapTilBruker((ResultSet) rs))
                .collect(toList());
    }

    @SneakyThrows
    private Bruker mapTilBruker(ResultSet rs){
        return new Bruker()
                .setNyForVeileder(rs.getBoolean(NY_FOR_VEILEDER))
                .setVeilederId(rs.getString(VEILEDERID));
    }
}
