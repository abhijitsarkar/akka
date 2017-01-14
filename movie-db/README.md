# MongoDB Operations

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