pipeline {

  agent any

  environment {
    NEXUS_PASSWORD = credentials('nexus-ode-password')
    IMAGE = "maven.opendigitaleducation.com/docker/repository/sre-docker-hosted/swarm-prestashop-operator:${params.VERSION}"
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Build and Push Docker image') {
      steps {
        script {
          def cmd = "docker buildx build \\\n"
          cmd += "  --platform linux/amd64,linux/arm64 \\\n"
          cmd += "  -t ${IMAGE} \\\n"
          cmd += "  --push \\\n"
          cmd += "  ."

          sh cmd
        }
      }
    }
  }
}
