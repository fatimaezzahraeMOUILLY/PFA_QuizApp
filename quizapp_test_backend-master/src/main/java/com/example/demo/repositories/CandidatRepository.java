package com.example.demo.repositories;

import com.example.demo.model.Condidats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CandidatRepository extends JpaRepository<Condidats, Long> {
    Optional<Condidats> findByEmail(String email);

    // Ajoutez cette méthode si elle est nécessaire
    List<Condidats> findByTestsId(Long testId);
}
