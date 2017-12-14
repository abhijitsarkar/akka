To run one test class

```
$ sbt 'testOnly *.MongoRepositoryActorSpec'
```

**Mongo**

```
> db.[database or collection].drop()
> db['test-journal'].find({}) # hyphenated-name

```

**wget**

```
$ kubectl exec -it <pod> -- wget -q -O- http://localhost:8080/apps/stats

$ kubectl exec -it <pod> -c k8s-watcher -- wget -q -O- http://localhost:8080/apps/stats
```
