package com.example.demo.service;

import com.example.demo.acl.Acl;
import com.example.demo.dto.UserSearchDTO;
import com.example.demo.dto.emailDTO;
import com.example.demo.model.*;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.TokenUtils;
import com.example.demo.view.UserLoginView;
import com.example.demo.view.UserRegisterView;
import com.example.demo.view.UserTokenState;
import javassist.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
@Slf4j
public class UserService {

    @Autowired
    private TokenUtils tokenUtils;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private AuthorityService authorityService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private Acl acl;

    public void addRestorePermissions(String role) throws IOException {
        this.acl.addRestorePermissions(role);
    }

    public UserSearchDTO xssPrevent(emailDTO emaill) throws SQLException {
        String sql = "select "
                + "first_name,last_name,email "
                + "from users where email = '"
                + emaill.getEmail()
                + "'";

        DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
        dataSourceBuilder.driverClassName("org.postgresql.Driver");
        dataSourceBuilder.url("jdbc:postgresql://localhost:5432/postgres?autoReconnect=true&useUnicode=true&characterEncoding=UTF-8");
        dataSourceBuilder.username("postgres");
        dataSourceBuilder.password("retturn.05be");

        DataSource dataSource = dataSourceBuilder.build();
        Connection c = dataSource.getConnection();
        ResultSet rs = c.createStatement().executeQuery(sql);

        String firstName = "";
        String lastName = "";
        String email = "";
        while (rs.next()) {
            firstName = rs.getString("first_name");
            lastName = rs.getString("last_name");
            email = rs.getString("email");
        }

        firstName = StringEscapeUtils.escapeHtml4(firstName);
        lastName = StringEscapeUtils.escapeHtml4(lastName);
        email = StringEscapeUtils.escapeHtml4(email);

        UserSearchDTO u = new UserSearchDTO();
        u.setEmail(email);
        u.setFirstName(firstName);
        u.setLastName(lastName);

        log.info("XSS is prevented!");
        return u;
    }

    public UserSearchDTO safeFindOneByEmail(emailDTO emaill) throws SQLException {
        String sql = "select "
                + "first_name, last_name, email from users "
                + "where email = ?";

        DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
        dataSourceBuilder.driverClassName("org.postgresql.Driver");
        dataSourceBuilder.url("jdbc:postgresql://localhost:5432/postgres?autoReconnect=true&useUnicode=true&characterEncoding=UTF-8");
        dataSourceBuilder.username("postgres");
        dataSourceBuilder.password("retturn.05be");

        DataSource dataSource = dataSourceBuilder.build();
        Connection c = dataSource.getConnection();
        PreparedStatement p = c.prepareStatement(sql);
        p.setString(1, emaill.getEmail());
        ResultSet rs = p.executeQuery();

        String firstName = "";
        String lastName = "";
        String email = "";
        while (rs.next()) {
            firstName = rs.getString("first_name");
            lastName = rs.getString("last_name");
            email = rs.getString("email");
        }

        UserSearchDTO u = new UserSearchDTO();
        u.setEmail(email);
        u.setFirstName(firstName);
        u.setLastName(lastName);

        log.info("SQL Injection is prevented!");
        return u;
        // omitted - process rows and return an account list
    }

    public Users register(UserRegisterView userRegisterView) throws NotFoundException {
        if (!userRegisterView.getPassword().equals(userRegisterView.getRepeatPassword()))
            return null;

        Users u = this.userService.findOneByEmail(userRegisterView.getEmail());
        if (u != null) {
            log.warn("User with this email already exists");
            return null;
        }

        return this.userService.save(userRegisterView);
    }

    public UserTokenState login(UserLoginView user) throws NotFoundException {
        Users u = this.findOneByEmailAndPassword(user.getEmail(), user.getPassword());
        if (u == null)
            return null;

        List<Authority> auth = this.authorityService.findAllByRoleName(u.getRole().getRole());
        final Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword(), auth));

        //ubaci username(email) + password u kontext
        SecurityContextHolder.getContext().setAuthentication(authentication);

        //Kreiraj token
        Users userToken = (Users) authentication.getPrincipal();
        String jwt = tokenUtils.generateToken(userToken.getEmail(), userToken.getRole().getRole());
        int expiresIn = tokenUtils.getExpiredIn();

        log.info(u + " has logged");

        return new UserTokenState(jwt, (long) expiresIn, userToken.isFirstTimeLogged());
    }

    public Users findOneByEmailAndPassword(String email, String password) throws NotFoundException {
        Users user = this.userRepository.findOneByEmail(email);
        if (user == null) {
            log.error("Bad credentials, email doesn't matches!");
            throw new NotFoundException("Not existing user");
        }
        if (!BCrypt.checkpw(password, user.getPassword())) {
            log.error("Bad credentials, password doesn't matches!");
            throw new NotFoundException("Not existing user");
        }
        log.info(user + " has been found by his email and password");

        return user;
    }

    public Users findOneByEmail(String email) throws NotFoundException {
        return this.userRepository.findOneByEmail(email);
    }

    public Users save(UserRegisterView user) {
        Users u = Users.builder().role(UserRole.valueOf("USER"))
                .firstName(user.getFirstName()).lastName(user.getLastName()).email(user.getEmail())
                .password(BCrypt.hashpw(user.getPassword(), BCrypt.gensalt(12))).build();

        log.info("New user " + u + " has been registered");
        return this.userRepository.save(u);
    }
}