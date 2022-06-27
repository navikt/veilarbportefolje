package no.nav.pto.veilarbportefolje.domene;


import no.nav.pto.veilarbportefolje.siste14aVedtak.Siste14aVedtakKafkaDTO;

public enum Innsatsgruppe {
    BATT,
    BFORM,
    IKVAL,
    GRADERT_VARIG,
    VARIG;

    public static Innsatsgruppe fraVedtaksstotte(Siste14aVedtakKafkaDTO.Innsatsgruppe innsatsgruppe) {
        switch (innsatsgruppe) {
            case STANDARD_INNSATS -> {
                return IKVAL;
            }
            case SITUASJONSBESTEMT_INNSATS -> {
                return BFORM;
            }
            case SPESIELT_TILPASSET_INNSATS -> {
                return BATT;
            }
            case GRADERT_VARIG_TILPASSET_INNSATS -> {
                return GRADERT_VARIG;
            }
            case VARIG_TILPASSET_INNSATS -> {
                return VARIG;
            }
            default ->
                throw new IllegalStateException("Manglende mapping av innsatsgruppe");
        }
    }

    public static boolean contains(String value) {
        if (value == null) {
            return false;
        }

        try {
            Innsatsgruppe.valueOf(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
