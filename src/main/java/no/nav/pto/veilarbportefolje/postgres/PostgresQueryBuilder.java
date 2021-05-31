package no.nav.pto.veilarbportefolje.postgres;

import lombok.SneakyThrows;
import net.logstash.logback.encoder.org.apache.commons.lang3.StringUtils;
import no.nav.pto.veilarbportefolje.database.PostgresTable;
import no.nav.pto.veilarbportefolje.domene.Bruker;
import no.nav.pto.veilarbportefolje.domene.BrukereMedAntall;
import no.nav.pto.veilarbportefolje.domene.Kjonn;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static java.lang.Integer.parseInt;
import static java.util.stream.Collectors.toList;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.BRUKER_VIEW.*;

public class PostgresQueryBuilder {
    private final StringJoiner whereStatement = new StringJoiner(" AND ", " WHERE ", ";");
    private final JdbcTemplate db;

    private boolean brukKunEssensiellInfo = true;

    public PostgresQueryBuilder(@Qualifier("PostgresJdbc") JdbcTemplate jdbcTemplate, String navKontor) {
        this.db = jdbcTemplate;
        whereStatement.add(eq(NAV_KONTOR, navKontor));
        whereStatement.add(eq(OPPFOLGING, true));

    }

    public BrukereMedAntall search(Integer fra, Integer antall) {
        List<Map<String, Object>> resultat;
        if (brukKunEssensiellInfo) {
            resultat = db.queryForList("SELECT * FROM " + PostgresTable.OPTIMALISER_BRUKER_VIEW.TABLE_NAME + whereStatement.toString());
        } else {
            resultat = db.queryForList("SELECT * FROM " + TABLE_NAME + whereStatement.toString());
        }

        List<Bruker> avskjertResultat;
        if (resultat.size() <= fra) {
            avskjertResultat = new LinkedList<>();
        } else {
            int tilIndex = (resultat.size() <= fra + antall) ? resultat.size() : fra + antall;
            avskjertResultat = resultat.subList(fra, tilIndex)
                    .stream()
                    .map(this::mapTilBruker)
                    .collect(toList());
        }

        return new BrukereMedAntall(resultat.size(), avskjertResultat);
    }

    public <T> void leggTilListeFilter(List<T> filtervalgsListe, String columnName) {
        if (!filtervalgsListe.isEmpty()) {
            brukKunEssensiellInfo = false;
            StringJoiner orStatement = new StringJoiner(" OR ", "(", ")");
            filtervalgsListe.forEach(filtervalg -> orStatement.add(columnName + " = '" + filtervalg + "'"));
            whereStatement.add(orStatement.toString());
        }
    }

    public void leggTilFodselsdagFilter(List<Integer> fodselsdager) {
        if (!fodselsdager.isEmpty()) {
            brukKunEssensiellInfo = false;
            StringJoiner orStatement = new StringJoiner(" OR ", "(", ")");
            fodselsdager.forEach(fodselsDag -> orStatement.add("date_part('DAY'," + FODSELS_DATO + ")" + " = " + fodselsDag));
            whereStatement.add(orStatement.toString());
        }
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
        brukKunEssensiellInfo = false;
        whereStatement.add(FORMIDLINGSGRUPPEKODE + " = ISERV");
    }

    public void venterPaSvarFraBruker() {
        brukKunEssensiellInfo = false;
        whereStatement.add(VENTER_PA_BRUKER + " IS NOT NULL");
    }

    public void venterPaSvarFraNav() {
        brukKunEssensiellInfo = false;
        whereStatement.add(VENTER_PA_NAV + " IS NOT NULL");
    }

    public void trengerVurdering(boolean erVedtakstottePilotPa) {
        brukKunEssensiellInfo = false;
        whereStatement.add(FORMIDLINGSGRUPPEKODE + " != 'ISERV' AND " + KVALIFISERINGSGRUPPEKODE + " IN ('IVURD', 'BKART')");
        if (erVedtakstottePilotPa) {
            whereStatement.add(VEDTAKSTATUS + " IS NULL");
        }
    }

    public void underVurdering(boolean erVedtakstottePilotPa) {
        brukKunEssensiellInfo = false;
        if (erVedtakstottePilotPa) {
            whereStatement.add(VEDTAKSTATUS + " IS NOT NULL");
        } else {
            throw new IllegalStateException();
        }
    }

    public void erSykmeldtMedArbeidsgiver(boolean erVedtakstottePilotPa) {
        brukKunEssensiellInfo = false;
        whereStatement.add(FORMIDLINGSGRUPPEKODE + " = 'IARBS' AND " + KVALIFISERINGSGRUPPEKODE + " NOT IN ('BATT', 'BFORM', 'IKVAL', 'VURDU', 'OPPFI', 'VARIG')");
        if (erVedtakstottePilotPa) {
            whereStatement.add(VEDTAKSTATUS + " IS NULL");
        }
    }

    public void harArbeidsliste() {
        brukKunEssensiellInfo = false;
        whereStatement.add(ARB_ENDRINGSTIDSPUNKT + " IS NOT NULL"); // TODO: diskuter dette.
    }

    public void navnOgFodselsnummerSok(String soketekst) {
        if (StringUtils.isNumeric(soketekst)) {
            whereStatement.add(FODSELSNR + " LIKE '" + soketekst + "%'");
        } else {
            String soketekstUpper = soketekst.toUpperCase();
            whereStatement.add("(UPPER(" + FORNAVN + ") LIKE '%" + soketekstUpper + "%' OR UPPER(" + ETTERNAVN + ") LIKE '%" + soketekstUpper + "%')");
        }
    }

    public void kjonnfilter(Kjonn kjonn) {
        brukKunEssensiellInfo = false;
        whereStatement.add(KJONN + " = '" + kjonn.name() + "'");
    }

    public void alderFilter(List<String> aldere) {
        brukKunEssensiellInfo = false;
        StringJoiner orStatement = new StringJoiner(" OR ", "(", ")");
        aldere.forEach(alder -> alderFilter(alder, orStatement));
        whereStatement.add(orStatement.toString());
    }

    private void alderFilter(String alder, StringJoiner orStatement){
        var today = LocalDate.now();
        if ("19-og-under".equals(alder)) {
            LocalDate nittenOgUnder = today.minusYears(20).minusDays(1);
            orStatement.add(FODSELS_DATO + " >= '" + nittenOgUnder.toString() + "'::date");
        } else {
            String[] fraTilAlder = alder.split("-");
            int fraAlder = parseInt(fraTilAlder[0]);
            int tilAlder = parseInt(fraTilAlder[1]);

            LocalDate fraAlderDate = today.minusYears(fraAlder);
            LocalDate tilAlderDate = today.minusYears(tilAlder + 1).minusDays(1);
            orStatement.add("("+FODSELS_DATO + " <= '" + fraAlderDate.toString() + "'::date AND "+FODSELS_DATO + " >= '" + tilAlderDate.toString() + "'::date"+")");
        }
    }

    @SneakyThrows
    private Bruker mapTilBruker(Map<String, Object> row) {
        Bruker bruker = new Bruker();
        if (brukKunEssensiellInfo) {
            return bruker.fraEssensiellInfo(row);
        } else {
            return bruker.fraBrukerView(row);
        }
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
