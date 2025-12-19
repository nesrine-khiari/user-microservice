package tn.fst.spring.usermicroservice.cqrs.aggregates;

import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

import tn.fst.spring.usermicroservice.cqrs.commands.CreateUserCommand;
import tn.fst.spring.usermicroservice.cqrs.events.UserCreatedEvent;

@Aggregate
public class UserAggregate {

    @AggregateIdentifier
    private Long id;
    private String name;
    private String email;

    protected UserAggregate() {}

    @CommandHandler
    public UserAggregate(CreateUserCommand command) {

        // ✅ INITIALISATION DE L’IDENTIFIANT AVANT apply()
        this.id = command.getId();

        apply(new UserCreatedEvent(
                command.getId(),
                command.getName(),
                command.getEmail()
        ));
    }

    @EventSourcingHandler
    public void on(UserCreatedEvent event) {
        this.id = event.getId();
        this.name = event.getName();
        this.email = event.getEmail();
    }
}
