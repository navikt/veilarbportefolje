package no.nav.pto.veilarbportefolje.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.env")
public class EnvironmentProperties {
    private String veilarbVeilederUrl;
    private String stsDiscoveryUrl;
    private String openAmDiscoveryUrl;
    private String openAmClientId;
    private String naisAadDiscoveryUrl;
    private String naisAadClientId;
    private String naisAadIssuer;
    private String openAmRefreshUrl;
    private String dbUrl;
    private List<String> admins;
    private String unleashUrl;
    private String abacVeilarbUrl;
    private String abacModiaUrl;
    private String opensearchUri;
    private String opensearchUsername;
    private String opensearchPassword;
}
