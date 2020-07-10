package no.nav.pto.veilarbportefolje.config;

public class FeatureToggle {
    private FeatureToggle() {
    }

    public static final String HOVEDINDEKSERING_MED_PAGING = "portefolje.ny_hovedindeksering";
    public static final String KAFKA_AKTIVITETER = "portefolje.kafka.aktiviteter";
    public static final String KAFKA_AKTIVITETER_BEHANDLE_MELDINGER = "portefolje.behandle.aktivitet.kafkamelding";
    public static final String KAFKA_CV = "veilarbportefolje.kafka.cv.killswitch";
    public static final String KAFKA_OPPFOLGING = "portefolje.kafka.oppfolging";
    public static final String KAFKA_OPPFOLGING_BEHANDLE_MELDINGER = "portefolje.kafka.oppfolging_behandle_meldinger";
    public static final String KAFKA_REGISTRERING = "veilarbportfolje.registrering";
    public static final String KAFKA_VEDTAKSTOTTE = "veilarbportfolje-hent-data-fra-vedtakstotte";
    public static final String KAFKA_VEILARBDIALOG = "veilarbdialog.kafka";
    public static final String MARKER_SOM_SLETTET = "portefolje_marker_som_slettet";
    public static final String VEDTAKSTOTTE_PILOT = "pto.vedtaksstotte.pilot";
}
