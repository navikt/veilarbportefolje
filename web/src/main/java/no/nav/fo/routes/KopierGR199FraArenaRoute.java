package no.nav.fo.routes;

import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.apache.camel.spring.SpringRouteBuilder;
import org.springframework.beans.factory.annotation.Value;

import static java.lang.String.format;

public class KopierGR199FraArenaRoute extends SpringRouteBuilder {

    @Value("${filmottak.loependeYtelser.sftp.username}")
    String username;

    @Value("${filmottak.loependeYtelser.sftp.password}")
    String password;

    @Value("${filmottak.loependeYtelser.sftp}")
    String server;

    private IndekserHandler indekserHandler;

    public KopierGR199FraArenaRoute(IndekserHandler indekserHandler) {
        this.indekserHandler = indekserHandler;
    }

    @Override
    public void configure() throws Exception {
        from(format(server, username, password))
                .autoStartup(true)
                .convertBodyTo(String.class)
                .unmarshal(new JaxbDataFormat("no.nav.melding.virksomhet.loependeytelser.v1"))
                .bean(indekserHandler, "indekser")
                .routeId(getClass().getName());
    }
}
