package no.nav.sbl.fo.service;

import no.nav.virksomhet.tjenester.enhet.meldinger.v1.HentEnhetListeRequest;
import no.nav.virksomhet.tjenester.enhet.meldinger.v1.HentEnhetListeResponse;
import no.nav.virksomhet.tjenester.enhet.v1.binding.Enhet;
import org.slf4j.Logger;

import javax.inject.Inject;

import static org.slf4j.LoggerFactory.getLogger;

public class VirksomhetEnhetServiceImpl {

    private static final Logger logger = getLogger(VirksomhetEnhetServiceImpl.class);

    @Inject
    private Enhet enhetWs;

}
