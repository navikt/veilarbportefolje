package no.nav.fo.provider.rest;

import no.nav.fo.service.SolrService;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("solr")
public class SolrController {

    @Inject
    SolrService solrService;

    @Path("hovedindeksering")
    @GET
    public boolean hovedIndeksering() {
        solrService.hovedindeksering();
        return true;
    }
}
