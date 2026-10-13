package no.nav.pto.veilarbportefolje.persononinfo

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.aap.AapService
import no.nav.pto.veilarbportefolje.dagpenger.DagpengerService
import no.nav.pto.veilarbportefolje.hendelsesfilter.HendelseService
import no.nav.pto.veilarbportefolje.tiltakspenger.TiltakspengerService
import no.nav.pto.veilarbportefolje.uforetrygd.UforetrygdService
import no.nav.pto.veilarbportefolje.ungdomsprogram.UngdomsprogramService
import no.nav.pto.veilarbportefolje.util.SecureLog.secureLog
import org.springframework.stereotype.Service

@Service
class IdentEndringService(
    private val hendelseService: HendelseService,
    private val aapService: AapService,
    private val tiltakspengerService: TiltakspengerService,
    private val dagpengerService: DagpengerService,
    private val ungdomsprogramService: UngdomsprogramService,
    private val uforetrygdService: UforetrygdService,
) {

    fun handterNyttFnr(aktorId: AktorId, gammeltFnr: Fnr, nyttFnr: Fnr) {
        secureLog.info(
            "Bruker med aktorId {} har fått nytt fnr. Gammelt fnr: {}, nytt fnr: {}",
            aktorId, gammeltFnr, nyttFnr
        )
        hendelseService.oppdaterPersonIdent(gammeltFnr, nyttFnr)
        oppdaterYtelser(gammeltFnr, nyttFnr)
    }

    private fun oppdaterYtelser(gammeltFnr: Fnr, nyttFnr: Fnr) {
        aapService.oppdaterNorskIdent(gammeltFnr, nyttFnr)
        tiltakspengerService.oppdaterNorskIdent(gammeltFnr, nyttFnr)
        dagpengerService.oppdaterNorskIdent(gammeltFnr, nyttFnr)
        ungdomsprogramService.oppdaterNorskIdent(gammeltFnr, nyttFnr)
        uforetrygdService.oppdaterNorskIdent(gammeltFnr, nyttFnr)
    }
}
