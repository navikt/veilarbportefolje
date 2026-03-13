package no.nav.pto.veilarbportefolje.oppfolgingsperiodeEndret.dto

import java.time.ZonedDateTime
import java.util.UUID

class GjeldendeOppfolgingsperiodeV3Dto(
    oppfolgingsperiodeUuid: UUID,
    sisteEndringsType: SisteEndringsType,
    aktorId: String,
    ident: String,
    startTidspunkt: ZonedDateTime,
    kontor: KontorDto,
    producerTimestamp: ZonedDateTime = ZonedDateTime.now(),
) : SisteOppfolgingsperiodeV3Dto(
    oppfolgingsperiodeUuid, sisteEndringsType, aktorId, ident, startTidspunkt, null, kontor, producerTimestamp,
)
