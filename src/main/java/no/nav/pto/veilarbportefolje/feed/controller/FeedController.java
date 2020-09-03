package no.nav.pto.veilarbportefolje.feed.controller;

import no.nav.pto.veilarbportefolje.feed.common.Authorization;
import no.nav.pto.veilarbportefolje.feed.consumer.FeedConsumer;
import org.slf4j.Logger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

import static java.util.Optional.ofNullable;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.web.bind.annotation.RequestMethod.HEAD;


@RestController
@RequestMapping("feed")
public class FeedController {

    private static final Logger LOG = getLogger(FeedController.class);

    private Map<String, FeedConsumer> consumers = new HashMap<>();


    public <DOMAINOBJECT extends Comparable<DOMAINOBJECT>> FeedController addFeed(String clientFeedname, FeedConsumer consumer) {
        LOG.info("ny feed-klient. navn={}", clientFeedname);
        consumers.put(clientFeedname, consumer);
        return this;
    }

    public FeedController() {
        LOG.info("starter");
    }

    @RequestMapping(path = "{name}", method = HEAD)
    public Response webhookCallback(@PathParam("name") String feedname) {
        return ofNullable(feedname)
                .map((name) -> consumers.get(name))
                .map((consumer) -> authorizeRequest(consumer, feedname))
                .map(FeedConsumer::webhookCallback)
                .map((hadCallback) -> Response.status(hadCallback ? 200 : 404))
                .orElse(Response.status(404))
                .build();
    }

    private <T extends Authorization> T authorizeRequest(T feed, String name) {
        if (!feed.getAuthorizationModule().isRequestAuthorized(name)) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        return feed;
    }

}
