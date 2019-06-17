import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public class Timer {
    private String getRequestResult = null;

    private long timeTo = Calendar.getInstance(TimeZone.getTimeZone("GMT-0")).getTime().getTime()/1000;
    private long timeFrom = timeTo - 60;
//    private long timeFrom = timeTo - 3600;
    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
    Instant ins;

    public long getTimeFrom() {
        return timeFrom;
    }

    public long getTimeTo() {
        return timeTo;
    }

    public void setTimeFrom(long timeFrom) {
        this.timeFrom = timeFrom;
    }

    public void setTimeTo(long timeTo) {
        this.timeTo = timeTo;
    }

    public void start(){
        while (true){
            //пишем за какой период времени считывают данные
            ins = Instant.ofEpochSecond(getTimeFrom() + 10800);
            System.out.println("\n" + getTimeFrom() + " " + getTimeTo() + " " + ins);
            //создаем файл для записи ошибок туда
            ExceptionLogger exception = new ExceptionLogger();
            exception.create();
            Connect connector = new Connect();

            GetRequests get = new GetRequests();
            //ломимся в апи вк трекера и берем данные за период
            try {
                getRequestResult = get.getAPIRequest("http://new.welcome-tracker.ru/api.php?api=71e5367021e4c6cf091f34434e5e9458&from="
                        + getTimeFrom() +  "&to=" + getTimeTo());
                System.out.println(getRequestResult);
//                getRequestResult = get.getAPIRequest("http://new.welcome-tracker.ru/api.php?api=71e5367021e4c6cf091f34434e5e9458&from=1544508000"
//                                +  "&to=1544518800");
                //пишем все ошибки в файл, если поймали ошибку, увеличиваем период запроса
            } catch (IOException e) {
                exception.write(e.toString());
                try {
                    waitToUptime();
                } catch (InterruptedException e1) {
                    exception.write(e1.toString());
                }
                increaseTimeForRequest();
                start();
            }

            if (getRequestResult.length() < 2){
                try {
                    waitToUptime();
                } catch (InterruptedException e) {
                    exception.write(e.toString());
                }
                increaseTime();
//                decreaseTime();
                try {
                    waitToUptime();
                } catch (InterruptedException e) {
                    exception.write(e.toString());
                }
                start();
            }
            //увеличиваем время на следующую минуту
            setTimeFrom(getTimeTo());
            setTimeTo(Calendar.getInstance(TimeZone.getTimeZone("GMT-0")).getTime().getTime()/1000);

            //фильтруем строку и запихиваем ее в список для обработки потоками
            get.requestResult(getRequestResult);
            try {
                get.setDbConnection(connector.connectDB());
            } catch (SQLException e) {
                exception.write(e.toString());
            }

            //потоками создаем отдельных клиентов из списка для обработки
            List<Thread> threads = new ArrayList<>();
            int threadCount = get.numberOfClients();
            for (int i = 0; i < threadCount; i++){
                threads.add(new Thread(new ThreadParser(get)));
            }

            for (int i = 0; i < threadCount; i++){
                threads.get(i).start();
            }

            for (int i = 0; i < threadCount; i++) {
                try {
                    threads.get(i).join();
                } catch (InterruptedException e) {
                    exception.write(e.toString());
                }
            }

            //пушим созданых клиентов в БД и удаляем их из листа клиентов
            try {
                get.pushToDB();
            } catch (SQLException e) {
                exception.write(e.toString());
            }

            SetRequests set = null;
            try {
                set = new SetRequests(connector.connectDB());
                set.createJSON();
                set.sendClient();
//                set.getClientId();
//                set.createJsonToDeal();
//                set.createDeal();
            } catch (SQLException | IOException e) {
                exception.write(e.toString());
            }

//            setTimeTo(getTimeTo() - 20);
//            setTimeFrom(getTimeFrom() - 20);

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                exception.write(e.toString());
            }
        }
    }

    private void waitToUptime() throws InterruptedException {
        Thread.sleep(30000);
    }

    private void increaseTime(){
        setTimeFrom(getTimeTo());
        setTimeTo(getTimeTo() + 60);
    }

    private void decreaseTime(){
        //уменшение на час, тест
//        setTimeTo(getTimeTo() - 3600);
//        setTimeFrom(getTimeFrom() - 3600);
        //уменьшение на день, тест
        setTimeTo(getTimeTo() - 7200);
        setTimeFrom(getTimeFrom() - 7200);
    }

    private void increaseTimeForRequest(){
        setTimeTo(getTimeTo() + 60);
        //уменшение на час, тест
//        setTimeFrom(getTimeFrom() - 3600);
        //уменшение на день, тест
//        setTimeTo(getTimeFrom() - 86400);
    }
}
