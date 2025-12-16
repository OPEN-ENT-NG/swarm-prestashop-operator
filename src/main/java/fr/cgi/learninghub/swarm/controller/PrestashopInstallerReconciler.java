package fr.cgi.learninghub.swarm.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;

import fr.cgi.learninghub.swarm.resource.Prestashop;
import fr.cgi.learninghub.swarm.resource.PrestashopInstallerSpec;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonDeletingOperation;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.api.reconciler.Context;

@ControllerConfiguration(namespaces = Constants.WATCH_CURRENT_NAMESPACE)
public class PrestashopInstallerReconciler implements Reconciler<Prestashop> {
  
    // K8S API utility
    private final KubernetesClient k8sClient;
    
    public PrestashopInstallerReconciler(KubernetesClient k8sClient) {
        this.k8sClient = k8sClient;
    }

    @Override
    public UpdateControl<Prestashop> reconcile(Prestashop resource, Context<Prestashop> context) {
        System.out.println("🛠️  Create / update Prestashop resource operator ! 🛠️");

        String namespace = resource.getMetadata().getNamespace();
        String name = resource.getMetadata().getName();

        PrestashopInstallerSpec.SiteSpec siteSpec = resource.getSpec().site();
        PrestashopInstallerSpec.DatabaseSpec dbSpec = resource.getSpec().database();

        // Create the Apache ConfigMap if it doesn't exist
        ConfigMap apacheConfigMap = k8sClient.configMaps().inNamespace(namespace).withName(name + "-apache-configmap").get();
        if (apacheConfigMap == null) {
            apacheConfigMap = loadYaml(ConfigMap.class, "/apache.configmap.yml");
            apacheConfigMap.getMetadata().setNamespace(namespace);
            apacheConfigMap.getMetadata().setName(name + "-apache-configmap");
            apacheConfigMap.getMetadata().setOwnerReferences(Collections.singletonList(new OwnerReferenceBuilder()
                    .withUid(resource.getMetadata().getUid())
                    .withApiVersion(resource.getApiVersion())
                    .withName(name)
                    .withKind(resource.getKind())
                    .build()
            ));

            // Customize the ConfigMap
            apacheConfigMap.getData().computeIfPresent("httpd.conf", (key, conf) -> conf.replace("<<SITE_PATH>>", siteSpec.path()));

            k8sClient.configMaps().inNamespace(namespace).resource(apacheConfigMap).createOr(NonDeletingOperation::update);
        }

        // Create the post-install ConfigMap if it doesn't exist
        ConfigMap postInstallConfigMap = k8sClient.configMaps().inNamespace(namespace).withName(name + "-post-install-configmap").get();
        if (postInstallConfigMap == null) {
            postInstallConfigMap = loadYaml(ConfigMap.class, "/post-install.configmap.yml");
            postInstallConfigMap.getMetadata().setNamespace(namespace);
            postInstallConfigMap.getMetadata().setName(name + "-post-install-configmap");
            postInstallConfigMap.getMetadata().setOwnerReferences(Collections.singletonList(new OwnerReferenceBuilder()
                    .withUid(resource.getMetadata().getUid())
                    .withApiVersion(resource.getApiVersion())
                    .withName(name)
                    .withKind(resource.getKind())
                    .build()
            ));

            // Customize the ConfigMap
            String virtualUri = (siteSpec.path().startsWith("/") ? siteSpec.path().substring(1) : siteSpec.path()) + "/";
            postInstallConfigMap.getData().computeIfPresent("script.sh", (key, conf) -> conf.replace("<<SITE_PATH>>", virtualUri));

            k8sClient.configMaps().inNamespace(namespace).resource(postInstallConfigMap).createOr(NonDeletingOperation::update);
        }

        // Create the Prestashop Service if it doesn't exist
        Service psService = k8sClient.services().inNamespace(namespace).withName(name).get();
        if (psService == null) {
            psService = loadYaml(Service.class, "/prestashop.service.yml");
            psService.getMetadata().setNamespace(namespace);
            psService.getMetadata().setName(name);
            psService.getMetadata().setOwnerReferences(Collections.singletonList(new OwnerReferenceBuilder()
                    .withUid(resource.getMetadata().getUid())
                    .withApiVersion(resource.getApiVersion())
                    .withName(name)
                    .withKind(resource.getKind())
                    .build()
            ));
            psService.getSpec().getSelector().put("app", name);

            k8sClient.services().inNamespace(namespace).resource(psService).createOr(NonDeletingOperation::update);
        }

        // Create the Prestashop StatefulSet if it doesn't exist
        StatefulSet psStatefulSet = k8sClient.apps().statefulSets().inNamespace(namespace).withName(name).get();
        if (psStatefulSet == null) {
            // Load the Prestashop default statefulset
            psStatefulSet = loadYaml(StatefulSet.class, "/prestashop.statefulset.yml");
            psStatefulSet.getMetadata().setNamespace(namespace);
            psStatefulSet.getMetadata().setName(name);
            psStatefulSet.getMetadata().getLabels().put("app.kubernetes.io/app", name);
            psStatefulSet.getMetadata().setOwnerReferences(Collections.singletonList(new OwnerReferenceBuilder()
                    .withUid(resource.getMetadata().getUid())
                    .withApiVersion(resource.getApiVersion())
                    .withName(name)
                    .withKind(resource.getKind())
                    .build()
            ));

            psStatefulSet.getSpec().getSelector().getMatchLabels().put("app", name);
            psStatefulSet.getSpec().getTemplate().getMetadata().getLabels().put("app", name);

            psStatefulSet.getSpec().getTemplate().getSpec().getVolumes().forEach(volume -> {
                if (volume.getConfigMap() != null)
                    volume.getConfigMap().setName(volume.getConfigMap().getName().replace("prestashop-site-id", name));
            });

            psStatefulSet.getSpec().getTemplate().getSpec().getContainers().getFirst().setEnv(Arrays.asList(
                    new EnvVar("DB_SERVER", dbSpec.host(), null),
                    new EnvVar("DB_PORT", String.valueOf(dbSpec.port()), null),
                    new EnvVar("DB_NAME", dbSpec.name(), null),
                    new EnvVar("DB_USER", dbSpec.user(), null),
                    new EnvVar("DB_PASSWD", null, new EnvVarSourceBuilder()
                            .withNewSecretKeyRef(dbSpec.passwordSecretKey(), dbSpec.passwordSecretName(), false)
                            .build()
                    ),
                    new EnvVar("PS_INSTALL_AUTO", "1", null),
                    new EnvVar("PS_DOMAIN", siteSpec.host(), null),
                    new EnvVar("PS_LANGUAGE", "fr", null),
                    new EnvVar("PS_COUNTRY", "FR", null),
                    new EnvVar("PS_FOLDER_ADMIN", "ps-admin", null),
                    new EnvVar("ADMIN_MAIL", siteSpec.adminEmail(), null),
                    new EnvVar("ADMIN_PASSWD", siteSpec.adminPassword(), null),
                    new EnvVar("PS_ENABLE_SSL", "1", null)
            ));

            k8sClient.apps().statefulSets().inNamespace(namespace).resource(psStatefulSet).createOr(NonDeletingOperation::update);
        }

        return UpdateControl.noUpdate();
    }

    /**
     *  Load a YAML file and transform it to a Java class.
     * 
     * @param clazz The java class to create
     * @param yamlPath The yaml file path in the classpath
     */
    private <T> T loadYaml(Class<T> clazz, String yamlPath) {
        try (InputStream is = getClass().getResourceAsStream(yamlPath)) {
          return Serialization.unmarshal(is, clazz);
        } catch (IOException ex) {
          throw new IllegalStateException("Cannot find yaml on classpath: " + yamlPath);
        }
    }

}