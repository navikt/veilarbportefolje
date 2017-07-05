package no.nav.fo.provider.rest;

import io.swagger.annotations.Api;
import no.nav.fo.service.SolrService;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Api(value = "Solr")
@Path("solr")
public class SolrController {

    @Inject
    private SolrService solrService;

    @Path("hovedindeksering")
    @GET
    public boolean hovedIndeksering() {
        solrService.hovedindeksering();
        return true;
    }

    @Path("deltaindeksering")
    @GET
    public boolean deltaIndeksering() {
        solrService.deltaindeksering();
        return true;
    }
}
