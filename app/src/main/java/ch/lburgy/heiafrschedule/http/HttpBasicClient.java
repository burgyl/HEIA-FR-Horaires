package ch.lburgy.heiafrschedule.http;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.lburgy.heiafrschedule.database.ClassHEIAFR;
import ch.lburgy.heiafrschedule.database.Lesson;
import ch.lburgy.heiafrschedule.database.LessonOfClassIncomplete;
import ch.lburgy.heiafrschedule.database.LessonOfRoomIncomplete;
import ch.lburgy.heiafrschedule.database.RoomHEIAFR;
import ch.lburgy.heiafrschedule.database.Teacher;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class HttpBasicClient {

    private static final String URL_CLASSES = "https://webapp.heia-fr.ch/horaire/bloc-horaire.jsp";
    private static final String URL_ROOMS = "https://webapp.heia-fr.ch/horaire/bloc-horaire-par-salle.jsp";
    private static final String URL_SCHEDULE_CLASS = "https://webapp.heia-fr.ch/horaire/bloc-jsp/horaire_par_classe.jsp?filiere=";
    private static final String URL_SCHEDULE_ROOM = "https://webapp.heia-fr.ch/horaire/bloc-jsp/horaire_par_salle.jsp?salle=";
    private static final String URL_TEACHER_INFOS = "https://webapp.heia-fr.ch/horaire/jsp/profParAbreviation.jsp?hefrAcronyme=";

    private OkHttpClient okHttpClient;
    private HttpClient httpClient;
    private final MediaType formDataType;
    private final Context context;

    public HttpBasicClient(Context context) {
        this.context = context;
        formDataType = MediaType.parse("application/x-www-form-urlencoded; charset=utf-8");
    }

    public HttpBasicClient(Context context, String username, String password) {
        this(context);
        initCredentials(username, password);
    }

    public void setCredentials(String username, String password) {
        initCredentials(username, password);
    }

    private void initCredentials(String username, String password) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            okHttpClient = new OkHttpClient.Builder().addInterceptor(new BasicAuthInterceptor(username, password)).build();
        } else {
            CredentialsProvider provider = new BasicCredentialsProvider();
            UsernamePasswordCredentials credentials
                    = new UsernamePasswordCredentials(username, password);
            provider.setCredentials(AuthScope.ANY, credentials);

            httpClient = HttpClientBuilder.create()
                    .setDefaultCredentialsProvider(provider)
                    .build();
        }
    }

    public List<ClassHEIAFR> getClasses() throws HttpException, UnknownHostException, NoInternetConnectionException, InterruptedIOException {
        ArrayList<ClassHEIAFR> classes = new ArrayList<>();
        Document doc = getDoc(URL_CLASSES, true);
        if (doc == null) return null;

        Element select = doc.getElementsByTag("select").first();
        Elements options = select.getElementsByTag("option");
        for (Element option : options) {
            classes.add(new ClassHEIAFR(option.attr("value")));
        }
        return classes;
    }

    public List<LessonOfClassIncomplete> getClassLessons(String classID) throws HttpException, UnknownHostException, NoInternetConnectionException, InterruptedIOException {
        String url = URL_SCHEDULE_CLASS + classID;
        Document doc = getDoc(url, true);
        if (doc == null) return null;
        ArrayList<LessonOfClassIncomplete> lessons = new ArrayList<>();

        Elements raws = doc.getElementsByTag("tr");
        int dayOfTheWeek = -1; // [0,6]

        for (int i = 3; i < raws.size() - 3; i++) {
            Elements cells = raws.get(i).getElementsByTag("td");
            String desc = cells.get(2).getElementsByTag("div").first().text();

            String day = cells.first().getElementsByTag("div").first().text();
            if (!"".equals(day))
                dayOfTheWeek++;

            if (!"".equals(desc)) {
                Lesson lesson = new Lesson();
                lesson.setName(desc);
                lesson.setDayOfTheWeek(dayOfTheWeek);

                Elements hours = cells.get(1).getElementsByTag("div");
                lesson.setTimeStart(regexGetHour(hours.first().text(), true));
                lesson.setTimeEnd(regexGetHour(hours.last().text(), false));

                Elements abbrCells = cells.get(3).child(0).children();
                ArrayList<String> teacherAbbrs = new ArrayList<>();
                for (Element abbrCell : abbrCells)
                    teacherAbbrs.add(abbrCell.getElementsByTag("a").text());

                Elements roomCells = cells.get(4).child(0).children();
                ArrayList<String> rooms = new ArrayList<>();
                for (Element roomCell : roomCells)
                    rooms.add(roomCell.text());

                LessonOfClassIncomplete lessonOfClassIncomplete = new LessonOfClassIncomplete(lesson, rooms, teacherAbbrs);
                lessons.add(lessonOfClassIncomplete);
            }
        }

        return lessons;
    }

    public Teacher getTeacherInfos(String teacherAbbr) throws HttpException, UnknownHostException, NoInternetConnectionException, InterruptedIOException {
        String url = URL_TEACHER_INFOS + teacherAbbr;
        Document doc = getDoc(url, false);
        if (doc == null) return null;

        Element table = doc.body().child(0).child(0).child(0).child(1).child(0);
        Elements raws = table.getElementsByTag("tr");

        String name = raws.first().child(1).text();
        String office = regexFixOffice(raws.get(1).child(1).text());
        String phone = raws.get(2).child(1).text();
        String email = raws.get(3).child(1).text();

        if ("".equals(office))
            office = null;
        if ("".equals(phone))
            phone = null;

        return new Teacher(teacherAbbr, name, email, phone, office);
    }

    public List<RoomHEIAFR> getRooms() throws HttpException, UnknownHostException, NoInternetConnectionException, InterruptedIOException {
        List<RoomHEIAFR> rooms = new ArrayList<>();
        Document doc = getDoc(URL_ROOMS, true);
        if (doc == null) return null;

        Element select = doc.getElementsByTag("select").first();
        Elements options = select.getElementsByTag("option");
        for (Element option : options) {
            rooms.add(new RoomHEIAFR(option.attr("value"), option.text()));
        }
        return rooms;
    }

    public List<LessonOfRoomIncomplete> getRoomLessons(String roomID) throws HttpException, UnknownHostException, NoInternetConnectionException, InterruptedIOException {
        String url = URL_SCHEDULE_ROOM + roomID;
        Document doc = getDoc(url, true);
        if (doc == null) return null;
        ArrayList<LessonOfRoomIncomplete> lessons = new ArrayList<>();

        Element table = doc.getElementsByTag("table").get(1).child(0);
        Element secondTable = table.getElementsByTag("table").first().child(0);
        Elements raws = secondTable.getElementsByTag("tr");
        int dayOfTheWeek = -1;

        for (int i = 1; i < raws.size(); i++) {
            Elements cells = raws.get(i).getElementsByTag("td");

            String dayString = cells.first().getElementsByTag("div").first().text();
            switch (dayString) {
                case "Lundi":
                    dayOfTheWeek = 0;
                    break;
                case "Mardi":
                    dayOfTheWeek = 1;
                    break;
                case "Mercredi":
                    dayOfTheWeek = 2;
                    break;
                case "Jeudi":
                    dayOfTheWeek = 3;
                    break;
                case "Vendredi":
                    dayOfTheWeek = 4;
                    break;
            }

            Elements hours = cells.get(1).getElementsByTag("div").first().getElementsByTag("div");
            String timeStart = regexGetHour(hours.first().text(), true);
            String timeEnd = regexGetHour(hours.last().text(), false);
            String name = cells.get(2).getElementsByTag("div").first().getElementsByTag("div").first().text();

            Elements classCells = cells.get(3).child(0).children();
            ArrayList<String> classes = new ArrayList<>();
            for (Element classCell : classCells)
                classes.add(classCell.getElementsByTag("a").text());

            Elements abbrCells = cells.get(4).child(0).children();
            ArrayList<String> teacherAbbrs = new ArrayList<>();
            for (Element abbrCell : abbrCells)
                teacherAbbrs.add(abbrCell.getElementsByTag("a").text());

            Lesson lesson = new Lesson(name, timeStart, timeEnd, dayOfTheWeek);
            LessonOfRoomIncomplete lessonOfRoomIncomplete = new LessonOfRoomIncomplete(lesson, classes, teacherAbbrs);
            lessons.add(lessonOfRoomIncomplete);
        }

        return lessons;
    }

    private String regexGetHour(String input, Boolean first) {
        Pattern pattern = Pattern.compile("[0-9]{2}:[0-9]{2}");
        Matcher matcher = pattern.matcher(input);

        if (first)
            if (matcher.find())
                return matcher.group();
            else
                return null;
        else {
            String lastFind = "";
            while (matcher.find()) {
                lastFind = matcher.group();
            }
            return lastFind;
        }
    }

    private String regexFixOffice(String office) {
        return office.replaceAll("([A-H][0-9]{2})\\.([0-9]{2})", "$1$2");
    }

    private Document getDoc(String url, boolean post) throws HttpException, UnknownHostException, NoInternetConnectionException, InterruptedIOException {
        if (!isConnectedToInternet()) throw new NoInternetConnectionException();
        String result = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Request request;
            if (post) {
                RequestBody body = RequestBody.create("", formDataType);
                request = new Request.Builder().url(url).post(body).build();
            } else {
                request = new Request.Builder().url(url).build();
            }

            Response response = null;
            try {
                response = okHttpClient.newCall(request).execute();
            } catch (IOException e) {
                if (e.getClass() == UnknownHostException.class)
                    throw (UnknownHostException) e;
                else if (e.getClass() == InterruptedIOException.class)
                    throw (InterruptedIOException) e;
                e.printStackTrace();
            }

            if (!response.isSuccessful()) throw new HttpException(response.code());

            ResponseBody responseBody = response.body();
            if (responseBody == null) return null;

            try {
                result = responseBody.string();
            } catch (IOException e) {
                return null;
            }
        } else {
            HttpUriRequest request;
            if (post) {
                request = new HttpPost(url);
            } else {
                request = new HttpGet(url);
            }

            HttpResponse response = null;
            try {
                response = httpClient.execute(request);
            } catch (IOException e) {
                if (e.getClass() == UnknownHostException.class)
                    throw (UnknownHostException) e;
                e.printStackTrace();
            }

            int httpCode = response.getStatusLine().getStatusCode();
            if (httpCode != 200) throw new HttpException(httpCode);

            HttpEntity entity = response.getEntity();
            if (entity != null) {
                try {
                    result = EntityUtils.toString(entity);
                } catch (IOException e) {
                    return null;
                }
            }
        }
        if (result != null)
            return Jsoup.parse(result);
        else
            return null;
    }

    public boolean isConnectedToInternet() {
        ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            for (NetworkInfo networkInfo : info)
                if (networkInfo.getState() == NetworkInfo.State.CONNECTED) {
                    return true;
                }
        }
        return false;
    }

    public static class NoInternetConnectionException extends Exception {
    }

    public static class HttpException extends Exception {
        private final int code;

        public HttpException(int code) {
            super("HTTP Exception " + code);
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }
}
