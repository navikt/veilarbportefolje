package no.nav.fo.internal;

import no.nav.fo.consumer.feed.FeedClient;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class FeedCallbackServlet extends HttpServlet{

    private FeedClient feedClient;

    @Override
    public void init() throws ServletException {
        this.feedClient = WebApplicationContextUtils.getWebApplicationContext(getServletContext()).getBean(FeedClient.class);
        super.init();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        feedClient.callback();
        resp.setStatus(200);
        resp.getWriter().write("Callback aktivert");
    }
}
