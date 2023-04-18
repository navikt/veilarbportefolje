package no.nav.pto.veilarbportefolje.domene;

import no.nav.pto.veilarbportefolje.opensearch.domene.Avvik14aStatistikkResponse.Avvik14aStatistikkAggregation.Avvik14aStatistikkFilter.Avvik14aStatistikkBuckets;

public record Avvik14aStatistikk(
        Long antallMedInnsatsgruppeUlik,
        Long antallMedHovedmaalUlik,
        Long antallMedInnsatsgruppeOgHovedmaalUlik,
        Long antallMedInnsatsgruppeManglerINyKilde
) {

    public static Avvik14aStatistikk of(Avvik14aStatistikkBuckets avvik14aStatistikkBuckets) {
        return new Avvik14aStatistikk(
                avvik14aStatistikkBuckets.getInnsatsgruppeUlik().getDoc_count(),
                avvik14aStatistikkBuckets.getHovedmaalUlik().getDoc_count(),
                avvik14aStatistikkBuckets.getInnsatsgruppeOgHovedmaalUlik().getDoc_count(),
                avvik14aStatistikkBuckets.getInnsatsgruppeManglerINyKilde().getDoc_count()
        );
    }
}
