package ecdar.backend;

import EcdarProtoBuf.ComponentProtos;
import EcdarProtoBuf.QueryProtos;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.abstractions.Query;
import ecdar.abstractions.QueryState;
import ecdar.controllers.EcdarController;
import ecdar.utility.UndoRedoStack;
import ecdar.utility.helpers.StringValidator;
import io.grpc.stub.StreamObserver;
import javafx.application.Platform;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

public class QueryHandler {
    private final BackendDriver backendDriver;
    private final ArrayList<BackendConnection> connections = new ArrayList<>();

    public QueryHandler(BackendDriver backendDriver) {
        this.backendDriver = backendDriver;
    }

    /**
     * Executes the specified query
     * @param query             query to be executed
     */
    public void executeQuery(Query query) throws NoSuchElementException {
        if (query.getQueryState().equals(QueryState.RUNNING) || !StringValidator.validateQuery(query.getQuery())) return;

        if (query.getQuery().isEmpty()) {
            query.setQueryState(QueryState.SYNTAX_ERROR);
            query.addError("Query is empty");
            return;
        }

        query.setQueryState(QueryState.RUNNING);
        query.errors().set("");

        GrpcRequest request = new GrpcRequest(backendConnection -> {
            connections.add(backendConnection); // Save reference for closing connection on exit

            var componentsInfoBuilder = BackendHelper.getComponentsInfoBuilder(query.getQuery());

            StreamObserver<QueryProtos.QueryResponse> responseObserver = new StreamObserver<>() {
                @Override
                public void onNext(QueryProtos.QueryResponse value) {
                    handleQueryResponse(value, query);
                }

                @Override
                public void onError(Throwable t) {
                    handleQueryBackendError(t, query);

                    // Release backend connection
                    backendDriver.addBackendConnection(backendConnection);
                    connections.remove(backendConnection);
                }

                @Override
                public void onCompleted() {
                    // Release backend connection
                    backendDriver.addBackendConnection(backendConnection);
                    connections.remove(backendConnection);
                }
            };

            // ToDo SW5: Not working with the updated gRPC Protos
            var queryBuilder = QueryProtos.QueryRequest.newBuilder()
                    .setUserId(1)
                    .setQueryId(1)
                    .setQuery(query.getType().getQueryName() + ": " + query.getQuery())
                    .setComponentsInfo(componentsInfoBuilder);

            backendConnection.getStub().withDeadlineAfter(backendDriver.getResponseDeadline(), TimeUnit.MILLISECONDS)
                    .sendQuery(queryBuilder.build(), responseObserver);
        }, query.getBackend());

        backendDriver.addRequestToExecutionQueue(request);
    }

    /**
     * Close all open backend connection and kill all locally running processes
     *
     * @throws IOException if any of the sockets do not respond
     */
    public void closeAllBackendConnections() throws IOException {
        for (BackendConnection con : connections) {
            con.close();
        }
    }

    private void handleQueryResponse(QueryProtos.QueryResponse value, Query query) {
        // If the query has been cancelled, ignore the result
        if (query.getQueryState() == QueryState.UNKNOWN) return;

        switch (value.getResponseCase()) {
            case QUERY_OK:
                QueryProtos.QueryResponse.QueryOk queryOk = value.getQueryOk();
                switch (queryOk.getResultCase()) {
                    case REFINEMENT:
                        if (queryOk.getRefinement().getSuccess()) {
                            query.setQueryState(QueryState.SUCCESSFUL);
                            query.getSuccessConsumer().accept(true);
                        } else {
                            query.setQueryState(QueryState.ERROR);
                            query.getFailureConsumer().accept(new BackendException.QueryErrorException(queryOk.getRefinement().getReason()));
                            query.getSuccessConsumer().accept(false);
                            query.getStateConsumer().accept(value.getQueryOk().getRefinement().getState());
                        }
                        break;

                    case CONSISTENCY:
                        if (queryOk.getConsistency().getSuccess()) {
                            query.setQueryState(QueryState.SUCCESSFUL);
                            query.getSuccessConsumer().accept(true);
                        } else {
                            query.setQueryState(QueryState.ERROR);
                            query.getFailureConsumer().accept(new BackendException.QueryErrorException(queryOk.getConsistency().getReason()));
                            query.getSuccessConsumer().accept(false);
                            query.getStateConsumer().accept(value.getQueryOk().getConsistency().getState());
                            query.getActionConsumer().accept(value.getQueryOk().getConsistency().getAction());
                        }
                        break;

                    case DETERMINISM:
                        if (queryOk.getDeterminism().getSuccess()) {
                            query.setQueryState(QueryState.SUCCESSFUL);
                            query.getSuccessConsumer().accept(true);
                        } else {
                            query.setQueryState(QueryState.ERROR);
                            query.getFailureConsumer().accept(new BackendException.QueryErrorException(queryOk.getDeterminism().getReason()));
                            query.getSuccessConsumer().accept(false);
                            query.getStateConsumer().accept(value.getQueryOk().getDeterminism().getState());
                        }
                        break;

                    case IMPLEMENTATION:
                        if (queryOk.getImplementation().getSuccess()) {
                            query.setQueryState(QueryState.SUCCESSFUL);
                            query.getSuccessConsumer().accept(true);
                        } else {
                            query.setQueryState(QueryState.ERROR);
                            query.getFailureConsumer().accept(new BackendException.QueryErrorException(queryOk.getImplementation().getReason()));
                            query.getSuccessConsumer().accept(false);
                            query.getStateConsumer().accept(value.getQueryOk().getImplementation().getState());
                        }
                        break;

                    case REACHABILITY:
                        if (queryOk.getReachability().getSuccess()) {
                            query.setQueryState(QueryState.SUCCESSFUL);
                            query.getSuccessConsumer().accept(true);
                        } else {
                            query.setQueryState(QueryState.ERROR);
                            query.getFailureConsumer().accept(new BackendException.QueryErrorException(queryOk.getReachability().getReason()));
                            query.getSuccessConsumer().accept(false);
                            query.getStateConsumer().accept(value.getQueryOk().getReachability().getState());
                        }
                        break;

                    case COMPONENT:
                        query.setQueryState(QueryState.SUCCESSFUL);
                        query.getSuccessConsumer().accept(true);
                        JsonObject returnedComponent = (JsonObject) JsonParser.parseString(queryOk.getComponent().getComponent().getJson());
                        addGeneratedComponent(new Component(returnedComponent));
                        break;

                    case ERROR:
                        query.setQueryState(QueryState.ERROR);
                        query.getFailureConsumer().accept(new BackendException.QueryErrorException(queryOk.getError()));
                        query.getSuccessConsumer().accept(false);
                        break;

                    case RESULT_NOT_SET:
                        query.setQueryState(QueryState.ERROR);
                        query.getSuccessConsumer().accept(false);
                        break;
                }
                break;

            case USER_TOKEN_ERROR:
                query.setQueryState(QueryState.ERROR);
                query.getFailureConsumer().accept(new BackendException.QueryErrorException(value.getUserTokenError().getErrorMessage()));
                query.getSuccessConsumer().accept(false);
                break;

            case RESPONSE_NOT_SET:
                query.setQueryState(QueryState.ERROR);
                query.getSuccessConsumer().accept(false);
                break;
        }
    }

    private void handleQueryBackendError(Throwable t, Query query) {
        // If the query has been cancelled, ignore the error
        if (query.getQueryState() == QueryState.UNKNOWN) return;

        // Each error starts with a capitalized description of the error equal to the gRPC error type encountered
        String errorType = t.getMessage().split(":\\s+", 2)[0];

        if ("DEADLINE_EXCEEDED".equals(errorType)) {
            query.setQueryState(QueryState.ERROR);
            query.getFailureConsumer().accept(new BackendException.QueryErrorException("The backend did not answer the request in time"));
        } else {
            try {
                query.setQueryState(QueryState.ERROR);
                query.getFailureConsumer().accept(new BackendException.QueryErrorException("The execution of this query failed with message:\n" + t.getLocalizedMessage()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void addGeneratedComponent(Component newComponent) {
        Platform.runLater(() -> {
            newComponent.setTemporary(true);

            ObservableList<Component> listOfGeneratedComponents = Ecdar.getProject().getTempComponents(); // ToDo NIELS: Refactor
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
                // Remove current component with name and add the newly generated one
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
}
