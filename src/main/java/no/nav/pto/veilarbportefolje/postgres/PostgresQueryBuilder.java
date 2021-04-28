package no.nav.pto.veilarbportefolje.postgres;

import lombok.SneakyThrows;
import net.logstash.logback.encoder.org.apache.commons.lang3.StringUtils;
import no.nav.pto.veilarbportefolje.database.PostgresTable;
import no.nav.pto.veilarbportefolje.domene.Bruker;
import no.nav.pto.veilarbportefolje.domene.BrukereMedAntall;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static java.util.stream.Collectors.toList;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.BRUKER_VIEW.*;

public class PostgresQueryBuilder {
    private final StringJoiner whereStatement = new StringJoiner(" AND ", " WHERE ", ";");
    private final JdbcTemplate db;

    private boolean filtererPaDialog = false;
    private boolean filtererPaOppfolgingArena = false;

    public PostgresQueryBuilder(@Qualifier("PostgresJdbc") JdbcTemplate jdbcTemplate, String navKontor) {
        this.db = jdbcTemplate;
        whereStatement.add(eq(NAV_KONTOR, navKontor));
        whereStatement.add(eq(OPPFOLGING, true));

    }

    public BrukereMedAntall search(Integer fra, Integer antall) {
        String tablesInUse = TABLE_NAME;

        if (filtererPaDialog)
            tablesInUse += " INNER JOIN " + PostgresTable.DIALOG.TABLE_NAME + " D WHERE D.AKTOERID=BRUKER.AKTOERID";
        if (filtererPaOppfolgingArena)
            tablesInUse += " INNER JOIN " + PostgresTable.OPPFOLGINGSBRUKER_ARENA.TABLE_NAME + " OA WHERE OA.AKTOERID=BRUKER.AKTOERID";

        List<Map<String, Object>> resultat = db.queryForList("SELECT * FROM " + tablesInUse + whereStatement.toString());
        List<Bruker> avskjertResultat;

        if (resultat.size() <= fra) {
            avskjertResultat = new LinkedList<>();
        } else {
            int tilIndex = (resultat.size() <= fra + antall) ? resultat.size() - 1 : fra + antall;
            avskjertResultat = resultat.subList(fra, tilIndex)
                    .stream()
                    .map(this::mapTilBruker)
                    .collect(toList());
        }

        return new BrukereMedAntall(resultat.size(), avskjertResultat);
    }

    public void minOversiktFilter(String veilederId) {
        whereStatement.add(eq(VEILEDERID, veilederId));
    }

    public void ufordeltBruker(List<String> veiledereMedTilgangTilEnhet) {
        StringJoiner veiledere = new StringJoiner(", ", "(" + VEILEDERID + " IS NULL OR " + VEILEDERID + " NOT IN (", "))");
        for (String s : veiledereMedTilgangTilEnhet) {
            veiledere.add("'" + s + "'");
        }

        whereStatement.add(veiledere.toString());
    }

    public void nyForVeileder() {
        whereStatement.add(NY_FOR_VEILEDER + " = TRUE");
    }

    public void ikkeServiceBehov() {
        filtererPaOppfolgingArena = true;
        whereStatement.add("OA." + PostgresTable.OPPFOLGINGSBRUKER_ARENA.FORMIDLINGSGRUPPEKODE + " = ISERV");
    }

    public void venterPaSvarFraBruker() {
        filtererPaDialog = true;
        whereStatement.add("D." + PostgresTable.DIALOG.VENTER_PA_BRUKER + " IS NOT NULL");
    }

    public void venterPaSvarFraNav() {
        filtererPaDialog = true;
        whereStatement.add("D." + PostgresTable.DIALOG.VENTER_PA_NAV + " IS NOT NULL");
    }

    public void navnOgFodselsnummerSok(String soketekst) {
        if (StringUtils.isNumeric(soketekst)) {
            whereStatement.add(FODSELSNR + " LIKE " + soketekst + "%");
        } else {
            whereStatement.add("(" + FORNAVN + " LIKE %" + soketekst + "% OR " + ETTERNAVN + "LIKE %" + soketekst + "%)");
        }
    }

    @SneakyThrows
    private Bruker mapTilBruker(Map<String, Object> row) {
        return new Bruker()
                .setNyForVeileder((boolean) row.get(NY_FOR_VEILEDER))
                .setVeilederId((String) row.get(VEILEDERID))
                .setDiskresjonskode((String) row.get(DISKRESJONSKODE))
                .setFnr((String) row.get(FODSELSNR))
                .setFornavn((String) row.get(FORNAVN))
                .setEtternavn((String) row.get(ETTERNAVN));
    }


    private String eq(String kolonne, boolean verdi) {
        if (verdi) {
            return kolonne + " = TRUE";
        } else {
            return kolonne + " = FALSE";
        }
    }


    private String eq(String kolonne, String verdi) {
        return kolonne + " = '" + verdi + "'";
    }

    private String eq(String kolonne, int verdi) {
        return kolonne + " = " + verdi;
    }

}
