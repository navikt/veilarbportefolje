package no.nav.pto.veilarbportefolje.postgres;

import lombok.SneakyThrows;
import no.nav.pto.veilarbportefolje.domene.Bruker;
import no.nav.pto.veilarbportefolje.domene.BrukereMedAntall;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static java.util.stream.Collectors.toList;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.BRUKER_VIEW.*;

public class PostgresQueryBuilder {
    private final StringJoiner whereStatement = new StringJoiner(" AND ", " WHERE " ,";");
    private final JdbcTemplate db;

    public PostgresQueryBuilder(JdbcTemplate jdbcTemplate, String navKontor) {
        this.db = jdbcTemplate;
        whereStatement.add(eq(NAV_KONTOR, navKontor));
        whereStatement.add(eq(OPPFOLGING, "TRUE"));
    }

    public BrukereMedAntall search(Integer fra, Integer antall){
        List<Map<String, Object>> resultat = db.queryForList("SELECT * FROM " + TABLE_NAME + whereStatement.toString());
        return new BrukereMedAntall(resultat.size(),
                resultat.subList(fra, fra+antall).stream()
                .map(this::mapTilBruker)
                .collect(toList()));
    }

    public void minOversiktFilter(String veilederId){
        whereStatement.add(eq(VEILEDERID, veilederId));
    }

    public void ufordeltBruker(List<String> veiledereMedTilgangTilEnhet){
        StringJoiner veiledere = new StringJoiner(", ", "( "+VEILEDERID + " IS NULL OR " + VEILEDERID + " NOT IN (" ,"))");
        veiledereMedTilgangTilEnhet.forEach(veiledere::add);

        whereStatement.add(veiledere.toString());
    }

    public void nyForVeileder(){
        whereStatement.add(NY_FOR_VEILEDER + " = TRUE");
    }

    @SneakyThrows
    private Bruker mapTilBruker(Map<String, Object> row){
        return new Bruker()
                .setNyForVeileder((boolean) row.get(NY_FOR_VEILEDER))
                .setVeilederId((String) row.get(VEILEDERID))
                .setDiskresjonskode((String) row.get(DISKRESJONSKODE))
                .setFnr((String) row.get(FODSELSNR))
                .setFornavn((String) row.get(FORNAVN))
                .setEtternavn((String) row.get(ETTERNAVN));
    }

    private String eq(String one, String two){
        return one + " = " +two;
    }
}
