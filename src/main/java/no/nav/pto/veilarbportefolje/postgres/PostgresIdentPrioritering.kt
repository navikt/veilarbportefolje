package no.nav.pto.veilarbportefolje.postgres

import no.nav.poao_tilgang.client.NorskIdent
import no.nav.pto.veilarbportefolje.opensearch.domene.PortefoljebrukerOpensearchModell
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import no.nav.pto.veilarbportefolje.persononinfo.domene.NorskIdentMedHistorikk
import no.nav.pto.veilarbportefolje.util.SecureLog.secureLog
import java.time.LocalDateTime

internal fun hentFnrIdenterPerBruker(
    pdlIdentRepository: PdlIdentRepository,
    brukere: List<PortefoljebrukerOpensearchModell>
): Map<NorskIdent, List<NorskIdentMedHistorikk>> {
    val fnrs = brukere.mapNotNull { it.fnr }
    val fnrIdenterPerBruker = pdlIdentRepository.hentFnrIdenterForBrukere(fnrs)
    fnrs.filterNot { fnrIdenterPerBruker.containsKey(it) }.forEach { fnr ->
        secureLog.error("Fant ingen folkeregisteridenter i bruker_identer for fnr: $fnr")
    }
    return fnrIdenterPerBruker
}

internal fun alleIdenter(fnrIdenterPerBruker: Map<NorskIdent, List<NorskIdentMedHistorikk>>): List<NorskIdent> =
    fnrIdenterPerBruker.values.flatten().map { it.fnr }.distinct()

// Hvis det finnes flere rader så skal aktiv ident prioriteres. Deretter velges raden med siste "sist endret" tidspunkt.
internal fun <T> velgDataForPrioritertIdent(
    datatype: String,
    fnr: NorskIdent?,
    identer: List<NorskIdentMedHistorikk>,
    data: Map<NorskIdent, T>,
    sistEndret: (T) -> LocalDateTime
): T? {
    val identerMedData = identer.filter { data.containsKey(it.fnr) }
    if (identerMedData.size > 1) {
        secureLog.warn("Fant $datatype på flere fnr for samme person. Oppslag: $fnr, identer med $datatype: ${identerMedData.map { it.fnr }}")
    }
    val valgtIdent = identerMedData.firstOrNull { !it.historisk }
        ?: identerMedData.maxByOrNull { sistEndret(data.getValue(it.fnr)) }
    return valgtIdent?.let { data[it.fnr] }
}
