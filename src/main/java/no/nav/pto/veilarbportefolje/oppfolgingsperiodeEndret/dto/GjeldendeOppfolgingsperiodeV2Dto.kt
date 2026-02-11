package no.nav.pto.veilarbportefolje.oppfolgingsperiodeEndret.dto

import java.time.ZonedDateTime
import java.util.UUID

class GjeldendeOppfolgingsperiodeV2Dto(
    oppfolgingsperiodeUuid: UUID,
    sisteEndringsType: SisteEndringsType,
    aktorId: String,
    ident: String,
    startTidspunkt: ZonedDateTime,
    kontor: KontorDto,
    producerTimestamp: ZonedDateTime = ZonedDateTime.now(),
) : SisteOppfolgingsperiodeV2Dto(
    oppfolgingsperiodeUuid, sisteEndringsType, aktorId, ident, startTidspunkt, null, kontor, producerTimestamp,
)