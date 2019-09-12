package no.nav.fo.veilarbportefolje.indeksering.domene;

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
