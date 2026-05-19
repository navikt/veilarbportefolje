package no.nav.pto.veilarbportefolje.aktiviteter.v1

import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicBoolean

@Component
class PortefoljeAktivitetKafkaConsumerState {
    private val stoppetPaaGrunnAvVedvarendeFeil = AtomicBoolean(false)

    fun markerStoppetPaaGrunnAvVedvarendeFeil() {
        stoppetPaaGrunnAvVedvarendeFeil.set(true)
    }

    fun nullstillFeilstopp() {
        stoppetPaaGrunnAvVedvarendeFeil.set(false)
    }

    fun erStoppetPaaGrunnAvVedvarendeFeil(): Boolean =
        stoppetPaaGrunnAvVedvarendeFeil.get()
}
