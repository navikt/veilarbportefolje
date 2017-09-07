package no.nav.fo.filmottak.tiltak;

import no.nav.fo.util.sql.SqlUtils;
import no.nav.melding.virksomhet.tiltakogaktiviteterforbrukere.v1.Bruker;
import no.nav.melding.virksomhet.tiltakogaktiviteterforbrukere.v1.Tiltakstyper;
import no.nav.metrics.MetricsFactory;
import org.slf4j.Logger;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

import static no.nav.fo.util.MetricsUtils.timed;
import static no.nav.fo.util.StreamUtils.batchProcess;
import static org.slf4j.LoggerFactory.getLogger;

public class TiltakRepository {

    private static final Logger logger = getLogger(TiltakRepository.class);

    @Inject
    private DataSource ds;

    @Inject
    private JdbcTemplate db;

    void slettBrukertiltak() {
        db.execute("TRUNCATE TABLE brukertiltak");
    }

    void slettEnhettiltak() {
        db.execute("TRUNCATE TABLE enhettiltak");
    }

    void slettTiltakskoder() {
        db.execute("DELETE FROM tiltakkodeverk");
    }

    void insertBrukertiltak(Bruker bruker) {
        bruker.getTiltaksaktivitetListe().forEach(
            tiltak -> {
                try {
                    SqlUtils.insert(db, "brukertiltak")
                        .value("fodselsnr", bruker.getPersonident())
                        .value("tiltakskode", tiltak.getTiltakstype())
                        .execute();
                } catch (DataIntegrityViolationException e) {
                    String logMsg = String.format("Kunne ikke lagre brukertiltak for %s med tiltakstype %s", bruker.getPersonident(), tiltak.getTiltakstype());
                    logger.warn(logMsg);
                    MetricsFactory.createEvent("veilarbportefolje.insertBrukertiltak.feilet").report();
                }
            }
        );
    }

    void insertTiltakskoder(Tiltakstyper tiltakskoder) {
        SqlUtils.insert(db, "tiltakkodeverk")
            .value("kode", tiltakskoder.getValue())
            .value("verdi", tiltakskoder.getTermnavn())
            .execute();
    }

    Map<String, List<String>> getEnhetTilFodselsnummereMap() {
        List<EnhetTilFnr> enhetTilFnrList = db.query("SELECT FODSELSNR AS FNR, NAV_KONTOR AS ENHETID FROM OPPFOLGINGSBRUKER WHERE NAV_KONTOR IS NOT NULL", new BeanPropertyRowMapper<>(EnhetTilFnr.class));
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

    void insertEnhettiltak(List<TiltakForEnhet> tiltakListe) {
        try (final Connection dsConnection =  ds.getConnection()){
            final PreparedStatement ps = dsConnection.prepareStatement("INSERT INTO ENHETTILTAK (ENHETID, TILTAKSKODE) VALUES (?, ?)");

            batchProcess(1, tiltakListe, timed("GR202.insertEnhetTiltak", tiltakForEnhetBatch -> {
                lagreTiltakForEnhetBatch(tiltakForEnhetBatch, ps);
            }));
        } catch (SQLException e) {
            logger.error("Kunne ikke koble til database for å lagre Tiltak for enhet", e);
        }
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
            logger.warn("Kunne ikke lagre TiltakForEnhet i databasen");
            MetricsFactory.createEvent("veilarbportefolje.insertEnhettiltak.feilet").report();
        }
    }
}
