//This is first step where latest code from the repo is cloned
def call(BRANCH_NAME, CREDENTIALS, GIT_REPO) {
    script {
        echo "Performing git clone."
        sh "pwd"
        sh "git clone -b ${BRANCH_NAME} https://${CREDENTIALS}@${GIT_REPO}"
    }
}