def call(Map config) {

    pipeline {
        agent any

        environment {
            TAG = "${env.BUILD_NUMBER}"
        }

        stages {

            stage('Build') {
                steps {
                    script {
                        def IMAGE_NAME = config.image
                        sh "docker build -t ${IMAGE_NAME}:${TAG} ."
                    }
                }
            }

            stage('Login Docker Hub') {
                steps {
                    withCredentials([usernamePassword(credentialsId: 'dockerhub', usernameVariable: 'USER', passwordVariable: 'PASS')]) {
                        sh 'echo $PASS | docker login -u $USER --password-stdin'
                    }
                }
            }

            stage('Push Image') {
                steps {
                    script {
                        def IMAGE_NAME = config.image
                        sh "docker push ${IMAGE_NAME}:${TAG}"
                    }
                }
            }

            stage('Update Infra') {
                steps {
                    script {
                        sh """
                        rm -rf infra || true
                        git clone https://github.com/viniciusmorao-dev/infra-demo-app.git infra
                        cd infra
                        sed -i 's/APP1_VERSION=.*/APP1_VERSION=${TAG}/' .env
                        docker-compose down || true

                        rm -rf /tmp/infra || true
                        mkdir -p /tmp/infra/nginx
                        
                        # Copiar arquivos específicos
                        cp infra/docker-compose.yaml /tmp/infra/
                        cp infra/.env /tmp/infra/
                        cp infra/nginx/default.conf /tmp/infra/nginx/
                    
                        docker-compose config   # imprime config final para debug
                        docker-compose up -d --build || true
                        """
                    }
                }
            }
        }
    }
}
