pipeline {
    agent any

    tools {
        jdk 'jdk-25' // Match the name in Jenkins Global Tool Configuration
        maven 'maven-3.9'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test') {
            steps {
                // Run only the CIHttpTests to verify API health
                sh './mvnw test -Dtest=CIHttpTests'
            }
        }
    }

    post {
        always {
            // Archive JUnit results to see them in Jenkins UI
            junit '**/target/surefire-reports/*.xml'
            
            // Archive the console output or specific report files as artifacts
            archiveArtifacts artifacts: '**/target/surefire-reports/*.txt', allowEmptyArchive: true
        }
        success {
            echo 'Rest Unit Tests passed successfully!'
        }
        failure {
            echo 'Rest Unit Tests failed. Check the archived artifacts or console log for the Markdown report.'
        }
    }
}
