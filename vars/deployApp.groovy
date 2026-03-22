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
                        
                        docker-compose config   # imprime config final para debug
                        docker-compose up -d --build || true
                        """
                    }
                }
            }
        }
    }
}
