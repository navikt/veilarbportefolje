package no.nav.pto.veilarbportefolje.elastic.domene;

public class CountResponse {
    private long count;

    private CountResponse() {
    }

    public CountResponse(long count) {
        this.count = count;
    }

    public long getCount() {
        return count;
    }
}
