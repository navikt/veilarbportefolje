package no.nav.pto.veilarbportefolje.dagpenger

import no.nav.poao_tilgang.client.NorskIdent
import no.nav.pto.veilarbportefolje.dagpenger.dto.DagpengerPeriodeDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository


@Repository
class DagpengerRepository(@Autowired private val db: JdbcTemplate) {

    fun upsertDagpenger(norskIdent: NorskIdent, dagpenger: DagpengerPeriodeDto) {

    }

    fun slettDagpengerForBruker(norskIdent: NorskIdent) {

    }
}
