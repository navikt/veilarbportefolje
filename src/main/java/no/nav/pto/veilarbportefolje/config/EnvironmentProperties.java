package no.nav.pto.veilarbportefolje.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.env")
public class EnvironmentProperties {
    private String naisAadDiscoveryUrl;
    private String naisAadClientId;
    private String dbUrl;
    private String unleashUrl;
    private String unleashApiToken;
    private String opensearchUri;
    private String opensearchUsername;
    private String opensearchPassword;
    private String kodeverkUrl;
    private String kodeverkScope;
    private String veilarboppfolgingUrl;
    private String veilarboppfolgingScope;
    private String veilarbvedtaksstotteUrl;
    private String veilarbvedtaksstotteScope;
    private String veilarbveilederUrl;
    private String veilarbveilederScope;
    private String veilarbarenaUrl;
    private String veilarbarenaScope;
    private String pdlUrl;
    private String pdlScope;
    private String poaoTilgangUrl;
    private String poaoTilgangScope;
    private String oppslagArbeidssoekerregisteretScope;
    private String oppslagArbeidssoekerregisteretUrl;
    private String ensligForsorgerUrl;
    private String ensligForsorgerScope;
}

