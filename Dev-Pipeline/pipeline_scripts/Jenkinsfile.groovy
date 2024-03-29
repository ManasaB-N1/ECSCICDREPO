@Library("BuildScripts") _

pipeline{
  agent any
  environment {
    AWS_SECRET=credentials('668b1783-4fd1-46ed-8fba-30bb2ffcf5bb')
    BRANCH_NAME=sh (returnStdout: true, script: 'aws ssm get-parameters --names \$AWS_SECRET --region ap-south-1 --with-decryption | jq -r .Parameters[0].Value | jq -r .Secrets.BRANCH_NAME').trim()
    CREDENTIALS=sh (returnStdout: true, script: 'aws ssm get-parameters --names \$AWS_SECRET --region ap-south-1 --with-decryption | jq -r .Parameters[0].Value | jq -r .Secrets.GIT_TOKEN').trim()
    GIT_REPO=sh (returnStdout: true, script: 'aws ssm get-parameters --names \$AWS_SECRET --region ap-south-1 --with-decryption | jq -r .Parameters[0].Value | jq -r .Secrets.GIT_REPO').trim()
    APP_IMAGE_LOCAL=sh (returnStdout: true, script: 'aws ssm get-parameters --names \$AWS_SECRET --region ap-south-1 --with-decryption | jq -r .Parameters[0].Value | jq -r .Secrets.APP_IMAGE_LOCAL').trim()
    REGION=sh (returnStdout: true, script: 'aws ssm get-parameters --names \$AWS_SECRET --region ap-south-1 --with-decryption | jq -r .Parameters[0].Value | jq -r .Secrets.REGION').trim()
    ECR_REPO=sh (returnStdout: true, script: 'aws ssm get-parameters --names \$AWS_SECRET --region ap-south-1 --with-decryption | jq -r .Parameters[0].Value | jq -r .Secrets.ECR_REPO').trim()
    FAMILY=sh (returnStdout: true, script: 'aws ssm get-parameters --names \$AWS_SECRET --region ap-south-1 --with-decryption | jq -r .Parameters[0].Value | jq -r .Secrets.FAMILY').trim()
    SERVICE=sh (returnStdout: true, script: 'aws ssm get-parameters --names \$AWS_SECRET --region ap-south-1 --with-decryption | jq -r .Parameters[0].Value | jq -r .Secrets.SERVICE').trim()
    CLUSTER=sh (returnStdout: true, script: 'aws ssm get-parameters --names \$AWS_SECRET --region ap-south-1 --with-decryption | jq -r .Parameters[0].Value | jq -r .Secrets.CLUSTER').trim()
    FROM_EMAIL=sh (returnStdout: true, script: 'aws ssm get-parameters --names \$AWS_SECRET --region ap-south-1 --with-decryption | jq -r .Parameters[0].Value | jq -r .Secrets.FROM_EMAIL').trim()
    TO_EMAIL=sh (returnStdout: true, script: 'aws ssm get-parameters --names \$AWS_SECRET --region ap-south-1 --with-decryption | jq -r .Parameters[0].Value | jq -r .Secrets.TO_EMAIL').trim()
    DESIRED_COUNT="1"
    FILENAME = "${env.FAMILY}-${env.BUILD_NUMBER}.json" // Do not change this 
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