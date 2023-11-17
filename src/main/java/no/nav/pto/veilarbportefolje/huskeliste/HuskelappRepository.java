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
import java.util.Optional;
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

    public Optional<HuskelappOutputDto> hentHuskelapp(UUID huskelappId) {
        return null;
    }

    public boolean slettHuskelapp(UUID huskelappId) {
        return false;
    }

    public HuskelappOutputDto oppdatereStatus(UUID huskelappId, HuskelappStatus status) {
        return null;
    }

    public HuskelappOutputDto oppdatereArkivertDato(UUID huskelappId) {
        return null;
    }
}
