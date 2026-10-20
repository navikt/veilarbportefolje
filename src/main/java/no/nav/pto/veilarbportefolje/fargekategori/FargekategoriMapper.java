package no.nav.pto.veilarbportefolje.fargekategori;

import lombok.SneakyThrows;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.NavIdent;

import java.sql.ResultSet;
import java.util.Optional;
import java.util.UUID;

import static no.nav.pto.veilarbportefolje.database.PostgresTable.FARGEKATEGORI.*;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toLocalDateOrNull;

public class FargekategoriMapper {
    private FargekategoriMapper() {}

    @SneakyThrows
    public static FargekategoriEntity fargekategoriMapper(ResultSet rs) {
        return new FargekategoriEntity(
                UUID.fromString(rs.getString(ID)),
                Fnr.of(rs.getString(FNR)),
                Optional.ofNullable(rs.getString(VERDI)).map(FargekategoriVerdi::valueOf).orElse(null),
                toLocalDateOrNull(rs.getTimestamp(SIST_ENDRET)),
                Optional.ofNullable(rs.getString(SIST_ENDRET_AV_VEILEDERIDENT)).map(NavIdent::of).orElse(null),
                Optional.ofNullable(rs.getString(ENHET_ID)).map(EnhetId::of).orElse(null)
        );
    }
}
