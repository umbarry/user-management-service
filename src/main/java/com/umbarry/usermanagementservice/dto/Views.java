package com.umbarry.usermanagementservice.dto;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Objects;

public class Views {
    public interface Reporter {}
    public interface Operator extends Reporter {}
    public interface Developer extends Operator {}

    public static Class<?> getViewForRole(Collection<? extends GrantedAuthority> authorities) {
        if (authorities.stream().anyMatch(a -> Objects.equals(a.getAuthority(), "ROLE_OWNER") || Objects.equals(a.getAuthority(), "ROLE_DEVELOPER") || Objects.equals(a.getAuthority(), "ROLE_MAINTAINER"))) {
            return Views.Developer.class;
        } else if (authorities.stream().anyMatch(a -> Objects.equals(a.getAuthority(), "ROLE_OPERATOR"))) {
            return Views.Operator.class;
        } else {
            return Views.Reporter.class;
        }
    }
}
