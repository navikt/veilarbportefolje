package no.nav.pto.veilarbportefolje.controller.dto;

import no.nav.pto.veilarbportefolje.domene.Statustall;

public record EnhetPortefoljeStatustallRespons(
        Statustall statustallMedBrukerinnsyn,
        Statustall statustallUtenBrukerinnsyn
) {
}
