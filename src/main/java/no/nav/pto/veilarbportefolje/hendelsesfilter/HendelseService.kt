package no.nav.pto.veilarbportefolje.hendelsesfilter

import no.nav.pto.veilarbportefolje.kafka.KafkaCommonKeyedConsumerService

class HendelseService : KafkaCommonKeyedConsumerService<HendelseRecordValue>() {
    override fun behandleKafkaMeldingLogikk(kafkaMelding: HendelseRecordValue, nokkel: String) {
        val hendelse = toHendelse(kafkaMelding, nokkel)
        // TODO: Handtere eventuelle feil i mapping

        when(kafkaMelding.operasjon) {
            Operasjon.START -> { startHendelse(hendelse) }
            Operasjon.ENDRING -> { oppdaterHendelse(hendelse)}
            Operasjon.STOPP -> { stoppHendelse(hendelse) }
        }
    }

    private fun startHendelse(hendelse: Hendelse) {
        TODO("Not implemented yet")
    }

    private fun oppdaterHendelse(hendelse: Hendelse) {
        TODO("Not yet implemented")
    }

    private fun stoppHendelse(hendelse: Hendelse) {
        TODO("Not yet implemented")
    }
}