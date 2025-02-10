package no.nav.pto.veilarbportefolje.gjeldende14aVedtak

import no.nav.pto.veilarbportefolje.siste14aVedtak.Siste14aVedtakForBruker
import java.time.ZoneId
import java.time.ZonedDateTime

class Gjeldende14aVedtakService {
    companion object {
        @JvmField
        val LANSERINGSDATO_VEILARBOPPFOLGING_OPPFOLGINGSPERIODE: ZonedDateTime =
            ZonedDateTime.of(2017, 12, 4, 0, 0, 0, 0, ZoneId.systemDefault())

        @JvmStatic
        fun erVedtakGjeldende(
            siste14aVedtakForBruker: Siste14aVedtakForBruker,
            startDatoInnevarendeOppfolgingsperiode: ZonedDateTime
        ): Boolean {
            val erVedtaketFattetIInnevarendeOppfolgingsperiode =
                siste14aVedtakForBruker.fattetDato.isAfter(startDatoInnevarendeOppfolgingsperiode)
            val erVedtaketFattetForLanseringsdatoForVeilarboppfolging = siste14aVedtakForBruker.fattetDato
                .isBefore(LANSERINGSDATO_VEILARBOPPFOLGING_OPPFOLGINGSPERIODE)
            val erStartdatoForOppfolgingsperiodeLikLanseringsdatoForVeilarboppfolging =
                !startDatoInnevarendeOppfolgingsperiode
                    .isAfter(LANSERINGSDATO_VEILARBOPPFOLGING_OPPFOLGINGSPERIODE)

            return erVedtaketFattetIInnevarendeOppfolgingsperiode ||
                    (erVedtaketFattetForLanseringsdatoForVeilarboppfolging
                            && erStartdatoForOppfolgingsperiodeLikLanseringsdatoForVeilarboppfolging)
        }
    }
}