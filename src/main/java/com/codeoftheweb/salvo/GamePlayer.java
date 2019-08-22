package com.codeoftheweb.salvo;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
public class GamePlayer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private long id;

    private LocalDateTime joinDate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="playerId")
    private Player player;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="gameId")
    private Game game;

    @OneToMany(mappedBy="gamePlayer", fetch=FetchType.EAGER, cascade = CascadeType.ALL)
    Set<Ship> ships = new HashSet<>();

    @OneToMany(mappedBy="gamePlayer", fetch=FetchType.EAGER, cascade = CascadeType.ALL)
    Set<Salvo> salvos = new HashSet<>();

    public GamePlayer(){}

    public GamePlayer(Player player, Game game, LocalDateTime date){
        this.player = player;
        this.game = game;
        this.joinDate = date;
    }

    public long getId() { return id; }

    public LocalDateTime getJoinDate() {
        return joinDate;
    }

    public Player getPlayer() {
        return player;
    }

    public Game getGame() {
        return game;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public void addShip(Ship ship){
        ship.setGamePlayer(this);
        ships.add(ship);
    }

    public void addShips(List<Ship> ships){
        ships.stream().forEach(this::addShip);
    }

    public Set<Ship> getShips() {
        return ships;
    }

    public void addSalvo(Salvo salvo){
        salvo.setGamePlayer(this);
        salvos.add(salvo);
    }

    public Set<Salvo> getSalvos() {
        return salvos ;
    }

    public Score getScore(){
        return this.player.getScore(this.game);
    }

    public GamePlayer getOpponent(){
        return this.game.getGameplayer().stream().filter(b -> b.getId() != this.getId()).findFirst().orElse(null);
    }

    public List<Ship> getSunkShips(){
        if(this.getOpponent()==null) {
            return null;
        }
        int lastTurn = this.getSalvos().size();
        List<String> salvoLocations = this.getOpponent().getSalvos().stream().filter(salvo->salvo.getTurn()<=lastTurn).flatMap(salvo->salvo.getLocations().stream().map(location->location)).collect(Collectors.toList());
        return this.getShips().stream().filter(ship->salvoLocations.containsAll(ship.getLocations())).collect(Collectors.toList());
    }

    public GameStatus getGameStatus(){
        if(this.getShips().size()== 0){
            return GameStatus.PLACE_SHIPS;
        }
        if(this.getOpponent()==null){
            if(this.getSalvos().size()==0){
                return GameStatus.SHOOT;
            }else {
                return GameStatus.WAIT;
            }
        }
        if(this.getSalvos().size()>this.getOpponent().getSalvos().size()){
            return GameStatus.WAIT;
        }
        if(this.getSalvos().size()==this.getOpponent().getSalvos().size()&&this.getSalvos().size()>0){
            if(this.getOpponent().getSunkShips().size()==this.getOpponent().getShips().size()&&this.getSunkShips().size()==this.getShips().size()) {
                return GameStatus.TIED;
            }
            if(this.getSunkShips().size()==this.getShips().size()){
                return GameStatus.LOST;
            }
            if(this.getOpponent().getSunkShips().size()==this.getOpponent().getShips().size()) {
                return GameStatus.WON;
            }
        }
        return GameStatus.SHOOT;
    }
}

