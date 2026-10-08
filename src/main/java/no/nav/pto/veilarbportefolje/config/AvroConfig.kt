package no.nav.pto.veilarbportefolje.config

import org.apache.avro.util.ClassSecurityValidator
import org.springframework.context.annotation.Configuration

@Configuration("avroConfig")
class AvroConfig {
    init {
        val trustedPackages = listOf(
            "no.nav.arbeid.cv.avro.",
            "no.nav.paw.arbeidssokerregisteret.api.",
        )
        ClassSecurityValidator.setGlobal(
            ClassSecurityValidator.composite(
                ClassSecurityValidator.DEFAULT,
                { clazz -> trustedPackages.any { clazz.name.startsWith(it) } }
            )
        )
    }
}
