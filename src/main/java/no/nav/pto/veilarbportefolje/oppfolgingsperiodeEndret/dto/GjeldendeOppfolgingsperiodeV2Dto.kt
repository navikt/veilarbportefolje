package no.nav.pto.veilarbportefolje.oppfolgingsperiodeEndret.dto

import java.time.ZonedDateTime
import java.util.UUID

class GjeldendeOppfolgingsperiodeV2Dto(
    oppfolgingsperiodeUuid: UUID,
    startTidspunkt: ZonedDateTime,
    val kontorId: String,
    val kontorNavn: String,
    aktorId: String,
    ident: String,
    sisteEndringsType: SisteEndringsType,
    producerTimestamp: ZonedDateTime,
) : SisteOppfolgingsperiodeV2Dto(
    oppfolgingsperiodeUuid, startTidspunkt, sisteEndringsType, aktorId, ident, producerTimestamp,
)