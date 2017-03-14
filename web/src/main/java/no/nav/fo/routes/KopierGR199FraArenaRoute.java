package no.nav.fo.routes;

import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.apache.camel.spring.SpringRouteBuilder;
import org.springframework.beans.factory.annotation.Value;

import static java.lang.String.format;

public class KopierGR199FraArenaRoute extends SpringRouteBuilder {

    @Value("${no.nav.modig.security.systemuser.username}")
    String username;

    @Value("${no.nav.modig.security.systemuser.password}")
    String password;

    @Value("${filmottak.sftp}")
    String server;

    @Override
    public void configure() throws Exception {
        from(format(server, username, password))
                .autoStartup(true)
                .convertBodyTo(String.class)
                .unmarshal(new JaxbDataFormat("no.nav.melding.virksomhet.loependeytelser.v1"))
                .bean(IndekserHandler.class, "indekser")
                .routeId(getClass().getName());
    }
}
