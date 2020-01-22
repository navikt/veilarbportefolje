package no.nav.pto.veilarbportefolje.domene;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.pto.veilarbportefolje.elastic.domene.Bucket;

import java.util.List;

import static java.util.stream.Collectors.toList;

@Data
@Accessors(chain = true)
public class FacetResults {
    private List<Facet> facetResults;

    public FacetResults() {
    }

    public FacetResults(List<Bucket> buckets) {
        this.facetResults =
                buckets.stream()
                        .map(entry -> new Facet()
                                .setValue(entry.getKey())
                                .setCount(entry.getDoc_count())
                        ).collect(toList());
    }
}
