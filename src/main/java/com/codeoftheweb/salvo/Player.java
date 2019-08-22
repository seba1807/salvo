package com.codeoftheweb.salvo;

import org.hibernate.annotations.GenericGenerator;
import javax.persistence.*;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.stream.Collectors.toList;


@Entity
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private long id;

    private String userName;

    private String password;

    @OneToMany(mappedBy="player", fetch=FetchType.EAGER)
    Set<GamePlayer> gameplayer;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @OneToMany(mappedBy="player", fetch=FetchType.EAGER)
    Set<Score> scores;

    public Player( ){ }

    public Player(String user, String password){
        this.userName = user;
        this.password = password;
    }

    public String getUserName(){
        return userName;
    }

    public long getId() { return id; }

    public void addGamePlayer(GamePlayer gamePlayer) {
        gamePlayer.setPlayer(this);
        gameplayer.add(gamePlayer);
    }

    public void addScore(Score score) {
        score.setPlayer(this);
        scores.add(score);
    }

    public List<Game> getGames() {
        return gameplayer.stream().map(sub -> sub.getGame()).collect(toList());
    }

    public Set<GamePlayer> getGameplayer() {
        return gameplayer;
    }

    public Set<Score> getScores() {
        return scores;
    }

    public Score getScore(Game game){
        return this.scores.stream().filter(gs -> gs.getGame().getId() == game.getId()).findFirst().orElse(null);
    }
}
