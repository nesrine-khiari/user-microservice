package tn.fst.spring.usermicroservice.cqrs.projections;

import org.axonframework.eventhandling.EventHandler;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import tn.fst.spring.usermicroservice.cqrs.events.UserCreatedEvent;
import tn.fst.spring.usermicroservice.cqrs.queries.GetUserByIdQuery;
import tn.fst.spring.usermicroservice.entities.User;
import tn.fst.spring.usermicroservice.repositories.UserRepository;

@Component
public class UserProjection {

    @Autowired
    private UserRepository userRepository;

    @EventHandler
    public void on(UserCreatedEvent event) {
        User user = new User();
        user.setId(event.getId());
        user.setName(event.getName());
        user.setEmail(event.getEmail());
        userRepository.save(user);
    }

    @QueryHandler
    public User handle(GetUserByIdQuery query) {
        return userRepository.findById(query.getId()).orElse(null);
    }
}
