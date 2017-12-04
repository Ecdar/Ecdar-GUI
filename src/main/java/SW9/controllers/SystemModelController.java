package SW9.controllers;

import SW9.abstractions.SystemModel;

/**
 *
 */
public class SystemModelController extends ModelController {
    private SystemModel systemModel;

    public SystemModel getSystemModel() {
        return systemModel;
    }

    public void setSystemModel(SystemModel systemModel) {
        this.systemModel = systemModel;
    }
}
