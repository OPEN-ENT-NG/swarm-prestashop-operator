package fr.cgi.learninghub.swarm.resource;

import java.util.List;

public record PrestashopInstallerSpec(SiteSpec site, DatabaseSpec database, StorageSpec storage) {
    public record SiteSpec(String id, String host, String name, String path, String adminEmail, String adminPassword) {}
    public record DatabaseSpec (String host, int port, String name, String user, String passwordSecretName, String passwordSecretKey) {}
    public record StorageSpec(String size, String storageClassName, List<String> accessModes) {}
}