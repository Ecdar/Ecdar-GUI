# Contributing
When contributing to this repository, make your own fork and create pull requests to this repo from there.

## Issues
If you find a bug or a missing feature, feel free to create an issue. The system is continuously under development and suggestions are always welcome.

### Bugs
To increase the chances of the issue being resolved, please include the following (or use the `Bug Report` issue template):
- Concise title that states the issue
- An in-depth description of the issue
  - What happened
  - What was expected to happen
  - Suggestion on possible cause [Not required]
- Images (if relevant)

### Feature
To increase the chances of the issue being resolved, please include the following (or use the `Feature Request` issue template):
- Concise title that describes the feature
- An in-depth description of the feature
  - What is the feature
  - Use case
  - How should it work for the user
  - Suggestions for implementation [Not required]
- Reasoning behind the request
  - Added value
  - Improved user experience

> :information_source: To help organising the issue, please attach relevant tags (eg. `bug`, `feature`, etc.).

## Pull Requests
Pull requests are continuously being reviewed and merged. In order to ease this process, please open a pull request as draft, as long as it is under development, to notify anyone else that a given feature/issue is being worked on.

Additionally, please add `Closes #{ISSUE_ID}` if the pull request is linked to a specific issue. If a PR addresses multiple pull requests, please add `Closes #{ISSUE_ID}` for each one.

A CI workflow is executed against all pull requests and must succeed before the pull request can be merged, so please make sure that you check the workflow status and potential error messages.

## Tests
All non-UI tests are executed as part of the CI workflow and hence must succeed before merging. The tests are written with JUnit and relevant tests should be added when new code is added. If you are new to JUnit, you can check out syntax and structure [here](https://junit.org/junit5/docs/current/user-guide/).

The test suite can be executed locally by running:
```shell
./gradlew test
```

> :information_source: Currently, the codebase has high coupling, which has made testing difficult and the test suite very small.

### UI Tests
For features that are highly coupled with the interface, a second test suite has been added under `src/test/java/ecdar/ui`. These tests are excluded from the `test` task are can be executed by running:
```shell
./gradlew uiTest
```
These tests are more intensive to run and utilizes a robot for interacting with a running process of the GUI. The tests are implemented using [TestFX](https://github.com/TestFX/TestFX). As these tests are more intensive, they are not run as part of the standard CI workflow.

You should prefer writing non-UI tests, as they are less demanding and are part of the CI workflow.

## Code Organisation
The code within the project is structure based on the Model-View-ViewModel (**MVVM**) architectural pattern. However, the terms _Abstraction_, _Presentation_, and _Controller_ are used instead of _Model_, _View_, and _View-Model_ respectively.
This means that each element in the system consists of:
- An _Abstraction_ (located in `abstractions` package).
- A _Controller_ (located in `controllers` package).
- A _Presentation_ (located in `presentations` package).
    - Most of the presentations are related to an `FXML` markup file that specifies the look of the presentation. These files are located in `src/main/resources/ecdar/presentations`.

In addition to these, a `utility` package is used for additional business logic to improve separation of concern and enhance the testability of the system.

### Abstractions
The abstractions are used to represent logical elements, such as `components`, `locations`, and `edges`. These classes should mostly be pure data objects. They are used to save and load data to and from existing project files.

### Controllers
The controllers contain the business logic of the system. They function as the link between the UI and the abstractions.
This is implemented such that an action performed to an element in the UI triggers a method inside the controller, which then alters the state of the related abstraction.

They implement the `Initializable` interface and are initialized through their associated presentation when an instance of that is instantiated. Hierarchically, a presentation therefore contains a controller.

Each controller controls an instance of its related abstraction. If an action to one element should affect another element, this effect is enforced through the controller.

### Presentations
As mentioned above, most of the presentations are split into a Java class and an FXML markup file. The Java class can be seen as a shell to initialize the FXML element from inside the business logic. It initializes the related controller and ensures that any needed elements are set within it. This allows the controllers to be initialized without any UI elements, which is very useful for testing, while ensuring that they are correctly connected while the UI is running.

The FXML files are markup specifying how the elements should look in the UI and have a reference to the related controller. Each element that should be addressable or changeable from the controller has an `fx:id` that is directly referenced as a member inside the controller. The direct connection to a controller allows events, such as `onPressed`, to trigger the correct methods in the controller and also helps IDEs identified any potentially missing methods or members.

> :question: **Why use both a controller and a presentation Java file?**\
> The advantage of during this is that the `controller` can contain all the business logic and bindings to the FXML elements, while the `presentation` can be used to instantiate and reference the UI elements inside the Java code. The `controller` should contain the logic and is bound within the FXML file, so the `presentation` Java file should be seen as a shell.

### Utility
To increase the testability and separation of concern further, the `utility` package is introduced. This package includes useful functionality that is either used in multiple unrelated classes or outside the responsibility of the given class.

An example of one of the classes located in this package is the `UndoRedoStack` used to keep track of actions performed by the user.

### Miscellaneous
Besides the packages mentioned above, some larger functionalities are located in their own packages. Here is a small description of each:
- `backend`: Responsible for the communication with the engines and model checking.
- `code_analysis`: Responsible for analysing the elements of the current project and construct messages if errors or warnings are encountered.
- `issues`: Classes for representing `Errors`, `Issues`, and `Warnings`.
- `model_canvas.arrow_heads`: Arrowheads used in the UI to visualize the direction of edges.
- `mutation [Deprecrated]`: Functionality for supporting mutation testing of components. **This feature is currently not implemented in the engines and is therefore currently not supported**.