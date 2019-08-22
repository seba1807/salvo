package com.codeoftheweb.salvo;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.*;

@Entity
public class Salvo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private long id;

    private int turn;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="gamePlayerId")
    private GamePlayer gamePlayer;

    @ElementCollection
    private List<String> locations = new ArrayList<>();

    public Salvo(){ }

    public Salvo(int turn, List<String> locations){
        this.turn = turn;
        this.locations = locations;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getTurn() {
        return turn;
    }

    public void setTurn(int turn) {
        this.turn = turn;
    }

    public GamePlayer getGamePlayer() {
        return gamePlayer;
    }

    public void setGamePlayer(GamePlayer gamePlayer) {
        this.gamePlayer = gamePlayer;
    }

    public List<String> getLocations() {
        return locations;
    }

    public void setLocations(List<String> locations) {
        this.locations = locations;
    }

    public Map<String, Object> getHitsMade(){
        Map<String, Object> hits = new LinkedHashMap<String, Object>();
        hits.put("turn", this.getTurn());
        hits.put("hit", (this.getGamePlayer().getOpponent()==null)?null:this.getGamePlayer().getOpponent().getShips().stream().map(b->hitShip(this, b)));
        return hits;
    }

    private Map<String, Object> hitShip(Salvo salvo, Ship ship) {
        Map<String, Object> hitShip = new LinkedHashMap<String, Object>();
        hitShip.put("ship", ship.getType());
        hitShip.put("hits", salvo.getLocations().stream().filter(b -> ship.getLocations().indexOf(b) != -1));
        return hitShip;
    }
}
