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
                        rm -rf infra
                        git clone https://github.com/viniciusmorao-dev/infra-demo-app.git infra
                        cd infra
                        sed -i 's/APP1_VERSION=.*/APP1_VERSION=${TAG}/' .env

                        rm -rf nginx/default.conf || true

                        if [ ! -f nginx/default.conf ]; then
                            echo "ERRO: arquivo nginx/default.conf não encontrado!"
                            exit 1
                        fi
                        
                        docker-compose up -d --build
                        """
                    }
                }
            }
        }
    }
}
