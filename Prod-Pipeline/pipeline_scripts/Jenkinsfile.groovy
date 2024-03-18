@Library("BuildScripts") _

pipeline{
  agent any
  environment {
    AWS_SECRET=credentials('2c24ec0c-2719-4b8b-9f88-77dbed09702a')
    REGION=sh (returnStdout: true, script: 'aws secretsmanager get-secret-value --secret-id \$AWS_SECRET | jq --raw-output .SecretString | jq -r ."REGION"').trim()
    FAMILY=sh (returnStdout: true, script: 'aws secretsmanager get-secret-value --secret-id \$AWS_SECRET | jq --raw-output .SecretString | jq -r ."FAMILY"').trim()
    APP_IMAGE_ECR_UAT=sh (returnStdout: true, script: 'aws secretsmanager get-secret-value --secret-id \$AWS_SECRET | jq --raw-output .SecretString | jq -r ."APP_IMAGE_ECR_UAT"').trim()
    APP_IMAGE_ECR_PROD=sh (returnStdout: true, script: 'aws secretsmanager get-secret-value --secret-id \$AWS_SECRET | jq --raw-output .SecretString | jq -r ."APP_IMAGE_ECR_PROD"').trim()
    SERVICE=sh (returnStdout: true, script: 'aws secretsmanager get-secret-value --secret-id \$AWS_SECRET | jq --raw-output .SecretString | jq -r ."SERVICE"').trim()
    CLUSTER=sh (returnStdout: true, script: 'aws secretsmanager get-secret-value --secret-id \$AWS_SECRET | jq --raw-output .SecretString | jq -r ."CLUSTER"').trim()
    FROM_EMAIL=sh (returnStdout: true, script: 'aws secretsmanager get-secret-value --secret-id \$AWS_SECRET | jq --raw-output .SecretString | jq -r ."FROM_EMAIL"').trim()
    TO_EMAIL=sh (returnStdout: true, script: 'aws secretsmanager get-secret-value --secret-id \$AWS_SECRET | jq --raw-output .SecretString | jq -r ."TO_EMAIL"').trim()
    DESIRED_COUNT="1"
    FILENAME = "${env.FAMILY}-${env.BUILD_NUMBER}.json" // Do not change this
  }
  stages {
    stage("Copy Image") {
      steps {
        script {
          try {
              timeout(time: 60, unit: 'SECONDS') {
                uatImageTag = input message: "UAT ECR Image Tag",
                          parameters: [string(name: 'UAT ECR Image Tag', description: "Please provide the UAT ECR Image Tag")]
                echo "$uatImageTag"
            }
          }
          catch(err) {
              echo "${err}"
              echo "No Tag number was specified"
              echo "${STAGE_NAME} Stage Failed in ${JOB_BASE_NAME} Jenkins Job"
          }
          appTag = incrementalTagging("${env.APP_IMAGE_ECR_PROD}")
          echo "${appTag}"
          copyImage("${env.APP_IMAGE_ECR_UAT}", "$uatImageTag", "${env.APP_IMAGE_ECR_PROD}", "$appTag")         
        }
      }
    }
    stage("Create Task Definition") {
      steps {
        script{
          createJson("${env.FAMILY}", "${env.APP_IMAGE_ECR_PROD}", "${env.FILENAME}", "${env.REGION}", "${JOB_BASE_NAME}","$appTag")
          newTaskArn = registerTaskDefinition("${env.REGION}", "${env.FILENAME}")
          echo "${newTaskArn}"
        }
      }
    }
    stage("Deploy Image") {
      steps {
        script {
          echo "Deploying Latest Docker Image into ECS"
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
