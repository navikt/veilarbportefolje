package no.nav.fo.internal;


import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.types.Pingable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.inject.Inject;

@Configuration
public class PingConfig {

    @Inject
    private Pep pep;

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
}
