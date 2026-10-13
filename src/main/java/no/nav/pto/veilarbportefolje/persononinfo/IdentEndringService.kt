package no.nav.pto.veilarbportefolje.persononinfo

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.aap.AapService
import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.ArbeidssoekerService
import no.nav.pto.veilarbportefolje.cv.CVServiceV2
import no.nav.pto.veilarbportefolje.dagpenger.DagpengerService
import no.nav.pto.veilarbportefolje.fargekategori.FargekategoriService
import no.nav.pto.veilarbportefolje.hendelsesfilter.HendelseService
import no.nav.pto.veilarbportefolje.huskelapp.HuskelappService
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerServiceV2
import no.nav.pto.veilarbportefolje.tiltakshendelse.TiltakshendelseService
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
    private val fargekategoriService: FargekategoriService,
    private val huskelappService: HuskelappService,
    private val arbeidssoekerService: ArbeidssoekerService,
    private val tiltakshendelseService: TiltakshendelseService,
    private val cvService: CVServiceV2,
    private val oppfolgingsbrukerService: OppfolgingsbrukerServiceV2,
) {

    fun handterNyttFnr(aktorId: AktorId, gammeltFnr: Fnr, nyttFnr: Fnr) {
        secureLog.info(
            "Bruker med aktorId {} har fått nytt fnr. Gammelt fnr: {}, nytt fnr: {}",
            aktorId, gammeltFnr, nyttFnr
        )
        hendelseService.oppdaterPersonIdent(gammeltFnr, nyttFnr)
        oppdaterYtelser(gammeltFnr, nyttFnr)
        fargekategoriService.oppdaterFnr(gammeltFnr, nyttFnr)
        huskelappService.oppdaterFnr(gammeltFnr, nyttFnr)
        arbeidssoekerService.oppdaterFnr(gammeltFnr, nyttFnr)
        tiltakshendelseService.oppdaterFnr(gammeltFnr, nyttFnr)
        cvService.oppdaterFnr(gammeltFnr, nyttFnr)
        oppfolgingsbrukerService.oppdaterIdentPaaNavKontor(aktorId, gammeltFnr, nyttFnr)
    }

    private fun oppdaterYtelser(gammeltFnr: Fnr, nyttFnr: Fnr) {
        aapService.oppdaterNorskIdent(gammeltFnr, nyttFnr)
        tiltakspengerService.oppdaterNorskIdent(gammeltFnr, nyttFnr)
        dagpengerService.oppdaterNorskIdent(gammeltFnr, nyttFnr)
        ungdomsprogramService.oppdaterNorskIdent(gammeltFnr, nyttFnr)
        uforetrygdService.oppdaterNorskIdent(gammeltFnr, nyttFnr)
    }
}
