#!/bin/bash
set -e

app=k8s-watcher

deployment=$(kubectl get deployment -l app=$app | head -2 | tail -1 | cut -d " " -f 1)
if [ ! -z "${deployment}" ]; then
    kubectl delete deployment $app --grace-period=0 --force
fi

id=$(docker images --filter=reference='*/'"$app"':*' --format '{{.ID}}')

if [ ! -z $id ]; then
    docker rmi -f $id

    while (($? > 0)); do
        printf "Docker image in use. Going to sleep.\n"
        sleep 2
        status=$(docker rmi -f $id)
    done
fi

sbt clean docker:publishLocal && \
  kubectl create -f k8s/deployment.yaml && \
  kubectl get po -l app=$app