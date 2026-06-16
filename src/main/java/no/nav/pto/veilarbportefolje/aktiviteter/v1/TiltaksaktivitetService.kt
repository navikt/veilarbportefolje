package no.nav.pto.veilarbportefolje.aktiviteter.v1

import no.nav.common.types.identer.EnhetId
import no.nav.pto.veilarbportefolje.aktiviteter.domene.AktivitetIkkeAktivStatuser
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.EnhetTiltak
import org.springframework.stereotype.Component

typealias TiltakKode = String

@Component
class TiltaksaktivitetService(
    val kafkaAktivitetMeldingRepository: KafkaAktivitetMeldingRepository,
    val brukertiltakRepository: BrukertiltakRepository
) {
    fun hentTiltakSomPersonDeltarPaaBulk(
        personIdenter: Set<String>
    ): Map<String, Set<TiltakKode>> {
        val personKafkaAktivitetMeldingerMap = kafkaAktivitetMeldingRepository.listKafkaAktivitetMeldingerBulk(
            personIdenter = personIdenter,
            avtalt = true,
            aktivitetStatusFilter = AktivitetIkkeAktivStatuser.entries.toSet()
        )
        val personBrukertiltakMap = brukertiltakRepository.hentBrukertiltakBulk(personidenter = personIdenter)

        return personIdenter.associateWith { personIdent ->
            val kafkaAktivitetMeldinger = personKafkaAktivitetMeldingerMap[personIdent] ?: emptyList()
            val brukerTiltak =
                personBrukertiltakMap[personIdent]?.filterNot { brukerTiltakFilter.contains(it.tiltakskode) } ?: emptyList()

            (kafkaAktivitetMeldinger.map { it.tiltakskode } + brukerTiltak.map { it.tiltakskode })
                .filterNotNull()
                .toSet()
        }
    }

    //Henter tiltakskoder kun fra den nye tiltakstabellen KAFKA_AKTIVITET_MELDING.

    fun finnTiltakstyperBrukerDeltarPaa(
        enhetId: EnhetId,
    ): EnhetTiltak {
        return kafkaAktivitetMeldingRepository.hentTiltakskoderForBrukerePaaEnhet(enhetId)
    }

    companion object {
        val brukerTiltakFilter = setOf("GRUPPEAMO", "GRUFAGYRKE")
    }
}