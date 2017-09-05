package no.nav.fo.internal;

import no.nav.fo.config.HovedindekseringScheduler;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class TotalHovedindekseringServlet extends HttpServlet {

    private HovedindekseringScheduler hovedindekseringScheduler;

    @Override
    public void init() throws ServletException {
        this.hovedindekseringScheduler = WebApplicationContextUtils.getWebApplicationContext(getServletContext()).getBean(HovedindekseringScheduler.class);
        super.init();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        hovedindekseringScheduler.prosessScheduler();
    }

}
