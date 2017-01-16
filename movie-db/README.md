# Movie Database
Manages a database of movies/series not unlike IMDB. Movies can be found, deleted and inserted by their IMDB ids.

### Find a movie
```
$ curl "http://localhost:9000/movies/tt0076759"
```

### Delete a movie
```
$ curl -X DELETE "http://localhost:9000/movies/tt0076759"
```

### Insert a movie
```
$ curl -X PUT "http://localhost:9000/movies/tt0076759"
```

> The insert operation looks up the movie from an internet source and updates/inserts in the local database.

### Insert movies by reading from an Excel file
```
$ curl -X POST -d '<file url>' "http://localhost:9000/movies"
```

> A sample file can be found in the `src/test/resources` directory

## Technologies Used
   * [Akka HTTP](http://doc.akka.io/docs/akka-http/current/scala.html)
   * [Akka Streams](http://doc.akka.io/docs/akka/current/scala.html)
   * [Cats](https://github.com/typelevel/cats)
   * [Apache POI](https://poi.apache.org/)
   * [Reactive Mongo](http://reactivemongo.org/)
   * [ScalaTest](http://www.scalatest.org/)
   * [ScalaMock](http://scalamock.org/)
   * MongoDB
   * Docker

### MongoDB Operations

1. Connect to Mongo shell.
   ```
   asarkar:~$ docker exec -it mongo mongo
   ```
2. List all databases.
   ```
   > show dbs
   ```
3. Switch to `moviedb`.
   ```
   > use moviedb
   ```
4. Show all collections.
   ```
   > show collections
   ```
5. Delete all documents.
   ```
   > db.movie.remove({})
   ```
6. Find all documents.
   ```
   > db.movie.find({})
   ```
7. Find all documents with IMDB rating greater than 7.0.
   ```
   > db.movie.find({imdbRating: { $gt: 7.0 }})
   ```