package no.nav.pto.veilarbportefolje.domene.filtervalg;

import com.fasterxml.jackson.annotation.JsonAlias;

public enum CVjobbprofil {
    @JsonAlias("HAR_DELT_CV")
    HAR_CV_HOS_NAV,
    @JsonAlias("HAR_IKKE_DELT_CV")
    HAR_IKKE_CV_HOS_NAV
}
