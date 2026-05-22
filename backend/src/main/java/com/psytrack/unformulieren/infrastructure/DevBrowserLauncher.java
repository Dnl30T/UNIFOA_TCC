package com.psytrack.unformulieren.infrastructure;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DevBrowserLauncher {

    @EventListener(ApplicationReadyEvent.class)
    public void openSwagger() throws Exception {
        Runtime.getRuntime().exec(new String[]{"xdg-open", "http://localhost:8080/swagger-ui/index.html"});
    }
}
