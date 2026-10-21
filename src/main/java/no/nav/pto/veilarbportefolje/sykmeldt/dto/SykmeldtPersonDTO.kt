package no.nav.pto.veilarbportefolje.sykmeldt.dto

import java.time.LocalDate

data class SykmeldtPersonDTO(
    val oppfolgingstilfelleList: List<OppfolgingstilfelleDTO>,
    val personIdent: String,
)

// Første i lista om det er flere er den vi skal bruke. De filtrerer bort fremtidige perioder. Om "end" er passert så er ikke bruker sykmeldt.
// arbeidstakerAtTilfelleEnd sier om bruker er arbeidstaker ved slutten ("end") av perioden.
data class OppfolgingstilfelleDTO(
    val arbeidstakerAtTilfelleEnd: Boolean,
    val start: LocalDate,
    val end: LocalDate,
)
