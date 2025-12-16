package fr.cgi.learninghub.swarm.resource;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.ShortNames;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("fr.cgi.learninghub.swarm")
@Version("v1")
@ShortNames("ps")
public class Prestashop extends CustomResource<PrestashopInstallerSpec, Void> implements Namespaced {

}