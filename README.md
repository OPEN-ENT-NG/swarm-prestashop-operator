You can run your application in dev mode, which enables live coding, using:

./mvnw compile quarkus

NOTE: Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.
# swarm-prestashop-k8s-operator

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Prestashop storage (PVC)

The operator now creates a PersistentVolumeClaim for Prestashop data and mounts it at `/var/www/html`.

Default behavior:
- PVC name: `<site-name>-prestashop-data`
- Default size: `1Gi`
- Default accessModes: `ReadWriteOnce`

You can override these defaults with the `storage` section in the CR spec:

```yaml
spec:
	storage:
		size: 5Gi
		storageClassName: fast-ssd
		accessModes:
			- ReadWriteOnce
```

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw compile quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true -Dquarkus.native.native-image-xmx=6G
```

You can then execute your native executable with: `./target/swarm-prestashop-k8s-operator-1.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Provided Code

### REST

Easily start your REST Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)

Deployment

The resources required for deployment can be found in the /resources directory.

To deploy the image, apply the generated Custom Resource Definition (CRD) manually using the following command:

kubectl apply -f prestashop.yml

## Utilisation simplifiée
### Definition de la CRD
La CRD ou Custom Ressource Definition est une ressouce kubernetes que nous pouvons personaliser et controller grace a un operateur.
Pour definir les specifications de cette dernière, on peut modifier les fichier presents dans "resources".
Une fois le code java modifié, recompiler le projet et le rerun, la ressource sera automatiquement generée dans le kubernetes.
### Utilisation de la CRD
Une fois que la CRD est publiée, on peut déployer cette ressource. On creer donc un fichier (ici prestashop.yml) qui contient une instantiation de cette ressource et on l'instancie grâce à "kubectl apply prestashop.yml -n {le nom du namespace}.

### Lien avec l´opérateur
Dans le code java de l'operateur (correspondant au code dans controller), on retrouve l'utilisation des spec associées a notre CRD (exemple "replicas" ici).
La partie réconcile de kuber'etes est donc une fonction qui va (en interrogeant l'API quarkus) s'assurer que la réalité sur le cluster, corresponde à nos demandes dans les soecifications.
Notre CRD est donc monitorée en quelque sorte par l'opérateur et elle.
Nous definissons également dans le reconcile quel déploiement la CRD va effectué.
Ici la CrD va déployer la ressource demandée dans le yml present dans /resources/deployment.yml.
