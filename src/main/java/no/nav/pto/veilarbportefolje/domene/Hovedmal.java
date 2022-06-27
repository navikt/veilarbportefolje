package no.nav.pto.veilarbportefolje.domene;

import no.nav.pto.veilarbportefolje.siste14aVedtak.Siste14aVedtakKafkaDTO;

public enum Hovedmal {
    SKAFFEA,
    BEHOLDEA,
    OKEDELT;

    public static Hovedmal fraVedtaksstotte(Siste14aVedtakKafkaDTO.Hovedmal hovedmal) {
        switch (hovedmal) {
            case SKAFFE_ARBEID -> {
                return SKAFFEA;
            }
            case BEHOLDE_ARBEID -> {
                return BEHOLDEA;
            }
            case OKE_DELTAKELSE -> {
                return OKEDELT;
            }
            default -> throw new IllegalStateException("Manglende mapping av innsatsgruppe");
        }
    }
}
