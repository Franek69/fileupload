import Mailer.Mailer;
import mongodb.MongoDBInstance;
import org.bson.types.ObjectId;
import spark.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
//import javax.mail.*;
//import javax.mail.internet.*;
//import javax.activation.*;

import static spark.Spark.*;
import static spark.debug.DebugScreen.enableDebugScreen;

public class Main {

    MongoDBInstance mongoDBInstance = new MongoDBInstance();

    public static void main(String[] args) {

        enableDebugScreen();

        // Erzeugt eine neue Datei-Instanz, indem die angegebene Pfadnamens-Zeichenkette in einen abstrakten Pfadnamen umgewandelt wird
        File uploadDir = new File("public/upload");
        File downloadDir = new File("public/download");

//  Pfad für Datei(zwischen)speicher

        uploadDir.mkdir(); // erstellt einen "upload"-Ordner falls dieser noch nicht existiert
        downloadDir.mkdir(); // erstellt einen "download"-Ordner falls dieser noch nicht existiert
        staticFiles.externalLocation("public"); // Legt den externen Ordner für statische Dateien fest


        post("/StartUpload", (req, res) -> {

            // Multipart-Konfiguration für die Formulardaten:
            req.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));

            // Kontrollausgabe für die Konsole, mit Session-ID für die Verlaufskontrolle
            System.out.println("Post /StartUpload - die Session mit der ID : "+ req.session().id()+ " wurde gestartet");

            // Festlegen der SessionAttribute aus den form-values
            req.session().attribute("nachname",req.queryParams("nachname"));
            req.session().attribute("vorname",req.queryParams("vorname"));
            req.session().attribute("id",req.queryParams("id"));

            // Bereitstellen der aktualisierten UploadWebSeite mit Aufruf der Methode
            return htmlToString("html/fileSelectionUpload.html");
                }
        );


        post("/UploadMongoDB", (req, res) -> {

            req.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));

            System.out.println("Post /UploadMongoDB gestartet - die Session-ID lautet: "+ req.session().id());

            System.out.println("Alle Attribute der Session: " + req.session().attributes());

            String nachname = ((String) req.session().attribute("nachname"));
            String vorname = ((String) req.session().attribute("vorname"));
            String id = ((String) req.session().attribute("id"));

            System.out.println("Nachname: " + nachname);
            System.out.println("Vorname: " + vorname);
            System.out.println("Id: " + id);

            // Zeitstempel wird erzeugt
            String dateTime = new SimpleDateFormat("yyyy-MM-dd-HH-mm").format(new Date());

            // Präfix wird aus empfangener Datei und Zeitstempel generiert
            String pref = req.raw().getPart("uploaded_file").getSubmittedFileName().split("\\.")[0] +  "_"
                    + dateTime + "_";

            // Suffix wird aus empfangener Datei gebildet
            String suf ;

            if (req.raw().getPart("uploaded_file").getSubmittedFileName().split("\\.").length >= 2)
            {
                suf = "." +req.raw().getPart("uploaded_file").getSubmittedFileName().split("\\.")[1];
            }
            else { suf =" ";}

            // temp lokale Datei zur Erstellung des Inputstreams
            Path tempFile = Files.createTempFile (uploadDir.toPath(),pref,suf);
            String fileName = (tempFile.toString()).substring((tempFile.toString()).indexOf('\\')+1);;
            String fileNameMail = fileName.substring(fileName.indexOf('\\')+1);
            String fileNameUpload = getFileName(req.raw().getPart("uploaded_file"));

// TODO: Dateinamen MIT Zeitstempel auf MDB ablegen

            System.out.println("tempfile: "+tempFile);
            System.out.println("fileNameUpload: "+ fileNameUpload);
            System.out.println("fileNameMail: "+ fileNameMail);

            // Dateizwischenablage

            try (InputStream input = req.raw().getPart("uploaded_file").getInputStream()) { // "name" muss identisch sein mit input Feld in form !
                Files.copy(input, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }

            // Übergabe des Dateipfades, Dateinamen an MongoDB, Empfang der MDB-ObjejktId

            ObjectId fileObjektId = null;

            fileObjektId = MongoDBInstance.init(tempFile,fileNameUpload);

// Email
            // neuer Dateiname der zum Download bereitgestellten Datei
            String fileNew = pref +suf;
            // Erstellt String mit url zum Download für Email
            String urlUpload = fileObjektId.toString();

            System.out.println("Objekt ID für Download-Link: " + urlUpload);
            // Email Betreffzeile
            String sub = "Datei-Upload, Teilnehmer: " + nachname + ", " + vorname + "  ID: " + id + "";
            // Inhalt der Email
            String content =

           "<!DOCTYPE html>" +
            "<html lang=de dir=\"ltr\">"+
            "<head>"+
            "<meta charset=\"utf-8\">" +
            "<title>Downloadstart</title>" +
            "<script src=\"https://code.jquery.com/jquery-3.1.1.js\"></script>" +
                   "<style>" +
                   "body {background-color: powderblue; font-family:JetBrains Mono;}" +
                   "h1   {color: blue;}" +
                   "p    {color: black;}" +
                   "</style>" +
            "</head>" +
            "<body>" +
            "<div class=\"container\">" +
            "<h1>Vom webserver generierte Email fuer den Dateidownload</h1>" +
                    "<div> Teilnehmer "+ nachname + ", " + vorname + "  ID: "+ id +", hat folgende Datei hochgeladen: </div>"+
                    "<br>" +
                    "<div><b>" + fileNew + "</b></div>"+
                    "<br>" +
                    "<div> Die Datei steht <a href=\"http://127.0.0.1:4567/DownloadMongoDB/" + urlUpload + "\"><b>hier</b></a> zum download bereit. </div>"+
                    "<br>" +
                    "<div> Dies ist eine automatisch generierte Nachricht, bitte antworten Sie nicht an diesen Absender.</div>" +
                    "<br>" +
                    "<div> Mit freundlichen Gruessen </div>"+
            "</div>" +
            "</body>" +
            "</html>";

            // Aufruf der Methode zum Versenden der Email, Übergabe der Paramter

            Mailer.send("burkertsfrank@gmail.com","/ndS1953","frank_burkert@live.de",sub,content);

            System.out.println("ObjektId der Uploaddatei (return von MongoDBInstance.init()) "+ fileObjektId);


            logInfo(req, tempFile);

            return  "<style>" +
                    "body {background-color: powderblue; font-family:JetBrains Mono;}" +
                    "h1   {color: blue;}" +
                    "p    {color: black;}" +
                    "</style>" +
                    "<h1>Bestätigung für den erfolgreichen Upload<h1>" +
                    "<h2>Sie haben folgende Datei erfolgreich hochgeladen:<h2>" +
                    "<h2>"+ fileNameUpload +"<h2>" +
                    //"<h1><img src='" + tempFile.getFileName() + "'>" +
                    "<embed src=\"upload/" + tempFile.getFileName() + "\" width=\" 100%\" height= \"100%\" />"
                    ;
        });

// Download start
        post("/startDownload", (req, res) -> {

                    // Multipart-Konfiguration für die Formulardaten:
                    req.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));

                    System.out.println("post /startDownload - Session ID : "+ req.session().id());

                    // Festlegen der SessionAttribute aus den form-values
                     req.session().attribute("behoerde",req.queryParams("behoerde"));
                     req.session().attribute("password",req.queryParams("password"));

                    System.out.println("Alle Attribute der Session: " + req.session().attributes());

                    String behoerde = ((String) req.session().attribute("behoerde"));
                    String password = ((String) req.session().attribute("password"));
                    String objID = ((String) req.session().attribute("objID"));

                    System.out.println("Behoerde: " + behoerde);
                    System.out.println("Passwort: " + password);
                    System.out.println("ObjectId: " + objID);

                    Object filename = MongoDBInstance.down(objID);

                    // Bereitstellen der aktualisierten Downloadseite
                    return "<style>" +
                            "body {background-color: powderblue; font-family:JetBrains Mono;}" +
                            "h1   {color: blue;}" +
                            "p    {color: black;}" +
                            "</style>" +
                            "<h1>Status: Empfänger (Behörde) hat sich identifiziert</h1>" +
                            "<br>" +
                            "<a href=\"http://127.0.0.1:4567/download/" +filename+ "\" download>" +
                            "<img src=\" \" alt=\"Download\">" +
                            "</a>";
        }
        );


 // Download von MongoDB
        get("/DownloadMongoDB/:id", (req, res) -> {

            req.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));
            Object mailObjId = req.params(":id");

            System.out.println("Objekt-Id aus Email übergeben: " + mailObjId);

            System.out.println("post /DownloadMongoDB - Session ID : "+ req.session().id());
// !!!! value übergeben
            req.session().attribute("objID",mailObjId.toString());


            return htmlToString("html/startDownload.html");

                }
        );
    }

    // Methode zur Umwandlung einer Html-Seite in einen String
    // append()-Methode: hiermit können Inhalte an das Ende einer bestehenden Datei anhängt werden
    private static Object htmlToString(String s) {

        StringBuilder contentBuilder = new StringBuilder();
        try {
            BufferedReader in = new BufferedReader(new FileReader(s));
            String str;
            while ((str = in.readLine()) != null) {
                contentBuilder.append(str);
            }
            in.close();
        } catch (
                IOException e) {
        }
        String content = contentBuilder.toString();
        return content;
    }

    // Methode zur Protokollierung in der Konsole, Name der hochgeladenen Datei, Speicherort, absoluter Dateiname

    private static void logInfo(Request req, Path tempFile) throws IOException, ServletException {
        System.out.println("Uploaded file '" + getFileName(req.raw().getPart("uploaded_file")) + "' saved as '" + tempFile + "'");
    }  // .toAbsolutePath()

    // getHeader() -- Gibt den Wert des angegebenen Headers ( hier "content-disposition") als Zeichenfolge zurück.
    //                zBsp: Content-Disposition: attachment; filename=fname.ext
    // trim() --      Gibt eine Zeichenkette zurück, deren Wert diese Zeichenkette ist, wobei alle führenden und abschließenden Leerzeichen entfernt sind.
    // substring() -- Gibt eine Zeichenfolge zurück, die eine Teilzeichenfolge dieser Zeichenfolge ist.
    //                Die Teilzeichenkette beginnt mit dem Zeichen am angegebenen Index und erstreckt sich
    //                bis zum Ende dieser Zeichenkette.

    private static String getFileName(Part part) {
        for (String cd : part.getHeader("content-disposition").split(";")) {
            if (cd.trim().startsWith("filename")) {
                return cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");  //?? replace wofür
            }
        }
        return null;
    }

}