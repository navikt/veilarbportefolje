package no.nav.pto.veilarbportefolje.aap

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.aap.domene.AapVedtakResponseDto
import no.nav.pto.veilarbportefolje.domene.AktorClient
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2
import no.nav.pto.veilarbportefolje.util.DateUtils.toLocalDate
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class AapService(val aapClient: AapClient, val aktorClient: AktorClient, val oppfolgingRepositoryV2: OppfolgingRepositoryV2) {

    fun hentAapVedtakForOppfolgingPeriode(personIdent: String): AapVedtakResponseDto {
        // funker dette også for d-nummer?
        val aktorId: AktorId = aktorClient.hentAktorId(Fnr.of(personIdent))
        val oppfolgingsStartdato = hentOppfolgingStartdato(aktorId)
        val ettAarIFramtiden = LocalDate.now().plusYears(1).toString()

        val aapRespons = aapClient.hentAapVedtak(personIdent, oppfolgingsStartdato.toString(), ettAarIFramtiden)
        val aapIOppfolgingsPeriode = aapRespons.vedtak
            .mapNotNull { vedtak ->
                val filtrertPeriode = filtrerAapKunIOppfolgingPeriode(oppfolgingsStartdato, vedtak.periode)
                filtrertPeriode?.let { vedtak.copy(periode = it) }
            }

        return AapVedtakResponseDto(vedtak = aapIOppfolgingsPeriode)
    }


    fun filtrerAapKunIOppfolgingPeriode(oppfolgingsStartdato: LocalDate, aapPeriode: AapVedtakResponseDto.Periode): AapVedtakResponseDto.Periode? {
        //perioder startet og avsluttet før oppfølgingstart utelates
        if (aapPeriode.fraOgMedDato.isBefore(oppfolgingsStartdato) && aapPeriode.tilOgMedDato.isBefore(oppfolgingsStartdato) ) {
            return null
        }
        return  aapPeriode
    }

    fun hentOppfolgingStartdato(aktorId: AktorId): LocalDate {
        val oppfolgingsdata = oppfolgingRepositoryV2.hentOppfolgingData(aktorId)
            .orElseThrow { IllegalStateException("Ingen oppfølgingsdata funnet for $aktorId") }

        if (oppfolgingsdata.oppfolging && oppfolgingsdata.startDato != null) {
            return toLocalDate(oppfolgingsdata.startDato)
        }

        throw IllegalStateException("Bruker er ikke under oppfølging")
    }
}
