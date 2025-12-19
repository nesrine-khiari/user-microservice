package tn.fst.spring.usermicroservice.controllers;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.web.bind.annotation.*;
import tn.fst.spring.usermicroservice.cqrs.commands.CreateUserCommand;
import tn.fst.spring.usermicroservice.cqrs.queries.GetUserByIdQuery;
import tn.fst.spring.usermicroservice.entities.User;

import java.util.concurrent.CompletableFuture;
import java.util.UUID;

@RestController
@RequestMapping("/users/commands")
public class UserCommandController {

    private final CommandGateway commandGateway;
    private final QueryGateway queryGateway;

    public UserCommandController(CommandGateway commandGateway, QueryGateway queryGateway) {
        this.commandGateway = commandGateway;
        this.queryGateway = queryGateway;
    }

    // --- Création d'un utilisateur avec génération d'ID si null ---
    @PostMapping
    public CompletableFuture<Long> createUser(@RequestBody User user) {
        Long userId = (user.getId() != null) ? user.getId()
                : UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;

        CreateUserCommand command = new CreateUserCommand(
                userId,
                user.getName(),
                user.getEmail()
        );

        return commandGateway.send(command);
    }

    // --- Récupération d'un utilisateur par ID via QueryGateway ---
    @GetMapping("/by-id/{id}")
    public CompletableFuture<User> getUser(@PathVariable Long id) {
        return queryGateway.query(
                new GetUserByIdQuery(id),
                User.class
        );
    }
}
