package no.nav.pto.veilarbportefolje.lagredefilter

import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest
import no.nav.pto.veilarbportefolje.database.PostgresTable.LAGREDE_FILTER_MINE_FILTER
import no.nav.pto.veilarbportefolje.domene.filtervalg.YtelseDagpenger
import no.nav.pto.veilarbportefolje.domene.getFiltervalgDefaults
import no.nav.pto.veilarbportefolje.lagredefilter.minefilter.MineFilterRepository
import no.nav.pto.veilarbportefolje.lagredefilter.minefilter.domene.NyttFilterRequest
import no.nav.pto.veilarbportefolje.lagredefilter.minefilter.domene.OppdaterFilterRequest
import no.nav.pto.veilarbportefolje.lagredefilter.minefilter.domene.SortOrderRequest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate

@SpringBootTest(classes = [ApplicationConfigTest::class])
class MineFilterRepositoryTest(
    @param:Autowired val mineFilterRepository: MineFilterRepository,
    @param:Autowired val jdbcTemplate: JdbcTemplate
) {

    @BeforeEach
    fun reset() {
        jdbcTemplate.update("TRUNCATE TABLE ${LAGREDE_FILTER_MINE_FILTER.TABLE_NAME}")
    }

    @Test
    fun `lagre nytt filter for veileder skal inserte ny rad`() {
        val filtervalg = getFiltervalgDefaults().copy(
            ytelseDagpenger = listOf(
                YtelseDagpenger.HAR_DAGPENGER_ORDINAER,
                YtelseDagpenger.HAR_DAGPENGER_MED_PERMITTERING
            )
        )

        val nyttFilterRequest = NyttFilterRequest(
            filterNavn = "Mitt filter",
            filterValg = filtervalg,
        )

        val lagretFilter = mineFilterRepository.lagreNyttFilterForVeileder("Z123456", nyttFilterRequest)

        assertThat(lagretFilter).isNotNull
        assertThat(lagretFilter.filterNavn).isEqualTo("Mitt filter")
        assertThat(lagretFilter.filterValg).isEqualTo(filtervalg)
    }

    @Test
    fun `hente filter for veileder skal hente alle rader for veileder`() {
        val veileder1 = "Z111111"
        val veileder2 = "Z222222"
        val filtervalg = getFiltervalgDefaults().copy(
            ytelseDagpenger = listOf(
                YtelseDagpenger.HAR_DAGPENGER_ORDINAER,
                YtelseDagpenger.HAR_DAGPENGER_MED_PERMITTERING
            )
        )

        mineFilterRepository.lagreNyttFilterForVeileder(
            veileder1,
            NyttFilterRequest(filterNavn = "Filter 1", filterValg = filtervalg)
        )
        mineFilterRepository.lagreNyttFilterForVeileder(
            veileder1,
            NyttFilterRequest(filterNavn = "Filter 2", filterValg = filtervalg)
        )
        mineFilterRepository.lagreNyttFilterForVeileder(
            veileder2,
            NyttFilterRequest(filterNavn = "Filter 3", filterValg = filtervalg)
        )

        val filterForVeileder1 = mineFilterRepository.hentFilterForVeileder(veileder1)
        val filterForVeileder2 = mineFilterRepository.hentFilterForVeileder(veileder2)

        assertThat(filterForVeileder1).hasSize(2)
        assertThat(filterForVeileder1.map { it.filterNavn }).containsExactlyInAnyOrder("Filter 1", "Filter 2")

        assertThat(filterForVeileder2).hasSize(1)
        assertThat(filterForVeileder2.first().filterNavn).isEqualTo("Filter 3")
    }

    @Test
    fun `oppdater lagret filter skal oppdatere filterNavn og filterValg for eksisterende rad`() {
        val veilederIdent = "Z123456"
        val opprinneligFiltervalg = getFiltervalgDefaults().copy(
            ytelseDagpenger = listOf(YtelseDagpenger.HAR_DAGPENGER_ORDINAER)
        )
        val lagretFilter = mineFilterRepository.lagreNyttFilterForVeileder(
            veilederIdent,
            NyttFilterRequest(filterNavn = "Original navn", opprinneligFiltervalg)
        )

        val oppdatertFilterValg = getFiltervalgDefaults().copy(navnEllerFnrQuery = "Ola Nordmann")
        val oppdatert = mineFilterRepository.oppdaterLagretFilterForVeileder(
            veilederIdent,
            OppdaterFilterRequest(
                filterId = lagretFilter.filterId,
                filterNavn = "Oppdatert navn",
                filterValg = oppdatertFilterValg
            )
        )

        assertThat(oppdatert.filterId).isEqualTo(lagretFilter.filterId)
        assertThat(oppdatert.filterNavn).isEqualTo("Oppdatert navn")

        val hentetEtterOppdatering = mineFilterRepository.hentFilterForVeileder(veilederIdent)
        assertThat(hentetEtterOppdatering).hasSize(1)
        assertThat(hentetEtterOppdatering.first().filterNavn).isEqualTo("Oppdatert navn")
        assertThat(hentetEtterOppdatering.first().filterValg).isEqualTo(oppdatertFilterValg)
    }

    @Test
    fun `oppdater lagret filter skal ikke oppdatere rad som tilhoerer en annen veileder`() {
        val veileder1 = "Z111111"
        val veileder2 = "Z222222"
        val lagretFilterVeileder1 = mineFilterRepository.lagreNyttFilterForVeileder(
            veileder1,
            NyttFilterRequest(filterNavn = "Veileder1 filter", filterValg = getFiltervalgDefaults())
        )

        assertThatThrownBy {
            mineFilterRepository.oppdaterLagretFilterForVeileder(
                veileder2,
                OppdaterFilterRequest(
                    filterId = lagretFilterVeileder1.filterId,
                    filterNavn = "Forsoek paa oppdatering",
                    filterValg = getFiltervalgDefaults()
                )
            )
        }.isInstanceOf(NoSuchElementException::class.java)

        val uendret = mineFilterRepository.hentFilterForVeileder(veileder1).first()
        assertThat(uendret.filterNavn).isEqualTo("Veileder1 filter")
    }

    @Test
    fun `oppdater lagret filter skal kaste feil naar filterId ikke finnes`() {
        assertThatThrownBy {
            mineFilterRepository.oppdaterLagretFilterForVeileder(
                "Z123456",
                OppdaterFilterRequest(
                    filterId = 999999,
                    filterNavn = "Finnes ikke",
                    filterValg = getFiltervalgDefaults()
                )
            )
        }.isInstanceOf(NoSuchElementException::class.java)
    }

    @Test
    fun `slette filter for veileder skal fjerne filteret`() {
        val veilederIdent = "Z123456"
        val lagretFilter = mineFilterRepository.lagreNyttFilterForVeileder(
            veilederIdent,
            NyttFilterRequest(filterNavn = "Test filter", filterValg = getFiltervalgDefaults())
        )

        val antallRaderSlettet = mineFilterRepository.slettFilterForVeileder(veilederIdent, lagretFilter.filterId)
        assertThat(antallRaderSlettet).isEqualTo(1)

        assertThat(mineFilterRepository.hentFilterForVeileder(veilederIdent)).isEmpty()
    }

    @Test
    fun `slette filter som ikke finnes skal returnere 0 for antall rader slettet`() {
        val antallRaderSlettet = mineFilterRepository.slettFilterForVeileder("Z123456", 112211)
        assertThat(antallRaderSlettet).isEqualTo(0)
    }

    @Test
    fun `slette filter skal ikke fjerne filter for en annen veileder`() {
        val veileder1 = "Z111111"
        val veileder2 = "Z222222"
        val lagretFilterVeileder1 = mineFilterRepository.lagreNyttFilterForVeileder(
            veileder1,
            NyttFilterRequest(filterNavn = "Veileder1 filter", filterValg = getFiltervalgDefaults())
        )

        val antallRaderSlettet = mineFilterRepository.slettFilterForVeileder(veileder2, lagretFilterVeileder1.filterId)
        assertThat(antallRaderSlettet).isEqualTo(0)
        assertThat(mineFilterRepository.hentFilterForVeileder(veileder1)).hasSize(1)
    }

    @Test
    fun `lagreSortering skal oppdatere sortOrder og returnere filtre sortert stigende`() {
        val veilederIdent = "Z123456"
        val filterA = mineFilterRepository.lagreNyttFilterForVeileder(
            veilederIdent,
            NyttFilterRequest(filterNavn = "Filter A", filterValg = getFiltervalgDefaults())
        )
        val filterB = mineFilterRepository.lagreNyttFilterForVeileder(
            veilederIdent,
            NyttFilterRequest(filterNavn = "Filter B", filterValg = getFiltervalgDefaults())
        )
        val filterC = mineFilterRepository.lagreNyttFilterForVeileder(
            veilederIdent,
            NyttFilterRequest(filterNavn = "Filter C", filterValg = getFiltervalgDefaults())
        )

        mineFilterRepository.lagreSortering(veilederIdent, SortOrderRequest(filterId = filterA.filterId, sortOrder = 3))
        mineFilterRepository.lagreSortering(veilederIdent, SortOrderRequest(filterId = filterB.filterId, sortOrder = 1))
        val resultat = mineFilterRepository.lagreSortering(
            veilederIdent,
            SortOrderRequest(filterId = filterC.filterId, sortOrder = 2)
        )

        assertThat(resultat.map { it.filterId })
            .containsExactly(filterB.filterId, filterC.filterId, filterA.filterId)
    }

    @Test
    fun `eksistererFilterNavn skal returnere true naar filternavn finnes for veileder`() {
        val veilederIdent = "Z123456"
        mineFilterRepository.lagreNyttFilterForVeileder(
            veilederIdent,
            NyttFilterRequest(filterNavn = "Mitt filter", filterValg = getFiltervalgDefaults())
        )

        assertThat(mineFilterRepository.eksistererFilterNavn(veilederIdent, "Mitt filter")).isTrue()
    }

    @Test
    fun `eksistererFilterNavn skal returnere false naar filternavn ikke finnes for veileder`() {
        val veilederIdent = "Z123456"
        mineFilterRepository.lagreNyttFilterForVeileder(
            veilederIdent,
            NyttFilterRequest(filterNavn = "Mitt filter", filterValg = getFiltervalgDefaults())
        )

        assertThat(mineFilterRepository.eksistererFilterNavn(veilederIdent, "Annet navn")).isFalse()
    }

    @Test
    fun `eksistererFilterNavn skal ignorere filter for andre veiledere`() {
        val veileder1 = "Z111111"
        val veileder2 = "Z222222"
        mineFilterRepository.lagreNyttFilterForVeileder(
            veileder1,
            NyttFilterRequest(filterNavn = "Delt navn", filterValg = getFiltervalgDefaults())
        )

        assertThat(mineFilterRepository.eksistererFilterNavn(veileder2, "Delt navn")).isFalse()
    }

    @Test
    fun `eksistererFilterNavn skal ekskludere angitt filterId`() {
        val veilederIdent = "Z123456"
        val lagretFilter = mineFilterRepository.lagreNyttFilterForVeileder(
            veilederIdent,
            NyttFilterRequest(filterNavn = "Mitt filter", filterValg = getFiltervalgDefaults())
        )

        assertThat(
            mineFilterRepository.eksistererFilterNavn(
                veilederIdent,
                "Mitt filter",
                ekskluderFilterId = lagretFilter.filterId
            )
        ).isFalse()
    }

    @Test
    fun `eksistererFilterNavn med ekskluderFilterId skal fortsatt oppdage duplikat i annen rad`() {
        val veilederIdent = "Z123456"
        val filterA = mineFilterRepository.lagreNyttFilterForVeileder(
            veilederIdent,
            NyttFilterRequest(filterNavn = "Filter A", filterValg = getFiltervalgDefaults())
        )
        mineFilterRepository.lagreNyttFilterForVeileder(
            veilederIdent,
            NyttFilterRequest(
                filterNavn = "Filter B",
                filterValg = getFiltervalgDefaults().copy(navnEllerFnrQuery = "Kari")
            )
        )

        assertThat(
            mineFilterRepository.eksistererFilterNavn(
                veilederIdent,
                "Filter B",
                ekskluderFilterId = filterA.filterId
            )
        ).isTrue()
    }

    @Test
    fun `eksistererFiltervalg skal returnere true naar samme filtervalg finnes for veileder`() {
        val veilederIdent = "Z123456"
        val filtervalg = getFiltervalgDefaults().copy(navnEllerFnrQuery = "Ola")
        mineFilterRepository.lagreNyttFilterForVeileder(
            veilederIdent,
            NyttFilterRequest(filterNavn = "Filter A", filterValg = filtervalg)
        )

        assertThat(mineFilterRepository.eksistererFiltervalg(veilederIdent, filtervalg)).isTrue()
    }

    @Test
    fun `eksistererFiltervalg skal returnere false naar filtervalg avviker`() {
        val veilederIdent = "Z123456"
        mineFilterRepository.lagreNyttFilterForVeileder(
            veilederIdent,
            NyttFilterRequest(
                filterNavn = "Filter A",
                filterValg = getFiltervalgDefaults().copy(navnEllerFnrQuery = "Ola")
            )
        )

        val annetFiltervalg = getFiltervalgDefaults().copy(navnEllerFnrQuery = "Kari")

        assertThat(mineFilterRepository.eksistererFiltervalg(veilederIdent, annetFiltervalg)).isFalse()
    }

    @Test
    fun `eksistererFiltervalg skal ignorere filter for andre veiledere`() {
        val veileder1 = "Z111111"
        val veileder2 = "Z222222"
        val filtervalg = getFiltervalgDefaults().copy(navnEllerFnrQuery = "Ola")
        mineFilterRepository.lagreNyttFilterForVeileder(
            veileder1,
            NyttFilterRequest(filterNavn = "Filter A", filterValg = filtervalg)
        )

        assertThat(mineFilterRepository.eksistererFiltervalg(veileder2, filtervalg)).isFalse()
    }

    @Test
    fun `eksistererFiltervalg skal ekskludere angitt filterId`() {
        val veilederIdent = "Z123456"
        val filtervalg = getFiltervalgDefaults().copy(navnEllerFnrQuery = "Ola")
        val lagretFilter = mineFilterRepository.lagreNyttFilterForVeileder(
            veilederIdent,
            NyttFilterRequest(filterNavn = "Filter A", filterValg = filtervalg)
        )

        assertThat(
            mineFilterRepository.eksistererFiltervalg(
                veilederIdent,
                filtervalg,
                ekskluderFilterId = lagretFilter.filterId
            )
        ).isFalse()
    }

    @Test
    fun `eksistererFiltervalg skal behandle filter som er like modulo defaults som duplikat`() {
        val veilederIdent = "Z123456"
        mineFilterRepository.lagreNyttFilterForVeileder(
            veilederIdent,
            NyttFilterRequest(
                filterNavn = "Filter A",
                filterValg = getFiltervalgDefaults().copy(navnEllerFnrQuery = "Ola")
            )
        )

        val nyttFiltervalgLiktMinusDefaults = getFiltervalgDefaults().copy(navnEllerFnrQuery = "Ola")

        assertThat(
            mineFilterRepository.eksistererFiltervalg(veilederIdent, nyttFiltervalgLiktMinusDefaults)
        ).isTrue()
    }
}
