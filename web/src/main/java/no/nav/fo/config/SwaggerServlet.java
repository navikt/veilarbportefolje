package no.nav.fo.config;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SwaggerServlet extends HttpServlet {
    private static final String SWAGGER_BASE_URI_PARAMETER = "?input_baseurl=/veilarbportefolje/internal/api/swagger.json";
    private static final String SWAGGER_INDEX_PATH = "/veilarbportefolje/internal/swagger/index.html" + SWAGGER_BASE_URI_PARAMETER;
    private static final String SWAGGER_REQUEST_PATTERN = "^.*/internal/swagger(.*)$";
    private static final String SWAGGER_INDEX_REQUEST_PATTERN = "^.*/internal/swagger/?$";
    private static final String SWAGGER_PATH = "/webjars/swagger-ui/2.2.10";

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (request.getRequestURI().matches(SWAGGER_INDEX_REQUEST_PATTERN)) {
            // Redirects /internal/swagger/ til /internal/swagger/index.html?input_baseurl=... for å få parameteret som en del av urlen
            response.sendRedirect(SWAGGER_INDEX_PATH);
        } else if (request.getRequestURI().matches(SWAGGER_REQUEST_PATTERN)) {
            Matcher matcher = Pattern.compile(SWAGGER_REQUEST_PATTERN).matcher(request.getRequestURI());
            matcher.find();
            String etterspurtFil = matcher.group(1);
            RequestDispatcher index = getServletContext().getRequestDispatcher(SWAGGER_PATH + etterspurtFil);
            index.forward(request, response);
        }
    }
}
