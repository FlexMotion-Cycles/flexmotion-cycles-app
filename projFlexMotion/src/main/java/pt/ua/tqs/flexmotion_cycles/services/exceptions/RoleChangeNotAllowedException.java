package pt.ua.tqs.flexmotion_cycles.services.exceptions;

public class RoleChangeNotAllowedException extends RuntimeException {

    public RoleChangeNotAllowedException() {
        super("Only the admin can change user roles");
    }
    
}
