package com.codeoftheweb.salvo;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;

@Entity
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private long id;

    private LocalDateTime creationDate;

    @OneToMany(mappedBy="game", fetch=FetchType.EAGER)
    Set<GamePlayer> gameplayer;

    @OneToMany(mappedBy="game", fetch=FetchType.EAGER)
    Set<Score> scores;

    public Game() { }

    public Game(LocalDateTime date){
        this.creationDate = date;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public long getId() { return id; }

    public void addGamePlayer(GamePlayer gamePlayer) {
        gamePlayer.setGame(this);
        gameplayer.add(gamePlayer);
    }

    public void addScore(Score score) {
        score.setGame(this);
        scores.add(score);
    }

    public List<Player> getPlayers() {
        return gameplayer.stream().map(sub -> sub.getPlayer()).collect(toList());
    }

    public Set<GamePlayer> getGameplayer() {
        return gameplayer;
    }

    public Set<Score> getScores() {
        return scores;
    }
}