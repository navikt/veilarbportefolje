package no.nav.fo.filmottak.tiltak;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.domene.Brukertiltak;
import no.nav.fo.domene.Tiltakkodeverk;
import no.nav.fo.util.sql.SqlUtils;
import no.nav.melding.virksomhet.tiltakogaktiviteterforbrukere.v1.Aktivitetstyper;
import no.nav.metrics.MetricsFactory;
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

@Slf4j
public class TiltakRepository {

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

    void lagreBrukertiltak(List<Brukertiltak> brukertiltak) {
        io.vavr.collection.List.ofAll(brukertiltak).sliding(1000,1000)
                .forEach(brukertiltakBatch -> Brukertiltak.batchInsert(db,brukertiltakBatch.toJavaList()));
    }

    void lagreTiltakskoder(List<Tiltakkodeverk> tiltakskoder) {
        Tiltakkodeverk.batchInsert(db,tiltakskoder);
    }

    void lagreAktivitetskoder(Aktivitetstyper aktivitetstyper) {
        SqlUtils.insert(db, "tiltakkodeverk")
                .value("kode", aktivitetstyper.getValue())
                .value("verdi", aktivitetstyper.getTermnavn())
                .execute();
    }

    Map<String, List<String>> hentEnhetTilFodselsnummereMap() {
        List<EnhetTilFnr> enhetTilFnrList = db.query(
            "SELECT FODSELSNR AS FNR, NAV_KONTOR AS ENHETID FROM OPPFOLGINGSBRUKER WHERE NAV_KONTOR IS NOT NULL",
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
        try (final Connection dsConnection =  ds.getConnection()){
            final PreparedStatement ps = dsConnection.prepareStatement("INSERT INTO ENHETTILTAK (ENHETID, TILTAKSKODE) VALUES (?, ?)");

            batchProcess(1, tiltakListe, timed("GR202.insertEnhetTiltak", tiltakForEnhetBatch -> {
                lagreTiltakForEnhetBatch(tiltakForEnhetBatch, ps);
            }));
        } catch (SQLException e) {
            log.error("Kunne ikke koble til database for å lagre tiltaksaktivitet for enhet", e);
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
            log.error("Kunne ikke lagre tiltaksaktivitet i databasen");
            MetricsFactory.createEvent("veilarbportefolje.lagreEnhettiltak.feilet").report();
        }
    }
}
