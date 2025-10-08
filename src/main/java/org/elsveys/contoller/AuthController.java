package org.elsveys.contoller;

import org.elsveys.model.dto.LoginRequest;
import org.elsveys.model.dto.RegisterRequest;
import org.elsveys.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            String token = authService.register(
                    request.getUsername(),
                    request.getPassword(),
                    request.getEmail()
            );

            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            response.put("username", request.getUsername());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            String token = authService.login(
                    request.getUsername(),
                    request.getPassword()
            );

            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            response.put("username", request.getUsername());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            boolean valid = authService.validateToken(token);

            Map<String, Object> response = new HashMap<>();
            response.put("valid", valid);

            if (valid) {
                response.put("username", authService.getUsernameFromToken(token));
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}