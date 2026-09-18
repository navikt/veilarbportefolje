package no.nav.pto.veilarbportefolje.lagredefilter.veiledergrupper

import no.nav.common.types.identer.EnhetId
import no.nav.pto.veilarbportefolje.client.VeilarbVeilederClient
import no.nav.pto.veilarbportefolje.lagredefilter.harGyldigFilterNavn
import no.nav.pto.veilarbportefolje.lagredefilter.harUniktNavnOgFiltervalg
import no.nav.pto.veilarbportefolje.lagredefilter.veiledergrupper.domene.LagretVeiledergruppe
import no.nav.pto.veilarbportefolje.lagredefilter.veiledergrupper.domene.NyVeiledergruppeRequest
import no.nav.pto.veilarbportefolje.lagredefilter.veiledergrupper.domene.OppdaterVeiledergruppeRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class VeiledergrupperService(
    private val veiledergrupperRepository: VeiledergrupperRepository,
    private val veilarbVeilederClient: VeilarbVeilederClient,
) {

    private val log = org.slf4j.LoggerFactory.getLogger(VeiledergrupperService::class.java)

    fun hentVeiledergrupperForEnhet(enhetId: String): List<LagretVeiledergruppe> {
        return veiledergrupperRepository.hentVeiledergrupperForEnhet(enhetId)
    }

    fun lagreNyVeiledergruppeForEnhet(
        enhetId: String,
        nyVeildergruppeRequest: NyVeiledergruppeRequest
    ): LagretVeiledergruppe {
        validerFilterNavnEllerKast(nyVeildergruppeRequest.filterNavn)
        validerVeiledereEllerKast(nyVeildergruppeRequest.veiledere)
        validerUnikhetEllerKast(enhetId, nyVeildergruppeRequest.filterNavn, nyVeildergruppeRequest.veiledere)
        return veiledergrupperRepository.lagreNyVeiledergruppeForEnhet(enhetId, nyVeildergruppeRequest)
    }

    fun oppdaterVeiledergruppeForEnhet(
        enhetId: String,
        oppdaterVeildergruppeRequest: OppdaterVeiledergruppeRequest
    ): LagretVeiledergruppe {
        validerFilterNavnEllerKast(oppdaterVeildergruppeRequest.filterNavn)
        validerVeiledereEllerKast(oppdaterVeildergruppeRequest.veiledere)
        validerUnikhetEllerKast(
            enhetId,
            oppdaterVeildergruppeRequest.filterNavn,
            oppdaterVeildergruppeRequest.veiledere,
            ekskluderFilterId = oppdaterVeildergruppeRequest.filterId
        )
        return veiledergrupperRepository.oppdaterVeiledergruppeForEnhet(enhetId, oppdaterVeildergruppeRequest)
    }

    fun slettVeiledergruppeForEnhet(enhetId: String, filterId: Int): Int {
        return veiledergrupperRepository.slettVeiledergruppeForEnhet(enhetId, filterId)
    }

    fun slettVeiledereSomIkkeErAktiveForHverEnhet() {
        val enheter = veiledergrupperRepository.hentAlleEnheter()

        enheter.forEach { enhetId ->
            try {
                val aktiveVeilederePaEnheten = veilarbVeilederClient
                    .hentVeilederePaaEnhetMachineToMachine(EnhetId.of(enhetId))
                    .toSet()

                if (aktiveVeilederePaEnheten.isEmpty()) {
                    log.warn("Ingen aktive veiledere returnert for enhet $enhetId, hopper over")
                    return@forEach
                }

                val lagredeVeiledergrupperPaEnheten = veiledergrupperRepository.hentVeiledergrupperForEnhet(enhetId)

                lagredeVeiledergrupperPaEnheten.forEach { lagretVeiledergruppe ->
                    val (veiledereSomFortsattErAktive, veiledereSomIkkeErAktive) =
                        lagretVeiledergruppe.veiledere.partition { it in aktiveVeilederePaEnheten }

                    if (veiledereSomIkkeErAktive.isEmpty()) {
                        return@forEach
                    }

                    log.info("Fjernet veiledere: ${veiledereSomIkkeErAktive.joinToString(", ")}")

                    // Hvis ingen veiledere er aktive lenger, slett hele veiledergruppen.
                    if (veiledereSomFortsattErAktive.isEmpty()) {
                        slettVeiledergruppeForEnhet(enhetId, lagretVeiledergruppe.filterId)
                        log.info("Fjernet veiledergruppe: ${lagretVeiledergruppe.filterNavn} fra enhet: $enhetId")

                    } else {
                        val updatedVeilederGruppe = OppdaterVeiledergruppeRequest(
                            filterId = lagretVeiledergruppe.filterId,
                            filterNavn = lagretVeiledergruppe.filterNavn,
                            veiledere = veiledereSomFortsattErAktive
                        )
                        oppdaterVeiledergruppeForEnhet(enhetId, updatedVeilederGruppe)
                        log.info("Oppdatert veiledergruppe: ${lagretVeiledergruppe.filterNavn} fra enhet: $enhetId")
                    }
                }
            } catch (e: Exception) {
                log.error("Feil ved opprydding av veiledergrupper for enhet: $enhetId", e)
            }
        }
    }

    private fun validerFilterNavnEllerKast(filterNavn: String) {
        if (!harGyldigFilterNavn(filterNavn)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST)
        }
    }

    private fun validerVeiledereEllerKast(veiledere: List<String>) {
        if (veiledere.isEmpty()) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST
            )
        }
    }

    private fun validerUnikhetEllerKast(
        enhetId: String,
        filterNavn: String,
        veiledere: List<String>,
        ekskluderFilterId: Int? = null
    ) {
        val navnEksisterer = veiledergrupperRepository.eksistererFilterNavn(enhetId, filterNavn, ekskluderFilterId)
        val veiledereEksisterer = veiledergrupperRepository.eksistererVeiledere(enhetId, veiledere, ekskluderFilterId)
        if (!harUniktNavnOgFiltervalg(navnEksisterer, veiledereEksisterer)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST)
        }
    }
}
