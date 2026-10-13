package no.nav.pto.veilarbportefolje.persononinfo

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.util.SecureLog.secureLog
import org.springframework.stereotype.Service

@Service
class IdentEndringService {

    fun handterNyttFnr(aktorId: AktorId, gammeltFnr: Fnr, nyttFnr: Fnr) {
        secureLog.info(
            "Bruker med aktorId {} har fått nytt fnr. Gammelt fnr: {}, nytt fnr: {}",
            aktorId, gammeltFnr, nyttFnr
        )
    }
}
