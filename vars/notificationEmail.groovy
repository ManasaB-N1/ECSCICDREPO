def call(from_email, to_email, region, stage, job_name, status) {
    def subject = ""
    def message = ""

    if (status == "success") {
        subject = "$job_name Jenkins Success Notification"
        message = "<p>$job_name Jenkins Job has been successfully executed</p>"
    } else if (status == "failure") {
        subject = "$job_name Jenkins Failure Notification"
        message = "<p>$stage stage Failed on Jenkins</p>"
    } else if (status == "start") {
        subject = "$job_name Jenkins Job Start Notification"
        message = "<p>$job_name Jenkins Job has been started</p>"
    } else {
        // Handle invalid status
        return
    }

    sh "aws ses send-email --from $from_email --to $to_email --html '$message' --subject '$subject' --region $region"
}
