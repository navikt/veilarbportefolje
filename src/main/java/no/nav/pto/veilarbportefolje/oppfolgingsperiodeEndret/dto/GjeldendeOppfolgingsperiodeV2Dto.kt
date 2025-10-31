package no.nav.pto.veilarbportefolje.oppfolgingsperiodeEndret.dto

import java.time.ZonedDateTime
import java.util.UUID

class GjeldendeOppfolgingsperiodeV2Dto(
    oppfolgingsperiodeId: UUID,
    startTidspunkt: ZonedDateTime,
    val kontorId: String,
    val kontorNavn: String,
    aktorId: String,
    ident: String,
    sisteEndringsType: SisteEndringsType,
    producerTimestamp: ZonedDateTime,
) : SisteOppfolgingsperiodeV2Dto(
    oppfolgingsperiodeId, startTidspunkt, sisteEndringsType, aktorId, ident, producerTimestamp,
)