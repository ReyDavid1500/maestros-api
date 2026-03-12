package com.maestros.security;

import com.maestros.exception.UnauthorizedException;
import com.maestros.model.sql.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthHelper {

    /**
     * Returns the currently authenticated {@link User} from the
     * {@link SecurityContextHolder}.
     *
     * @throws UnauthorizedException if there is no authenticated user in the
     *                               current context
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof User user)) {
            throw new UnauthorizedException("No autenticado");
        }

        return user;
    }
}
