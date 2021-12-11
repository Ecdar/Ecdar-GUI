package ecdar.backend;

import EcdarProtoBuf.ComponentProtos;
import EcdarProtoBuf.EcdarBackendGrpc;
import EcdarProtoBuf.QueryProtos;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.protobuf.Empty;
import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.abstractions.QueryState;
import ecdar.controllers.EcdarController;
import ecdar.utility.UndoRedoStack;
import io.grpc.*;
import io.grpc.stub.StreamObserver;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import org.springframework.util.SocketUtils;

import java.io.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class BackendDriver {
    private int maxNumberOfBackendConnections = 5;
    private final List<BackendConnection> reveaalConnections = new CopyOnWriteArrayList<>();
    private final List<BackendConnection> jEcdarConnections = new CopyOnWriteArrayList<>();

    private final String hostAddress;

    private final int deadlineForResponses = 20000;
    private final int rerunQueryDelay = 200;

    public BackendDriver(String hostAddress) {
        this.hostAddress = hostAddress;
    }

    /**
     * Add the query to execution queue with consumers for success and failure, executed when response received from backends
     *
     * @param query         the query to be executed
     * @param backend       the backend to execute the query on
     * @param success       consumer for a successful response
     * @param failure       consumer for a failure response
     * @param queryListener query listener for referencing the query for GUI purposes
     */
    public void addQueryToExecutionQueue(String query, BackendHelper.BackendNames backend, Consumer<Boolean> success, Consumer<BackendException> failure, QueryListener queryListener) {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                new ExecutableQuery(query, backend, success, failure, queryListener).execute();
            }
        }, rerunQueryDelay);
    }

    /**
     * Asynchronous method for fetching inputs and outputs for the given refinement query and adding these to the
     * ignored inputs and outputs pane for the given query
     *
     * @param query the ignored input output query containing the query and related GUI elements
     */
    public void getInputOutputs(IgnoredInputOutputQuery query) {
        ExecutableQuery executableQuery = new ExecutableQuery(query.getQuery().getQuery(), BackendHelper.BackendNames.Reveaal,
                a -> {

                },
                e -> {

                },
                new QueryListener(query.getQuery()));

        // Get available connection or start new (only Reveaal supports ignored inputs/outputs)
        final Optional<BackendConnection> connection;
        connection = reveaalConnections.stream().filter((element) -> !element.isRunningQuery()).findFirst();
        final BackendConnection backendConnection = connection.orElseGet(() ->
                (startNewBackendConnection(BackendHelper.BackendNames.Reveaal)));
        // If the query connection is null, there are no available sockets
        // and the maximum number of sockets has already been reached
        if (backendConnection == null) {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    getInputOutputs(query);
                }
            }, rerunQueryDelay);
            return;
        }

        backendConnection.setExecutableQuery(executableQuery);

        QueryProtos.ComponentsUpdateRequest.Builder componentsBuilder = QueryProtos.ComponentsUpdateRequest.newBuilder();
        for (Component c : Ecdar.getProject().getComponents()) {
            componentsBuilder.addComponents(ComponentProtos.Component.newBuilder().setJson(c.serialize().toString()).build());
        }

        StreamObserver<Empty> observer = new StreamObserver<>() {
            private boolean error = false;

            @Override
            public void onNext(Empty value) {
            }

            @Override
            public void onError(Throwable t) {
                error = true;
            }

            @Override
            public void onCompleted() {
                if (!error) {
                    StreamObserver<EcdarProtoBuf.QueryProtos.QueryResponse> responseObserver = new StreamObserver<>() {
                        @Override
                        public void onNext(QueryProtos.QueryResponse value) {
                            if (value.hasQuery() && value.getQuery().hasIgnoredInputOutputs()) {
                                System.out.println(value.getQuery().getIgnoredInputOutputs());
                            } else {
                                System.out.println("Not the desired output: " + value);
                            }
                        }

                        @Override
                        public void onError(Throwable t) {
                        }

                        @Override
                        public void onCompleted() {
                            backendConnection.setExecutableQuery(null);
                        }
                    };
                    backendConnection.getStub().withDeadlineAfter(deadlineForResponses, TimeUnit.MILLISECONDS).sendQuery(QueryProtos.Query.newBuilder().setId(0).setQuery(query.getQuery().getQuery()).setIgnoredInputOutputs(QueryProtos.IgnoredInputOutputs.newBuilder().getDefaultInstanceForType()).build(), responseObserver);
                }
            }
        };

        backendConnection.getStub().withDeadlineAfter(deadlineForResponses, TimeUnit.MILLISECONDS).updateComponents(componentsBuilder.build(), observer);
    }

    /**
     * Close every open connection for all backends
     *
     * @throws IOException originally thrown by related process when it is destroyed
     */
    public void closeAllBackendConnections() throws IOException {
        for (BackendConnection s : reveaalConnections) s.close();
        for (BackendConnection s : jEcdarConnections) s.close();
    }

    public int getMaxNumberOfSockets() {
        return maxNumberOfBackendConnections;
    }

    /**
     * Change  the number of backend connection currently running for each backend.
     *
     * This will close connections if there are currently more connection open than the desired number
     * @param i the new maximum for open connections
     */
    public void setMaxNumberOfConnections(int i) {
        maxNumberOfBackendConnections = i;

        closeAdditionalConnections(reveaalConnections);
        closeAdditionalConnections(jEcdarConnections);
    }

    private void closeAdditionalConnections(List<BackendConnection> connections) {
        while (connections.size() > maxNumberOfBackendConnections) {
            BackendConnection unoccupiedConnection = connections.stream().filter(backendConnection -> backendConnection.executableQuery == null).findFirst().orElse(null);
            if (unoccupiedConnection != null) {
                try {
                    unoccupiedConnection.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    connections.remove(unoccupiedConnection);
                }
            }
        }
    }

    private void executeQuery(ExecutableQuery executableQuery) {
        if (executableQuery.queryListener.getQuery().getQueryState() == QueryState.UNKNOWN) return;

        // Get available connection or start new
        final Optional<BackendConnection> connection;
        if (executableQuery.backend.equals(BackendHelper.BackendNames.jEcdar)) {
            connection = jEcdarConnections.stream().filter((element) -> !element.isRunningQuery()).findFirst();
        } else {
            connection = reveaalConnections.stream().filter((element) -> !element.isRunningQuery()).findFirst();
        }

        final BackendConnection backendConnection = connection.orElseGet(() -> executableQuery.backend.equals(BackendHelper.BackendNames.Reveaal) ?
                (startNewBackendConnection(BackendHelper.BackendNames.Reveaal)) : startNewBackendConnection(BackendHelper.BackendNames.jEcdar));

        // If the query connection is null, there are no available sockets
        // and the maximum number of sockets has already been reached
        if (backendConnection == null) {
            if (executableQuery.tries < 5) {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        executableQuery.execute();
                    }
                }, rerunQueryDelay);
            } else {
                executableQuery.failure.accept(new BackendException("Failed to execute query after five tries"));
                executableQuery.queryListener.getQuery().setQueryState(QueryState.ERROR);
            }
            return;
        }

        backendConnection.setExecutableQuery(executableQuery);

        QueryProtos.ComponentsUpdateRequest.Builder componentsBuilder = QueryProtos.ComponentsUpdateRequest.newBuilder();
        for (Component c : Ecdar.getProject().getComponents()) {
            componentsBuilder.addComponents(ComponentProtos.Component.newBuilder().setJson(c.serialize().toString()).build());
        }

        StreamObserver<Empty> observer = new StreamObserver<>() {
            private boolean error = false;

            @Override
            public void onNext(Empty value) {
            }

            @Override
            public void onError(Throwable t) {
                // Check if query has been canceled
                if (executableQuery.queryListener.getQuery().getQueryState() != QueryState.UNKNOWN) {
                    handleBackendError(t, backendConnection);
                    error = true;
                    backendConnection.setExecutableQuery(null);
                }
            }

            @Override
            public void onCompleted() {
                if (!error) {
                    StreamObserver<EcdarProtoBuf.QueryProtos.QueryResponse> responseObserver = new StreamObserver<>() {
                        @Override
                        public void onNext(QueryProtos.QueryResponse value) {
                            if (executableQuery.queryListener.getQuery().getQueryState() != QueryState.UNKNOWN) {
                                handleResponse(backendConnection.getExecutableQuery(), value);
                            }
                            backendConnection.setExecutableQuery(null);
                        }

                        @Override
                        public void onError(Throwable t) {
                            if (executableQuery.queryListener.getQuery().getQueryState() != QueryState.UNKNOWN) {
                                handleBackendError(t, backendConnection);
                            }
                            backendConnection.setExecutableQuery(null);
                        }

                        @Override
                        public void onCompleted() {
                        }
                    };
                    backendConnection.getStub().withDeadlineAfter(deadlineForResponses, TimeUnit.MILLISECONDS).sendQuery(QueryProtos.Query.newBuilder().setId(0).setQuery(backendConnection.getExecutableQuery().query).build(), responseObserver);
                }
            }
        };

        backendConnection.getStub().withDeadlineAfter(deadlineForResponses, TimeUnit.MILLISECONDS).updateComponents(componentsBuilder.build(), observer);
    }

    private void handleBackendError(Throwable t, BackendConnection backendConnection) {
        // Each error starts with a capitalized description of the error equal to the gRPC error type encountered
        String errorType = t.getMessage().split(":\\s+", 2)[0];
        final ExecutableQuery query = backendConnection.getExecutableQuery();

        switch (errorType) {
            case "CANCELLED":
                backendConnection.getExecutableQuery().queryListener.getQuery().setQueryState(QueryState.ERROR);
                backendConnection.getExecutableQuery().failure.accept(new BackendException.QueryErrorException("The query was cancelled"));
                break;
            case "DEADLINE_EXCEEDED":
                backendConnection.getExecutableQuery().queryListener.getQuery().setQueryState(QueryState.ERROR);
                backendConnection.getExecutableQuery().failure.accept(new BackendException.QueryErrorException("The backend did not answer the request in time"));

                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        query.execute();
                    }
                }, rerunQueryDelay);

                break;
            case "UNIMPLEMENTED":
                backendConnection.getExecutableQuery().queryListener.getQuery().setQueryState(QueryState.SYNTAX_ERROR);
                backendConnection.getExecutableQuery().failure.accept(new BackendException.QueryErrorException("The query type is not supported by the backend"));
                break;
            case "INTERNAL":
                backendConnection.getExecutableQuery().queryListener.getQuery().setQueryState(QueryState.ERROR);
                backendConnection.getExecutableQuery().failure.accept(new BackendException.QueryErrorException("Reveaal was unable to execute this query:\n" + t.getMessage().split(": ", 2)[1]));
                break;
            case "UNKNOWN":
                backendConnection.getExecutableQuery().queryListener.getQuery().setQueryState(QueryState.ERROR);
                backendConnection.getExecutableQuery().failure.accept(new BackendException.QueryErrorException("The backend encountered an unknown error"));
                break;
            default:
                backendConnection.getExecutableQuery().queryListener.getQuery().setQueryState(QueryState.ERROR);
                backendConnection.getExecutableQuery().failure.accept(new BackendException.QueryErrorException("The query failed and gave the following error: " + errorType));

//                try {
//                    backendConnection.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }

                break;
        }

        backendConnection.setExecutableQuery(null);
    }

    private void handleResponse(ExecutableQuery executableQuery, QueryProtos.QueryResponse value) {
        if (value.hasRefinement() && value.getRefinement().getSuccess()) {
            executableQuery.queryListener.getQuery().setQueryState(QueryState.SUCCESSFUL);
            executableQuery.success.accept(true);
        } else if (value.hasConsistency() && value.getConsistency().getSuccess()) {
            System.out.println("Consistency");
            executableQuery.queryListener.getQuery().setQueryState(QueryState.SUCCESSFUL);
            executableQuery.success.accept(true);
        } else if (value.hasDeterminism() && value.getDeterminism().getSuccess()) {
            System.out.println("Determinism");
            executableQuery.queryListener.getQuery().setQueryState(QueryState.SUCCESSFUL);
            executableQuery.success.accept(true);
        } else if (value.hasComponent()) {
            System.out.println("Component");
            executableQuery.queryListener.getQuery().setQueryState(QueryState.SUCCESSFUL);
            executableQuery.success.accept(true);
            
            JsonObject returnedComponent = (JsonObject) JsonParser.parseString("{\n" +
                    "  \"name\": \"Administration\",\n" +
                    "  \"declarations\": \"clock z;\",\n" +
                    "  \"locations\": [\n" +
                    "    {\n" +
                    "      \"id\": \"L0\",\n" +
                    "      \"nickname\": \"\",\n" +
                    "      \"invariant\": \"\",\n" +
                    "      \"type\": \"INITIAL\",\n" +
                    "      \"urgency\": \"NORMAL\",\n" +
                    "      \"x\": 130.0,\n" +
                    "      \"y\": 100.0,\n" +
                    "      \"nicknameX\": 30.0,\n" +
                    "      \"nicknameY\": -10.0,\n" +
                    "      \"invariantX\": 30.0,\n" +
                    "      \"invariantY\": 10.0\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"id\": \"L1\",\n" +
                    "      \"nickname\": \"\",\n" +
                    "      \"invariant\": \"z \\u003c\\u003d 2\",\n" +
                    "      \"type\": \"NORMAL\",\n" +
                    "      \"urgency\": \"NORMAL\",\n" +
                    "      \"x\": 310.0,\n" +
                    "      \"y\": 100.0,\n" +
                    "      \"nicknameX\": 30.0,\n" +
                    "      \"nicknameY\": -10.0,\n" +
                    "      \"invariantX\": 50.0,\n" +
                    "      \"invariantY\": -40.0\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"id\": \"L2\",\n" +
                    "      \"nickname\": \"\",\n" +
                    "      \"invariant\": \"\",\n" +
                    "      \"type\": \"NORMAL\",\n" +
                    "      \"urgency\": \"NORMAL\",\n" +
                    "      \"x\": 310.0,\n" +
                    "      \"y\": 300.0,\n" +
                    "      \"nicknameX\": 30.0,\n" +
                    "      \"nicknameY\": -10.0,\n" +
                    "      \"invariantX\": 30.0,\n" +
                    "      \"invariantY\": 10.0\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"id\": \"L3\",\n" +
                    "      \"nickname\": \"\",\n" +
                    "      \"invariant\": \"z\\u003c\\u003d2\",\n" +
                    "      \"type\": \"NORMAL\",\n" +
                    "      \"urgency\": \"NORMAL\",\n" +
                    "      \"x\": 130.0,\n" +
                    "      \"y\": 300.0,\n" +
                    "      \"nicknameX\": 30.0,\n" +
                    "      \"nicknameY\": -10.0,\n" +
                    "      \"invariantX\": 20.0,\n" +
                    "      \"invariantY\": 20.0\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"edges\": [\n" +
                    "    {\n" +
                    "      \"sourceLocation\": \"L0\",\n" +
                    "      \"targetLocation\": \"L1\",\n" +
                    "      \"status\": \"INPUT\",\n" +
                    "      \"select\": \"\",\n" +
                    "      \"guard\": \"\",\n" +
                    "      \"update\": \"z\\u003d0\",\n" +
                    "      \"sync\": \"grant\",\n" +
                    "      \"nails\": [\n" +
                    "        {\n" +
                    "          \"x\": 180.0,\n" +
                    "          \"y\": 100.0,\n" +
                    "          \"propertyType\": \"SYNCHRONIZATION\",\n" +
                    "          \"propertyX\": -20.0,\n" +
                    "          \"propertyY\": -30.0\n" +
                    "        },\n" +
                    "        {\n" +
                    "          \"x\": 230.0,\n" +
                    "          \"y\": 100.0,\n" +
                    "          \"propertyType\": \"UPDATE\",\n" +
                    "          \"propertyX\": -20.0,\n" +
                    "          \"propertyY\": -30.0\n" +
                    "        }\n" +
                    "      ]\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"sourceLocation\": \"L1\",\n" +
                    "      \"targetLocation\": \"L2\",\n" +
                    "      \"status\": \"OUTPUT\",\n" +
                    "      \"select\": \"\",\n" +
                    "      \"guard\": \"\",\n" +
                    "      \"update\": \"\",\n" +
                    "      \"sync\": \"coin\",\n" +
                    "      \"nails\": [\n" +
                    "        {\n" +
                    "          \"x\": 310.0,\n" +
                    "          \"y\": 190.0,\n" +
                    "          \"propertyType\": \"SYNCHRONIZATION\",\n" +
                    "          \"propertyX\": 10.0,\n" +
                    "          \"propertyY\": -10.0\n" +
                    "        }\n" +
                    "      ]\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"sourceLocation\": \"L2\",\n" +
                    "      \"targetLocation\": \"L3\",\n" +
                    "      \"status\": \"INPUT\",\n" +
                    "      \"select\": \"\",\n" +
                    "      \"guard\": \"\",\n" +
                    "      \"update\": \"z\\u003d0\",\n" +
                    "      \"sync\": \"pub\",\n" +
                    "      \"nails\": [\n" +
                    "        {\n" +
                    "          \"x\": 250.0,\n" +
                    "          \"y\": 300.0,\n" +
                    "          \"propertyType\": \"SYNCHRONIZATION\",\n" +
                    "          \"propertyX\": -20.0,\n" +
                    "          \"propertyY\": -40.0\n" +
                    "        },\n" +
                    "        {\n" +
                    "          \"x\": 200.0,\n" +
                    "          \"y\": 300.0,\n" +
                    "          \"propertyType\": \"UPDATE\",\n" +
                    "          \"propertyX\": -20.0,\n" +
                    "          \"propertyY\": -40.0\n" +
                    "        }\n" +
                    "      ]\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"sourceLocation\": \"L3\",\n" +
                    "      \"targetLocation\": \"L0\",\n" +
                    "      \"status\": \"OUTPUT\",\n" +
                    "      \"select\": \"\",\n" +
                    "      \"guard\": \"\",\n" +
                    "      \"update\": \"\",\n" +
                    "      \"sync\": \"patent\",\n" +
                    "      \"nails\": [\n" +
                    "        {\n" +
                    "          \"x\": 130.0,\n" +
                    "          \"y\": 200.0,\n" +
                    "          \"propertyType\": \"SYNCHRONIZATION\",\n" +
                    "          \"propertyX\": 10.0,\n" +
                    "          \"propertyY\": -10.0\n" +
                    "        }\n" +
                    "      ]\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"sourceLocation\": \"L1\",\n" +
                    "      \"targetLocation\": \"L1\",\n" +
                    "      \"status\": \"INPUT\",\n" +
                    "      \"select\": \"\",\n" +
                    "      \"guard\": \"\",\n" +
                    "      \"update\": \"\",\n" +
                    "      \"sync\": \"grant\",\n" +
                    "      \"nails\": [\n" +
                    "        {\n" +
                    "          \"x\": 310.0,\n" +
                    "          \"y\": 60.0,\n" +
                    "          \"propertyType\": \"SYNCHRONIZATION\",\n" +
                    "          \"propertyX\": 10.0,\n" +
                    "          \"propertyY\": -20.0\n" +
                    "        },\n" +
                    "        {\n" +
                    "          \"x\": 290.0,\n" +
                    "          \"y\": 60.0,\n" +
                    "          \"propertyType\": \"NONE\",\n" +
                    "          \"propertyX\": 0.0,\n" +
                    "          \"propertyY\": 0.0\n" +
                    "        }\n" +
                    "      ]\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"sourceLocation\": \"L1\",\n" +
                    "      \"targetLocation\": \"L1\",\n" +
                    "      \"status\": \"INPUT\",\n" +
                    "      \"select\": \"\",\n" +
                    "      \"guard\": \"\",\n" +
                    "      \"update\": \"\",\n" +
                    "      \"sync\": \"pub\",\n" +
                    "      \"nails\": [\n" +
                    "        {\n" +
                    "          \"x\": 350.0,\n" +
                    "          \"y\": 100.0,\n" +
                    "          \"propertyType\": \"SYNCHRONIZATION\",\n" +
                    "          \"propertyX\": 10.0,\n" +
                    "          \"propertyY\": -10.0\n" +
                    "        },\n" +
                    "        {\n" +
                    "          \"x\": 350.0,\n" +
                    "          \"y\": 120.0,\n" +
                    "          \"propertyType\": \"NONE\",\n" +
                    "          \"propertyX\": 0.0,\n" +
                    "          \"propertyY\": 0.0\n" +
                    "        }\n" +
                    "      ]\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"sourceLocation\": \"L2\",\n" +
                    "      \"targetLocation\": \"L2\",\n" +
                    "      \"status\": \"INPUT\",\n" +
                    "      \"select\": \"\",\n" +
                    "      \"guard\": \"\",\n" +
                    "      \"update\": \"\",\n" +
                    "      \"sync\": \"grant\",\n" +
                    "      \"nails\": [\n" +
                    "        {\n" +
                    "          \"x\": 350.0,\n" +
                    "          \"y\": 300.0,\n" +
                    "          \"propertyType\": \"SYNCHRONIZATION\",\n" +
                    "          \"propertyX\": -20.0,\n" +
                    "          \"propertyY\": -40.0\n" +
                    "        },\n" +
                    "        {\n" +
                    "          \"x\": 350.0,\n" +
                    "          \"y\": 320.0,\n" +
                    "          \"propertyType\": \"NONE\",\n" +
                    "          \"propertyX\": 0.0,\n" +
                    "          \"propertyY\": 0.0\n" +
                    "        }\n" +
                    "      ]\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"sourceLocation\": \"L3\",\n" +
                    "      \"targetLocation\": \"L3\",\n" +
                    "      \"status\": \"INPUT\",\n" +
                    "      \"select\": \"\",\n" +
                    "      \"guard\": \"\",\n" +
                    "      \"update\": \"\",\n" +
                    "      \"sync\": \"grant\",\n" +
                    "      \"nails\": [\n" +
                    "        {\n" +
                    "          \"x\": 130.0,\n" +
                    "          \"y\": 340.0,\n" +
                    "          \"propertyType\": \"SYNCHRONIZATION\",\n" +
                    "          \"propertyX\": -20.0,\n" +
                    "          \"propertyY\": 10.0\n" +
                    "        },\n" +
                    "        {\n" +
                    "          \"x\": 110.0,\n" +
                    "          \"y\": 340.0,\n" +
                    "          \"propertyType\": \"NONE\",\n" +
                    "          \"propertyX\": 0.0,\n" +
                    "          \"propertyY\": 0.0\n" +
                    "        }\n" +
                    "      ]\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"sourceLocation\": \"L3\",\n" +
                    "      \"targetLocation\": \"L3\",\n" +
                    "      \"status\": \"INPUT\",\n" +
                    "      \"select\": \"\",\n" +
                    "      \"guard\": \"\",\n" +
                    "      \"update\": \"\",\n" +
                    "      \"sync\": \"pub\",\n" +
                    "      \"nails\": [\n" +
                    "        {\n" +
                    "          \"x\": 90.0,\n" +
                    "          \"y\": 280.0,\n" +
                    "          \"propertyType\": \"SYNCHRONIZATION\",\n" +
                    "          \"propertyX\": -50.0,\n" +
                    "          \"propertyY\": -20.0\n" +
                    "        },\n" +
                    "        {\n" +
                    "          \"x\": 90.0,\n" +
                    "          \"y\": 300.0,\n" +
                    "          \"propertyType\": \"NONE\",\n" +
                    "          \"propertyX\": 0.0,\n" +
                    "          \"propertyY\": 0.0\n" +
                    "        }\n" +
                    "      ]\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"description\": \"\",\n" +
                    "  \"x\": 5.0,\n" +
                    "  \"y\": 5.0,\n" +
                    "  \"width\": 450.0,\n" +
                    "  \"height\": 400.0,\n" +
                    "  \"includeInPeriodicCheck\": false\n" +
                    "}");

            addGeneratedComponent(new Component(returnedComponent));
        } else {
            System.out.println(value.getError());
            executableQuery.queryListener.getQuery().setQueryState(QueryState.ERROR);
            executableQuery.success.accept(false);
        }
    }

    private void addGeneratedComponent(Component newComponent) {
        Platform.runLater(() -> {
            newComponent.setTemporary(true);

            ObservableList<Component> listOfGeneratedComponents = Ecdar.getProject().getTempComponents();
            Component matchedComponent = null;

            for (Component currentGeneratedComponent : listOfGeneratedComponents) {
                int comparisonOfNames = currentGeneratedComponent.getName().compareTo(newComponent.getName());

                if (comparisonOfNames == 0) {
                    matchedComponent = currentGeneratedComponent;
                    break;
                } else if (comparisonOfNames < 0) {
                    break;
                }
            }

            if (matchedComponent == null) {
                UndoRedoStack.pushAndPerform(() -> { // Perform
                    Ecdar.getProject().getTempComponents().add(newComponent);
                }, () -> { // Undo
                    Ecdar.getProject().getTempComponents().remove(newComponent);
                }, "Created new component: " + newComponent.getName(), "add-circle");
            } else {
                Component finalMatchedComponent = matchedComponent;
                UndoRedoStack.pushAndPerform(() -> { // Perform
                    Ecdar.getProject().getTempComponents().remove(finalMatchedComponent);
                    Ecdar.getProject().getTempComponents().add(newComponent);
                }, () -> { // Undo
                    Ecdar.getProject().getTempComponents().remove(newComponent);
                    Ecdar.getProject().getTempComponents().add(finalMatchedComponent);
                }, "Created new component: " + newComponent.getName(), "add-circle");
            }

            EcdarController.getActiveCanvasPresentation().getController().setActiveModel(newComponent);
        });
    }

    private BackendConnection startNewBackendConnection(BackendHelper.BackendNames backend) {
        boolean isReveaal = backend.equals(BackendHelper.BackendNames.Reveaal);

        if ((isReveaal ? reveaalConnections.size() < maxNumberOfBackendConnections
                : jEcdarConnections.size() < maxNumberOfBackendConnections)) {
            try {
                Process p;
                int portNumber = SocketUtils.findAvailableTcpPort();

                do {
                    ProcessBuilder pb;
                    if (isReveaal) {
                        pb = new ProcessBuilder("src/Reveaal", "-p", this.hostAddress + ":" + portNumber);
                    } else {
                        pb = new ProcessBuilder("java", "-jar", "src/libs/j-Ecdar.jar");
                    }

                    p = pb.start();
                    // If the process is not alive, it failed while starting up, try again
                } while (!p.isAlive());

                ManagedChannel channel = ManagedChannelBuilder.forTarget(this.hostAddress + ":" + portNumber)
                        .usePlaintext()
                        .keepAliveTime(1000, TimeUnit.MILLISECONDS)
                        .build();

                EcdarBackendGrpc.EcdarBackendStub stub = EcdarBackendGrpc.newStub(channel);
                BackendConnection newConnection = new BackendConnection(p, stub);

                if (isReveaal) {
                    reveaalConnections.add(newConnection);
                } else {
                    jEcdarConnections.add(newConnection);
                }

                return newConnection;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    private class ExecutableQuery {
        private final String query;
        private final BackendHelper.BackendNames backend;
        private final Consumer<Boolean> success;
        private final Consumer<BackendException> failure;
        private final QueryListener queryListener;
        public int tries = 0;

        ExecutableQuery(String query, BackendHelper.BackendNames backend, Consumer<Boolean> success, Consumer<BackendException> failure, QueryListener queryListener) {
            this.query = query;
            this.backend = backend;
            this.success = success;
            this.failure = failure;
            this.queryListener = queryListener;
        }

        /**
         * Execute the query using the backend driver
         */
        public void execute() {
            executeQuery(this);
            tries++;
        }
    }

    private class BackendConnection {
        private final Process process;
        private final EcdarBackendGrpc.EcdarBackendStub stub;
        private ExecutableQuery executableQuery = null;

        BackendConnection(Process process, EcdarBackendGrpc.EcdarBackendStub stub) throws IOException {
            this.process = process;
            this.stub = stub;
        }

        /**
         * Get the gRPC stub of the connection to use for query execution
         *
         * @return the gRPC stub of this connection
         */
        public EcdarBackendGrpc.EcdarBackendStub getStub() {
            return stub;
        }

        /**
         * Get the executable query currently being executed or waiting for execution with this connection
         *
         * @return the query
         */
        public ExecutableQuery getExecutableQuery() {
            return executableQuery;
        }

        /**
         * Set the executable query to execute with the connection
         *
         * @param executableQuery query to execute
         */
        public void setExecutableQuery(ExecutableQuery executableQuery) {
            this.executableQuery = executableQuery;
        }

        /**
         * Get whether this backend connection is currently occupied with execution of a query
         *
         * @return true if the connection is in use
         */
        public boolean isRunningQuery() {
            return executableQuery != null;
        }

        /**
         * Close the gRPC connection and end the process
         *
         * @throws IOException originally thrown by the destroy method on java.lang.Process
         */
        public void close() throws IOException {
            // Remove the connection from the connection list
            if (jEcdarConnections.remove(this) || reveaalConnections.remove(this)) {
                System.out.println("Successfully closed connection to backend");
            } else {
                System.out.println("Tried to remove a connection not present in either connection list");
            }

            process.destroy();
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
