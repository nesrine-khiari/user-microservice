package tn.fst.spring.usermicroservice.cqrs.projections;

import org.axonframework.queryhandling.QueryHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import tn.fst.spring.usermicroservice.cqrs.queries.GetUserByIdQuery;
import tn.fst.spring.usermicroservice.entities.User;
import tn.fst.spring.usermicroservice.repositories.UserRepository;

@Component
public class UserQueryHandler {

    @Autowired
    private UserRepository userRepository;

    @QueryHandler
    public User handle(GetUserByIdQuery query) {
        return userRepository.findById(query.getId())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }
}
