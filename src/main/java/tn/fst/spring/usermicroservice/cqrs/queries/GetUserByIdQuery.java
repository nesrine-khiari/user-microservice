package tn.fst.spring.usermicroservice.cqrs.queries;

public class GetUserByIdQuery {

    private final Long id;

    public GetUserByIdQuery(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
