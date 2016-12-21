package no.nav.sbl.fo.config.endpoints;

import no.nav.virksomhet.organisering.enhetogressurs.v1.Ressurs;
import no.nav.virksomhet.tjenester.enhet.meldinger.v1.*;
import no.nav.virksomhet.tjenester.enhet.v1.binding.Enhet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@Configuration
public class VirksomhetEnhetMOCK {

    @Bean
    public static Enhet virksomhetEnhet() {
        return new Enhet() {

            @Override
            public HentEnhetListeResponse hentEnhetListe(HentEnhetListeRequest req) {
                HentEnhetListeResponse response = new HentEnhetListeResponse();
                response.getEnhetListe().addAll(singletonList(createEnhet("1000", "testenhet")));
                response.setRessurs(createRessurs());
                return response;
            }

            @Override
            public FinnEnhetListeResponse finnEnhetListe(FinnEnhetListeRequest req) {
                return new FinnEnhetListeResponse();
            }
            @Override
            public HentRessursListeResponse hentRessursListe(HentRessursListeRequest req) {
                return new HentRessursListeResponse();
            }

            @Override
            public void ping(){

            }

            private no.nav.virksomhet.organisering.enhetogressurs.v1.Enhet createEnhet(String enhetId, String navn) {
                no.nav.virksomhet.organisering.enhetogressurs.v1.Enhet enhet = mock(no.nav.virksomhet.organisering.enhetogressurs.v1.Enhet.class);
                when(enhet.getEnhetId()).thenReturn(enhetId);
                when(enhet.getNavn()).thenReturn(navn);
                return enhet;
        }

        private Ressurs createRessurs() {
            Ressurs ressurs = new Ressurs();
            ressurs.setRessursId("X123456");
            ressurs.setNavn("Navn Navnesen");
            ressurs.setEtternavn("Navnesen");
            ressurs.setFornavn("Navn");
            return ressurs;
        }



    };

    }

}
