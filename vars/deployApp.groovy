def call(Map config) {

    pipeline {
        agent any

        environment {
            IMAGE_NAME = config.image
            TAG = "${env.BUILD_NUMBER}"
        }

        stages {

            stage('Build') {
                steps {
                    sh 'docker build -t $IMAGE_NAME:$TAG .'
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
                    sh 'docker push $IMAGE_NAME:$TAG'
                }
            }

            stage('Update Infra') {
                steps {
                    sh """
                    rm -rf infra || true
                    git clone https://seu-repo-infra.git infra
                    cd infra

                    sed -i 's/APP1_VERSION=.*/APP1_VERSION=${TAG}/' .env

                    docker-compose down
                    docker-compose up -d
                    """
                }
            }
        }
    }
}
