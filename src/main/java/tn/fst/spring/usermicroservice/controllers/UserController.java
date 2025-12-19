package tn.fst.spring.usermicroservice.controllers;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import tn.fst.spring.usermicroservice.entities.User;
import tn.fst.spring.usermicroservice.repositories.UserRepository;

import java.util.*;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private UserRepository userRepository;

    @Value("${server.port}")
    private String serverPort;

    private final String PRODUCT_SERVICE_BASE_URL = "http://product-microservice/products";

    // Stockage local des users
    private final List<User> userList = new ArrayList<>();

    // --- GET all users réel avec port ---
    @GetMapping
    @Retry(name = "myRetry", fallbackMethod = "fallbackGetAllUsers")
    @RateLimiter(name = "myRateLimiter", fallbackMethod = "fallbackGetAllUsers")
    @CircuitBreaker(name = "usermicroService", fallbackMethod = "fallbackGetAllUsers")
    public List<User> getAllUsers() {
        System.out.println("getAllUsers appelé à " + new Date());

        List<User> users = userRepository.findAll(); // récupère de la DB
        List<User> usersWithPort = new ArrayList<>();

        if (users.isEmpty()) {
            User emptyUser = new User();
            emptyUser.setName("No users found (port " + serverPort + ")");
            usersWithPort.add(emptyUser);
        } else {
            for (User u : users) {
                usersWithPort.add(copyUserWithPort(u));
            }
        }
        return usersWithPort;
    }

    // Fallback pour getAllUsers
    public List<User> fallbackGetAllUsers(Throwable e) {
        System.out.println("FALLBACK getAllUsers activé: " + e.getMessage());

        List<User> fallbackList = new ArrayList<>();
        User fallbackUser = new User();
        fallbackUser.setId(-1L);
        fallbackUser.setName("Service users temporairement indisponible (port " + serverPort + ")");
        fallbackUser.setEmail("fallback@example.com");
        fallbackList.add(fallbackUser);

        return fallbackList;
    }

    // --- Appel vers product-microservice via Ribbon ---
    @GetMapping("/products")
    @Retry(name = "myRetry", fallbackMethod = "fallbackGetProducts")
    @CircuitBreaker(name = "productService", fallbackMethod = "fallbackGetProducts")
    public List<Map<String, Object>> getProductsFromProductService() {
        List<Map<String, Object>> response = restTemplate.getForObject(PRODUCT_SERVICE_BASE_URL, List.class);
        return response;
    }

    // Fallback pour getProductsFromProductService
    public List<Map<String, Object>> fallbackGetProducts(Throwable e) {
        System.out.println("FALLBACK getProducts activé: " + e.getMessage());

        Map<String, Object> fallbackProduct = new HashMap<>();
        fallbackProduct.put("id", -1);
        fallbackProduct.put("name", "Produit service indisponible (port " + serverPort + ")");
        fallbackProduct.put("price", 0.0);

        return Arrays.asList(fallbackProduct);
    }

    @GetMapping("/{id}")
    @Retry(name = "myRetry", fallbackMethod = "fallbackGetUserById")
    @CircuitBreaker(name = "usermicroService", fallbackMethod = "fallbackGetUserById")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userList.stream()
                .filter(u -> u.getId().equals(id))
                .findFirst()
                .map(u -> ResponseEntity.ok(copyUserWithPort(u)))
                .orElse(ResponseEntity.notFound().build());
    }

    // Fallback pour getUserById
    public ResponseEntity<User> fallbackGetUserById(Long id, Throwable e) {
        System.out.println("FALLBACK getUserById activé pour id=" + id + ": " + e.getMessage());

        User fallbackUser = new User();
        fallbackUser.setId(id);
        fallbackUser.setName("Utilisateur non disponible (port " + serverPort + ")");
        fallbackUser.setEmail("fallback@example.com");

        return ResponseEntity.ok(fallbackUser);
    }

    @PostMapping("/create")
    @Retry(name = "myRetry", fallbackMethod = "fallbackCreateUser")
    @CircuitBreaker(name = "usermicroService", fallbackMethod = "fallbackCreateUser")
    public User createUser(@RequestBody User user) {
        userList.add(user);
        return copyUserWithPort(user);
    }

    // Fallback pour createUser
    public User fallbackCreateUser(User user, Throwable e) {
        System.out.println("FALLBACK createUser activé: " + e.getMessage());

        User fallbackUser = new User();
        fallbackUser.setId(-2L); // ID temporaire
        fallbackUser.setName(user.getName() + " (non sauvegardé - port " + serverPort + ")");
        fallbackUser.setEmail(user.getEmail());

        return fallbackUser;
    }

    @PutMapping("/{id}")
    @Retry(name = "myRetry", fallbackMethod = "fallbackUpdateUser")
    @CircuitBreaker(name = "usermicroService", fallbackMethod = "fallbackUpdateUser")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        for (int i = 0; i < userList.size(); i++) {
            if (userList.get(i).getId().equals(id)) {
                userList.set(i, user);
                return ResponseEntity.ok(copyUserWithPort(user));
            }
        }
        return ResponseEntity.notFound().build();
    }

    // Fallback pour updateUser
    public ResponseEntity<User> fallbackUpdateUser(Long id, User user, Throwable e) {
        System.out.println("FALLBACK updateUser activé pour id=" + id + ": " + e.getMessage());

        User fallbackUser = new User();
        fallbackUser.setId(id);
        fallbackUser.setName("Mise à jour échouée (port " + serverPort + ")");
        fallbackUser.setEmail("update-failed@example.com");

        return ResponseEntity.ok(fallbackUser);
    }

    @DeleteMapping("/{id}")
    @Retry(name = "myRetry", fallbackMethod = "fallbackDeleteUser")
    @CircuitBreaker(name = "usermicroService", fallbackMethod = "fallbackDeleteUser")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        boolean removed = userList.removeIf(u -> u.getId().equals(id));
        if (removed) return ResponseEntity.ok("Deleted user " + id + " on port " + serverPort);
        return ResponseEntity.notFound().build();
    }

    // Fallback pour deleteUser
    public ResponseEntity<String> fallbackDeleteUser(Long id, Throwable e) {
        System.out.println("FALLBACK deleteUser activé pour id=" + id + ": " + e.getMessage());

        return ResponseEntity.ok("Suppression échouée pour user " + id + " (port " + serverPort + ")");
    }

    // --- Méthode utilitaire pour ajouter le port à un User ---
    private User copyUserWithPort(User user) {
        User copy = new User();
        copy.setId(user.getId());
        copy.setName(user.getName() + " (port " + serverPort + ")");
        copy.setEmail(user.getEmail());
        return copy;
    }

    // --- Endpoint de test Resilience4j ---
    @GetMapping("/test/rate-limiter")
    @RateLimiter(name = "myRateLimiter", fallbackMethod = "testRateLimiterFallback")
    public String testRateLimiter() {
        return "Requête users acceptée à " + new Date() + " (port " + serverPort + ")";
    }

    public String testRateLimiterFallback(Throwable e) {
        return "⏸️ Rate limit users atteint ! Veuillez patienter. " + new Date() + " (port " + serverPort + ")";
    }

    @GetMapping("/test/retry")
    @Retry(name = "myRetry", fallbackMethod = "testRetryFallback")
    public String testRetry() {
        System.out.println("Tentative testRetry users à " + new Date());
        return "✅ Succès users après retry à " + new Date() + " (port " + serverPort + ")";
    }

    public String testRetryFallback(Throwable e) {
        return "❌ Échec users après 3 tentatives. Erreur: " + e.getMessage() + " (port " + serverPort + ")";
    }
}