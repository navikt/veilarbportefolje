package no.nav.pto.veilarbportefolje.fargekategori;

import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.NavIdent;

import java.time.ZonedDateTime;
import java.util.UUID;

public record FargekategoriEntity(
        UUID id,
        Fnr fnr,
        FargekategoriVerdi fargekategoriVerdi,
        ZonedDateTime sistEndret,
        NavIdent endretAv
) {}
