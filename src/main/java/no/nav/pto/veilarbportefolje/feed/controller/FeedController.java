package no.nav.pto.veilarbportefolje.feed.controller;

import no.nav.pto.veilarbportefolje.feed.consumer.FeedConsumer;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.web.bind.annotation.RequestMethod.HEAD;


@RestController
@RequestMapping("/api/feed")
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
    public void webhookCallback(@PathVariable("name") String feedname) {
        final FeedConsumer feedConsumer = consumers.get(feedname);

        final boolean authorized = feedConsumer.getAuthorizationModule().isRequestAuthorized(feedname);
        if (!authorized) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Consumer is not authorized to read feed");
        }

        final boolean callback = feedConsumer.webhookCallback();
        if (!callback) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Could not find webhook callback");
        }
    }

}
