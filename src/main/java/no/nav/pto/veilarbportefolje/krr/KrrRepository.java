package no.nav.pto.veilarbportefolje.krr;

import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.domene.KrrDTO;
import no.nav.pto.veilarbportefolje.util.UnderOppfolgingRegler;
import no.nav.sbl.sql.InsertBatchQuery;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static no.nav.pto.veilarbportefolje.util.DbUtils.parseJaNei;

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
        db.query(sql, rs -> {
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
        });

        fnrConsumer.accept(fnrListe);
    }

    public int[] lagreKRRInformasjon(List<KrrDTO> digitalKontaktinformasjonListe) {
        return new InsertBatchQuery<KrrDTO>(db, "KRR")
                .add("fodselsnr", KrrDTO::getFnr, String.class)
                .add("reservasjon", KrrDTO::getReservertIKrr, String.class)
                .add("sisteverifisert", KrrDTO::getSistVerifisert, Timestamp.class)
                .add("lagttilidb", KrrDTO::getLagtTilIDB, Timestamp.class)
                .execute(digitalKontaktinformasjonListe);
    }
}
