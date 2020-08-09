package no.nav.pto.veilarbportefolje.scheduled;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@WebServlet(
        name = "PersonIdAktorIdServlet",
        urlPatterns = {"/internal/aktorid_to_personid"}
)

public class PersonIdAktorIdServlet extends HttpServlet{

    private PersonIdToAktorIdSchedule personIdToAktorIdSchedule;

    @Autowired
    public PersonIdAktorIdServlet(PersonIdToAktorIdSchedule personIdToAktorIdSchedule) {
        this.personIdToAktorIdSchedule = personIdToAktorIdSchedule;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        personIdToAktorIdSchedule.mapAktorId();
        resp.getWriter().write("Oppdatering av aktoerider startet");
        resp.setStatus(200);
    }
}
