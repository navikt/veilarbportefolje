package no.nav.pto.veilarbportefolje.feed.controller;

import no.nav.pto.veilarbportefolje.feed.common.Authorization;
import no.nav.pto.veilarbportefolje.feed.consumer.FeedConsumer;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

import static java.util.Optional.ofNullable;
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
    public ResponseEntity webhookCallback(@PathVariable("name") String feedname) {
        return ofNullable(feedname)
                .map((name) -> consumers.get(name))
                .map((consumer) -> authorizeRequest(consumer, feedname))
                .map(FeedConsumer::webhookCallback)
                .map((hadCallback) -> ResponseEntity.status(hadCallback ? 200 : 404))
                .orElse(ResponseEntity.status(404))
                .build();
    }

    private <T extends Authorization> T authorizeRequest(T feed, String name) {
        if (!feed.getAuthorizationModule().isRequestAuthorized(name)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return feed;
    }

}
