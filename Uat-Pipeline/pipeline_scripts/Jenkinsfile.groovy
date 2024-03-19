@Library("BuildScripts") _

pipeline{
  agent any
  environment {
    AWS_SECRET=credentials('06a9bb8c-9875-4c75-9ce3-bf0a30ae5a94')
    BRANCH_NAME=sh (returnStdout: true, script: 'aws secretsmanager get-secret-value --secret-id \$AWS_SECRET | jq --raw-output .SecretString | jq -r ."BRANCH_NAME"').trim()
    CREDENTIALS=sh (returnStdout: true, script: 'aws secretsmanager get-secret-value --secret-id \$AWS_SECRET | jq --raw-output .SecretString | jq -r ."GIT_TOKEN"').trim()
    GIT_REPO=sh (returnStdout: true, script: 'aws secretsmanager get-secret-value --secret-id \$AWS_SECRET | jq --raw-output .SecretString | jq -r ."GIT_REPO"').trim()
    APP_IMAGE_LOCAL=sh (returnStdout: true, script: 'aws secretsmanager get-secret-value --secret-id \$AWS_SECRET | jq --raw-output .SecretString | jq -r ."APP_IMAGE_LOCAL"').trim()
    REGION=sh (returnStdout: true, script: 'aws secretsmanager get-secret-value --secret-id \$AWS_SECRET | jq --raw-output .SecretString | jq -r ."REGION"').trim()
    ECR_REPO=sh (returnStdout: true, script: 'aws secretsmanager get-secret-value --secret-id \$AWS_SECRET | jq --raw-output .SecretString | jq -r ."ECR_REPO"').trim()
    FAMILY=sh (returnStdout: true, script: 'aws secretsmanager get-secret-value --secret-id \$AWS_SECRET | jq --raw-output .SecretString | jq -r ."FAMILY"').trim()
    SERVICE=sh (returnStdout: true, script: 'aws secretsmanager get-secret-value --secret-id \$AWS_SECRET | jq --raw-output .SecretString | jq -r ."SERVICE"').trim()
    CLUSTER=sh (returnStdout: true, script: 'aws secretsmanager get-secret-value --secret-id \$AWS_SECRET | jq --raw-output .SecretString | jq -r ."CLUSTER"').trim()
    FROM_EMAIL=sh (returnStdout: true, script: 'aws secretsmanager get-secret-value --secret-id \$AWS_SECRET | jq --raw-output .SecretString | jq -r ."FROM_EMAIL"').trim()
    TO_EMAIL=sh (returnStdout: true, script: 'aws secretsmanager get-secret-value --secret-id \$AWS_SECRET | jq --raw-output .SecretString | jq -r ."TO_EMAIL"').trim()
    DESIRED_COUNT="1"
    FILENAME = "${env.FAMILY}-${env.BUILD_NUMBER}.json" // Do not change this
    }
  stages {
    stage("Git Clone") {      
      steps {
        script {
          try{
            echo "Cloning Git Repo"
            notificationEmail("${env.FROM_EMAIL}", "${env.TO_EMAIL}", "${env.REGION}", "${STAGE_NAME}", "${JOB_BASE_NAME}", "start")
            pullGit("${env.BRANCH_NAME}", "${env.CREDENTIALS}", "${env.GIT_REPO}")
          }
          catch(err) {
                    echo "${STAGE_NAME} Stage Failed in ${JOB_BASE_NAME} Jenkins Job"
                    notificationEmail("${env.FROM_EMAIL}", "${env.TO_EMAIL}", "${env.REGION}", "${STAGE_NAME}", "${JOB_BASE_NAME}", "failure")
                    throw err
          }
        }
      }
    }
    stage("Build Image") {      
      steps {
        script {
          try{
            echo "Building new image"
            buildImage("${env.APP_IMAGE_LOCAL}")
          }
          catch(err) {
                    echo "${STAGE_NAME} Stage Failed in ${JOB_BASE_NAME} Jenkins Job"
                    notificationEmail("${env.FROM_EMAIL}", "${env.TO_EMAIL}", "${env.REGION}", "${STAGE_NAME}", "${JOB_BASE_NAME}", "failure")
                    throw err
          }
        }
      }
    }
    stage("Push Image") {      
      steps {
        script { 
          try{ 
            echo "Pushing Docker Image to ECR"
            appTag = incrementalTagging("${env.ECR_REPO}")
            echo "${appTag}" 
            pushImage("${env.REGION}", "${env.ECR_REPO}", "${env.APP_IMAGE_LOCAL}", "$appTag")
          }
          catch(err) {
                    echo "${STAGE_NAME} Stage Failed in ${JOB_BASE_NAME} Jenkins Job"
                    notificationEmail("${env.FROM_EMAIL}", "${env.TO_EMAIL}", "${env.REGION}", "${STAGE_NAME}", "${JOB_BASE_NAME}", "failure")
                    throw err
          }
        }
      }
    }
    stage("Create Task Definition") {      
      steps {
        script {
          try{
            echo "Creating New Task Definition Version"
            createJson("${env.FAMILY}", "${env.ECR_REPO}", "${env.FILENAME}", "${env.REGION}", "${JOB_BASE_NAME}","$appTag")
            newTaskArn = registerTaskDefinition("${env.REGION}", "${env.FILENAME}")
            echo "${newTaskArn}"
          }
          catch(err) {
                    echo "${STAGE_NAME} Stage Failed in ${JOB_BASE_NAME} Jenkins Job"
                    notificationEmail("${env.FROM_EMAIL}", "${env.TO_EMAIL}", "${env.REGION}", "${STAGE_NAME}", "${JOB_BASE_NAME}", "failure")
                    throw err
          }         
        }
      }
    }
    stage("Deploy Latest Build") {
      steps {
        script {
          try{
            echo "Deploying Latest Docker Image into ECR"
            deployImage("${env.CLUSTER}","${env.SERVICE}", "${env.REGION}", "$newTaskArn", "${env.DESIRED_COUNT}")
          }
          catch(err) {
                    echo "${STAGE_NAME} Stage Failed in ${JOB_BASE_NAME} Jenkins Job"
                    notificationEmail("${env.FROM_EMAIL}", "${env.TO_EMAIL}", "${env.REGION}", "${STAGE_NAME}", "${JOB_BASE_NAME}", "failure")
                    throw err
          }          
        }
      }
    }
    stage("Success Notification"){
      steps{
        script{
          notificationEmail("${env.FROM_EMAIL}", "${env.TO_EMAIL}", "${env.REGION}", "${STAGE_NAME}", "${JOB_BASE_NAME}", "success")
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
  }  
}
