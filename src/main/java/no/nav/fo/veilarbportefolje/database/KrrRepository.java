package no.nav.fo.veilarbportefolje.database;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbportefolje.domene.KrrDAO;
import no.nav.fo.veilarbportefolje.util.UnderOppfolgingRegler;
import no.nav.sbl.sql.InsertBatchQuery;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static no.nav.fo.veilarbportefolje.util.DbUtils.dbTimerNavn;
import static no.nav.fo.veilarbportefolje.util.DbUtils.parseJaNei;
import static no.nav.fo.veilarbportefolje.util.MetricsUtils.timed;

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

    public int[] lagreKRRInformasjon(List<KrrDAO> digitalKontaktinformasjonListe) {
        return new InsertBatchQuery<KrrDAO>(db, "KRR")
                .add("fodselsnr", KrrDAO::getFnr, String.class)
                .add("reservasjon", KrrDAO::getReservertIKrr, String.class)
                .add("sisteverifisert", KrrDAO::getSistVerifisert, Timestamp.class)
                .add("lagttilidb", KrrDAO::getLagtTilIDB, Timestamp.class)
                .execute(digitalKontaktinformasjonListe);
    }
}
