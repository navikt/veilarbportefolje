package no.nav.fo.consumer;


import no.nav.fo.domene.AktivitetDataFraFeed;

import org.slf4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

public class AktivitetFeedHandler {

    private static final Logger LOG = getLogger(AktivitetFeedHandler.class);


    @Inject
    public AktivitetFeedHandler() {

    }

    @Transactional
    public void handleFeedPage(List<AktivitetDataFraFeed> aktivitetOppdateringer) {

    }

}
