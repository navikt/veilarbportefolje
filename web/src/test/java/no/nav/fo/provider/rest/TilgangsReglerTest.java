package no.nav.fo.provider.rest;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class TilgangsReglerTest {


    @Test
    public void skalReturnereFalseDersomIkkeIListe() {
        System.setProperty("portefolje.pilot.enhetliste", "0000,0001");

        assertThat(TilgangsRegler.enhetErIPilot("0002"), is(false));
    }

    @Test
    public void skalReturnereTrueDersomEnhetErIPilot() {
        System.setProperty("portefolje.pilot.enhetliste", "0000,0001");

        assertThat(TilgangsRegler.enhetErIPilot("0000"), is(true));
    }

    @Test
    public void skalReturnereTrueDersomListeIkkeFinnes() {
        assertThat(TilgangsRegler.enhetErIPilot("0002"), is(true));
    }

    @Test
    public void skalReturnereTrueDersomIngenEnheterIListe() {
        System.setProperty("portefolje.pilot.enhetliste", "[]");
        assertThat(TilgangsRegler.enhetErIPilot("0002"), is(true));
    }


}