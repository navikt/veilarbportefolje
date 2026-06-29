package no.nav.pto.veilarbportefolje.aap.dto

import no.nav.pto.veilarbportefolje.aap.domene.AapRettighetstype
import no.nav.pto.veilarbportefolje.aap.domene.AapVedtakStatus
import java.time.LocalDate


data class AapVedtakResponseDto(
    val vedtak: List<Vedtak>, // vedtak må fornyes minst en gang i året for å forlenge retten på ytelsen.
    val sakstatus: String, // kelvin-teamet rydder i enumsene her, så bør gjøres om til gyldige enums når de er ferdige. Er status på saken, som er flere statuser en for faktiske vedtak, eg avslag.
    val maksdato: LocalDate?  // kan være null i starten, men etterhvert som maksdatoer blir generert skal den alltid komme med.
    // Maksdato er en beregnet dato som viser hvor lenge en bruker har rett på ytelsen, typisk 3 år for første gang man får aap.
) {
    data class Vedtak(
        val status: AapVedtakStatus, // status på vedtaket (det kan være flere vedtak på en sak)
        val saksnummer: String, // saknummer for aap - vil i framtiden brukes til å gi en direkte lenke til kelvin for veilederne
        val periode: Periode, // Perioden vedtaket er gyldig
        val rettighetsType: AapRettighetstype, // hva slags type aap en bruker har fått. "Bistandsbehov" er den mest vanlige / ordinære.
    )

    data class Periode(
        val fraOgMedDato: LocalDate,
        val tilOgMedDato: LocalDate
    )
}

