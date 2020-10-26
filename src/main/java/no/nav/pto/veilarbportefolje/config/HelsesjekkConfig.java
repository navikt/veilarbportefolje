package no.nav.pto.veilarbportefolje.config;

import no.nav.common.abac.Pep;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.health.selftest.SelfTestCheck;
import no.nav.common.health.selftest.SelfTestChecks;
import no.nav.common.health.selftest.SelfTestMeterBinder;
import no.nav.pto.veilarbportefolje.hovedindeksering.arenafiler.gr199.ytelser.KopierGR199FraArena;
import no.nav.pto.veilarbportefolje.hovedindeksering.arenafiler.gr202.tiltak.TiltakHandler;
import no.nav.pto.veilarbportefolje.elastic.ElasticConfig;
import no.nav.pto.veilarbportefolje.kafka.KafkaConfig;
import no.nav.pto.veilarbportefolje.kafka.KafkaHelsesjekk;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static no.nav.pto.veilarbportefolje.config.DatabaseConfig.dbPinger;
import static no.nav.pto.veilarbportefolje.elastic.ElasticConfig.FORVENTET_MINIMUM_ANTALL_DOKUMENTER;

@Configuration
public class HelsesjekkConfig {

    @Bean
    public SelfTestChecks selfTestChecks(AktorregisterClient aktorregisterClient,
                                         Pep veilarbPep,
                                         TiltakHandler tiltakHandler,
                                         KopierGR199FraArena kopierGR199FraArena,
                                         JdbcTemplate jdbcTemplate) {
        List<SelfTestCheck> asyncSelftester = List.of(
                new SelfTestCheck(String.format("Sjekker at antall dokumenter > %s", FORVENTET_MINIMUM_ANTALL_DOKUMENTER), false, ElasticConfig::checkHealth),
                new SelfTestCheck("Database for portefolje", true, () -> dbPinger(jdbcTemplate)),
                new SelfTestCheck("Aktorregister", true, aktorregisterClient),
                new SelfTestCheck("ABAC", true, veilarbPep.getAbacClient()),
                new SelfTestCheck("Sjekker henting av tiltaksfil fra arena over sftp", true, tiltakHandler::sftpTiltakPing),
                new SelfTestCheck("Sjekker henting av ytelser-fil fra arena over sftp", true, kopierGR199FraArena::sftpLopendeYtelserPing)
        );

        List<SelfTestCheck> kafkaSelftester = Arrays.stream(KafkaConfig.Topic.values())
                .map(topic -> new SelfTestCheck("Sjekker at vi får kontakt med partisjonene for " + topic, false, new KafkaHelsesjekk(topic)))
                .collect(toList());

        final List<SelfTestCheck> selftests = concat(asyncSelftester.stream(), kafkaSelftester.stream()).collect(toList());
        return new SelfTestChecks(selftests);
    }

    @Bean
    public SelfTestMeterBinder selfTestMeterBinder(SelfTestChecks selfTestChecks) {
        return new SelfTestMeterBinder(selfTestChecks);
    }
}
