@Library("BuildScripts") _

pipeline{
  agent any
  environment {
    BRANCH_NAME = 'main'
    CREDENTIALS='8a28dca5-0aef-47a8-b62b-f40a071d2715'
    GIT_REPO = 'github.com/ManasaB-N1/ECSAPPREPO.git' 
    APP_IMAGE_LOCAL = 'nginx'
    REGION = 'ap-south-1'
    ECR_REPO = '317185619046.dkr.ecr.ap-south-1.amazonaws.com/bnmuat'
    FAMILY = 'TEST-Prod-ECS-testappbnm'
    SERVICE = 'nginx'
    CLUSTER = 'PROJECT2-Dev-ECS'
    FROM_EMAIL= 'manasa.bn@axcess.io'
    TO_EMAIL = 'manasa.bn@axcess.io'
    DESIRED_COUNT="1"
    FILENAME = "${env.FAMILY}-${env.BUILD_NUMBER}.json"
    }
  stages {
    stage("Git Clone") {      
      steps {
        script {
          echo "Cloning Git Repo"
          pullGit("${env.BRANCH_NAME}", "${env.CREDENTIALS}", "${env.GIT_REPO}")
        }
      }
    }
    stage("Build Image") {      
      steps {
        script {
          echo "Building new image"
          buildImage("${env.APP_IMAGE_LOCAL}")
        }
      }
    }
    stage("Push Image") {      
      steps {
        script { 
          echo "Pushing Docker Image to ECR"
          appTag = incrementalTagging("${env.ECR_REPO}")
          echo "${appTag}" 
          pushImage("${env.REGION}", "${env.ECR_REPO}", "${env.APP_IMAGE_LOCAL}", "$appTag")
        }
      }
    }
    stage("Create Task Definition") {      
      steps {
        script {
          echo "Creating New Task Definition Version"
          createJson("${env.FAMILY}", "${env.ECR_REPO}", "${env.FILENAME}", "${env.REGION}", "${JOB_BASE_NAME}","$appTag")
          newTaskArn = registerTaskDefinition("${env.REGION}", "${env.FILENAME}")
          echo "${newTaskArn}"         
        }
      }
    }
    stage("Deploy Latest Build") {
      steps {
        script {
          echo "Deploying Latest Docker Image into ECR"
          deployImage("${env.CLUSTER}","${env.SERVICE}", "${env.REGION}", "$newTaskArn", "${env.DESIRED_COUNT}")          
        }
      }
    }  
  }
  post {
    always {
        sh 'docker system prune -a -f'        
        sh 'sudo rm -rf /var/lib/jenkins/caches'
        cleanWs()
    }
    failure{
      slackSend( message: "${JOB_BASE_NAME} Job Failed")
    }
    aborted{
      slackSend( message: "${JOB_BASE_NAME} Job aborted")
    }
    success{
      slackSend( message: "${JOB_BASE_NAME} Job executed Successfully")
    }
  }  
}
