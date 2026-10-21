package no.nav.pto.veilarbportefolje.sykmeldt.dto

import java.time.OffsetDateTime

data class SykmeldtPersonKafkaDTO(
    val uuid: String,
    val createdAt: OffsetDateTime,
    val personIdentNumber: String,
)
