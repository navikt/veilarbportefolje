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
import no.nav.pto.veilarbportefolje.database.PostgresTable.YTELSER_AAP
import no.nav.pto.veilarbportefolje.database.PostgresTable.YTELSER_DAGPENGER
import no.nav.pto.veilarbportefolje.database.PostgresTable.YTELSER_UFORETRYGD
import no.nav.pto.veilarbportefolje.opensearch.domene.PortefoljebrukerOpensearchModell
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent.Gruppe
import no.nav.pto.veilarbportefolje.uforetrygd.UforetrygdRepository
import no.nav.pto.veilarbportefolje.uforetrygd.dto.UforetrygdResponseDto
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
    @param:Autowired val pdlIdentRepository: PdlIdentRepository,
    @param:Autowired val jdbcTemplate: JdbcTemplate
) {

    @BeforeEach
    fun reset() {
        jdbcTemplate.update("TRUNCATE TABLE ${YTELSER_AAP.TABLE_NAME}")
        jdbcTemplate.update("TRUNCATE TABLE ${YTELSER_DAGPENGER.TABLE_NAME}")
        jdbcTemplate.update("TRUNCATE TABLE ${YTELSER_UFORETRYGD.TABLE_NAME}")
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
    fun `flettInnAapKelvin skal velge aap på ikke-historisk fnr når det finnes på flere fnr`() {
        val fnr = randomFnr().get()
        val historiskFnr = randomFnr().get()
        val tilOgMedGjeldende = LocalDate.now().plusMonths(3)
        val tilOgMedHistorisk = LocalDate.now().plusMonths(6)

        pdlIdentRepository.upsertIdenter(
            listOf(
                PDLIdent(randomAktorId().get(), false, Gruppe.AKTORID),
                PDLIdent(fnr, false, Gruppe.FOLKEREGISTERIDENT),
                PDLIdent(historiskFnr, true, Gruppe.FOLKEREGISTERIDENT)
            )
        )
        aapRepository.upsertAap(fnr, AapEntity("FERDIGBEHANDLET", null, aapVedtak(tilOgMedGjeldende)))
        aapRepository.upsertAap(historiskFnr, AapEntity("FERDIGBEHANDLET", null, aapVedtak(tilOgMedHistorisk)))

        val bruker = PortefoljebrukerOpensearchModell(fnr = fnr)
        postgresYtelserMapper.flettInnYtelser(listOf(bruker))

        assertThat(bruker.aap_kelvin).isTrue()
        assertThat(bruker.aap_kelvin_tom_vedtaksdato).isEqualTo(tilOgMedGjeldende)
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
    fun `flettInnDagpenger skal velge dagpenger på ikke-historisk fnr når det finnes på flere fnr`() {
        val fnr = randomFnr().get()
        val historiskFnr = randomFnr().get()
        val tilOgMedGjeldende = LocalDate.now().plusMonths(3)

        lagreIdenter(fnr, historisk = listOf(historiskFnr))
        dagpengerRepository.upsertDagpengerPerioder(fnr, dagpengerPeriode(tilOgMedGjeldende), null)
        dagpengerRepository.upsertDagpengerPerioder(historiskFnr, dagpengerPeriode(LocalDate.now().plusMonths(6)), null)

        val bruker = PortefoljebrukerOpensearchModell(fnr = fnr)
        postgresYtelserMapper.flettInnYtelser(listOf(bruker))

        assertThat(bruker.dagpenger?.harDagpenger).isTrue()
        assertThat(bruker.dagpenger?.datoStans).isEqualTo(tilOgMedGjeldende)
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
    fun `flettInnUforetrygd skal velge uføretrygd på ikke-historisk fnr når det finnes på flere fnr`() {
        val fnr = randomFnr().get()
        val historiskFnr = randomFnr().get()
        val virkningsdatoGjeldende = LocalDate.now().minusMonths(3)

        lagreIdenter(fnr, historisk = listOf(historiskFnr))
        uforetrygdRepository.upsertUforetrygd(
            fnr,
            uforetrygd(uforegrad = 50, virkningsdato = virkningsdatoGjeldende)
        )
        uforetrygdRepository.upsertUforetrygd(
            historiskFnr,
            uforetrygd(uforegrad = 60, virkningsdato = LocalDate.now().minusMonths(6))
        )

        val bruker = PortefoljebrukerOpensearchModell(fnr = fnr)
        postgresYtelserMapper.flettInnYtelser(listOf(bruker))

        assertThat(bruker.uforetrygd?.virkningsdato).isEqualTo(virkningsdatoGjeldende)
        assertThat(bruker.uforetrygd?.uforegrad).isEqualTo(50)
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
}
