package de.unistuttgart.iste.gits.media_service.exception;

public class NoAccessToMediaRecord extends RuntimeException{

    public NoAccessToMediaRecord() {
        super("User does not have access to one ore more courses");
    }
}
