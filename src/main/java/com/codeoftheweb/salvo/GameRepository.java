package com.codeoftheweb.salvo;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<Game, Long>{
    List<Game> findBycreationDate(LocalDateTime creationDate);
}