package no.nav.pto.veilarbportefolje.arenafiler.gr202.tiltak;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.metrics.Event;
import no.nav.common.metrics.MetricsClient;
import no.nav.melding.virksomhet.tiltakogaktiviteterforbrukere.v1.Tiltaksaktivitet;
import no.nav.pto.veilarbportefolje.domene.Tiltakkodeverk;
import no.nav.melding.virksomhet.tiltakogaktiviteterforbrukere.v1.Aktivitetstyper;
import no.nav.sbl.sql.SqlUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

import static no.nav.pto.veilarbportefolje.util.StreamUtils.batchProcess;

@Slf4j
@Repository
public class TiltakRepository {

    private final JdbcTemplate db;
    private final MetricsClient metricsClient;

    @Autowired
    public TiltakRepository(JdbcTemplate db, MetricsClient metricsClient){
        this.db = db;
        this.metricsClient = metricsClient;
    }

    void slettBrukertiltak() {
        db.execute("TRUNCATE TABLE brukertiltak");
    }

    void slettEnhettiltak() {
        db.execute("TRUNCATE TABLE enhettiltak");
    }

    void slettTiltakskoder() {
        db.execute("DELETE FROM tiltakkodeverk");
    }

    void lagreBrukertiltak(List<Brukertiltak> brukertiltak) {
        io.vavr.collection.List.ofAll(brukertiltak).sliding(1000, 1000)
                .forEach(brukertiltakBatch -> Brukertiltak.batchInsert(db, brukertiltakBatch.toJavaList()));
    }

    void lagreTiltakskoder(List<Tiltakkodeverk> tiltakskoder) {
        Tiltakkodeverk.batchInsert(db, tiltakskoder);
    }

    void lagreAktivitetskoder(Aktivitetstyper aktivitetstyper) {
        SqlUtils.insert(db, "tiltakkodeverk")
                .value("kode", aktivitetstyper.getValue())
                .value("verdi", aktivitetstyper.getTermnavn())
                .execute();
    }

    Map<String, List<String>> hentEnhetTilFodselsnummereMap() {
        String sql = "SELECT FODSELSNR AS FNR, NAV_KONTOR AS ENHETID FROM OPPFOLGINGSBRUKER WHERE NAV_KONTOR IS NOT NULL";
        List<EnhetTilFnr> enhetTilFnrList = db.query(
                sql,
                new BeanPropertyRowMapper<>(EnhetTilFnr.class)
        );
        return mapEnhetTilFnrs(enhetTilFnrList);
    }

    Map<String, List<String>> mapEnhetTilFnrs(List<EnhetTilFnr> raderFraDb) {
        Map<String, List<String>> enhetTilFnrMap = new HashMap<>();

        raderFraDb.forEach(rad -> {
            String enhet = rad.getEnhetId();
            String ident = rad.getFnr();
            List<String> brukereForEnhet = enhetTilFnrMap.getOrDefault(enhet, new ArrayList<>());
            brukereForEnhet.add(ident);
            enhetTilFnrMap.put(enhet, brukereForEnhet);
        });
        return enhetTilFnrMap;
    }

    void lagreEnhettiltak(List<TiltakForEnhet> tiltakListe) {
        db.execute((ConnectionCallback<Object>) connection -> {
            lagreEnhettiltak(tiltakListe, connection);
            return null;
        });
    }

    private void lagreEnhettiltak(List<TiltakForEnhet> tiltakListe, Connection connection) throws SQLException {
        String sql = "INSERT INTO ENHETTILTAK (ENHETID, TILTAKSKODE) VALUES (?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql);
        batchProcess(1, tiltakListe, tiltakForEnhetBatch -> lagreTiltakForEnhetBatch(tiltakForEnhetBatch, ps));
    }

    private void lagreTiltakForEnhetBatch(Collection<TiltakForEnhet> tiltakForEnhetBatch, PreparedStatement ps) {
        try {
            for (TiltakForEnhet tiltakForEnhet : tiltakForEnhetBatch) {
                ps.setString(1, tiltakForEnhet.getEnhetid());
                ps.setString(2, tiltakForEnhet.getTiltakskode());
                ps.addBatch();
                ps.clearParameters();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            log.error("Kunne ikke lagre tiltaksaktivitet i databasen");
            metricsClient.report(new Event("veilarbportefolje.lagreEnhettiltak.feilet").setFailed());
        }
    }
}
