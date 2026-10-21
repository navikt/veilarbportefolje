package no.nav.pto.veilarbportefolje.postgres

import no.nav.common.types.identer.Fnr
import no.nav.poao_tilgang.client.NorskIdent
import no.nav.pto.veilarbportefolje.aap.AapRepository
import no.nav.pto.veilarbportefolje.aap.domene.AapVedtakEntity
import no.nav.pto.veilarbportefolje.dagpenger.DagpengerRepository
import no.nav.pto.veilarbportefolje.dagpenger.domene.DagpengerVedtakEntity
import no.nav.pto.veilarbportefolje.domene.opensearchmodell.DagpengerForOpensearch
import no.nav.pto.veilarbportefolje.domene.opensearchmodell.SykmeldtForOpensearch
import no.nav.pto.veilarbportefolje.domene.opensearchmodell.UforetrygdForOpensearch
import no.nav.pto.veilarbportefolje.domene.opensearchmodell.UngdomsprogramForOpensearch
import no.nav.pto.veilarbportefolje.ensligforsorger.EnsligeForsorgereService
import no.nav.pto.veilarbportefolje.opensearch.domene.PortefoljebrukerOpensearchModell
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import no.nav.pto.veilarbportefolje.persononinfo.domene.NorskIdentMedHistorikk
import no.nav.pto.veilarbportefolje.sykmeldt.SykmeldtRepository
import no.nav.pto.veilarbportefolje.sykmeldt.domene.SykmeldtEntity
import no.nav.pto.veilarbportefolje.tiltakspenger.TiltakspengerRespository
import no.nav.pto.veilarbportefolje.tiltakspenger.domene.TiltakspengerRettighet
import no.nav.pto.veilarbportefolje.tiltakspenger.domene.TiltakspengerVedtakEntity
import no.nav.pto.veilarbportefolje.uforetrygd.UforetrygdRepository
import no.nav.pto.veilarbportefolje.uforetrygd.domene.UforetrygdEntity
import no.nav.pto.veilarbportefolje.ungdomsprogram.UngdomsprogramRepository
import no.nav.pto.veilarbportefolje.ungdomsprogram.domene.UngdomsprogramPeriodeEntity
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class PostgresYtelserMapper(
    private val aapRepository: AapRepository,
    private val dagpengerRepository: DagpengerRepository,
    private val uforetrygdRepository: UforetrygdRepository,
    private val tiltakspengerRepository: TiltakspengerRespository,
    private val ungdomsprogramRepository: UngdomsprogramRepository,
    private val ensligeForsorgereService: EnsligeForsorgereService,
    private val sykmeldtRepository: SykmeldtRepository,
    private val pdlIdentRepository: PdlIdentRepository
) {

    fun flettInnYtelser(brukere: List<PortefoljebrukerOpensearchModell>) {
        val fnrIdenterPerBruker = hentFnrIdenterPerBruker(pdlIdentRepository, brukere)
        flettInnAapKelvin(brukere, fnrIdenterPerBruker)
        flettInnDagpenger(brukere, fnrIdenterPerBruker)
        flettInnUforetrygd(brukere, fnrIdenterPerBruker)
        flettInnTiltakspenger(brukere, fnrIdenterPerBruker)
        flettInnUngdomsprogram(brukere, fnrIdenterPerBruker)
        flettInnEnsligeForsorgere(brukere, fnrIdenterPerBruker)
        flettInnSykmeldt(brukere, fnrIdenterPerBruker)
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
                velgDataForPrioritertIdent("AAP", bruker.fnr, identer, aapVedtakForAlleIdenterPerBruker, AapVedtakEntity::radSistEndret)
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
            val aktueltVedtak = velgDataForPrioritertIdent(
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
            val aktueltVedtak = velgDataForPrioritertIdent(
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
            val aktueltVedtak = velgDataForPrioritertIdent(
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

    private fun flettInnUngdomsprogram(
        brukere: List<PortefoljebrukerOpensearchModell>,
        fnrIdenterPerBruker: Map<NorskIdent, List<NorskIdentMedHistorikk>>
    ) {
        val ungdomsprogramForAlleIdenterPerBruker =
            ungdomsprogramRepository.hentUngdomsprogramPerioder(alleIdenter(fnrIdenterPerBruker))

        brukere.forEach { bruker ->
            val identer = bruker.fnr?.let { fnrIdenterPerBruker[it] } ?: emptyList()
            val aktuellPeriode = velgDataForPrioritertIdent(
                "ungdomsprogram",
                bruker.fnr,
                identer,
                ungdomsprogramForAlleIdenterPerBruker,
                UngdomsprogramPeriodeEntity::radSistEndret
            )
            val iDag = LocalDate.now()
            val periodeErGjeldende = aktuellPeriode != null &&
                    (aktuellPeriode.tilOgMed == null || !aktuellPeriode.tilOgMed.isBefore(iDag)) &&
                    !aktuellPeriode.maksdato.isBefore(iDag)

            bruker.ungdomsprogram = if (periodeErGjeldende) {
                UngdomsprogramForOpensearch(
                    fraOgMed = aktuellPeriode.fraOgMed,
                    tilOgMed = aktuellPeriode.tilOgMed,
                    maksdato = aktuellPeriode.maksdato,
                    harForlengetPeriode = aktuellPeriode.harForlengetPeriode
                )
            } else null
        }
    }

    private fun flettInnEnsligeForsorgere(
        brukere: List<PortefoljebrukerOpensearchModell>,
        fnrIdenterPerBruker: Map<NorskIdent, List<NorskIdentMedHistorikk>>
    ) {
        val overgangsstonadForAlleIdenterPerBruker = ensligeForsorgereService
            .hentEnsligeForsorgerOvergangsstønadTiltak(alleIdenter(fnrIdenterPerBruker).map { Fnr.of(it) })
            .mapKeys { it.key.get() }

        brukere.forEach { bruker ->
            val identer = bruker.fnr?.let { fnrIdenterPerBruker[it] } ?: emptyList()
            val aktueltVedtak = velgDataForPrioritertIdent(
                "enslig forsørger",
                bruker.fnr,
                identer,
                overgangsstonadForAlleIdenterPerBruker,
                { it.radSistEndret ?: LocalDateTime.MIN }
            )

            bruker.enslige_forsorgere_overgangsstonad = aktueltVedtak?.toEnsligeForsorgereOpensearchDto()
        }
    }

    // flyttes til PostgresMapperV2 når annen brach er merget inn
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
