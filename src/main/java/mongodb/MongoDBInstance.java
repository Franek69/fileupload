package mongodb;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.client.FindIterable;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSFile;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.io.*;
import java.nio.file.Path;

public class MongoDBInstance {
    private static String DATABASE = "uploadserver";

    public static ObjectId init(Path filepath, String filename) {
        // Verbindung zum lokalen MongoDB-Server wird hergestellt
        MongoClient mongoClient = new MongoClient("localhost", 27017);
        // Verbindung mit der Datenbank namens "uploadserver"
        MongoDatabase database = mongoClient.getDatabase(DATABASE);
// GritFS
        // Erstellt einen gridFSBucket(Sammelbehälter) mit einem benutzerdefinierten Bucket-Namen "uploadfiles".
        GridFSBucket gridFSFilesBucket = GridFSBuckets.create(database, "uploadfiles");
        ObjectId fileId = null;
//Upload
        //!! Neue Datei erstellen aus Inputstream,....
        try {
            InputStream streamToUploadFrom = new FileInputStream(new File(""+filepath+""));
            // Parameter für die Dateispeicherung vergeben
            GridFSUploadOptions options = new GridFSUploadOptions()
                    .chunkSizeBytes(358400)
                    .metadata(new Document("type", "TestUploadFile" ));

            fileId = gridFSFilesBucket.uploadFromStream(filename,streamToUploadFrom,options);
            System.out.println("Uploaded file id : " +fileId);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
        }
        //return String.valueOf(fileId);
        return fileId;
    }

    public static Object down(String downObjectId) {

        ObjectId mongoObjId = new ObjectId(downObjectId);

        MongoClient mongoClient = new MongoClient("localhost", 27017);
        MongoDatabase database = mongoClient.getDatabase(DATABASE);
        GridFSBucket gridFSFilesBucket = GridFSBuckets.create(database, "uploadfiles");

        // Dateinamen aus ObjectId ermitteln
        MongoCollection<Document> collection = database.getCollection("uploadfiles.files");
        System.out.println("Anzahl der Dokumente in 'uploadfiles.files': " + collection.countDocuments());

        //Abfrage der Objekt-Daten mittels übergebener Object-Id:
        BasicDBObject searchQuery = new BasicDBObject();
        searchQuery.put("_id",mongoObjId);
        Document myDoc = collection.find(searchQuery).first();
        // Ausgabe im JSON-Format
        System.out.println("Datensatz des Downloadobjektes im JSON-Format: "+ myDoc.toJson());
        // Auslesen des Dateinamens und speichern als String
        String filename = myDoc.get("filename").toString();
        System.out.println("'filename' aus dem JsonObjekt: "+ filename);

        // Download per OutputStream und übergebener Objekt Id
        try {
            FileOutputStream streamToDownloadTo = new FileOutputStream("public/download/" + filename);
            gridFSFilesBucket.downloadToStream(mongoObjId, streamToDownloadTo);
            streamToDownloadTo.close();
            // System.out.println(streamToDownloadTo.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return filename;
    }

    public void createFile(String filename,long size, String owner) {

    }
}
