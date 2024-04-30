package no.nav.pto.veilarbportefolje.util

import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.arbeidsliste.Arbeidsliste
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteDTO
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteRepositoryV2
import no.nav.pto.veilarbportefolje.arbeidssoeker.v1.registrering.ArbeidssokerRegistreringRepositoryV2
import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.Profileringsresultat
import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.Profilering
import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.ArbeidssoekerPeriode
import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.OpplysningerOmArbeidssoeker
import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.OpplysningerOmArbeidssoekerJobbsituasjon
import no.nav.pto.veilarbportefolje.database.PostgresTable
import no.nav.pto.veilarbportefolje.domene.Kjonn
import no.nav.pto.veilarbportefolje.domene.value.NavKontor
import no.nav.pto.veilarbportefolje.domene.value.VeilederId
import no.nav.pto.veilarbportefolje.huskelapp.HuskelappRepository
import no.nav.pto.veilarbportefolje.huskelapp.controller.dto.HuskelappOpprettRequest
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerEntity
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerRepositoryV3
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import no.nav.pto.veilarbportefolje.persononinfo.PdlPersonRepository
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent.Gruppe
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLPerson
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomNavKontor
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomVeilederId
import no.nav.pto.veilarbportefolje.util.TestDataUtils.tilfeldigDatoTilbakeITid
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.ResultSet
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

class TestDataClient(
    private val jdbcTemplatePostgres: JdbcTemplate,
    private val arbeidssokerRegistreringRepositoryV2: ArbeidssokerRegistreringRepositoryV2,
    private val oppfolgingsbrukerRepository: OppfolgingsbrukerRepositoryV3,
    private val arbeidslisteRepositoryV2: ArbeidslisteRepositoryV2,
    private val opensearchTestClient: OpensearchTestClient,
    private val oppfolgingRepositoryV2: OppfolgingRepositoryV2,
    private val pdlIdentRepository: PdlIdentRepository,
    private val pdlPersonRepository: PdlPersonRepository,
    private val huskelappRepository: HuskelappRepository
) {
    fun endreNavKontorForBruker(aktoerId: AktorId, navKontor: NavKontor) {
        jdbcTemplatePostgres.update(
            """
                        update oppfolgingsbruker_arena_v2 set nav_kontor = ?
                        where fodselsnr = (select fnr from aktive_identer where aktorId = ?)
                        
                        """.trimIndent(),
            navKontor.value, aktoerId.get()
        )
    }

    fun setupBrukerMedArbeidsliste(
        aktoerId: AktorId,
        navKontor: NavKontor,
        veilederId: VeilederId,
        startDato: ZonedDateTime
    ) {
        val fnr = randomFnr()
        pdlIdentRepository.upsertIdenter(
            listOf(
                PDLIdent(aktoerId.get(), false, Gruppe.AKTORID),
                PDLIdent(fnr.get(), false, Gruppe.FOLKEREGISTERIDENT)
            )
        )
        arbeidslisteRepositoryV2.insertArbeidsliste(
            ArbeidslisteDTO(fnr)
                .setAktorId(aktoerId)
                .setNavKontorForArbeidsliste(navKontor.value)
                .setVeilederId(veilederId)
                .setKategori(Arbeidsliste.Kategori.GUL)
        )

        lagreBrukerUnderOppfolging(aktoerId, fnr, navKontor, veilederId, startDato, null)
        opensearchTestClient.oppdaterArbeidsliste(aktoerId, true)
    }

    fun setupBrukerMedHuskelapp(
        aktoerId: AktorId,
        navKontor: NavKontor,
        veilederId: VeilederId,
        startDato: ZonedDateTime
    ) {
        val fnr = randomFnr()
        pdlIdentRepository.upsertIdenter(
            listOf(
                PDLIdent(aktoerId.get(), false, Gruppe.AKTORID),
                PDLIdent(fnr.get(), false, Gruppe.FOLKEREGISTERIDENT)
            )
        )
        huskelappRepository.opprettHuskelapp(
            HuskelappOpprettRequest(
                fnr,
                LocalDate.now(),
                "test",
                EnhetId.of(navKontor.value)
            ), veilederId
        )

        lagreBrukerUnderOppfolging(aktoerId, fnr, navKontor, veilederId, startDato, null)
        opensearchTestClient.oppdaterArbeidsliste(aktoerId, true)
    }

    fun lagreBrukerUnderOppfolging(aktoerId: AktorId, startDato: ZonedDateTime) {
        val fnr = randomFnr()
        lagreBrukerUnderOppfolging(aktoerId, fnr, randomNavKontor(), VeilederId.of(null), startDato, null)
    }

    fun lagreBrukerUnderOppfolging(aktoerId: AktorId, fnr: Fnr) {
        lagreBrukerUnderOppfolging(
            aktoerId,
            fnr,
            randomNavKontor(),
            randomVeilederId(),
            tilfeldigDatoTilbakeITid(),
            null
        )
    }

    fun lagreBrukerUnderOppfolging(
        aktoerId: AktorId,
        navKontor: NavKontor,
        veilederId: VeilederId,
        startDato: ZonedDateTime, diskresjonKode: String?
    ) {
        val fnr = randomFnr()
        lagreBrukerUnderOppfolging(aktoerId, fnr, navKontor, veilederId, startDato, diskresjonKode)
    }

    fun lagreBrukerUnderOppfolging(aktoerId: AktorId, fnr: Fnr, navKontor: String?, diskresjonKode: String?) {
        val veilederId = randomVeilederId()
        lagreBrukerUnderOppfolging(
            aktoerId,
            fnr,
            NavKontor.of(navKontor),
            veilederId,
            ZonedDateTime.now(),
            diskresjonKode
        )
    }

    fun lagreBrukerUnderOppfolging(aktoerId: AktorId, fnr: Fnr, navKontor: NavKontor, veilederId: VeilederId) {
        lagreBrukerUnderOppfolging(aktoerId, fnr, navKontor, veilederId, ZonedDateTime.now(), null)
    }

    fun hentUnderOppfolgingOgAktivIdent(aktoerId: AktorId?): Boolean {
        return oppfolgingRepositoryV2.erUnderOppfolgingOgErAktivIdent(aktoerId)
    }

    private fun lagreBrukerUnderOppfolging(
        aktoerId: AktorId,
        fnr: Fnr,
        navKontor: NavKontor,
        veilederId: VeilederId,
        startDato: ZonedDateTime,
        diskresjonKode: String?
    ) {
        pdlIdentRepository.upsertIdenter(
            listOf(
                PDLIdent(aktoerId.get(), false, Gruppe.AKTORID),
                PDLIdent(fnr.get(), false, Gruppe.FOLKEREGISTERIDENT)
            )
        )
        pdlPersonRepository.upsertPerson(
            fnr,
            PDLPerson().setFoedsel(LocalDate.now()).setKjonn(Kjonn.K).setDiskresjonskode(diskresjonKode)
        )
        oppfolgingRepositoryV2.settUnderOppfolging(aktoerId, startDato)
        oppfolgingRepositoryV2.settVeileder(aktoerId, veilederId)
        arbeidssokerRegistreringRepositoryV2.upsertBrukerRegistrering(
            ArbeidssokerRegistrertEvent(aktoerId.get(), null, null, null, null, null)
        )
        oppfolgingsbrukerRepository.leggTilEllerEndreOppfolgingsbruker(
            OppfolgingsbrukerEntity(
                fnr.get(), null, null,
                navKontor.value, null, null, null,
                ZonedDateTime.now()
            )
        )
        opensearchTestClient.createUserInOpensearch(aktoerId)
    }

    companion object {
        @JvmStatic
        fun getArbeidssoekerPeriodeFraDb(
            jdbcTemplate: JdbcTemplate,
            arbeidssoekerPeriodeId: UUID
        ): ArbeidssoekerPeriode? {
            return try {
                jdbcTemplate.queryForObject(
                    """SELECT * FROM ${PostgresTable.SISTE_ARBEIDSSOEKER_PERIODE.TABLE_NAME} WHERE ${PostgresTable.SISTE_ARBEIDSSOEKER_PERIODE.ARBEIDSSOKER_PERIODE_ID} =?""",
                    { rs: ResultSet, _ ->
                        ArbeidssoekerPeriode(
                            rs.getObject(
                                PostgresTable.SISTE_ARBEIDSSOEKER_PERIODE.ARBEIDSSOKER_PERIODE_ID,
                                UUID::class.java
                            ),
                            Fnr.of(rs.getString(PostgresTable.SISTE_ARBEIDSSOEKER_PERIODE.FNR))
                        )
                    },
                    arbeidssoekerPeriodeId
                )
            } catch (e: EmptyResultDataAccessException) {
                null
            }
        }

        @JvmStatic
        fun getOpplysningerOmArbeidssoekerFraDb(
            jdbcTemplate: JdbcTemplate,
            arbeidssoekerPeriode: UUID
        ): OpplysningerOmArbeidssoeker? {
            return try {
                jdbcTemplate.queryForObject(
                    """SELECT * FROM ${PostgresTable.OPPLYSNINGER_OM_ARBEIDSSOEKER.TABLE_NAME} WHERE ${PostgresTable.OPPLYSNINGER_OM_ARBEIDSSOEKER.PERIODE_ID} =?""",
                    { rs: ResultSet, _ ->
                        val opplysningerOmArbeidssoekerId =
                            rs.getObject(
                                PostgresTable.OPPLYSNINGER_OM_ARBEIDSSOEKER.OPPLYSNINGER_OM_ARBEIDSSOEKER_ID,
                                UUID::class.java
                            )
                        OpplysningerOmArbeidssoeker(
                            opplysningerOmArbeidssoekerId,
                            rs.getObject(PostgresTable.OPPLYSNINGER_OM_ARBEIDSSOEKER.PERIODE_ID, UUID::class.java),
                            DateUtils.toZonedDateTime(rs.getTimestamp(PostgresTable.OPPLYSNINGER_OM_ARBEIDSSOEKER.SENDT_INN_TIDSPUNKT)),
                            rs.getString(PostgresTable.OPPLYSNINGER_OM_ARBEIDSSOEKER.UTDANNING_NUS_KODE),
                            rs.getString(PostgresTable.OPPLYSNINGER_OM_ARBEIDSSOEKER.UTDANNING_BESTATT),
                            rs.getString(PostgresTable.OPPLYSNINGER_OM_ARBEIDSSOEKER.UTDANNING_GODKJENT),
                            OpplysningerOmArbeidssoekerJobbsituasjon(opplysningerOmArbeidssoekerId, emptyList())
                        )
                    },
                    arbeidssoekerPeriode
                )
            } catch (e: EmptyResultDataAccessException) {
                null
            }
        }

        @JvmStatic
        fun getOpplysningerOmArbeidssoekerJobbsituasjonFraDb(
            jdbcTemplate: JdbcTemplate,
            opplysningerOmArbeidssoekerId: UUID
        ): OpplysningerOmArbeidssoekerJobbsituasjon? {
            val jobbSituasjoner: List<String> = try {
                jdbcTemplate.queryForList(
                    """SELECT * FROM ${PostgresTable.OPPLYSNINGER_OM_ARBEIDSSOEKER_JOBBSITUASJON.TABLE_NAME} WHERE ${PostgresTable.OPPLYSNINGER_OM_ARBEIDSSOEKER_JOBBSITUASJON.OPPLYSNINGER_OM_ARBEIDSSOEKER_ID} =?""",
                    opplysningerOmArbeidssoekerId
                ).map { rs ->
                    rs[PostgresTable.OPPLYSNINGER_OM_ARBEIDSSOEKER_JOBBSITUASJON.JOBBSITUASJON] as String
                }
            } catch (e: EmptyResultDataAccessException) {
                return null
            }

            return if (jobbSituasjoner.isEmpty())
                null
            else
                OpplysningerOmArbeidssoekerJobbsituasjon(
                    opplysningerOmArbeidssoekerId,
                    jobbSituasjoner
                )

        }

        @JvmStatic
        fun getProfileringFraDb(jdbcTemplate: JdbcTemplate, periodeId: UUID): Profilering? {
            return try {
                jdbcTemplate.queryForObject(
                    """SELECT * FROM ${PostgresTable.PROFILERING.TABLE_NAME} WHERE ${PostgresTable.PROFILERING.PERIODE_ID} =?""",
                    { rs: ResultSet, _ ->
                        Profilering(
                            rs.getObject(PostgresTable.PROFILERING.PERIODE_ID, UUID::class.java),
                            Profileringsresultat.valueOf(rs.getString(PostgresTable.PROFILERING.PROFILERING_RESULTAT)),
                            DateUtils.toZonedDateTime(rs.getTimestamp(PostgresTable.PROFILERING.SENDT_INN_TIDSPUNKT))
                        )
                    },
                    periodeId
                )
            } catch (e: EmptyResultDataAccessException) {
                null
            }
        }
    }
}
