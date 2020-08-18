package no.nav.pto.veilarbportefolje.feed.controller;

import no.nav.pto.veilarbportefolje.feed.common.*;
import no.nav.pto.veilarbportefolje.feed.consumer.FeedConsumer;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.*;

import static java.util.Optional.ofNullable;
import static org.slf4j.LoggerFactory.getLogger;


@Component
@Consumes("application/json")
@Produces("application/json")
@Path("feed")
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

    // CONSUMER CONTROLLER

    @HEAD
    @Path("{name}")
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
