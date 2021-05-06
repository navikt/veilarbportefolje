package no.nav.pto.veilarbportefolje.kafka;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.env.kafka.aiven")
public class KafkaAivenProperties {
    String kafkaBrokers;
    String securityProtocol;
    String kafkaCertificate;
    String kafkaCertificatePath;
    String kafkaPrivateKey;
    String kafkaPrivateKeyPath;
    String kafkaCA;
    String kafkaCAPath;
    String kafkaCredstorePass;
    String kafkaKeystorePath;
    String kafkaTrustorePath;
}
