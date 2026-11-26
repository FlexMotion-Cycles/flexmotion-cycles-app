package pt.ua.tqs.flexmotion_cycles.services.exceptions;

public class UserNotFoundException extends RuntimeException{
    public UserNotFoundException(Long userId) {
        super("User " + userId + " not found");
    }
}
