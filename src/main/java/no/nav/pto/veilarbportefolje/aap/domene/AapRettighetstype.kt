package no.nav.pto.veilarbportefolje.aap.domene

enum class AapRettighetstype {
    BISTANDSBEHOV,
    SYKEPENGEERSTATNING,
    STUDENT,
    ARBEIDSSØKER,
    VURDERES_FOR_UFØRETRYGD;

    companion object {
        fun tilFrontendtekst(rettighetstype: AapRettighetstype?): String? {
            if (rettighetstype == null) return null

            return when (rettighetstype) {
                BISTANDSBEHOV -> "Bistandsbehov"
                SYKEPENGEERSTATNING -> "Sykepengeerstatning"
                STUDENT -> "Student"
                ARBEIDSSØKER -> "Arbeidssøker"
                VURDERES_FOR_UFØRETRYGD -> "Vurderes for uføretrygd"
            }
        }
    }
}
