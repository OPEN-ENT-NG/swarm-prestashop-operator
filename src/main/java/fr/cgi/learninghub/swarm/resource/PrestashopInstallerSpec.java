package fr.cgi.learninghub.swarm.resource;

public record PrestashopInstallerSpec(SiteSpec site, DatabaseSpec database) {
    public record SiteSpec(String id, String host, String name, String path, String adminEmail, String adminPassword) {}
    public record DatabaseSpec (String host, int port, String name, String user, String passwordSecretName, String passwordSecretKey) {}
}