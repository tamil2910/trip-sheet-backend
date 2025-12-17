package com.example.trip_sheet_backend.config;

import org.springframework.stereotype.Component;

@Component("permissionResolver")
public class PermissionResolver {

    public String createPermission(Object controller) {
        String controllerName = controller.getClass().getSimpleName();
        String entityName = controllerName.replace("Controller", "");
        return "CAN_CREATE_" + entityName.toUpperCase();
    }

    public String updatePermission(Object controller) {
        String controllerName = controller.getClass().getSimpleName();
        String entityName = controllerName.replace("Controller", "");
        return "CAN_UPDATE_" + entityName.toUpperCase();
    }

    public String deletePermission(Object controller) {
        String controllerName = controller.getClass().getSimpleName();
        String entityName = controllerName.replace("Controller", "");
        return "CAN_DELETE_" + entityName.toUpperCase();
    }

    public String readPermission(Object controller) {
        String controllerName = controller.getClass().getSimpleName();
        String entityName = controllerName.replace("Controller", "");
        return "CAN_READ_" + entityName.toUpperCase();
    }
}
