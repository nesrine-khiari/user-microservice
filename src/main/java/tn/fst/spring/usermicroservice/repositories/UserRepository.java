package tn.fst.spring.usermicroservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.fst.spring.usermicroservice.entities.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
}
