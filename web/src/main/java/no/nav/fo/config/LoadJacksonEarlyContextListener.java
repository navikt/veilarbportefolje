package no.nav.fo.config;


import no.nav.json.JsonUtils;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.Map;

public class LoadJacksonEarlyContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        JsonUtils.fromJson("{}", Map.class);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

    }

}
