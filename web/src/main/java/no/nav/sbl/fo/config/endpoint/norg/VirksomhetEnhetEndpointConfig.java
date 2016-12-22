package no.nav.sbl.fo.config.endpoint.norg;

import no.nav.sbl.dialogarena.common.cxf.CXFClient;
import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.virksomhet.tjenester.enhet.v1.binding.Enhet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.xml.namespace.QName;

@Configuration
public class VirksomhetEnhetEndpointConfig {



    @Bean
    public Pingable virksomhetEnhetPing() {
        Enhet virksomhetEnhet = new CXFClient<>(Enhet.class)
                .address(System.getProperty("norg.virksomhet_enhet.url"))
                .wsdl("wsdl/no/nav/virksomhet/tjenester/enhet/v1/Binding.wsdl")
                .endpointName(new QName("http://nav.no/virksomhet/tjenester/enhet/v1/Binding", "EnhetPort"))
                .serviceName(new QName("http://nav.no/virksomhet/tjenester/enhet/v1/Binding", "Enhet"))
                .configureStsForSystemUser()
                .build();

        return () -> {
            try {
                virksomhetEnhet.ping();
                return Pingable.Ping.lyktes("VirksomhetEnhet");
            } catch (Exception e) {
                return Pingable.Ping.feilet("Feilet", e);
            }
        };
    }
}
