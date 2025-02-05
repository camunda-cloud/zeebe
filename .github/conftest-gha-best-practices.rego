package main

# The `input` variable is a data structure that contains a YAML file's contents
# as objects and arrays. See https://www.openpolicyagent.org/docs/latest/philosophy/#how-does-opa-work

deny[msg] {
    # only enforced on workflows that have jobs defined (prevents problems on empty YAML documents)
    input.jobs

    # check if the workflow specifies a human-readable name, helps with debugging
    not input.name

    msg := "This GitHub Actions workflow has no name property!"
}

deny[msg] {
    # only enforced on Unified CI and related workflows
    input.name == ["CI", "Zeebe CI"][_]

    count(get_jobs_without_timeoutminutes(input.jobs)) > 0

    msg := sprintf("There are GitHub Actions jobs without timeout-minutes! Affected job IDs: %s",
        [concat(", ", get_jobs_without_timeoutminutes(input.jobs))])
}

warn[msg] {
    # only enforced on Unified CI and related workflows
    input.name == ["CI", "Zeebe CI"][_]

    count(get_jobs_with_timeoutminutes_higher_than(input.jobs, 15)) > 0

    msg := sprintf("There are GitHub Actions jobs with too high (>15) timeout-minutes! Affected job IDs: %s",
        [concat(", ", get_jobs_with_timeoutminutes_higher_than(input.jobs, 15))])
}

deny[msg] {
    # only enforced on Unified CI and related workflows
    input.name == ["CI", "Zeebe CI"][_]

    count(get_jobs_without_cihealth(input.jobs)) > 0

    msg := sprintf("There are GitHub Actions jobs that don't send CI Health metrics! Affected job IDs: %s",
        [concat(", ", get_jobs_without_cihealth(input.jobs))])
}

deny[msg] {
    # The "on" key gets transformed by conftest into "true" due to some legacy
    # YAML standards, see https://stackoverflow.com/q/42283732/2148786 - so
    # "on.push" becomes "true.push" which is why below statements use "true"
    # instead of "on". Weird quirk...

    # This rule should encourage using "on.pull_request" trigger for workflows
    # that should run on PRs since that trigger allows to do change detection
    # and has more CI health metrics available (target branch).

    # enforce on workflows that have on.push trigger defined
    input["true"].push
    # but don't specify a set of branches
    not input["true"].push.branches

    msg := "This GitHub Actions workflows triggers on.push to any branch! Use on.pull_request for non-main/stable* branches."
}

###########################   RULE HELPERS   ##################################

get_jobs_without_timeoutminutes(jobInput) = jobs_without_timeoutminutes {
    jobs_without_timeoutminutes := { job_id |
        job := jobInput[job_id]

        # not enforced on jobs that invoke other reusable workflows (instead enforced there)
        not job.uses

        # check if there is timeout-minutes specified
        not job["timeout-minutes"]
    }
}

get_jobs_with_timeoutminutes_higher_than(jobInput, max_timeout) = jobs_without_timeoutminutes {
    jobs_without_timeoutminutes := { job_id |
        job := jobInput[job_id]

        # not enforced on jobs that invoke other reusable workflows (instead enforced there)
        not job.uses

        # check if timeout-minutes is higher than allowed maximum
        job["timeout-minutes"] > max_timeout
    }
}

get_jobs_without_cihealth(jobInput) = jobs_without_cihealth {
    jobs_without_cihealth := { job_id |
        job := jobInput[job_id]

        # not enforced on "special" jobs needed for control flow in Unified CI
        job_id != "detect-changes"
        job_id != "check-results"
        job_id != "test-summary"

        # not enforced on jobs that invoke other reusable workflows (instead enforced there)
        not job.uses

        # check that there is at least one step that invokes CI Health metric submission helper action
        cihealth_steps := { step |
            step := job.steps[_]
            step.name == "Observe build status"
            step.uses == "./.github/actions/observe-build-status"
        }
        count(cihealth_steps) < 1
    }
}
