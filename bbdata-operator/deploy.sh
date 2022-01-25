#!/bin/bash

k3d cluster delete myCluster
k3d cluster create myCluster -p "8080-8088:30080-30088@agent:0" -a 2

kubectl apply -f bbdata-operator.yml
sleep 10
kubectl apply -f bbdata-deploy.yml
kubectl -n bbdata-operator logs -f po/bbdata-operator