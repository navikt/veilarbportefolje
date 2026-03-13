package no.nav.pto.veilarbportefolje.oppfolgingsperiodeEndret.dto

import java.time.ZonedDateTime
import java.util.UUID

class AvsluttetOppfolgingsperiodeV3Dto(
    oppfolgingsperiodeUuid: UUID,
    sisteEndringsType: SisteEndringsType,
    aktorId: String,
    ident: String,
    startTidspunkt: ZonedDateTime,
    sluttTidspunkt: ZonedDateTime,
    producerTimestamp: ZonedDateTime = ZonedDateTime.now(),
) : SisteOppfolgingsperiodeV3Dto(
    oppfolgingsperiodeUuid, sisteEndringsType, aktorId, ident, startTidspunkt, sluttTidspunkt, null, producerTimestamp,
)
