package no.nav.pto.veilarbportefolje.sykefravaer.dto

import java.time.OffsetDateTime

data class SykefravaerPersonKafkaDTO(
    val uuid: String,
    val createdAt: OffsetDateTime,
    val personIdentNumber: String,
)
