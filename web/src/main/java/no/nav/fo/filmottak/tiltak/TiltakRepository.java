package no.nav.fo.filmottak.tiltak;

import no.nav.fo.database.BrukerRepository;
import no.nav.fo.util.sql.SqlUtils;
import no.nav.melding.virksomhet.tiltakogaktiviteterforbrukere.v1.Bruker;
import no.nav.melding.virksomhet.tiltakogaktiviteterforbrukere.v1.Tiltakstyper;
import no.nav.metrics.MetricsFactory;
import org.slf4j.Logger;
import org.springframework.dao.DataIntegrityViolationException;
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

public class TiltakRepository extends BrukerRepository {

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

    void insertBrukertiltak(Bruker bruker, Set<String> brukerTiltak) {
        bruker.getTiltaksaktivitetListe().forEach(
            tiltak -> {
                brukerTiltak.add(tiltak.getTiltakstype());
                try {
                    SqlUtils.insert(db, "brukertiltak")
                        .value("personid", bruker.getPersonident())
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

    void slettTiltakskoder() {
        db.execute("DELETE FROM tiltakkodeverk");
    }

    void insertTiltakskoder(Tiltakstyper tiltakskoder) {
        SqlUtils.insert(db, "tiltakkodeverk")
            .value("kode", tiltakskoder.getValue())
            .value("verdi", tiltakskoder.getTermnavn())
            .execute();
    }

    Map<String, List<String>> getEnhetMedPersonIder() {
        List<Map<String, Object>> maps = db.queryForList("SELECT FODSELSNR, NAV_KONTOR FROM OPPFOLGINGSBRUKER WHERE NAV_KONTOR IS NOT NULL");

        Map<String, List<String>> reduce = new HashMap<>();
        maps.forEach(dbRadMap -> {
            String enhet = (String) dbRadMap.get("NAV_KONTOR");
            String ident = dbRadMap.get("FODSELSNR").toString();
            List<String> brukereForEnhet = reduce.getOrDefault(enhet, new ArrayList<>());
            brukereForEnhet.add(ident);
            reduce.put(enhet, brukereForEnhet);
        });

        return reduce;
    }

    public void insertEnhettiltak(List<TiltakForEnhet> tiltakListe) {
        try {
            final Connection dsConnection =  ds.getConnection();
            final PreparedStatement ps = dsConnection.prepareStatement("INSERT INTO ENHETTILTAK (ENHETID, TILTAKSKODE) VALUES (?, ?)");

            batchProcess(1, tiltakListe, timed("GR202.insertEnhetTiltak", tiltakForEnhetBatch -> {
                lagreTiltakForEnhetBatch(tiltakForEnhetBatch, ps);
            }));

            dsConnection.close();
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
            logger.error("Kunne ikke lagre TiltakForEnhet i databasen");
            MetricsFactory.createEvent("veilarbportefolje.insertEnhettiltak.feilet").report();
        }
    }
}
