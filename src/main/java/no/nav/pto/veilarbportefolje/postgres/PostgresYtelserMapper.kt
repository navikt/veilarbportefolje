package no.nav.pto.veilarbportefolje.postgres

import no.nav.poao_tilgang.client.NorskIdent
import no.nav.pto.veilarbportefolje.aap.AapRepository
import no.nav.pto.veilarbportefolje.aap.domene.AapVedtakEntity
import no.nav.pto.veilarbportefolje.dagpenger.DagpengerRepository
import no.nav.pto.veilarbportefolje.dagpenger.domene.DagpengerVedtakEntity
import no.nav.pto.veilarbportefolje.domene.opensearchmodell.DagpengerForOpensearch
import no.nav.pto.veilarbportefolje.domene.opensearchmodell.UforetrygdForOpensearch
import no.nav.pto.veilarbportefolje.opensearch.domene.PortefoljebrukerOpensearchModell
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import no.nav.pto.veilarbportefolje.persononinfo.domene.NorskIdentMedHistorikk
import no.nav.pto.veilarbportefolje.tiltakspenger.TiltakspengerRespository
import no.nav.pto.veilarbportefolje.tiltakspenger.domene.TiltakspengerRettighet
import no.nav.pto.veilarbportefolje.tiltakspenger.domene.TiltakspengerVedtakEntity
import no.nav.pto.veilarbportefolje.uforetrygd.UforetrygdRepository
import no.nav.pto.veilarbportefolje.uforetrygd.domene.UforetrygdEntity
import no.nav.pto.veilarbportefolje.util.SecureLog.secureLog
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class PostgresYtelserMapper(
    private val aapRepository: AapRepository,
    private val dagpengerRepository: DagpengerRepository,
    private val uforetrygdRepository: UforetrygdRepository,
    private val tiltakspengerRepository: TiltakspengerRespository,
    private val pdlIdentRepository: PdlIdentRepository
) {

    fun flettInnYtelser(brukere: List<PortefoljebrukerOpensearchModell>) {
        val fnrIdenterPerBruker = hentFnrIdenterPerBruker(brukere)
        flettInnAapKelvin(brukere, fnrIdenterPerBruker)
        flettInnDagpenger(brukere, fnrIdenterPerBruker)
        flettInnUforetrygd(brukere, fnrIdenterPerBruker)
        flettInnTiltakspenger(brukere, fnrIdenterPerBruker)
    }

    private fun flettInnAapKelvin(
        brukere: List<PortefoljebrukerOpensearchModell>,
        fnrIdenterPerBruker: Map<NorskIdent, List<NorskIdentMedHistorikk>>
    ) {
        val aapVedtakForAlleIdenterPerBruker =
            aapRepository.hentAapKelvinVedtak(alleIdenter(fnrIdenterPerBruker))

        brukere.forEach { bruker ->
            val identer = bruker.fnr?.let { fnrIdenterPerBruker[it] } ?: emptyList()
            val aktueltVedtak =
                velgVedtak("AAP", bruker.fnr, identer, aapVedtakForAlleIdenterPerBruker, AapVedtakEntity::radSistEndret)
            val vedtakErGjeldende =
                aktueltVedtak != null && aktueltVedtak.nyestePeriodeTom.isAfter(LocalDate.now().minusDays(1))

            bruker.aap_kelvin = vedtakErGjeldende
            bruker.aap_kelvin_tom_vedtaksdato = if (vedtakErGjeldende) aktueltVedtak.nyestePeriodeTom else null
            bruker.aap_kelvin_rettighetstype = if (vedtakErGjeldende) aktueltVedtak.rettighetstype else null
            bruker.aap_kelvin_maksdato = if (vedtakErGjeldende) aktueltVedtak.maksdato else null
        }
    }

    private fun flettInnDagpenger(
        brukere: List<PortefoljebrukerOpensearchModell>,
        fnrIdenterPerBruker: Map<NorskIdent, List<NorskIdentMedHistorikk>>
    ) {
        val dagpengerVedtakForAlleIdenterPerBruker =
            dagpengerRepository.hentDagpengerVedtak(alleIdenter(fnrIdenterPerBruker))

        brukere.forEach { bruker ->
            val identer = bruker.fnr?.let { fnrIdenterPerBruker[it] } ?: emptyList()
            val aktueltVedtak = velgVedtak(
                "dagpenger",
                bruker.fnr,
                identer,
                dagpengerVedtakForAlleIdenterPerBruker,
                DagpengerVedtakEntity::radSistEndret
            )
            val vedtakErGjeldende = aktueltVedtak != null &&
                    (aktueltVedtak.nyestePeriodeTom == null || aktueltVedtak.nyestePeriodeTom.isAfter(
                        LocalDate.now().minusDays(1)
                    ))

            bruker.dagpenger = DagpengerForOpensearch(
                harDagpenger = vedtakErGjeldende,
                rettighetstype = if (vedtakErGjeldende) aktueltVedtak.rettighetstype else null,
                datoStans = if (vedtakErGjeldende) aktueltVedtak.nyestePeriodeTom else null,
                antallResterendeDager = if (vedtakErGjeldende) aktueltVedtak.antallResterendeDager else null
            )
        }
    }

    private fun flettInnUforetrygd(
        brukere: List<PortefoljebrukerOpensearchModell>,
        fnrIdenterPerBruker: Map<NorskIdent, List<NorskIdentMedHistorikk>>
    ) {
        val uforetrygdForAlleIdenterPerBruker =
            uforetrygdRepository.hentUforetrygdVedtak(alleIdenter(fnrIdenterPerBruker))

        brukere.forEach { bruker ->
            val identer = bruker.fnr?.let { fnrIdenterPerBruker[it] } ?: emptyList()
            val aktueltVedtak = velgVedtak(
                "uføretrygd",
                bruker.fnr,
                identer,
                uforetrygdForAlleIdenterPerBruker,
                UforetrygdEntity::radSistEndret
            )

            bruker.uforetrygd = aktueltVedtak?.let {
                UforetrygdForOpensearch(
                    virkningsdato = it.virkningsdato,
                    uforegrad = it.uføregrad
                )
            }
        }
    }

    private fun flettInnTiltakspenger(
        brukere: List<PortefoljebrukerOpensearchModell>,
        fnrIdenterPerBruker: Map<NorskIdent, List<NorskIdentMedHistorikk>>
    ) {
        val tiltakspengerVedtakForAlleIdenterPerBruker =
            tiltakspengerRepository.hentTiltakspengerVedtak(alleIdenter(fnrIdenterPerBruker))

        brukere.forEach { bruker ->
            val identer = bruker.fnr?.let { fnrIdenterPerBruker[it] } ?: emptyList()
            val aktueltVedtak = velgVedtak(
                "tiltakspenger",
                bruker.fnr,
                identer,
                tiltakspengerVedtakForAlleIdenterPerBruker,
                TiltakspengerVedtakEntity::radSistEndret
            )
            val harAktivYtelse = aktueltVedtak != null && aktueltVedtak.rettighet != TiltakspengerRettighet.INGENTING
            val vedtakErGjeldende = harAktivYtelse && aktueltVedtak.tom.isAfter(LocalDate.now().minusDays(1))

            bruker.tiltakspenger = vedtakErGjeldende
            bruker.tiltakspenger_vedtaksdato_tom = if (vedtakErGjeldende) aktueltVedtak.tom else null
            bruker.tiltakspenger_rettighet = if (vedtakErGjeldende) aktueltVedtak.rettighet else null
        }
    }

    private fun hentFnrIdenterPerBruker(brukere: List<PortefoljebrukerOpensearchModell>): Map<NorskIdent, List<NorskIdentMedHistorikk>> {
        val fnrs = brukere.mapNotNull { it.fnr }
        val fnrIdenterPerBruker = pdlIdentRepository.hentFnrIdenterForBrukere(fnrs)
        fnrs.filterNot { fnrIdenterPerBruker.containsKey(it) }.forEach { fnr ->
            secureLog.error("Fant ingen folkeregisteridenter i bruker_identer for fnr: $fnr")
        }
        return fnrIdenterPerBruker
    }

    private fun alleIdenter(fnrIdenterPerBruker: Map<NorskIdent, List<NorskIdentMedHistorikk>>): List<NorskIdent> =
        fnrIdenterPerBruker.values.flatten().map { it.fnr }.distinct()

    // Hvis det finnes flere rader så skal aktiv ident prioriteres. Deretter velges vedtak med siste "rad sist endret" tidspunkt (vil nok aldri inntreffe).
    private fun <T> velgVedtak(
        ytelse: String,
        fnr: NorskIdent?,
        identer: List<NorskIdentMedHistorikk>,
        vedtak: Map<NorskIdent, T>,
        radSistEndret: (T) -> LocalDateTime
    ): T? {
        val identerMedVedtak = identer.filter { vedtak.containsKey(it.fnr) }
        if (identerMedVedtak.size > 1) {
            secureLog.warn("Fant $ytelse på flere fnr for samme person. Oppslag: $fnr, identer med $ytelse: ${identerMedVedtak.map { it.fnr }}")
        }
        val valgtIdent = identerMedVedtak.firstOrNull { !it.historisk }
            ?: identerMedVedtak.maxByOrNull { radSistEndret(vedtak.getValue(it.fnr)) }
        return valgtIdent?.let { vedtak[it.fnr] }
    }
}
