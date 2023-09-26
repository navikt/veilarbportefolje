package no.nav.pto.veilarbportefolje.registrering.endring

import no.nav.paw.besvarelse.AndreForhold
import no.nav.paw.besvarelse.AndreForholdSvar
import no.nav.paw.besvarelse.ArbeidssokerBesvarelseEvent
import no.nav.paw.besvarelse.Besvarelse
import no.nav.paw.besvarelse.DinSituasjon
import no.nav.paw.besvarelse.DinSituasjonSvar
import no.nav.paw.besvarelse.DinSituasjonTilleggsData
import no.nav.paw.besvarelse.EndretAv
import no.nav.paw.besvarelse.HelseHinder
import no.nav.paw.besvarelse.HelseHinderSvar
import no.nav.paw.besvarelse.OpprettetAv
import no.nav.paw.besvarelse.SisteStilling
import no.nav.paw.besvarelse.SisteStillingSvar
import no.nav.paw.besvarelse.Utdanning
import no.nav.paw.besvarelse.UtdanningBestatt
import no.nav.paw.besvarelse.UtdanningBestattSvar
import no.nav.paw.besvarelse.UtdanningGodkjent
import no.nav.paw.besvarelse.UtdanningGodkjentSvar
import no.nav.paw.besvarelse.UtdanningSvar
import java.time.Instant
import java.time.LocalDate

fun getArbeidssokerBesvarelseEvent(aktorId: String): ArbeidssokerBesvarelseEvent {
    val utdanning = Utdanning()
    utdanning.endretAv = "BRUKER"
    utdanning.endretTidspunkt = Instant.now()
    utdanning.verdi = UtdanningSvar.HOYERE_UTDANNING_5_ELLER_MER
    utdanning.gjelderFraDato = LocalDate.now()
    utdanning.gjelderTilDato = LocalDate.now()

    val utdanningBestatt = UtdanningBestatt()
    utdanningBestatt.endretAv = "BRUKER"
    utdanningBestatt.endretTidspunkt = Instant.now()
    utdanningBestatt.verdi = UtdanningBestattSvar.JA
    utdanningBestatt.gjelderFraDato = LocalDate.now()
    utdanningBestatt.gjelderTilDato = LocalDate.now()

    val utdanningGodkjent = UtdanningGodkjent()
    utdanningGodkjent.endretAv = "BRUKER"
    utdanningGodkjent.endretTidspunkt = Instant.now()
    utdanningGodkjent.verdi = UtdanningGodkjentSvar.JA
    utdanningGodkjent.gjelderFraDato = LocalDate.now()
    utdanningGodkjent.gjelderTilDato = LocalDate.now()

    val andreForhold = AndreForhold()
    andreForhold.endretAv = "BRUKER"
    andreForhold.endretTidspunkt = Instant.now()
    andreForhold.verdi = AndreForholdSvar.JA
    andreForhold.gjelderFraDato = LocalDate.now()
    andreForhold.gjelderTilDato = LocalDate.now()

    val tilleggsdata = DinSituasjonTilleggsData()
    tilleggsdata.gjelderFraDato = null
    tilleggsdata.harNyJobb = null
    tilleggsdata.oppsigelseDato = null
    tilleggsdata.permitteringForlenget = null
    tilleggsdata.permitteringsProsent = null
    tilleggsdata.forsteArbeidsdagDato = null
    tilleggsdata.sisteArbeidsdagDato = null
    tilleggsdata.stillingsProsent = null

    val dinSituasjon = DinSituasjon()
    dinSituasjon.endretAv = "BRUKER"
    dinSituasjon.endretTidspunkt = Instant.now()
    dinSituasjon.verdi = DinSituasjonSvar.ENDRET_PERMITTERINGSPROSENT
    dinSituasjon.gjelderFraDato = LocalDate.now()
    dinSituasjon.gjelderTilDato = LocalDate.now()
    dinSituasjon.tilleggsData = tilleggsdata

    val helseHinder = HelseHinder()
    helseHinder.endretAv = "BRUKER"
    helseHinder.endretTidspunkt = Instant.now()
    helseHinder.verdi = HelseHinderSvar.NEI
    helseHinder.gjelderFraDato = LocalDate.now()
    helseHinder.gjelderTilDato = LocalDate.now()

    val sisteStilling = SisteStilling()
    sisteStilling.endretAv = "BRUKER"
    sisteStilling.endretTidspunkt = Instant.now()
    sisteStilling.verdi = SisteStillingSvar.HAR_HATT_JOBB
    sisteStilling.gjelderFraDato = LocalDate.now()
    sisteStilling.gjelderTilDato = LocalDate.now()

    val besvarelse = Besvarelse()
    besvarelse.utdanning = utdanning
    besvarelse.utdanningBestatt = utdanningBestatt
    besvarelse.utdanningGodkjent = utdanningGodkjent
    besvarelse.andreForhold = andreForhold
    besvarelse.dinSituasjon = dinSituasjon
    besvarelse.helseHinder = helseHinder
    besvarelse.sisteStilling = sisteStilling

    val kafkaRegistreringMelding = ArbeidssokerBesvarelseEvent()
    kafkaRegistreringMelding.aktorId = aktorId
    kafkaRegistreringMelding.registreringsId = 423423423
    kafkaRegistreringMelding.registreringsTidspunkt = Instant.now()
    kafkaRegistreringMelding.endret = true
    kafkaRegistreringMelding.endretAv = EndretAv.BRUKER
    kafkaRegistreringMelding.endretTidspunkt = Instant.now()
    kafkaRegistreringMelding.foedselsnummer = "12345678910"
    kafkaRegistreringMelding.opprettetAv = OpprettetAv.VEILEDER
    kafkaRegistreringMelding.id = 2314
    kafkaRegistreringMelding.besvarelse = besvarelse

    return kafkaRegistreringMelding
}
