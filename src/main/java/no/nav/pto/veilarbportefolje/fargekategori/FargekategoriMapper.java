package no.nav.pto.veilarbportefolje.fargekategori;

import lombok.SneakyThrows;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.NavIdent;

import java.sql.ResultSet;
import java.util.UUID;

import static no.nav.pto.veilarbportefolje.database.PostgresTable.FARGEKATEGORI.*;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toLocalDate;

public class FargekategoriMapper {
    private FargekategoriMapper() {}

    @SneakyThrows
    public static FargekategoriEntity fargekategoriMapper(ResultSet rs) {
        return new FargekategoriEntity(
                UUID.fromString(rs.getString(ID)),
                Fnr.of(rs.getString(FNR)),
                FargekategoriVerdi.valueOf(rs.getString(VERDI)),
                toLocalDate(rs.getTimestamp(SIST_ENDRET)),
                NavIdent.of(rs.getString(SIST_ENDRET_AV_VEILEDERIDENT)),
                EnhetId.of(rs.getString(ENHET_ID))
        );
    }
}
