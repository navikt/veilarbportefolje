package no.nav.pto.veilarbportefolje.fargekategori;

import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.NavIdent;

import java.time.LocalDate;
import java.util.UUID;

public record FargekategoriEntity(
        UUID id,
        Fnr fnr,
        FargekategoriVerdi fargekategoriVerdi,
        LocalDate sistEndret,
        NavIdent endretAv,
        EnhetId enhetId
) {}
