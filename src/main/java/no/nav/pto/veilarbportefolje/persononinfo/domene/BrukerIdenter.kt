package no.nav.pto.veilarbportefolje.persononinfo.domene

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.NorskIdent
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository

data class BrukerIdenter(
    private val identer: List<PDLIdent>
) {
    init {
        require(identer.any { erAktivNorskIdent(it) }) { "Må inneholde minst én aktiv FOLKEREGISTERIDENT" }
        require(identer.any { erAktivAktorId(it) }) { "Må inneholde minst én aktiv AKTORID" }
    }

    val aktivNorskIdent: NorskIdent
        get() = NorskIdent.of(PdlIdentRepository.pdlIdentVerdi(identer.first { erAktivNorskIdent(it) }))

    val aktivAktorId: AktorId
        get() = AktorId.of(PdlIdentRepository.pdlIdentVerdi(identer.first { erAktivAktorId(it) }))

    val alleNorskeIdenter: List<NorskIdent>
        get() = identer
            .filter { PdlIdentRepository.pdlIdentGruppe(it) == PDLIdent.Gruppe.FOLKEREGISTERIDENT }
            .map { NorskIdent.of(PdlIdentRepository.pdlIdentVerdi(it)) }

    val alleAktorId: List<AktorId>
        get() = identer
            .filter { PdlIdentRepository.pdlIdentGruppe(it) == PDLIdent.Gruppe.AKTORID }
            .map { AktorId.of(PdlIdentRepository.pdlIdentVerdi(it)) }

    companion object {
        private fun erAktivNorskIdent(ident: PDLIdent): Boolean =
            PdlIdentRepository.pdlIdentGruppe(ident) == PDLIdent.Gruppe.FOLKEREGISTERIDENT &&
                    !PdlIdentRepository.pdlIdentErHistorisk(ident)

        private fun erAktivAktorId(ident: PDLIdent): Boolean =
            PdlIdentRepository.pdlIdentGruppe(ident) == PDLIdent.Gruppe.AKTORID &&
                    !PdlIdentRepository.pdlIdentErHistorisk(ident)

        @JvmStatic
        fun fraPdlIdenter(pdlIdenter: List<PDLIdent>): BrukerIdenter? {
            if (pdlIdenter.none { erAktivNorskIdent(it) } || pdlIdenter.none { erAktivAktorId(it) }) {
                return null
            }
            return BrukerIdenter(pdlIdenter)
        }
    }
}
