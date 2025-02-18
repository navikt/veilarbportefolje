package no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.gjeldende14aVedtak

import no.nav.common.types.identer.AktorId
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2
import no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.siste14aVedtak.Siste14aVedtakForBruker
import no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.siste14aVedtak.Siste14aVedtakRepository
import no.nav.pto.veilarbportefolje.vedtakstotte.Hovedmal
import no.nav.pto.veilarbportefolje.vedtakstotte.Innsatsgruppe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

@Service
class Gjeldende14aVedtakService(
    @Autowired val siste14aVedtakRepository: Siste14aVedtakRepository,
    @Autowired val oppfolgingRepositoryV2: OppfolgingRepositoryV2
) {
    fun hentGjeldende14aVedtak(brukerIdenter: Set<AktorId>): Map<AktorId, Optional<Gjeldende14aVedtak>> {
        val aktorIdSiste14aVedtakMap: Map<AktorId, Optional<Siste14aVedtakForBruker>> =
            siste14aVedtakRepository.hentSiste14aVedtakForBrukere(brukerIdenter)
                .mapValues { Optional.ofNullable(it.value) }
        val aktorIdStartDatoOppfolgingMap: Map<AktorId, Optional<ZonedDateTime>> =
            oppfolgingRepositoryV2.hentStartDatoForOppfolging(brukerIdenter)

        return brukerIdenter.associate { brukerIdent ->
            val maybeSiste14aVedtak: Optional<Siste14aVedtakForBruker> =
                aktorIdSiste14aVedtakMap[brukerIdent] ?: Optional.empty()
            val maybeStartDatoOppfolging: Optional<ZonedDateTime> =
                aktorIdStartDatoOppfolgingMap[brukerIdent] ?: Optional.empty()

            if (maybeSiste14aVedtak.isEmpty || maybeStartDatoOppfolging.isEmpty) {
                return@associate brukerIdent to Optional.empty<Gjeldende14aVedtak>()
            }

            if (!sjekkOmVedtakErGjeldende(maybeSiste14aVedtak.get(), maybeStartDatoOppfolging.get())) {
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
        val aktorIdSiste14aVedtakMap: Map<AktorId, Optional<Siste14aVedtakForBruker>> =
            siste14aVedtakRepository.hentSiste14aVedtakForBrukere(setOf(brukerIdent))
                .mapValues { Optional.ofNullable(it.value) }
        val aktorIdStartDatoOppfolgingMap: Map<AktorId, Optional<ZonedDateTime>> =
            oppfolgingRepositoryV2.hentStartDatoForOppfolging(setOf(brukerIdent))

        val maybeSiste14aVedtak: Optional<Siste14aVedtakForBruker> =
            aktorIdSiste14aVedtakMap[brukerIdent] ?: Optional.empty()
        val maybeStartDatoOppfolging: Optional<ZonedDateTime> =
            aktorIdStartDatoOppfolgingMap[brukerIdent] ?: Optional.empty()

        if (maybeSiste14aVedtak.isEmpty || maybeStartDatoOppfolging.isEmpty) {
            return Optional.empty<Gjeldende14aVedtak>()
        }

        if (!sjekkOmVedtakErGjeldende(maybeSiste14aVedtak.get(), maybeStartDatoOppfolging.get())) {
            return Optional.empty<Gjeldende14aVedtak>()
        }

        return maybeSiste14aVedtak.get().let {
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

    companion object {
        @JvmField
        val LANSERINGSDATO_VEILARBOPPFOLGING_OPPFOLGINGSPERIODE: ZonedDateTime =
            ZonedDateTime.of(2017, 12, 4, 0, 0, 0, 0, ZoneId.systemDefault())

        @JvmStatic
        fun sjekkOmVedtakErGjeldende(
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
    }
}

data class Gjeldende14aVedtak(
    val aktorId: AktorId,
    val innsatsgruppe: Innsatsgruppe,
    val hovedmal: Hovedmal?,
    val fattetDato: ZonedDateTime
)