package no.nav.pto.veilarbportefolje.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.env")
public class EnvironmentProperties {

    private String abacUrl;
    private String dbUrl;
    private String difiUrl;
    private String aktorregisterUrl;
    private String stsDiscoveryUrl;
    private String arenaLoependeYtelserUrl;
    private String arenaPaagaaendeAktiviteterUrl;
    private String arenaFilmottakSFTPUsername;
    private String arenaFilmottakSFTPPassword;
    private String openAmDiscoveryUrl;
    private String openAmClientId;
    private String azureAdDiscoveryUrl;
    private String azureAdClientId;
    private String openAmRefreshUrl;
    private String soapStsUrl;
}