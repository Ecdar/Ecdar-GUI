package ecdar.backend;

import EcdarProtoBuf.QueryProtos;
import EcdarProtoBuf.QueryProtos.QueryRequest.Settings;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.abstractions.Query;
import ecdar.abstractions.QueryState;
import ecdar.abstractions.QueryType;
import ecdar.controllers.EcdarController;
import ecdar.utility.UndoRedoStack;
import ecdar.utility.helpers.StringValidator;
import io.grpc.stub.StreamObserver;
import javafx.application.Platform;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.UUID;
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

            var queryBuilder = QueryProtos.QueryRequest.newBuilder()
                    .setUserId(1)
                    .setQueryId(UUID.randomUUID().hashCode())
                    .setSettings(Settings.newBuilder().setDisableClockReduction(true))
                    .setQuery(query.getType().getQueryName() + ": " + query.getQuery())
                    .setComponentsInfo(componentsInfoBuilder);

            backendConnection.getStub().withDeadlineAfter(backendDriver.getResponseDeadline(), TimeUnit.MILLISECONDS)
                    .sendQuery(queryBuilder.build(), responseObserver);
        }, query.getBackend());

        backendDriver.addRequestToExecutionQueue(request);
    }

    /**
     * Close all open backend connection and kill all locally running processes
     */
    public void closeAllBackendConnections() {
        for (BackendConnection con : connections) {
            con.close();
        }
    }

    private void handleQueryResponse(QueryProtos.QueryResponse value, Query query) {
        // If the query has been cancelled, ignore the result
        if (query.getQueryState() == QueryState.UNKNOWN) return;
        switch (value.getResultCase()) {
            case REFINEMENT:
                if (value.getRefinement().getSuccess()) {
                    query.setQueryState(QueryState.SUCCESSFUL);
                    query.getSuccessConsumer().accept(true);
                } else {
                    query.setQueryState(QueryState.ERROR);
                    query.getFailureConsumer().accept(new BackendException.QueryErrorException(value.getRefinement().getReason()));
                    query.getSuccessConsumer().accept(false);
                    query.getStateActionConsumer().accept(value.getRefinement().getState(),
                            value.getRefinement().getActionList());
                }
                break;

            case CONSISTENCY:
                if (value.getConsistency().getSuccess()) {
                    query.setQueryState(QueryState.SUCCESSFUL);
                    query.getSuccessConsumer().accept(true);
                } else {
                    query.setQueryState(QueryState.ERROR);
                    query.getFailureConsumer().accept(new BackendException.QueryErrorException(value.getConsistency().getReason()));
                    query.getSuccessConsumer().accept(false);
                    query.getStateActionConsumer().accept(value.getConsistency().getState(),
                            value.getConsistency().getActionList());
                }
                break;

            case DETERMINISM:
                if (value.getDeterminism().getSuccess()) {
                    query.setQueryState(QueryState.SUCCESSFUL);
                    query.getSuccessConsumer().accept(true);
                } else {
                    query.setQueryState(QueryState.ERROR);
                    query.getFailureConsumer().accept(new BackendException.QueryErrorException(value.getDeterminism().getReason()));
                    query.getSuccessConsumer().accept(false);
                    query.getStateActionConsumer().accept(value.getDeterminism().getState(),
                            value.getDeterminism().getActionList());

                }
                break;
            
            case IMPLEMENTATION:
                if (value.getImplementation().getSuccess()) {
                    query.setQueryState(QueryState.SUCCESSFUL);
                    query.getSuccessConsumer().accept(true);
                } else {
                    query.setQueryState(QueryState.ERROR);
                    query.getFailureConsumer().accept(new BackendException.QueryErrorException(value.getImplementation().getReason()));
                    query.getSuccessConsumer().accept(false);
                    //ToDo: These errors are not implemented in the Reveaal backend.
                    query.getStateActionConsumer().accept(value.getImplementation().getState(),
                            new ArrayList<>());
                }
                break;

          case REACHABILITY:
              if (value.getReachability().getSuccess()) {
                  query.setQueryState(QueryState.SUCCESSFUL);
                  Ecdar.showToast("Reachability check was successful and the location can be reached.");

                  //create list of edge id's
                  ArrayList<String> edgeIds = new ArrayList<>();
                  for(var pathsList : value.getReachability().getComponentPathsList()){
                      for(var id : pathsList.getEdgeIdsList().toArray()) {
                          edgeIds.add(id.toString());
                      }
                  }
                  //highlight the edges
                  Ecdar.getSimulationHandler().highlightReachabilityEdges(edgeIds);
                  query.getSuccessConsumer().accept(true);
              }
              else if(!value.getReachability().getSuccess()){
                  Ecdar.showToast("Reachability check was successful but the location cannot be reached.");
                  query.getSuccessConsumer().accept(true);
              } else {
                  query.setQueryState(QueryState.ERROR);
                  Ecdar.showToast("Error from backend: Reachability check was unsuccessful!");
                  query.getFailureConsumer().accept(new BackendException.QueryErrorException(value.getReachability().getReason()));
                  query.getSuccessConsumer().accept(false);
                  //ToDo: These errors are not implemented in the Reveaal backend.
                  query.getStateActionConsumer().accept(value.getReachability().getState(),
                          new ArrayList<>());
              }
              break;

          case COMPONENT:
              query.setQueryState(QueryState.SUCCESSFUL);
              query.getSuccessConsumer().accept(true);
              JsonObject returnedComponent = (JsonObject) JsonParser.parseString(value.getComponent().getComponent().getJson());
              addGeneratedComponent(new Component(returnedComponent));
              break;

          case ERROR:
              query.setQueryState(QueryState.ERROR);
              Ecdar.showToast(value.getError());
              query.getFailureConsumer().accept(new BackendException.QueryErrorException(value.getError()));
              query.getSuccessConsumer().accept(false);
              break;

          case RESULT_NOT_SET:
              query.setQueryState(QueryState.ERROR);
              query.getSuccessConsumer().accept(false);
              break;
        }
    }

    private void handleQueryBackendError(Throwable t, Query query) {
        // If the query has been cancelled, ignore the error
        if (query.getQueryState() == QueryState.UNKNOWN) return;

        // due to lack of information from backend if the reachability check shows that a location can NOT be reached, this is the most accurate information we can provide
        if(query.getType() == QueryType.REACHABILITY){
            Ecdar.showToast("Timeout (no response from backend): The reachability query failed. This might be due to the fact that the location is not reachable.");
        }

        // Each error starts with a capitalized description of the error equal to the gRPC error type encountered
        String errorType = t.getMessage().split(":\\s+", 2)[0];

        if ("DEADLINE_EXCEEDED".equals(errorType)) {
            query.setQueryState(QueryState.ERROR);
            query.getFailureConsumer().accept(new BackendException.QueryErrorException("The backend did not answer the request in time"));
        } else {
            try {
                query.setQueryState(QueryState.ERROR);
                query.getFailureConsumer().accept(new BackendException.QueryErrorException("The execution of this query failed with message:" + System.lineSeparator() + t.getLocalizedMessage()));
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
