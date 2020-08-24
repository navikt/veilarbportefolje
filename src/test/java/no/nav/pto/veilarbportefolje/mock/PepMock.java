package no.nav.pto.veilarbportefolje.mock;

import no.nav.common.abac.AbacClient;
import no.nav.common.abac.Pep;
import no.nav.common.abac.domain.AbacPersonId;
import no.nav.common.abac.domain.request.ActionId;

public class PepMock implements Pep {

    private final AbacClient abacClient;

    public PepMock(AbacClient abacClient) {
        this.abacClient = abacClient;
    }


    @Override
    public boolean harVeilederTilgangTilEnhet(String s, String s1) {
        return true;
    }

    @Override
    public boolean harVeilederTilgangTilPerson(String s, ActionId actionId, AbacPersonId abacPersonId) {
        return true;
    }

    @Override
    public boolean harTilgangTilPerson(String s, ActionId actionId, AbacPersonId abacPersonId) {
        return true;
    }

    @Override
    public boolean harVeilederTilgangTilOppfolging(String s) {
        return true;
    }

    @Override
    public boolean harVeilederTilgangTilModia(String s) {
        return true;
    }

    @Override
    public boolean harVeilederTilgangTilKode6(String s) {
        return true;
    }

    @Override
    public boolean harVeilederTilgangTilKode7(String s) {
        return true;
    }

    @Override
    public boolean harVeilederTilgangTilEgenAnsatt(String s) {
        return true;
    }

    @Override
    public AbacClient getAbacClient() {
        return abacClient;
    }
}
