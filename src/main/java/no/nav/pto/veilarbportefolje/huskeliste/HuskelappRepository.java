package no.nav.pto.veilarbportefolje.huskeliste;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.huskeliste.controller.dto.HuskelappInputDto;
import no.nav.pto.veilarbportefolje.huskeliste.controller.dto.HuskelappOutputDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class HuskelappRepository {
    private final JdbcTemplate db;
    @Qualifier("PostgresJdbcReadOnly")
    private final JdbcTemplate dbReadOnly;


    public UUID opprettHuskelapp(HuskelappInputDto inputDto) {
        return UUID.randomUUID();
    }

    public List<HuskelappOutputDto> hentHuskelapp(EnhetId enhetId, VeilederId veilederId) {
        return null;
    }

    public HuskelappOutputDto hentHuskelapp(Fnr brukerFnr) {
        return null;
    }

    public HuskelappOutputDto slettHuskelapp(String huskelappId) {
        return null;
    }

    public HuskelappOutputDto oppdatereStatus(String huskelappId, HuskelappStatus status) {
        return null;
    }

    public HuskelappOutputDto oppdatereArkivertDato(String huskelappId) {
        return null;
    }
}
