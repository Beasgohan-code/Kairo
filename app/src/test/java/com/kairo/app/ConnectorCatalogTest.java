package com.kairo.app;

import com.kairo.app.data.ConnectorCatalog;
import com.kairo.app.data.ConnectorDefinition;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ConnectorCatalogTest {
    @Test
    public void includesTheCoreDeploymentAndAutomationConnectors() {
        boolean github = false;
        boolean vercel = false;
        boolean n8n = false;
        boolean team = false;
        boolean planning = false;
        boolean data = false;
        boolean notifications = false;
        for (ConnectorDefinition connector : ConnectorCatalog.all()) {
            github |= "github".equals(connector.getId());
            vercel |= "vercel".equals(connector.getId());
            n8n |= "n8n".equals(connector.getId());
            team |= "slack".equals(connector.getId()) || "notion".equals(connector.getId());
            planning |= "linear".equals(connector.getId());
            data |= "supabase".equals(connector.getId());
            notifications |= "discord".equals(connector.getId());
        }
        assertTrue(github && vercel && n8n && team && planning && data && notifications);
    }
}
