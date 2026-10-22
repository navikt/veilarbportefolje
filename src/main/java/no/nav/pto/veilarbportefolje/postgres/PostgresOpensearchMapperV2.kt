package no.nav.pto.veilarbportefolje.postgres

import no.nav.poao_tilgang.client.NorskIdent
import no.nav.pto.veilarbportefolje.domene.HuskelappForBruker
import no.nav.pto.veilarbportefolje.domene.opensearchmodell.SykmeldtForOpensearch
import no.nav.pto.veilarbportefolje.fargekategori.FargekategoriRepository
import no.nav.pto.veilarbportefolje.hendelsesfilter.Hendelse
import no.nav.pto.veilarbportefolje.hendelsesfilter.HendelseRepository
import no.nav.pto.veilarbportefolje.hendelsesfilter.IngenHendelseForPersonException
import no.nav.pto.veilarbportefolje.hendelsesfilter.Kategori
import no.nav.pto.veilarbportefolje.huskelapp.HuskelappRepository
import no.nav.pto.veilarbportefolje.opensearch.domene.PortefoljebrukerOpensearchModell
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import no.nav.pto.veilarbportefolje.persononinfo.domene.NorskIdentMedHistorikk
import no.nav.pto.veilarbportefolje.sykmeldt.SykmeldtRepository
import no.nav.pto.veilarbportefolje.sykmeldt.domene.SykmeldtEntity
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.common.types.identer.NorskIdent as NorskIdentType


// V2 versjon i kotlin som vi kan flytte alle nye "flett inn" funksjoner til.
// Etterhvert kan vi få V1 over til kotlin og slå de sammen, men nå kan vi ta de gradvis.
@Service
class PostgresOpensearchMapperV2(
    private val huskelappRepository: HuskelappRepository,
    private val fargekategoriRepository: FargekategoriRepository,
    private val hendelseRepository: HendelseRepository,
    private val pdlIdentRepository: PdlIdentRepository,
    private val sykmeldtRepository: SykmeldtRepository,
) {

    fun flettInnData(brukere: List<PortefoljebrukerOpensearchModell>) {
        val fnrIdenterPerBruker = hentFnrIdenterPerBruker(pdlIdentRepository, brukere)
        flettInnHuskelapp(brukere, fnrIdenterPerBruker)
        flettInnFargekategori(brukere, fnrIdenterPerBruker)
        flettInnEldsteHendelsePerKategori(brukere, fnrIdenterPerBruker)
        flettInnSykmeldt(brukere, fnrIdenterPerBruker)
    }

    private fun flettInnHuskelapp(
        brukere: List<PortefoljebrukerOpensearchModell>,
        fnrIdenterPerBruker: Map<NorskIdent, List<NorskIdentMedHistorikk>>
    ) {
        val aktivHuskelappPerIdent = huskelappRepository.hentAktivHuskelappPerIdent(alleIdenter(fnrIdenterPerBruker))

        brukere.forEach { bruker ->
            val identer = bruker.fnr?.let { fnrIdenterPerBruker[it] } ?: emptyList()
            val huskelapp = velgDataForPrioritertIdent(
                "huskelapp",
                bruker.fnr,
                identer,
                aktivHuskelappPerIdent,
                { it.endretDato()?.atStartOfDay() ?: LocalDateTime.MIN }
            )

            bruker.huskelapp = huskelapp
                ?.takeIf { it.frist() != null || it.kommentar() != null }
                ?.let {
                    HuskelappForBruker(
                        it.frist(),
                        it.kommentar(),
                        it.endretDato(),
                        it.endretAv().value,
                        it.huskelappId()?.toString(),
                        it.enhetId().get()
                    )
                }
        }
    }

    private fun flettInnFargekategori(
        brukere: List<PortefoljebrukerOpensearchModell>,
        fnrIdenterPerBruker: Map<NorskIdent, List<NorskIdentMedHistorikk>>
    ) {
        val fargekategoriPerIdent = fargekategoriRepository.hentFargekategoriPerIdent(alleIdenter(fnrIdenterPerBruker))

        brukere.forEach { bruker ->
            val identer = bruker.fnr?.let { fnrIdenterPerBruker[it] } ?: emptyList()
            val fargekategori = velgDataForPrioritertIdent(
                "fargekategori",
                bruker.fnr,
                identer,
                fargekategoriPerIdent,
                { it.sistEndret() ?: LocalDateTime.MIN }
            )

            bruker.fargekategori = fargekategori?.verdi()
            bruker.fargekategori_enhetId = fargekategori?.enhetId()
        }
    }

    private fun flettInnEldsteHendelsePerKategori(
        brukere: List<PortefoljebrukerOpensearchModell>,
        fnrIdenterPerBruker: Map<NorskIdent, List<NorskIdentMedHistorikk>>
    ) {
        brukere.forEach { bruker ->
            val identer = bruker.fnr?.let { fnrIdenterPerBruker[it] } ?: emptyList()
            val fnrIdenter = identer.map { NorskIdentType.of(it.fnr) }

            bruker.hendelser = Kategori.entries.mapNotNull { kategori ->
                hentEldsteHendelse(fnrIdenter, kategori)?.let { kategori to it.hendelse }
            }.toMap()
        }
    }

    private fun hentEldsteHendelse(fnrIdenter: List<NorskIdentType>, kategori: Kategori): Hendelse? =
        try {
            hendelseRepository.getEldste(fnrIdenter, kategori)
        } catch (_: IngenHendelseForPersonException) {
            null
        }

    private fun flettInnSykmeldt(
        brukere: List<PortefoljebrukerOpensearchModell>,
        fnrIdenterPerBruker: Map<NorskIdent, List<NorskIdentMedHistorikk>>
    ) {
        val sykmeldtForAlleIdenterPerBruker =
            sykmeldtRepository.hentSykmeldtPerioder(alleIdenter(fnrIdenterPerBruker))

        brukere.forEach { bruker ->
            val identer = bruker.fnr?.let { fnrIdenterPerBruker[it] } ?: emptyList()
            val aktuellPeriode = velgDataForPrioritertIdent(
                "sykmeldt",
                bruker.fnr,
                identer,
                sykmeldtForAlleIdenterPerBruker,
                SykmeldtEntity::radSistEndret
            )
            val periodeErGjeldende = aktuellPeriode != null && !aktuellPeriode.slutt.isBefore(LocalDate.now())

            bruker.sykmeldt = if (periodeErGjeldende) {
                SykmeldtForOpensearch(
                    startdato = aktuellPeriode.start,
                    sluttdato = aktuellPeriode.slutt,
                    harArbeidsgiver = aktuellPeriode.harArbeidsgiver
                )
            } else null
        }
    }
}
