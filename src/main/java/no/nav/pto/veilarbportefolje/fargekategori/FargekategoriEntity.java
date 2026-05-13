package no.nav.pto.veilarbportefolje.fargekategori;

import com.fasterxml.jackson.annotation.JsonFormat;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.NavIdent;

import java.time.LocalDate;
import java.util.UUID;

public record FargekategoriEntity(
        UUID id,
        Fnr fnr,
        FargekategoriVerdi fargekategoriVerdi,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate sistEndret,
        NavIdent endretAv,
        EnhetId enhetId
) {}
