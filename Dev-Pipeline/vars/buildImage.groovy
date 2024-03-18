//Latest image is build from the latest clone from pullBitbucket.groovy
def call(APP_IMAGE_LOCAL) {
  sh "sudo docker build . -t ${APP_IMAGE_LOCAL}"
}
