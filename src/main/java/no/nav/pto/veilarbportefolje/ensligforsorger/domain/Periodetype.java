package no.nav.pto.veilarbportefolje.ensligforsorger.domain;

/**
 * Type periode et vedtak om overgangsstønad til enslig forsørger gjelder for. Beskrivelsene som vises
 * til veileder er definert i {@link no.nav.pto.veilarbportefolje.ensligforsorger.mapping.PeriodetypeTilBeskrivelse}.
 * <p>
 * Kombinert med {@link Aktivitetstype} avgjør periodetypen om bruker har aktivitetsplikt, se
 * {@link no.nav.pto.veilarbportefolje.ensligforsorger.mapping.AktivitetsTypeTilAktivitetsplikt}. For
 * {@link #MIGRERING} (og enkelte umappede kombinasjoner) er aktivitetsplikt ikke kjent og vil være
 * {@code null} i OpenSearch-dokumentet — dette er en gyldig domenetilstand, ikke feil i dataene.
 */
public enum Periodetype {
    /** Vedtak migrert fra Infotrygd, uten fullstendig informasjon om aktivitetsplikt. */
    MIGRERING,
    /** Forlengelse av en pågående stønadsperiode. */
    FORLENGELSE,
    /** Ordinær hovedperiode med overgangsstønad. */
    HOVEDPERIODE,
    /** Perioden er en følge av sanksjon (f.eks. brudd på aktivitetsplikt). */
    SANKSJON,
    /** Periode innvilget før fødsel. */
    PERIODE_FØR_FØDSEL,
    /** Utvidelse av stønadsperioden. */
    UTVIDELSE,
    /** Ny periode innvilget for et nytt barn. */
    NY_PERIODE_FOR_NYTT_BARN,
    /** Periode knyttet til at barnet er særlig tilsynskrevende. */
    SÆRLIG_TILSYNSKREVENDE_BARN,
    /** Periode knyttet til at barnet er under 14 måneder. */
    BARN_UNDER_14_MÅNEDER,
    /** Periode knyttet til forbigående sykdom hos barnet. */
    FORBIGÅENDE_SYKDOM_HOS_BARNET
}
