package com.example.demo.view;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class UserLoginView {

    @NotNull(message = "Email can't be null")
    @Email
    private String email;

    @NotNull(message = "Password can't be null")
    @Size(min=10, message = "Password must be equal or greater then 10 characters, to start with capital letter and to have special character")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])(?=.*\\W).*$")
    private String password;

    public UserLoginView() {
        super();
    }

    public UserLoginView(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
