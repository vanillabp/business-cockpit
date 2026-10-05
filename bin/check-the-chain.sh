#!/usr/bin/env bash
#
# Looks at the chain of Business Cockpit repositories from the outside and asks four
# questions about their main branches:
#
#   1. Do the four repositories name the same version where they have to?
#   2. Has a pin stayed behind without anybody saying so? That is: a newer release is
#      older than Renovate's waiting time, and there is neither an open pull request for
#      it nor a rule in renovate.json which tells Renovate to leave it alone. Part of this
#      question is whether Renovate still reads the pin at all.
#   3. Was the newest run of every workflow on main green?
#   4. Does the published snapshot belong to the head of main?
#
# Nothing is built and no test is run again. Everything is read from the GitHub API and
# from Maven Central. A green state and a state nobody has looked at for days look the
# same from outside, so the answer always starts and ends with the time it was asked.
#
# It changes nothing. It prints a report in Markdown and exits with
#   0  when every question was answered and nothing was found,
#   1  when something was found,
#   2  when a question could not be answered and nothing was found otherwise.
# A question which could not be answered is never green.
#
# Needs bash, curl, jq, perl, sort -V, GNU date and the GitHub CLI (gh), logged in.
# Question 4 also reads GitHub Packages, which asks for a token even for a public
# package. Pass one with the scope read:packages in PACKAGES_TOKEN, and its user in
# PACKAGES_USER. Without it question 4 is answered from the publish runs alone, and the
# report says so.
#
# Run it from anywhere:
#
#   bin/check-the-chain.sh
#   bin/check-the-chain.sh --at 2026-10-01T12:00:00Z
#
# With --at, questions 1 and 2 read the POMs as main had them at that time, which is how
# the check is shown to see a case that has already been fixed. Questions 3 and 4 are
# about today and ignore it.
#
# The nightly workflow .github/workflows/chain-check.yaml runs the same script and turns
# a finding into an issue.

set -uo pipefail

# Renovate waits five days before it offers a release, see minimumReleaseAge in
# vanillabp/renovate-config, and then needs a night to open the pull request. A release
# younger than this is still on its way and not a finding.
renovate_wait_days=7

# A pin changed on main is read by Renovate in the next night. Until then its dependency
# dashboard still shows the old version, and that is not a finding either.
dashboard_wait_hours=36

# The four repositories whose pins are compared. The first one is the root of the chain.
repos=(
    vanillabp/business-cockpit
    vanillabp/businesscockpit-camunda7-adapter
    vanillabp/businesscockpit-camunda8-adapter
    vanillabp/businesscockpit-process-engine-api-adapter
)

# The repositories whose main branch and snapshot are watched: the four, and the MongoDB
# changeset library. It lives in another account and is released to Maven Central on its
# own, but this repository builds against its snapshot, so a broken or stale snapshot
# there reaches us first. Its pins are not compared, because none of its versions ends up
# next to an extension in a workflow module.
#
# Per repository: the coordinate of one published artifact, group and artifact, and the
# POM which names its version. The artifact is the one the next repository of the chain
# reads, or the core of an adapter.
watched=(
    "vanillabp/business-cockpit io.vanillabp.businesscockpit extensions-commons"
    "vanillabp/businesscockpit-camunda7-adapter io.vanillabp.businesscockpit businesscockpit-camunda7-adapter"
    "vanillabp/businesscockpit-camunda8-adapter io.vanillabp.businesscockpit businesscockpit-camunda8-adapter"
    "vanillabp/businesscockpit-process-engine-api-adapter io.vanillabp.businesscockpit businesscockpit-process-engine-api-adapter"
    "Phactum/mongodb-changesets com.phactum.mongodb mongodb-changesets"
)

# The pins which are a promise between the four repositories, one sentence each on why.
# A pin which is not here is not compared, and the list below says why for the ones which
# look as if they belonged.
#
# Per pin: name, Maven Central coordinate whose releases decide whether it stayed behind
# (or '-' for a snapshot, which has no release to fall behind), and the reason.
pins=(
    "release-parent|io.vanillabp:release-parent|The release parent decides the plugins, the Javadoc gate and how a release is cut, and the four are released together."
    "java|-|The lowest Java an application needs is the highest of the four, so they promise one."
    "spring-boot|org.springframework.boot:spring-boot-dependencies|The extension and its BPMS half run in the same Spring Boot application, so they are compiled against the Spring Boot that application brings."
    "quarkus|io.quarkus:quarkus-bom|The extension and its BPMS half are Quarkus extensions of the same application, and a Quarkus extension is built for one Quarkus version."
    "jacoco|org.jacoco:jacoco-maven-plugin|The coverage gate is the same number in every repository, and it means the same thing only when the same JaCoCo counts it."
    "vanillabp|-|The extension and its BPMS half join the same VanillaBP platform in one application."
    "business-cockpit|-|An adapter builds against the extension of the cockpit version it says, and that has to be the one this repository builds."
)

# How each repository names each pin. Two names for one thing are normal here: this
# repository calls every version 'version.<name>', the three adapter repositories use
# '<name>.version'. Renovate reads both. What has to be the same is the value.
#
# Per line: pin, repository, file, how to read it, what to read, and a label for the
# report when a repository names a pin twice.
#   property <name>   a property of the POM
#   parent            the version of the parent
#   project           the version of the project, with ${revision} resolved
#   artifact <id>     the version written right after that artifactId
reads=(
    "release-parent|vanillabp/business-cockpit|pom.xml|parent||"
    "release-parent|vanillabp/businesscockpit-camunda7-adapter|pom.xml|parent||"
    "release-parent|vanillabp/businesscockpit-camunda8-adapter|pom.xml|parent||"
    "release-parent|vanillabp/businesscockpit-process-engine-api-adapter|pom.xml|parent||"
    "java|vanillabp/business-cockpit|pom.xml|property|version.java|"
    "java|vanillabp/businesscockpit-camunda7-adapter|pom.xml|property|version.java|"
    "java|vanillabp/businesscockpit-camunda8-adapter|pom.xml|property|version.java|"
    "java|vanillabp/businesscockpit-process-engine-api-adapter|pom.xml|property|version.java|"
    "spring-boot|vanillabp/business-cockpit|pom.xml|artifact|spring-boot-dependencies|BOM"
    "spring-boot|vanillabp/business-cockpit|pom.xml|artifact|spring-boot-maven-plugin|plugin"
    "spring-boot|vanillabp/businesscockpit-camunda7-adapter|pom.xml|property|spring-boot.version|"
    "spring-boot|vanillabp/businesscockpit-camunda8-adapter|pom.xml|property|spring-boot.version|"
    "spring-boot|vanillabp/businesscockpit-process-engine-api-adapter|pom.xml|property|spring-boot.version|"
    "quarkus|vanillabp/business-cockpit|extensions-commons/pom.xml|property|version.quarkus|"
    "quarkus|vanillabp/businesscockpit-camunda7-adapter|pom.xml|property|quarkus.version|"
    "quarkus|vanillabp/businesscockpit-camunda8-adapter|pom.xml|property|quarkus.version|"
    "quarkus|vanillabp/businesscockpit-process-engine-api-adapter|pom.xml|property|quarkus.version|"
    "jacoco|vanillabp/business-cockpit|pom.xml|property|jacoco.version|"
    "jacoco|vanillabp/businesscockpit-camunda7-adapter|pom.xml|property|jacoco.version|"
    "jacoco|vanillabp/businesscockpit-camunda8-adapter|pom.xml|property|jacoco.version|"
    "jacoco|vanillabp/businesscockpit-process-engine-api-adapter|pom.xml|property|jacoco.version|"
    "vanillabp|vanillabp/business-cockpit|pom.xml|property|version.vanillabp|"
    "vanillabp|vanillabp/businesscockpit-camunda7-adapter|pom.xml|property|adapter-platform.version|"
    "vanillabp|vanillabp/businesscockpit-camunda8-adapter|pom.xml|property|adapter-platform.version|"
    "vanillabp|vanillabp/businesscockpit-process-engine-api-adapter|pom.xml|property|adapter-platform.version|"
    "business-cockpit|vanillabp/business-cockpit|pom.xml|project||"
    "business-cockpit|vanillabp/businesscockpit-camunda7-adapter|pom.xml|property|business-cockpit.version|"
    "business-cockpit|vanillabp/businesscockpit-camunda8-adapter|pom.xml|property|business-cockpit.version|"
    "business-cockpit|vanillabp/businesscockpit-process-engine-api-adapter|pom.xml|property|business-cockpit.version|"
)

# Pins which look as if they belonged to the list above and do not. Printed in every
# report, so whoever reads a quiet report sees what was left out on purpose.
not_compared=(
    "The Camunda 8 client pins of the release lines (camunda8.version.line-*), the Camunda 8 adapter per line and protobuf: they follow camunda-community-hub/vanillabp-camunda8-adapter and are raised by hand, and the nightly matrix of businesscockpit-camunda8-adapter holds them against that repository."
    "testcontainers.version: tests only. No jar of one of the four carries it into another, and only the Camunda 8 adapter names it, for the cluster that camunda-community-hub/vanillabp-camunda8-adapter starts."
    "lombok.version: it translates the source and is gone from the class files, so it never meets another repository's code."
)

at=""
case "${1:-}" in
    --at)
        at=${2:-}
        [ -n "$at" ] || { echo "--at needs a time, like 2026-10-01T12:00:00Z" >&2; exit 2; } ;;
    "") ;;
    *)
        echo "Usage: $0 [--at <time in UTC>]" >&2
        exit 2 ;;
esac

out=$(mktemp -d)
trap 'rm -rf "$out"' EXIT

findings=0
unanswered=0

say() { printf '%s\n' "$*"; }
found() {
    findings=$((findings + 1))
    printf -- '- FOUND: %s\n' "$*"
}
not_answered() {
    unanswered=$((unanswered + 1))
    printf -- '- NOT ANSWERED: %s\n' "$*"
}
fine() { printf -- '- ok: %s\n' "$*"; }
note() { printf -- '- note: %s\n' "$*"; }

utc_now() { date -u +%Y-%m-%dT%H:%M:%SZ; }
epoch() { date -u -d "$1" +%s 2>/dev/null; }
# The time the questions are about: now, or the time given with --at.
about_epoch() { if [ -n "$at" ]; then epoch "$at"; else date -u +%s; fi; }
days_since() { echo $(( ($(about_epoch) - $(epoch "$1")) / 86400 )); }
hours_since() { echo $(( ($(about_epoch) - $(epoch "$1")) / 3600 )); }

# The commit of main which the files are read from: the head, or with --at the newest
# commit of main at that time.
main_ref() {
    local repo=$1
    local cache="$out/ref/$repo"
    if [ ! -f "$cache" ]; then
        mkdir -p "$(dirname "$cache")"
        if [ -z "$at" ]; then
            echo main > "$cache"
        else
            gh api "repos/$repo/commits?sha=main&until=$at&per_page=1" --jq '.[0].sha // empty' \
                2>/dev/null > "$cache" || { rm -f "$cache"; return 1; }
        fi
    fi
    local ref
    ref=$(cat "$cache")
    [ -n "$ref" ] || return 1
    printf '%s' "$ref"
}

# A file of a repository as it is on main, with the XML comments removed, so a version
# named in a comment is never read as the version. Cached per run.
main_file() {
    local repo=$1 path=$2 ref
    local cache="$out/files/$repo/$path"
    if [ ! -f "$cache" ]; then
        ref=$(main_ref "$repo") || return 1
        mkdir -p "$(dirname "$cache")"
        gh api "repos/$repo/contents/$path?ref=$ref" -H 'Accept: application/vnd.github.raw' \
            2>/dev/null | perl -0pe 's/<!--.*?-->//gs' > "$cache.tmp" || {
            rm -f "$cache.tmp"
            return 1
        }
        mv "$cache.tmp" "$cache"
    fi
    cat "$cache"
}

read_pin() {
    local repo=$1 path=$2 how=$3 what=$4
    local pom
    pom=$(main_file "$repo" "$path") || return 1
    case "$how" in
        property)
            printf '%s' "$pom" | sed -n "s|.*<$what>\([^<]*\)</$what>.*|\1|p" | head -1 ;;
        parent)
            printf '%s' "$pom" | perl -0ne 'print $1 if /<parent>.*?<version>\s*([^<\s]+)\s*<\/version>/s' ;;
        project)
            local v
            v=$(printf '%s' "$pom" | perl -0pe 's/<parent>.*?<\/parent>//s' \
                | perl -0ne 'print $1 if /<version>\s*([^<\s]+)\s*<\/version>/s')
            if [ "$v" = '${revision}' ]; then
                v=$(printf '%s' "$pom" | sed -n 's|.*<revision>\([^<]*\)</revision>.*|\1|p' | head -1)
            fi
            printf '%s' "$v" ;;
        artifact)
            printf '%s' "$pom" \
                | perl -0ne "print \$1 if /<artifactId>\\Q$what\\E<\\/artifactId>\\s*<version>\\s*([^<\\s]+)\\s*<\\/version>/s" ;;
    esac
}

highest() { sort -V | tail -1; }

# The releases of a coordinate on Maven Central, without milestones, betas and release
# candidates: Renovate offers those to nobody here, so they are no reason to move.
central_path() {
    local group=${1%%:*} artifact=${1##*:}
    printf 'https://repo1.maven.org/maven2/%s/%s' "${group//.//}" "$artifact"
}
releases() {
    curl --silent --fail --max-time 30 "$(central_path "$1")/maven-metadata.xml" \
        | sed -n 's|.*<version>\([^<]*\)</version>.*|\1|p' \
        | grep -E '^[0-9]+(\.[0-9]+)*(\.Final)?$' | sort -V
}
# Maven Central keeps no release date in its metadata. The time the POM of the release
# was written is the closest thing, and it is what Renovate's waiting time counts from.
released_at() {
    local artifact=${1##*:}
    local date
    date=$(curl --silent --fail --head --max-time 30 \
        "$(central_path "$1")/$2/$artifact-$2.pom" \
        | sed -n 's/^[Ll]ast-[Mm]odified: *//p' | tr -d '\r')
    [ -n "$date" ] || return 1
    date -u -d "$date" +%Y-%m-%dT%H:%M:%SZ
}

# Whether renovate.json of a repository tells Renovate to leave a coordinate alone. That
# is a reason written down, so a pin it holds back is not a silent one. Both forms of
# matchPackageNames used here are understood: '/regex/' and a glob ending in '{/,}**'.
renovate_leaves_alone() {
    local repo=$1 coordinate=$2 config
    config=$(main_file "$repo" renovate.json) || return 1
    printf '%s' "$config" | jq -e --arg c "$coordinate" '
        [.packageRules[]?
         | select(.enabled == false)
         | .matchPackageNames[]?
         | if startswith("/") and endswith("/") then
             (.[1:-1] as $re | $c | test($re))
           else
             (sub("\\{/,\\}\\*\\*$"; "") as $p | ($c == $p) or ($c | startswith($p + ":")))
           end]
        | any' > /dev/null
}

dashboard() {
    local repo=$1
    local cache="$out/dashboard/$repo"
    if [ ! -f "$cache" ]; then
        mkdir -p "$(dirname "$cache")"
        gh issue list -R "$repo" --state open --search 'Dependency Dashboard in:title' \
            --json number,title,updatedAt,body --limit 5 2>/dev/null \
            | jq -c '[.[] | select(.title == "Dependency Dashboard")] | .[0] // empty' \
            > "$cache" || return 1
    fi
    cat "$cache"
}

# The pull requests which are open now, or with --at the ones which were open then.
open_pull_requests() {
    local repo=$1
    local cache="$out/pulls/$repo"
    if [ ! -f "$cache" ]; then
        mkdir -p "$(dirname "$cache")"
        if [ -z "$at" ]; then
            gh pr list -R "$repo" --state open --limit 100 --json number,title,body \
                2>/dev/null > "$cache" || { rm -f "$cache"; return 1; }
        else
            gh pr list -R "$repo" --state all --limit 300 \
                --json number,title,body,createdAt,closedAt 2>/dev/null \
                | jq --arg t "$at" '[.[] | select(.createdAt <= $t
                                     and (.closedAt == null or .closedAt > $t))]' \
                > "$cache" || { rm -f "$cache"; return 1; }
        fi
    fi
    cat "$cache"
}

asked_at=$(utc_now)
say "# Chain check of ${asked_at}"
say
say "Main branches of: ${repos[*]#vanillabp/}, and Phactum/mongodb-changesets for the runs and the snapshot."
if [ -n "$at" ]; then
    say
    say "Questions 1 and 2 read main as it was at $at:"
    for repo in "${repos[@]}"; do
        say "- $repo at $(main_ref "$repo" || echo 'no commit found')"
    done
fi

# ---------------------------------------------------------------------------------------
say
say "## 1. Do the four repositories name the same version?"
say
for pin_line in "${pins[@]}"; do
    IFS='|' read -r pin coordinate reason <<< "$pin_line"
    : > "$out/pin-$pin"
    for read_line in "${reads[@]}"; do
        IFS='|' read -r rpin repo path how what label <<< "$read_line"
        [ "$rpin" = "$pin" ] || continue
        name=${repo#vanillabp/}${label:+ ($label)}
        value=$(read_pin "$repo" "$path" "$how" "$what")
        if [ -z "$value" ]; then
            not_answered "$pin: nothing found in $repo, $path ($how $what). Has the pin been renamed?"
            continue
        fi
        printf '%s\t%s\t%s\t%s\n' "$name" "$value" "$repo" "$path" >> "$out/pin-$pin"
    done
    distinct=$(cut -f2 "$out/pin-$pin" | sort -u)
    count=$(printf '%s\n' "$distinct" | grep -c . || true)
    if [ "$count" -gt 1 ]; then
        found "$pin is not the same everywhere. $reason"
        while IFS=$'\t' read -r name value _ _; do
            say "  - $name: $value"
        done < "$out/pin-$pin"
        say "  - highest: $(printf '%s\n' "$distinct" | highest). Which one is right is not decided here."
    elif [ "$count" -eq 1 ]; then
        fine "$pin $distinct in all of them"
    fi
done
say
say "Not compared, on purpose:"
for line in "${not_compared[@]}"; do
    say "- $line"
done

# ---------------------------------------------------------------------------------------
say
say "## 2. Has a pin stayed behind without anybody saying so?"
say
if [ -n "$at" ]; then
    note "The dependency dashboards are only known as they are today, so with --at they are not read."
fi
for pin_line in "${pins[@]}"; do
    IFS='|' read -r pin coordinate reason <<< "$pin_line"
    [ "$coordinate" != "-" ] || continue
    all_releases=$(releases "$coordinate")
    if [ -z "$all_releases" ]; then
        not_answered "$pin: Maven Central did not answer for $coordinate."
        continue
    fi
    # one verdict per repository, even when a repository names the pin twice
    while IFS=$'\t' read -r repo value path; do
        name=${repo#vanillabp/}

        # The releases above the pin. The newest is what an update would bring today, and
        # the newest which is older than Renovate's waiting time is the one Renovate should
        # already have offered. A pin can be overdue while the newest release is still
        # young: 3.39.3 stayed for a week while 3.39.4 and 3.39.5 came out, and a release of
        # the day before would have hidden that. With --at, a release which came out after
        # that time does not count.
        newest=""
        newest_at=""
        overdue=""
        overdue_at=""
        undated=""
        for candidate in $(printf '%s\n' "$all_releases" | tac); do
            [ "$candidate" != "$value" ] || break
            [ "$(printf '%s\n%s\n' "$value" "$candidate" | highest)" != "$value" ] || break
            when=$(released_at "$coordinate" "$candidate") || when=""
            if [ -z "$when" ]; then
                undated="$undated $candidate"
                continue
            fi
            if [ -n "$at" ] && [ "$(epoch "$when")" -gt "$(epoch "$at")" ]; then
                continue
            fi
            if [ -z "$newest" ]; then
                newest=$candidate
                newest_at=$when
            fi
            if [ "$(days_since "$when")" -ge "$renovate_wait_days" ]; then
                overdue=$candidate
                overdue_at=$when
                break
            fi
        done

        # Is Renovate reading this pin at all? Its dashboard lists every dependency it
        # found, with the version it found. A pin it does not list, or lists with another
        # version long after main changed it, is a pin nobody will ever be offered an
        # update for.
        if [ -z "$at" ] && ! renovate_leaves_alone "$repo" "$coordinate"; then
            board=$(dashboard "$repo")
            if [ -z "$board" ]; then
                found "$name has no open Dependency Dashboard, so nothing says whether Renovate still visits it."
            elif ! printf '%s' "$board" | jq -r .body | grep -qF "\`$coordinate $value\`"; then
                board_at=$(printf '%s' "$board" | jq -r .updatedAt)
                changed_at=$(gh api "repos/$repo/commits?sha=main&path=$path&per_page=1" \
                    --jq '.[0].commit.committer.date' 2>/dev/null)
                listed=$(printf '%s' "$board" | jq -r .body \
                    | sed -n "s|.*\`$coordinate \([^\`]*\)\`.*|\1|p" | sort -u | tr '\n' ' ')
                if [ -n "$changed_at" ] && [ "$(hours_since "$changed_at")" -lt "$dashboard_wait_hours" ]; then
                    note "$name: the dashboard does not show $coordinate $value yet, and $path changed on $changed_at. Renovate reads it in the next night."
                elif [ -n "$listed" ]; then
                    found "$name pins $coordinate $value, and its Dependency Dashboard still shows ${listed% }. The dashboard was last written on $board_at, $path last changed on ${changed_at:-an unknown date}. Renovate has not read the POM since."
                else
                    found "$name pins $coordinate $value in $path, and its Dependency Dashboard (written on $board_at) does not list $coordinate at all. Renovate does not see this pin."
                fi
            fi
        fi

        if [ -n "$undated" ]; then
            not_answered "$name: $pin $value, and Maven Central gives no date for$undated."
        fi
        if [ -z "$newest" ]; then
            [ -n "$undated" ] || fine "$name: $pin $value is the newest release"
            continue
        fi
        if [ -z "$overdue" ]; then
            note "$name: $pin $value, $newest was released on $newest_at, $(days_since "$newest_at") days ago. Renovate waits for it."
            continue
        fi
        age=$(days_since "$overdue_at")
        if renovate_leaves_alone "$repo" "$coordinate"; then
            note "$name: $pin $value, $overdue was released on $overdue_at. renovate.json tells Renovate to leave $coordinate alone, and says why."
            continue
        fi
        pulls=$(open_pull_requests "$repo") || {
            not_answered "$name: $pin $value, $overdue was released on $overdue_at, and the open pull requests could not be read."
            continue
        }
        # A pull request of Renovate names each package and its step in a table, as in
        # "`3.39.3` → `3.40.0`". One which starts from the pin moves it, whatever it moves
        # it to.
        pr=$(printf '%s' "$pulls" | jq -r --arg c "$coordinate" --arg v "$value" \
            '[.[] | select((.body // "" | contains($c)) and (.body // "" | contains("`" + $v + "` → `")))]
             | .[0] | if . then "#\(.number) \(.title)" else empty end')
        if [ -n "$pr" ]; then
            fine "$name: $pin $value, pull request $pr moves it"
            continue
        fi
        where=""
        if [ -z "$at" ]; then
            board=$(dashboard "$repo")
            where=" Its Dependency Dashboard does not mention $overdue."
            if printf '%s' "$board" | jq -r '.body // ""' | grep -qF "$overdue"; then
                where=" Its Dependency Dashboard mentions $overdue, so Renovate knows and holds it back."
            fi
        fi
        found "$name pins $pin $value. $overdue was released on $overdue_at, $age days ago, and no open pull request moves the pin.$where Newest release: $newest of $newest_at."
    done < <(awk -F'\t' '!seen[$3]++ { print $3 "\t" $2 "\t" $4 }' "$out/pin-$pin")
done

# ---------------------------------------------------------------------------------------
say
say "## 3. Was the newest run on main green?"
say
for line in "${watched[@]}"; do
    read -r repo _ _ <<< "$line"
    # Runs of main started by a push, the clock or a person. A run started by another run
    # reports about that run, and pull-request runs are not about main.
    runs=$(gh api "repos/$repo/actions/runs?branch=main&exclude_pull_requests=true&per_page=100" \
        --jq '[.workflow_runs[]
               | select(.event == "push" or .event == "schedule" or .event == "workflow_dispatch")
               | {name, status, conclusion, head_sha, created_at, html_url}]' 2>/dev/null)
    if [ -z "$runs" ]; then
        not_answered "$repo: the list of runs could not be read."
        continue
    fi
    printf '%s' "$runs" > "$out/runs-${repo//\//_}"
    if [ "$(printf '%s' "$runs" | jq length)" -eq 0 ]; then
        found "$repo has no run on main among the newest hundred."
        continue
    fi
    while IFS=$'\t' read -r workflow; do
        newest=$(printf '%s' "$runs" | jq -c --arg w "$workflow" '[.[] | select(.name == $w)] | .[0]')
        done_run=$(printf '%s' "$runs" | jq -c --arg w "$workflow" \
            '[.[] | select(.name == $w and .status == "completed")] | .[0] // empty')
        running=""
        if [ "$(printf '%s' "$newest" | jq -r .status)" != "completed" ]; then
            running=" A newer run is $(printf '%s' "$newest" | jq -r .status) since $(printf '%s' "$newest" | jq -r .created_at)."
        fi
        if [ -z "$done_run" ]; then
            note "$repo, $workflow: no finished run yet.$running"
            continue
        fi
        IFS='|' read -r conclusion sha created url <<< "$(printf '%s' "$done_run" \
            | jq -r '[(.conclusion // ""), .head_sha[0:8], .created_at, .html_url] | join("|")')"
        if [ "$conclusion" = "success" ]; then
            fine "$repo, $workflow: success on $sha, started $created, $(days_since "$created") days ago.$running"
        else
            found "$repo, $workflow: $conclusion on $sha, started $created. $url$running"
        fi
    done < <(printf '%s' "$runs" | jq -r '[.[].name] | unique | .[]')
done

# ---------------------------------------------------------------------------------------
say
say "## 4. Does the published snapshot belong to the head of main?"
say
if [ -z "${PACKAGES_TOKEN:-}" ]; then
    not_answered "PACKAGES_TOKEN is not set, so the snapshots themselves were not read. What follows is answered from the publish runs alone."
fi
for line in "${watched[@]}"; do
    read -r repo group artifact <<< "$line"
    head=$(gh api "repos/$repo/commits/main" --jq '[.sha, .commit.committer.date] | @tsv' 2>/dev/null)
    if [ -z "$head" ]; then
        not_answered "$repo: the head of main could not be read."
        continue
    fi
    IFS=$'\t' read -r head_sha head_at <<< "$head"
    version=$(read_pin "$repo" pom.xml project "")
    case "$version" in
        *-SNAPSHOT) ;;
        "")
            not_answered "$repo: the version of pom.xml could not be read."
            continue ;;
        *)
            note "$repo is at $version, which is no snapshot. Question 3 is all there is."
            continue ;;
    esac

    # the run of the publish workflow for exactly this commit
    runs_file="$out/runs-${repo//\//_}"
    publish=""
    [ ! -f "$runs_file" ] || publish=$(jq -c --arg s "$head_sha" \
        '[.[] | select(.name == "Publish to GitHub Packages" and .head_sha == $s)] | .[0] // empty' \
        "$runs_file")
    if [ -z "$publish" ]; then
        found "$repo: the head of main, ${head_sha:0:8} of $head_at, has no publish run. The snapshot $version is older than main."
        continue
    fi
    # '|' and not a tab: a tab is white space to 'read', and an empty conclusion would
    # shift every field after it
    IFS='|' read -r p_status p_conclusion p_created p_url <<< "$(printf '%s' "$publish" \
        | jq -r '[.status, (.conclusion // ""), .created_at, .html_url] | join("|")')"
    if [ "$p_status" != "completed" ]; then
        note "$repo: the publish run of ${head_sha:0:8} is $p_status since $p_created."
        continue
    fi
    if [ "$p_conclusion" != "success" ]; then
        found "$repo: the publish run of ${head_sha:0:8} ended with $p_conclusion, so $version is older than main. $p_url"
        continue
    fi
    if [ -z "${PACKAGES_TOKEN:-}" ]; then
        fine "$repo: ${head_sha:0:8} of $head_at was published by the run of $p_created"
        continue
    fi

    # The snapshot itself. The metadata of the version says when it was last written.
    account=${repo%%/*}
    url="https://maven.pkg.github.com/$repo/${group//.//}/$artifact/$version/maven-metadata.xml"
    metadata=$(curl --silent --fail --max-time 30 \
        --user "${PACKAGES_USER:-token}:$PACKAGES_TOKEN" "$url") || {
        not_answered "$repo: $group:$artifact:$version could not be read from GitHub Packages of $account."
        continue
    }
    stamp=$(printf '%s' "$metadata" | sed -n 's|.*<lastUpdated>\([0-9]\{14\}\)</lastUpdated>.*|\1|p' | tail -1)
    if [ -z "$stamp" ]; then
        not_answered "$repo: the metadata of $group:$artifact:$version names no time."
        continue
    fi
    snapshot_at=$(date -u -d "${stamp:0:8} ${stamp:8:2}:${stamp:10:2}:${stamp:12:2}" +%Y-%m-%dT%H:%M:%SZ)
    if [ "$(epoch "$snapshot_at")" -lt "$(epoch "$p_created")" ]; then
        found "$repo: $group:$artifact:$version was written on $snapshot_at, before the publish run of the head of main started on $p_created."
    else
        fine "$repo: $artifact:$version written on $snapshot_at, the head ${head_sha:0:8} was published by the run of $p_created"
    fi
done

# ---------------------------------------------------------------------------------------
say
if [ "$findings" -gt 0 ]; then
    say "Found $findings thing(s), and $unanswered question(s) were not answered. Asked at $asked_at, done at $(utc_now)."
    exit 1
fi
if [ "$unanswered" -gt 0 ]; then
    say "Nothing found, but $unanswered question(s) were not answered, so this is not green. Asked at $asked_at, done at $(utc_now)."
    exit 2
fi
say "Nothing found. Asked at $asked_at, done at $(utc_now)."
exit 0
