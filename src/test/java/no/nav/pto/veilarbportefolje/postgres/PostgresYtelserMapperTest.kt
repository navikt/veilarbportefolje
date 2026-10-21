package no.nav.pto.veilarbportefolje.postgres

import no.nav.pto.veilarbportefolje.aap.AapRepository
import no.nav.pto.veilarbportefolje.aap.domene.AapEntity
import no.nav.pto.veilarbportefolje.aap.domene.AapRettighetstype
import no.nav.pto.veilarbportefolje.aap.domene.AapVedtakStatus
import no.nav.pto.veilarbportefolje.aap.dto.AapVedtakResponseDto
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest
import no.nav.pto.veilarbportefolje.dagpenger.DagpengerRepository
import no.nav.pto.veilarbportefolje.dagpenger.domene.DagpengerRettighetstype
import no.nav.pto.veilarbportefolje.dagpenger.dto.DagpengerBeregningerResponseDto
import no.nav.pto.veilarbportefolje.dagpenger.dto.DagpengerPeriodeDto
import no.nav.pto.veilarbportefolje.database.PostgresTable.*
import no.nav.pto.veilarbportefolje.ensligforsorger.EnsligeForsorgereRepository
import no.nav.pto.veilarbportefolje.ensligforsorger.domain.Aktivitetstype
import no.nav.pto.veilarbportefolje.ensligforsorger.domain.Periodetype
import no.nav.pto.veilarbportefolje.ensligforsorger.domain.Stønadstype
import no.nav.pto.veilarbportefolje.ensligforsorger.domain.Vedtaksresultat
import no.nav.pto.veilarbportefolje.ensligforsorger.dto.input.Barn
import no.nav.pto.veilarbportefolje.ensligforsorger.dto.input.VedtakOvergangsstønadArbeidsoppfølging
import no.nav.pto.veilarbportefolje.opensearch.domene.PortefoljebrukerOpensearchModell
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent.Gruppe
import no.nav.pto.veilarbportefolje.tiltakspenger.TiltakspengerRespository
import no.nav.pto.veilarbportefolje.tiltakspenger.domene.TiltakspengerRettighet
import no.nav.pto.veilarbportefolje.tiltakspenger.dto.TiltakspengerResponseDto
import no.nav.pto.veilarbportefolje.uforetrygd.UforetrygdRepository
import no.nav.pto.veilarbportefolje.uforetrygd.dto.UforetrygdResponseDto
import no.nav.pto.veilarbportefolje.ungdomsprogram.UngdomsprogramRepository
import no.nav.pto.veilarbportefolje.ungdomsprogram.dto.Periode
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate

@SpringBootTest(classes = [ApplicationConfigTest::class])
class PostgresYtelserMapperTest(
    @param:Autowired val postgresYtelserMapper: PostgresYtelserMapper,
    @param:Autowired val aapRepository: AapRepository,
    @param:Autowired val dagpengerRepository: DagpengerRepository,
    @param:Autowired val uforetrygdRepository: UforetrygdRepository,
    @param:Autowired val tiltakspengerRepository: TiltakspengerRespository,
    @param:Autowired val ungdomsprogramRepository: UngdomsprogramRepository,
    @param:Autowired val ensligeForsorgereRepository: EnsligeForsorgereRepository,
    @param:Autowired val pdlIdentRepository: PdlIdentRepository,
    @param:Autowired val jdbcTemplate: JdbcTemplate
) {

    @BeforeEach
    fun reset() {
        jdbcTemplate.update("TRUNCATE TABLE ${YTELSER_AAP.TABLE_NAME}")
        jdbcTemplate.update("TRUNCATE TABLE ${YTELSER_DAGPENGER.TABLE_NAME}")
        jdbcTemplate.update("TRUNCATE TABLE ${YTELSER_UFORETRYGD.TABLE_NAME}")
        jdbcTemplate.update("TRUNCATE TABLE ${YTELSER_TILTAKSPENGER.TABLE_NAME}")
        jdbcTemplate.update("TRUNCATE TABLE ${YTELSER_UNGDOMSPROGRAM.TABLE_NAME}")
        jdbcTemplate.update("TRUNCATE TABLE enslige_forsorgere CASCADE")
        jdbcTemplate.update("TRUNCATE TABLE bruker_identer")
    }

    @Test
    fun `flettInnAapKelvin skal finne aap lagret på historisk fnr`() {
        val fnr = randomFnr().get()
        val historiskFnr = randomFnr().get()
        val tilOgMed = LocalDate.now().plusMonths(3)
        val maksdato = LocalDate.now().plusYears(2)

        pdlIdentRepository.upsertIdenter(
            listOf(
                PDLIdent(randomAktorId().get(), false, Gruppe.AKTORID),
                PDLIdent(fnr, false, Gruppe.FOLKEREGISTERIDENT),
                PDLIdent(historiskFnr, true, Gruppe.FOLKEREGISTERIDENT)
            )
        )
        aapRepository.upsertAap(historiskFnr, AapEntity("FERDIGBEHANDLET", maksdato, aapVedtak(tilOgMed)))

        val bruker = PortefoljebrukerOpensearchModell(fnr = fnr)
        postgresYtelserMapper.flettInnYtelser(listOf(bruker))

        assertThat(bruker.aap_kelvin).isTrue()
        assertThat(bruker.aap_kelvin_tom_vedtaksdato).isEqualTo(tilOgMed)
        assertThat(bruker.aap_kelvin_rettighetstype).isEqualTo(AapRettighetstype.BISTANDSBEHOV)
        assertThat(bruker.aap_kelvin_maksdato).isEqualTo(maksdato)
    }

    @Test
    fun `flettInnAapKelvin skal sette aap_kelvin false når periode er utløpt eller null`() {
        val fnrUtenAap = randomFnr().get()
        val fnrMedUtloptPeriode = randomFnr().get()
        lagreIdenter(fnrMedUtloptPeriode)
        lagreIdenter(fnrUtenAap)
        aapRepository.upsertAap(
            fnrMedUtloptPeriode,
            AapEntity("FERDIGBEHANDLET", null, aapVedtak(LocalDate.now().minusDays(1)))
        )

        val brukerMedUtloptPeriode = PortefoljebrukerOpensearchModell(fnr = fnrMedUtloptPeriode)
        val brukerUtenAap = PortefoljebrukerOpensearchModell(fnr = fnrUtenAap)
        postgresYtelserMapper.flettInnYtelser(listOf(brukerMedUtloptPeriode, brukerUtenAap))

        assertThat(brukerMedUtloptPeriode.aap_kelvin).isFalse()
        assertThat(brukerUtenAap.aap_kelvin).isFalse()
    }

    @Test
    fun `flettInnDagpenger skal finne dagpenger lagret på historisk fnr`() {
        val fnr = randomFnr().get()
        val historiskFnr = randomFnr().get()
        val tilOgMed = LocalDate.now().plusMonths(3)

        lagreIdenter(fnr, historisk = listOf(historiskFnr))
        dagpengerRepository.upsertDagpengerPerioder(
            historiskFnr,
            dagpengerPeriode(tilOgMed),
            DagpengerBeregningerResponseDto(
                fraOgMed = LocalDate.now(),
                sats = 600,
                utbetaltBeløp = 400,
                gjenståendeDager = 42
            )
        )

        val bruker = PortefoljebrukerOpensearchModell(fnr = fnr)
        postgresYtelserMapper.flettInnYtelser(listOf(bruker))

        assertThat(bruker.dagpenger?.harDagpenger).isTrue()
        assertThat(bruker.dagpenger?.rettighetstype).isEqualTo(DagpengerRettighetstype.DAGPENGER_ARBEIDSSOKER_ORDINAER)
        assertThat(bruker.dagpenger?.datoStans).isEqualTo(tilOgMed)
        assertThat(bruker.dagpenger?.antallResterendeDager).isEqualTo(42)
    }

    @Test
    fun `flettInnDagpenger skal sette harDagpenger false når periode er utløpt eller null`() {
        val fnrUtenDapgener = randomFnr().get()
        val fnrMedUloptDagpenger = randomFnr().get()
        lagreIdenter(fnrMedUloptDagpenger)
        lagreIdenter(fnrUtenDapgener)
        dagpengerRepository.upsertDagpengerPerioder(
            fnrMedUloptDagpenger,
            dagpengerPeriode(LocalDate.now().minusDays(1)),
            null
        )

        val brukerMedUtloptDagpenger = PortefoljebrukerOpensearchModell(fnr = fnrMedUloptDagpenger)
        val brukerUtenDapgener = PortefoljebrukerOpensearchModell(fnr = fnrUtenDapgener)
        postgresYtelserMapper.flettInnYtelser(listOf(brukerMedUtloptDagpenger, brukerUtenDapgener))

        assertThat(brukerMedUtloptDagpenger.dagpenger?.harDagpenger).isFalse()
        assertThat(brukerUtenDapgener.dagpenger?.harDagpenger).isFalse()
    }

    @Test
    fun `flettInnUforetrygd skal finne uføretrygd lagret på historisk fnr`() {
        val fnr = randomFnr().get()
        val historiskFnr = randomFnr().get()
        val virkningsdato = LocalDate.now().minusMonths(3)

        lagreIdenter(fnr, historisk = listOf(historiskFnr))
        uforetrygdRepository.upsertUforetrygd(
            historiskFnr,
            uforetrygd(uforegrad = 50, virkningsdato = virkningsdato)
        )

        val bruker = PortefoljebrukerOpensearchModell(fnr = fnr)
        postgresYtelserMapper.flettInnYtelser(listOf(bruker))

        assertThat(bruker.uforetrygd?.virkningsdato).isEqualTo(virkningsdato)
        assertThat(bruker.uforetrygd?.uforegrad).isEqualTo(50)
    }

    @Test
    fun `flettInnTiltakspenger skal finne tiltakspenger lagret på historisk fnr`() {
        val fnr = randomFnr().get()
        val historiskFnr = randomFnr().get()
        val tilOgMed = LocalDate.now().plusMonths(3)

        lagreIdenter(fnr, historisk = listOf(historiskFnr))
        tiltakspengerRepository.upsertTiltakspenger(historiskFnr, tiltakspengerVedtak(tilOgMed))

        val bruker = PortefoljebrukerOpensearchModell(fnr = fnr)
        postgresYtelserMapper.flettInnYtelser(listOf(bruker))

        assertThat(bruker.tiltakspenger).isTrue()
        assertThat(bruker.tiltakspenger_vedtaksdato_tom).isEqualTo(tilOgMed)
        assertThat(bruker.tiltakspenger_rettighet).isEqualTo(TiltakspengerRettighet.TILTAKSPENGER)
    }

    @Test
    fun `flettInnTiltakspenger skal sette tiltakspenger false når periode er utløpt, rettighet er INGENTING eller vedtak mangler`() {
        val fnrMedUtloptPeriode = randomFnr().get()
        val fnrMedIngenRettighet = randomFnr().get()
        val fnrUtenTiltakspenger = randomFnr().get()
        lagreIdenter(fnrMedUtloptPeriode)
        lagreIdenter(fnrMedIngenRettighet)
        lagreIdenter(fnrUtenTiltakspenger)
        tiltakspengerRepository.upsertTiltakspenger(
            fnrMedUtloptPeriode,
            tiltakspengerVedtak(LocalDate.now().minusDays(1))
        )
        tiltakspengerRepository.upsertTiltakspenger(
            fnrMedIngenRettighet,
            tiltakspengerVedtak(LocalDate.now().plusMonths(3)).copy(rettighet = TiltakspengerRettighet.INGENTING)
        )

        val brukerMedUtloptPeriode = PortefoljebrukerOpensearchModell(fnr = fnrMedUtloptPeriode)
        val brukerMedIngenRettighet = PortefoljebrukerOpensearchModell(fnr = fnrMedIngenRettighet)
        val brukerUtenTiltakspenger = PortefoljebrukerOpensearchModell(fnr = fnrUtenTiltakspenger)
        postgresYtelserMapper.flettInnYtelser(
            listOf(brukerMedUtloptPeriode, brukerMedIngenRettighet, brukerUtenTiltakspenger)
        )

        assertThat(brukerMedUtloptPeriode.tiltakspenger).isFalse()
        assertThat(brukerMedIngenRettighet.tiltakspenger).isFalse()
        assertThat(brukerUtenTiltakspenger.tiltakspenger).isFalse()
        assertThat(brukerUtenTiltakspenger.tiltakspenger_vedtaksdato_tom).isNull()
        assertThat(brukerUtenTiltakspenger.tiltakspenger_rettighet).isNull()
    }

    @Test
    fun `flettInnUngdomsprogram skal finne ungdomsprogram lagret på historisk fnr`() {
        val fnr = randomFnr().get()
        val historiskFnr = randomFnr().get()
        val periode = ungdomsprogramPeriode(tilOgMed = LocalDate.now().plusMonths(3))

        lagreIdenter(fnr, historisk = listOf(historiskFnr))
        ungdomsprogramRepository.upsertUngdomsprogram(historiskFnr, periode)

        val bruker = PortefoljebrukerOpensearchModell(fnr = fnr)
        postgresYtelserMapper.flettInnYtelser(listOf(bruker))

        assertThat(bruker.ungdomsprogram?.fraOgMed).isEqualTo(periode.fraOgMed)
        assertThat(bruker.ungdomsprogram?.tilOgMed).isEqualTo(periode.tilOgMed)
        assertThat(bruker.ungdomsprogram?.maksdato).isEqualTo(periode.periodeMaksDato)
        assertThat(bruker.ungdomsprogram?.harForlengetPeriode).isFalse()
    }

    @Test
    fun `flettInnUngdomsprogram skal sette ungdomsprogram når periode er åpen og maksdato er fram i tid`() {
        val fnr = randomFnr().get()
        lagreIdenter(fnr)
        ungdomsprogramRepository.upsertUngdomsprogram(fnr, ungdomsprogramPeriode(tilOgMed = null))

        val bruker = PortefoljebrukerOpensearchModell(fnr = fnr)
        postgresYtelserMapper.flettInnYtelser(listOf(bruker))

        assertThat(bruker.ungdomsprogram).isNotNull()
        assertThat(bruker.ungdomsprogram?.tilOgMed).isNull()
    }

    @Test
    fun `flettInnUngdomsprogram skal sette ungdomsprogram null når periode eller maksdato er utløpt eller periode mangler`() {
        val fnrMedUtloptPeriode = randomFnr().get()
        val fnrMedUtloptMaksdato = randomFnr().get()
        val fnrUtenUngdomsprogram = randomFnr().get()
        lagreIdenter(fnrMedUtloptPeriode)
        lagreIdenter(fnrMedUtloptMaksdato)
        lagreIdenter(fnrUtenUngdomsprogram)
        ungdomsprogramRepository.upsertUngdomsprogram(
            fnrMedUtloptPeriode,
            ungdomsprogramPeriode(tilOgMed = LocalDate.now().minusDays(1))
        )
        ungdomsprogramRepository.upsertUngdomsprogram(
            fnrMedUtloptMaksdato,
            ungdomsprogramPeriode(tilOgMed = null, maksdato = LocalDate.now().minusDays(1))
        )

        val brukerMedUtloptPeriode = PortefoljebrukerOpensearchModell(fnr = fnrMedUtloptPeriode)
        val brukerMedUtloptMaksdato = PortefoljebrukerOpensearchModell(fnr = fnrMedUtloptMaksdato)
        val brukerUtenUngdomsprogram = PortefoljebrukerOpensearchModell(fnr = fnrUtenUngdomsprogram)
        postgresYtelserMapper.flettInnYtelser(
            listOf(brukerMedUtloptPeriode, brukerMedUtloptMaksdato, brukerUtenUngdomsprogram)
        )

        assertThat(brukerMedUtloptPeriode.ungdomsprogram).isNull()
        assertThat(brukerMedUtloptMaksdato.ungdomsprogram).isNull()
        assertThat(brukerUtenUngdomsprogram.ungdomsprogram).isNull()
    }

    @Test
    fun `flettInnEnsligeForsorgere skal finne overgangsstønad lagret på historisk fnr`() {
        val fnr = randomFnr().get()
        val historiskFnr = randomFnr().get()
        val tilOgMed = LocalDate.now().plusMonths(3)

        lagreIdenter(fnr, historisk = listOf(historiskFnr))
        ensligeForsorgereRepository.lagreOvergangsstonad(overgangsstonadVedtak(1L, historiskFnr, tilOgMed))

        val bruker = PortefoljebrukerOpensearchModell(fnr = fnr)
        postgresYtelserMapper.flettInnYtelser(listOf(bruker))

        assertThat(bruker.enslige_forsorgere_overgangsstonad?.utlopsDato()).isEqualTo(tilOgMed)
        assertThat(bruker.enslige_forsorgere_overgangsstonad?.harAktivitetsplikt()).isFalse()
        assertThat(bruker.enslige_forsorgere_overgangsstonad?.yngsteBarnsFødselsdato()).isEqualTo(LocalDate.of(2023, 5, 4))
    }

    @Test
    fun `flettInnEnsligeForsorgere skal sette overgangsstønad null når periode er utløpt eller vedtak mangler`() {
        val fnrMedUtloptPeriode = randomFnr().get()
        val fnrUtenOvergangsstonad = randomFnr().get()
        lagreIdenter(fnrMedUtloptPeriode)
        lagreIdenter(fnrUtenOvergangsstonad)
        ensligeForsorgereRepository.lagreOvergangsstonad(
            overgangsstonadVedtak(1L, fnrMedUtloptPeriode, LocalDate.now().minusDays(1))
        )

        val brukerMedUtloptPeriode = PortefoljebrukerOpensearchModell(fnr = fnrMedUtloptPeriode)
        val brukerUtenOvergangsstonad = PortefoljebrukerOpensearchModell(fnr = fnrUtenOvergangsstonad)
        postgresYtelserMapper.flettInnYtelser(listOf(brukerMedUtloptPeriode, brukerUtenOvergangsstonad))

        assertThat(brukerMedUtloptPeriode.enslige_forsorgere_overgangsstonad).isNull()
        assertThat(brukerUtenOvergangsstonad.enslige_forsorgere_overgangsstonad).isNull()
    }

    private fun lagreIdenter(fnr: String, historisk: List<String> = emptyList()) {
        pdlIdentRepository.upsertIdenter(
            listOf(
                PDLIdent(randomAktorId().get(), false, Gruppe.AKTORID),
                PDLIdent(fnr, false, Gruppe.FOLKEREGISTERIDENT)
            ) + historisk.map { PDLIdent(it, true, Gruppe.FOLKEREGISTERIDENT) }
        )
    }


    private fun dagpengerPeriode(tilOgMed: LocalDate?) = DagpengerPeriodeDto(
        fraOgMedDato = LocalDate.now().minusMonths(6),
        tilOgMedDato = tilOgMed,
        ytelseType = DagpengerRettighetstype.DAGPENGER_ARBEIDSSOKER_ORDINAER,
        kilde = "DP_SAK"
    )


    private fun aapVedtak(tilOgMed: LocalDate) = AapVedtakResponseDto.Vedtak(
        status = AapVedtakStatus.LØPENDE,
        saksnummer = "SAK-1",
        periode = AapVedtakResponseDto.Periode(
            fraOgMedDato = LocalDate.now().minusMonths(6),
            tilOgMedDato = tilOgMed
        ),
        rettighetsType = AapRettighetstype.BISTANDSBEHOV
    )

    private fun uforetrygd(uforegrad: Int, virkningsdato: LocalDate) = UforetrygdResponseDto(
        lopendeUforetrygd = true,
        uforegrad = uforegrad,
        forsteVirkningstidspunkt = virkningsdato
    )

    private fun tiltakspengerVedtak(tilOgMed: LocalDate) = TiltakspengerResponseDto(
        sakId = "SAK-1",
        fom = LocalDate.now().minusMonths(6),
        tom = tilOgMed,
        rettighet = TiltakspengerRettighet.TILTAKSPENGER,
        kilde = "tp"
    )

    private fun ungdomsprogramPeriode(
        tilOgMed: LocalDate?,
        maksdato: LocalDate = LocalDate.now().plusYears(1)
    ) = Periode(
        fraOgMed = LocalDate.now().minusMonths(6),
        tilOgMed = tilOgMed,
        harForlengetPeriode = false,
        periodeMaksDato = maksdato
    )

    private fun overgangsstonadVedtak(vedtakId: Long, fnr: String, tilOgMed: LocalDate) =
        VedtakOvergangsstønadArbeidsoppfølging(
            vedtakId,
            fnr,
            listOf(Barn(null, LocalDate.of(2023, 5, 4))),
            Stønadstype.OVERGANGSSTØNAD,
            listOf(
                no.nav.pto.veilarbportefolje.ensligforsorger.dto.input.Periode(
                    LocalDate.now().minusMonths(6),
                    tilOgMed,
                    Periodetype.NY_PERIODE_FOR_NYTT_BARN,
                    Aktivitetstype.BARN_UNDER_ETT_ÅR
                )
            ),
            Vedtaksresultat.INNVILGET
        )
}
