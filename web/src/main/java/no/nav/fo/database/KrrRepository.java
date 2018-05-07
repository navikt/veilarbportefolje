package no.nav.fo.database;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.domene.DigitalKontaktInformasjon;
import no.nav.fo.util.UnderOppfolgingRegler;
import no.nav.fo.util.sql.InsertBatchQuery;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static no.nav.fo.util.DbUtils.dbTimerNavn;
import static no.nav.fo.util.DbUtils.parseJaNei;
import static no.nav.fo.util.MetricsUtils.timed;

@Slf4j
public class KrrRepository {

    @Inject
    JdbcTemplate db;

    public void slettKrrInformasjon(){
        log.info("Starter sletting av data i KRR tabell");
        db.execute("TRUNCATE TABLE KRR");
        log.info("Ferdig med sletting av data i KRR tabell");
    }

    public void iterateFnrsUnderOppfolging(int fetchSize, Consumer<List<String>> fnrConsumer) {
        String sql = "SELECT AKTOERID, FODSELSNR, KVALIFISERINGSGRUPPEKODE, FORMIDLINGSGRUPPEKODE, OPPFOLGING FROM VW_PORTEFOLJE_INFO";
        List<String> fnrListe = new ArrayList<>();
        timed(dbTimerNavn(sql), () -> db.query(sql, rs -> {
            String formidlingsgruppeKode = rs.getString("FORMIDLINGSGRUPPEKODE");
            String servicegruppeKode = rs.getString("KVALIFISERINGSGRUPPEKODE");
            boolean underOppfolging = parseJaNei(rs.getString("OPPFOLGING"), "OPPFOLGING");
            if (underOppfolging || UnderOppfolgingRegler.erUnderOppfolging(formidlingsgruppeKode, servicegruppeKode)) {
                fnrListe.add(rs.getString("FODSELSNR"));

                if (fnrListe.size() == fetchSize) {
                    fnrConsumer.accept(fnrListe);
                    fnrListe.clear();
                }
            }
        }));
        fnrConsumer.accept(fnrListe);
    }

    public int[] lagreKRRInformasjon(List<DigitalKontaktInformasjon> digitalKontaktinformasjonListe) {
        InsertBatchQuery<DigitalKontaktInformasjon> insertQuery = new InsertBatchQuery(db, "KRR");

        return insertQuery
                .add("fodselsnr", DigitalKontaktInformasjon::getFnr, String.class)
                .add("reservasjon", DigitalKontaktInformasjon::getReservertIKrr, String.class)
                .add("sisteverifisert", DigitalKontaktInformasjon::getSistVerifisert, Timestamp.class)
                .add("lagttilidb", DigitalKontaktInformasjon::getLagtTilIDB, Timestamp.class)
                .execute(digitalKontaktinformasjonListe);
    }
}
