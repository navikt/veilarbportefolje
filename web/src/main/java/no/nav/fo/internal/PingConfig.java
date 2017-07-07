package no.nav.fo.internal;


import no.nav.fo.service.PepClient;
import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.sbl.dialogarena.types.Pingable.Ping.PingMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.inject.Inject;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

@Configuration
public class PingConfig {

    @Inject
    private PepClient pep;

    @Value("${loependeytelser.path}")
    String filpath;

    @Value("${loependeytelser.filnavn}")
    String filnavn;

    @Bean
    public Pingable pepPing() {
        PingMetadata metadata = new PingMetadata(
                "ABAC via " + System.getProperty("abac.endpoint.url"),
                "Tilgangskontroll, sjekk om NAV-ansatt har tilgang til bruker.",
                true
        );

        return () -> {
            try {
                pep.ping();
                return Pingable.Ping.lyktes(metadata);
            } catch (Exception e) {
                return Pingable.Ping.feilet(metadata, e);
            }
        };
    }

    @Bean
    public Pingable issoPing() throws IOException {
        PingMetadata metadata = new PingMetadata(
                "ISSO via " + System.getProperty("isso.isalive.url"),
                "Sjekker om is-alive til ISSO svarer. Single-signon pålogging.",
                true
        );

        return () -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(System.getProperty("isso.isalive.url")).openConnection();
                connection.connect();
                if (connection.getResponseCode() == 200) {
                    return Pingable.Ping.lyktes(metadata);
                }
                return Pingable.Ping.feilet(metadata, new Exception("Statuskode: " + connection.getResponseCode()));
            } catch (Exception e) {
                return Pingable.Ping.feilet(metadata, e);
            }
        };
    }

    @Bean
    public Pingable nfsPing() {
        PingMetadata metadata = new PingMetadata(
                "NFS via" + System.getProperty("loependeytelser.path"),
                "Sjekk om fil med brukere som mottar ytelser ligger på disk",
                true
        );

        return () -> {
            File file = new File(filpath, filnavn);
            if (file.exists()) {
                return Pingable.Ping.lyktes(metadata);
            } else {
                return Pingable.Ping.feilet(metadata, new FileNotFoundException("File not found at " + filpath + filnavn));
            }
        };
    }

    @Bean
    public Pingable aktivitetPing() throws IOException {
        PingMetadata metadata = new PingMetadata(
                "" + System.getProperty("aktiviteter.feed.isalive.url"),
                "Sjekker om is-alive til VeilarbAktivitet svarer.",
                false
        );

        return () -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(System.getProperty("aktiviteter.feed.isalive.url")).openConnection();
                connection.connect();
                if (connection.getResponseCode() == 200) {
                    return Pingable.Ping.lyktes(metadata);
                }
                return Pingable.Ping.feilet(metadata, new Exception("Statuskode: " + connection.getResponseCode()));
            } catch (Exception e) {
                return Pingable.Ping.feilet(metadata, e);
            }
        };
    }
}
