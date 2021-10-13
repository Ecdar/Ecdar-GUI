package ecdar.backend;

import ecdar.Ecdar;
import ecdar.abstractions.QueryState;
import ecdar.utility.protobuf.ProtoBufConverter;
import javafx.util.Pair;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BackendDriver {
    private final Pair<AtomicInteger, AtomicInteger> numberOfReveaalSockets = new Pair<>(new AtomicInteger(0), new AtomicInteger(5));
    private final ArrayList<QuerySocket> reveaalSockets = new ArrayList<>();

    private final Pair<AtomicInteger, AtomicInteger> numberOfJEcdarSockets = new Pair<>(new AtomicInteger(0), new AtomicInteger(5));
    private final ArrayList<QuerySocket> jEcdarSockets = new ArrayList<>();

    private final Queue<ExecutableQuery> waitingQueries = new LinkedList<>();

    private Integer portNum = 5425;

    public BackendDriver() {
        new Thread(() -> {
            while(true) {
                for (QuerySocket socket : reveaalSockets) {
                    try {
                        readFromSocket(socket);
                        System.out.println("Data read");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                for (QuerySocket socket : jEcdarSockets) {
                    try {
                        readFromSocket(socket);
                        System.out.println("Data read");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                ExecutableQuery query = waitingQueries.poll();
                if (query != null) query.execute();
                else System.out.println("Query was null");

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }).start();
    }

    public void addQueryToExecutionQueue(String query, BackendHelper.BackendNames backend, Consumer<Boolean> success, Consumer<BackendException> failure, QueryListener queryListener) {
        waitingQueries.add(new ExecutableQuery(query, backend, success, failure, queryListener));
    }

    private void readFromSocket(QuerySocket socket) throws IOException {
        if (!socket.isRunningQuery()) {
            return;
        }

        String line;
        while ((line = socket.getReader().readLine()) != null) {
            System.out.println(line);

            // Skip the returned query and possible notes, as these should not be shown in the GUI
            if (line.startsWith("note:")) {
                continue;
            }

            // Process the query result
            if ((line.endsWith("true") || line.startsWith("Query: Query")) && (socket.getExecutableQuery().queryListener.getQuery().getQueryState().getStatusCode() <= QueryState.SUCCESSFUL.getStatusCode())) {
                socket.getExecutableQuery().queryListener.getQuery().setQueryState(QueryState.SUCCESSFUL);
                socket.getExecutableQuery().success.accept(true);
            } else if (line.endsWith("result: false")) {
                socket.getExecutableQuery().queryListener.getQuery().setQueryState(QueryState.ERROR);
                socket.getExecutableQuery().success.accept(false);
            } else {
                socket.getExecutableQuery().queryListener.getQuery().setQueryState(QueryState.SYNTAX_ERROR);
                socket.getExecutableQuery().failure.accept(new BackendException.QueryErrorException(line));
            }

            socket.executableQuery = null;
            // ToDo NIELS: Maybe just trigger poll() on query queue here instead of while loop (would require some initial poll technique)
        }
    }

    private void executeQuery(String query, BackendHelper.BackendNames backend, Consumer<Boolean> success, Consumer<BackendException> failure, QueryListener queryListener) {
        if(queryListener.getQuery().getQueryState() == QueryState.UNKNOWN) return;

        // Get available socket or start new
        Optional<QuerySocket> socket;
        if (backend.equals(BackendHelper.BackendNames.jEcdar)) {
            socket = jEcdarSockets.stream().filter((element) -> !element.isRunningQuery()).findFirst();
        } else {
            socket = reveaalSockets.stream().filter((element) -> !element.isRunningQuery()).findFirst();
        }

        QuerySocket querySocket = socket.orElseGet(() -> getNewSocket(backend));

        // If the query socket is null, there are no available sockets
        // and the maximum number of sockets has already been reached
        if (querySocket == null) {
            System.out.println("Query re-queued");
            waitingQueries.add(new ExecutableQuery(query, backend, success, failure, queryListener));
            return;
        }

        try {
            System.out.println("Trying to execute query...");
            querySocket.getWriter().write(ProtoBufConverter.getQueryProtoBuf(query).getQueryBytes().toStringUtf8().toCharArray());

            int inte;
            while ((inte = querySocket.getReader().read()) != -1) {
                System.out.println(inte);

                querySocket.executableQuery.success.accept(true);
            }
        } catch (IOException e) {
            e.printStackTrace();
            failure.accept(new BackendException.BadBackendQueryException(e.getMessage()));
        }
    }

    private QuerySocket getNewSocket(BackendHelper.BackendNames backend) {
        if (backend.equals(BackendHelper.BackendNames.jEcdar)) {
            return startNewQuerySocket(backend, numberOfJEcdarSockets, jEcdarSockets);
        } else {
            return startNewQuerySocket(backend, numberOfReveaalSockets, reveaalSockets);
        } // ToDO NIELS: Refactor
    }

    private QuerySocket startNewQuerySocket(BackendHelper.BackendNames backend, Pair<AtomicInteger, AtomicInteger> numberOfSockets, ArrayList<QuerySocket> querySockets) {
        if (numberOfSockets.getKey().get() < numberOfSockets.getValue().get()) {
            try {
                portNum += 1;

                ProcessBuilder pb;
                if (backend.equals(BackendHelper.BackendNames.jEcdar)) {
                    pb = new ProcessBuilder("java", "-jar", "src/libs/j-Ecdar.jar");
                } else {
                    pb = new ProcessBuilder("src/Reveaal", "-p 127.0.0.1:" + portNum);
                    System.out.println("Reveaal started on port: " + portNum);
                }
                pb.redirectErrorStream(true);
                Process process = pb.start();

                var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                var writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

                Thread.sleep(200);

                System.out.println(process.isAlive());

//                    System.out.println(line);
//
//                    String[] socketInfo = line.trim().split(" ");
//
//                    if (socketInfo.length != 2) continue;

                QuerySocket newSocket = new QuerySocket(new Socket((String) null, portNum, null, 0));

                querySockets.add(newSocket);
                numberOfSockets.getKey().getAndIncrement();

                return newSocket;

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Max number of sockets already reached");
        }

        return null;
    }

    public Pair<ArrayList<String>, ArrayList<String>> getInputOutputs(String query) {
        if (!query.startsWith("refinement")) {
            return null;
        }

        // Pair is used as a tuple, not a key-value pair
        Pair<ArrayList<String>, ArrayList<String>> inputOutputs = new Pair<>(new ArrayList<>(), new ArrayList<>());

        ProcessBuilder pb = new ProcessBuilder("src/Reveaal", "-c", Ecdar.projectDirectory.get(), query.replaceAll("\\s", ""));
        pb.redirectErrorStream(true);
        try {
            //Start the j-Ecdar process
            Process ReveaalEngineInstance = pb.start();

            //Communicate with the j-Ecdar process
            try (
                    var ReveaalReader = new BufferedReader(new InputStreamReader(ReveaalEngineInstance.getInputStream()));
            ) {
                //Read the result of the query from the j-Ecdar process
                String line;
                while ((line = ReveaalReader.readLine()) != null) {
                    // Process the query result
                    if (line.endsWith("extra inputs")){
                        Matcher m = Pattern.compile("[\"]([^\"]+)[\"]").matcher(line);
                        while(m.find()){
                            inputOutputs.getKey().add(m.group(1));
                        }
                    } else if (line.startsWith("extra outputs")) {
                        Matcher m = Pattern.compile("[\"]([^\"]+)[\"]").matcher(line);
                        while(m.find()){
                            inputOutputs.getValue().add(m.group(1));
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return inputOutputs;
    }

    private class ExecutableQuery {
        private final String query;
        private final BackendHelper.BackendNames backend;
        private final Consumer<Boolean> success;
        private final Consumer<BackendException> failure;
        private final QueryListener queryListener;

        ExecutableQuery(String query, BackendHelper.BackendNames backend, Consumer<Boolean> success, Consumer<BackendException> failure, QueryListener queryListener) {
            this.query = query;
            this.backend = backend;
            this.success = success;
            this.failure = failure;
            this.queryListener = queryListener;
        }

        public void execute() {
            executeQuery(this.query, this.backend, this.success, this.failure, this.queryListener);
        }

    }

    private class QuerySocket {
        private final BufferedReader reader;
        private final BufferedWriter writer;
        private final Socket socket;
        private ExecutableQuery executableQuery = null;

        QuerySocket(Socket socket) throws IOException {
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.socket = socket;
        }

        public BufferedReader getReader() {
            return reader;
        }

        public BufferedWriter getWriter() {
            return writer;
        }

        public ExecutableQuery getExecutableQuery() {
            return executableQuery;
        }

        public void setExecutableQuery(ExecutableQuery executableQuery) {
            this.executableQuery = executableQuery;
        }

        public boolean isRunningQuery() {
            return executableQuery == null;
        }

        public void close() throws IOException {
            reader.close();
            writer.close();
            socket.close();

            // Remove the socket from the socket list
            if (jEcdarSockets.remove(this)) {
                numberOfJEcdarSockets.getKey().getAndDecrement();
            } else if(reveaalSockets.remove(this)) {
                numberOfReveaalSockets.getKey().getAndDecrement();
            } else {
                // ToDo NIELS: Somehow the socket was not present in either list
                System.out.println("The socket was not found in the socket lists");
            }
        }

    }

    enum TraceType {
        NONE, SOME, SHORTEST, FASTEST;


        @Override
        public String toString() {
            return "trace " + this.ordinal();
        }
    }
}
