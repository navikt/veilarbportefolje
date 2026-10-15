package no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.gjeldende14aVedtak

import no.nav.common.types.identer.AktorId
import no.nav.pto.veilarbportefolje.domene.NavKontor
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerRepositoryV3
import no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.siste14aVedtak.Siste14aVedtakForBruker
import no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.siste14aVedtak.Siste14aVedtakRepository
import no.nav.pto.veilarbportefolje.util.DateUtils.fromZonedDateTimeToLocalDateOrNull
import no.nav.pto.veilarbportefolje.vedtakstotte.Hovedmal
import no.nav.pto.veilarbportefolje.vedtakstotte.Innsatsgruppe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

@Service
class Gjeldende14aVedtakService(
    @param:Autowired val siste14aVedtakRepository: Siste14aVedtakRepository,
    @param:Autowired val oppfolgingRepositoryV2: OppfolgingRepositoryV2,
    @param:Autowired val oppfolgingsbrukerRepositoryV3: OppfolgingsbrukerRepositoryV3
) {
    fun hentGjeldende14aVedtak(brukerIdenter: Set<AktorId>): Map<AktorId, Optional<Gjeldende14aVedtak>> {
        val aktorIdSiste14aVedtakMap: Map<AktorId, Optional<Siste14aVedtakForBruker>> =
            siste14aVedtakRepository.hentSiste14aVedtakForBrukere(brukerIdenter)
                .mapValues { Optional.ofNullable(it.value) }
        val aktorIdStartDatoOppfolgingMap: Map<AktorId, Optional<ZonedDateTime>> =
            oppfolgingRepositoryV2.hentStartDatoForOppfolging(brukerIdenter)
        val brukereMedVedtakFraVedtaksstotte: Set<AktorId> = aktorIdSiste14aVedtakMap
            .filterValues { it.isPresent && !it.get().isFraArena }
            .keys
        val aktorIdNavKontorMap: Map<AktorId, NavKontor> =
            oppfolgingsbrukerRepositoryV3.hentNavKontorForBrukere(brukereMedVedtakFraVedtaksstotte)

        return brukerIdenter.associate { brukerIdent ->
            val maybeSiste14aVedtak: Optional<Siste14aVedtakForBruker> =
                aktorIdSiste14aVedtakMap[brukerIdent] ?: Optional.empty()
            val maybeStartDatoOppfolging: Optional<ZonedDateTime> =
                aktorIdStartDatoOppfolgingMap[brukerIdent] ?: Optional.empty()

            if (maybeSiste14aVedtak.isEmpty || maybeStartDatoOppfolging.isEmpty) {
                return@associate brukerIdent to Optional.empty<Gjeldende14aVedtak>()
            }

            if (!sjekkOmVedtakErGjeldende(
                    maybeSiste14aVedtak.get(),
                    maybeStartDatoOppfolging.get(),
                    aktorIdNavKontorMap[brukerIdent]
                )
            ) {
                return@associate brukerIdent to Optional.empty<Gjeldende14aVedtak>()
            }

            return@associate brukerIdent to maybeSiste14aVedtak.get().let {
                Optional.of(
                    Gjeldende14aVedtak(
                        aktorId = it.aktorId,
                        innsatsgruppe = it.innsatsgruppe,
                        hovedmal = it.hovedmal,
                        fattetDato = it.fattetDato
                    )
                )
            }
        }
    }

    fun hentGjeldende14aVedtak(brukerIdent: AktorId): Optional<Gjeldende14aVedtak> {
        return hentGjeldende14aVedtak(setOf(brukerIdent))[brukerIdent] ?: Optional.empty()
    }

    companion object {
        @JvmField
        val LANSERINGSDATO_VEILARBOPPFOLGING_OPPFOLGINGSPERIODE: ZonedDateTime =
            ZonedDateTime.of(2017, 12, 4, 0, 0, 0, 0, ZoneId.systemDefault())

        private const val NAV_OSTENSJO = "0321"

        fun sjekkOmVedtakErGjeldende(
            siste14aVedtakForBruker: Siste14aVedtakForBruker,
            startDatoInnevarendeOppfolgingsperiode: ZonedDateTime,
            navKontor: NavKontor?
        ): Boolean {
            return if (siste14aVedtakForBruker.isFraArena) {
                erVedtakFraArenaGjeldende(siste14aVedtakForBruker, startDatoInnevarendeOppfolgingsperiode)
            } else {
                erVedtakFraVeilarbvedtaksstotteGjeldende(
                    siste14aVedtakForBruker, startDatoInnevarendeOppfolgingsperiode, navKontor
                )
            }
        }

        private fun erVedtakFraArenaGjeldende(
            siste14aVedtakForBruker: Siste14aVedtakForBruker,
            startDatoInnevarendeOppfolgingsperiode: ZonedDateTime
        ): Boolean {
            // 2025-02-18
            // Vi har oppdaget at vedtak fattet i Arena får "fattetDato" lik midnatt den dagen vedtaket ble fattet.
            // Derfor har vi valgt å innfør en "grace periode" på 4 døgn. Dvs. dersom vedtaket ble fattet etter
            // "oppfølgingsperiode startdato - 4 døgn", så anser vi det som gjeldende.
            val erVedtaketFattetIInnevarendeOppfolgingsperiodeMedGracePeriodePa4Dogn =
                siste14aVedtakForBruker.fattetDato.isAfter(startDatoInnevarendeOppfolgingsperiode.minusDays(4))
            val erVedtaketFattetForLanseringsdatoForVeilarboppfolging = siste14aVedtakForBruker.fattetDato
                .isBefore(LANSERINGSDATO_VEILARBOPPFOLGING_OPPFOLGINGSPERIODE)
            val erStartdatoForOppfolgingsperiodeLikLanseringsdatoForVeilarboppfolging =
                !startDatoInnevarendeOppfolgingsperiode
                    .isAfter(LANSERINGSDATO_VEILARBOPPFOLGING_OPPFOLGINGSPERIODE)

            return erVedtaketFattetIInnevarendeOppfolgingsperiodeMedGracePeriodePa4Dogn ||
                    (erVedtaketFattetForLanseringsdatoForVeilarboppfolging
                            && erStartdatoForOppfolgingsperiodeLikLanseringsdatoForVeilarboppfolging)
        }

        private fun erVedtakFraVeilarbvedtaksstotteGjeldende(
            siste14aVedtakForBruker: Siste14aVedtakForBruker,
            startDatoInnevarendeOppfolgingsperiode: ZonedDateTime,
            navKontor: NavKontor?
        ): Boolean {
            // Nav-østensjø var pilot for løsningen, og det finnes dermed noen få vedtak som har fattet dato før oppfølingstartet.
            // Disse håndteres her (kun 11 stk per 10.2026) med grace periode på samme dag
            if (navKontor?.value == NAV_OSTENSJO) {
                val fattetDato = fromZonedDateTimeToLocalDateOrNull(siste14aVedtakForBruker.fattetDato)
                val startdato = fromZonedDateTimeToLocalDateOrNull(startDatoInnevarendeOppfolgingsperiode)
                return !fattetDato.isBefore(startdato)
            }

            return siste14aVedtakForBruker.fattetDato.isAfter(startDatoInnevarendeOppfolgingsperiode)
        }
    }
}

data class Gjeldende14aVedtak(
    val aktorId: AktorId,
    val innsatsgruppe: Innsatsgruppe,
    val hovedmal: Hovedmal?,
    val fattetDato: ZonedDateTime
)
