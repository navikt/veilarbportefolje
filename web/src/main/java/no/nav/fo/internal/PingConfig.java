package no.nav.fo.internal;


import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.types.Pingable;
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
    private Pep pep;

    @Value("${loependeytelser.path}")
    String filpath;

    @Value("${loependeytelser.filnavn}")
    String filnavn;

    @Bean
    public Pingable pepPing() {
        return () -> {
            try {
                pep.ping();
                return Pingable.Ping.lyktes("ABAC");
            } catch( Exception e) {
                return Pingable.Ping.feilet("ABAC",e);
            }
        };
    }

    @Bean
    public Pingable issoPing() throws IOException {
        return () -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(System.getProperty("isso.isalive.url")).openConnection();
                connection.connect();
                if (connection.getResponseCode() == 200) {
                    return Pingable.Ping.lyktes("ISSO");
                }
                return Pingable.Ping.feilet("ISSO", new Exception("Statuskode: " + connection.getResponseCode()));
            } catch (Exception e) {
                return Pingable.Ping.feilet("ISSO", e);
            }
        };
    }

    @Bean
    public Pingable nfsPing() {
        return () -> {
            File file = new File(filpath, filnavn);
            if(file.exists()) {
                return Pingable.Ping.lyktes("NFS");
            }else{
                return Pingable.Ping.feilet("NFS", new FileNotFoundException("File not found at " + filpath+filnavn));
            }
        };
    }

    @Bean
    public Pingable aktivitetPing() throws IOException {
        return () -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(System.getProperty("veilarbaktivitet.isalive.url")).openConnection();
                connection.connect();
                if (connection.getResponseCode() == 200) {
                    return Pingable.Ping.lyktes("VeilArbAktivitet");
                }
                return Pingable.Ping.feilet("VeilArbAktivitet", new Exception("Statuskode: " + connection.getResponseCode()));
            } catch (Exception e) {
                return Pingable.Ping.feilet("VeilArbAktivitet", e);
            }
        };
    }
}
