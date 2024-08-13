package no.nav.pto.veilarbportefolje.arbeidssoeker.v1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ArbeidssokerRegistreringRepositoryV2 {
    private final JdbcTemplate db;
    public void slettBrukerRegistrering(AktorId aktoerId) {
        db.update("DELETE FROM BRUKER_REGISTRERING WHERE AKTOERID = ?", aktoerId.get());
        secureLog.info("Slettet brukerregistrering for bruker: {}", aktoerId);
    }

    public void slettBrukerProfilering(AktorId aktoerId) {
        db.update("DELETE FROM BRUKER_PROFILERING WHERE AKTOERID = ?", aktoerId.get());
        secureLog.info("Slettet brukerprofilering for bruker: {}", aktoerId);
    }

    public void slettEndringIRegistrering(AktorId aktoerId) {
        db.update("DELETE FROM ENDRING_I_REGISTRERING WHERE AKTOERID = ?", aktoerId.get());
        secureLog.info("Slettet endring i registrering for bruker: {}", aktoerId);
    }
}
