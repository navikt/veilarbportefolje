package no.nav.fo.veilarbportefolje.krr;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbportefolje.database.KrrTabell;
import no.nav.fo.veilarbportefolje.database.VwPortefoljeInfoTabell;
import no.nav.fo.veilarbportefolje.util.DbUtils;
import no.nav.sbl.sql.DbConstants;
import no.nav.sbl.sql.InsertBatchQuery;
import no.nav.sbl.sql.SqlUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.inject.Inject;
import java.util.List;

import static no.nav.fo.veilarbportefolje.database.KrrTabell.KRR;
import static no.nav.fo.veilarbportefolje.database.VwPortefoljeInfoTabell.VW_PORTEFOLJE_INFO;
import static no.nav.fo.veilarbportefolje.database.VwPortefoljeInfoTabell.brukerErUnderOppfolging;
import static no.nav.fo.veilarbportefolje.util.DbUtils.safeToJaNei;

@Slf4j
public class KrrRepository {

    private JdbcTemplate db;

    @Inject
    public KrrRepository(JdbcTemplate db) {
        this.db = db;
    }

    public void slettKrrInformasjon() {
        log.info("Starter sletting av data i KRR tabell");
        DbUtils.truncateTable(db, KRR);
        log.info("Ferdig med sletting av data i KRR tabell");

    }

    public List<String> hentAlleFnrUnderOppfolging() {
        return SqlUtils
                .select(db, VW_PORTEFOLJE_INFO, rs -> rs.getString(VwPortefoljeInfoTabell.Kolonne.FODSELSNR))
                .column(VwPortefoljeInfoTabell.Kolonne.FODSELSNR)
                .where(brukerErUnderOppfolging())
                .executeToList();
    }

    public void lagreKrrKontaktInfo(List<KrrKontaktInfoDTO> dtoer) {
        int[] result = new InsertBatchQuery<KrrKontaktInfoDTO>(db, KRR)
                .add(KrrTabell.Kolonne.FODSELSNR, KrrKontaktInfoDTO::getPersonident, String.class)
                .add(KrrTabell.Kolonne.RESERVASJON, dto -> safeToJaNei(dto.isReservert()), String.class)
                .add(KrrTabell.Kolonne.LAGTTILIDB, DbConstants.CURRENT_TIMESTAMP)
                .execute(dtoer);

        if (result.length != dtoer.size()) {
            log.warn("{} inserts KRR-tabellen feilet", result.length - dtoer.size());
        }
    }
}
