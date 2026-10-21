package no.nav.pto.veilarbportefolje.sykmeldt.dto

import java.time.LocalDate

data class SykmeldtPersonDTO(
    val oppfolgingstilfelleList: List<OppfolgingstilfelleDTO>,
    val personIdent: String,
)

// Aktuell periode er den første i lista. De filtrerer bort fremtidige perioder. Om "end" er passert så er ikke bruker sykmeldt.
// arbeidstakerAtTilfelleEnd sier om bruker er arbeidstaker ved slutten ("end") av perioden.
data class OppfolgingstilfelleDTO(
    val arbeidstakerAtTilfelleEnd: Boolean,
    val start: LocalDate,
    val end: LocalDate,
)
