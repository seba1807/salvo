package com.codeoftheweb.salvo;


        import org.springframework.beans.factory.annotation.Autowired;
        import org.springframework.http.HttpStatus;
        import org.springframework.http.ResponseEntity;
        import org.springframework.security.authentication.AnonymousAuthenticationToken;
        import org.springframework.security.core.Authentication;
        import org.springframework.security.crypto.password.PasswordEncoder;
        import org.springframework.web.bind.annotation.*;

        import java.time.LocalDateTime;
        import java.util.*;
        import java.util.stream.Collectors;


@RestController
@RequestMapping("/api")
public class SalvoController {

    @Autowired
    private  GameRepository gamerepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    GamePlayerRepository gamePlayerRepository;

    @Autowired
    ScoreRepository scoreRepository;

    @RequestMapping("/games")
    public Map<String, Object> getAllGames(Authentication authentication){
        Map<String, Object> playerGames = new LinkedHashMap<String, Object>();
        List<Game> games =  gamerepository.findAll();
        playerGames.put("player",(isGuest(authentication))?"guest":makeAuthPlayerDTO(playerRepository.findByUserName(authentication.getName())));
        playerGames.put("games",games.stream().map(b -> makeGameDTO(b)).collect(Collectors.toList()));
        return playerGames;
    }

    @GetMapping("/game_view/{GamePlayerId}")
    public ResponseEntity<Map<String, Object>> GamePlayerView(@PathVariable long GamePlayerId, Authentication authentication){
        Optional<GamePlayer> gamePlayer = gamePlayerRepository.findById(GamePlayerId);
        if(authentication.getName() == gamePlayer.get().getPlayer().getUserName()){
            return new ResponseEntity<>(makeGameDTOView(gamePlayer.get()), HttpStatus.OK);
        }else {
            return new ResponseEntity<>(makeMap(Const.KEY_ERROR, Const.MSG_ERROR_UNAUTHORIZED), HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping("/players")
    public ResponseEntity<Map<String,Object>> postNewPlayer(@RequestParam String username, @RequestParam String password){
        if (username.isEmpty() || password.isEmpty()) {
            return new ResponseEntity<>(makeMap(Const.KEY_ERROR, Const.MSG_ERROR_NOUSER), HttpStatus.FORBIDDEN);
        }
        Player player = playerRepository.findByUserName(username);
        if(player != null){
            return new ResponseEntity<>(makeMap(Const.KEY_ERROR, Const.MSG_ERROR_USEREXIST_CONFLICT), HttpStatus.CONFLICT);
        }
        Player newPlayer = playerRepository.save(new Player(username, passwordEncoder.encode(password)));
        return new ResponseEntity<>(makeMap("id", newPlayer.getId()), HttpStatus.CREATED);
    }

    @PostMapping("/games")
    public ResponseEntity<Map<String,Object>> createNewGame(Authentication authentication){
        if (isGuest(authentication)) {
            return new ResponseEntity<>(makeMap(Const.KEY_ERROR, Const.MSG_ERROR_NOLOGGING), HttpStatus.UNAUTHORIZED);
        }
        Player currentPlayer = playerRepository.findByUserName(authentication.getName());
        Game newGame = gamerepository.save(new Game(LocalDateTime.now()));
        GamePlayer newGamePlayer = gamePlayerRepository.save(new GamePlayer(currentPlayer, newGame,LocalDateTime.now()));
        return new ResponseEntity<>(makeMap("gpid", newGamePlayer.getId()), HttpStatus.CREATED);
    }

    @PostMapping("/game/{GameId}/players")
    public ResponseEntity<Map<String,Object>> joinGame(@PathVariable long GameId, Authentication authentication){
        if(isGuest(authentication)){
            return new ResponseEntity<>(makeMap(Const.KEY_ERROR, Const.MSG_ERROR_NOLOGGING), HttpStatus.UNAUTHORIZED);
        }
        Optional<Game> game = gamerepository.findById(GameId);
        if(!game.isPresent()){
            return new ResponseEntity<>(makeMap(Const.KEY_ERROR, Const.MSG_ERROR_GAME), HttpStatus.FORBIDDEN);
        }
        if(game.get().getPlayers().size() == 2){
            return new ResponseEntity<>(makeMap(Const.KEY_ERROR, Const.MSG_ERROR_GAMEFULL), HttpStatus.FORBIDDEN);
        }
        Player currentPlayer = playerRepository.findByUserName(authentication.getName());
        GamePlayer newGamePlayer = gamePlayerRepository.save(new GamePlayer(currentPlayer, game.get(),LocalDateTime.now()));
        return new ResponseEntity<>(makeMap("gpid", newGamePlayer.getId()), HttpStatus.CREATED);
    }

    @PostMapping("/games/players/{gamePlayerId}/ships")
    public ResponseEntity<Map<String,Object>> createShip(@PathVariable long gamePlayerId, Authentication authentication, @RequestBody List<Ship> ships){
        Optional<GamePlayer> gamePlayer = gamePlayerRepository.findById(gamePlayerId);
        if(isGuest(authentication)){
            return new ResponseEntity<>(makeMap(Const.KEY_ERROR, Const.MSG_ERROR_NOLOGGING), HttpStatus.UNAUTHORIZED);
        }
        if(!gamePlayer.isPresent()){
            return new ResponseEntity<>(makeMap(Const.KEY_ERROR, Const.MSG_ERROR_GAMEPLAYER), HttpStatus.FORBIDDEN);
        }
        if(authentication.getName() != gamePlayer.get().getPlayer().getUserName()) {
            return new ResponseEntity<>(makeMap(Const.KEY_ERROR, Const.MSG_ERROR_UNAUTHORIZED), HttpStatus.UNAUTHORIZED);
        }
        if(gamePlayer.get().getShips().size() != 0) {
            return new ResponseEntity<>(makeMap(Const.KEY_ERROR, Const.MSG_ERROR_ALREADYSHIPS), HttpStatus.FORBIDDEN);
        }
        gamePlayer.get().addShips(ships);
        gamePlayerRepository.save(gamePlayer.get());
        return new ResponseEntity<>(makeMap(Const.KEY_CREATED, Const.MSG_CREATED_SHIPSAVED), HttpStatus.CREATED);
    }

    @PostMapping("/games/players/{gamePlayerId}/salvos")
    public ResponseEntity<Map<String,Object>> createSalvo(@PathVariable long gamePlayerId, Authentication authentication, @RequestBody Salvo salvo){
        Optional<GamePlayer> gamePlayer = gamePlayerRepository.findById(gamePlayerId);
        if(isGuest(authentication)){
            return new ResponseEntity<>(makeMap(Const.KEY_ERROR, Const.MSG_ERROR_NOLOGGING), HttpStatus.UNAUTHORIZED);
        }
        if(!gamePlayer.isPresent()){
            return new ResponseEntity<>(makeMap(Const.KEY_ERROR, Const.MSG_ERROR_GAMEPLAYER), HttpStatus.FORBIDDEN);
        }
        if(authentication.getName() != gamePlayer.get().getPlayer().getUserName()) {
            return new ResponseEntity<>(makeMap(Const.KEY_ERROR, Const.MSG_ERROR_UNAUTHORIZED), HttpStatus.UNAUTHORIZED);
        }
        if(gamePlayer.get().getGameStatus()==GameStatus.LOST||gamePlayer.get().getGameStatus()==GameStatus.WON||gamePlayer.get().getGameStatus()==GameStatus.TIED){
            return new ResponseEntity<>(makeMap(Const.KEY_ERROR, Const.MSG_ERROR_SALVOFINISHED), HttpStatus.FORBIDDEN);
        }
        salvo.setTurn(gamePlayer.get().getSalvos().size()+1);
        if(gamePlayer.get().getOpponent()!=null) {
            if (salvo.getTurn() - gamePlayer.get().getOpponent().getSalvos().size() > 1 || salvo.getTurn() - gamePlayer.get().getOpponent().getSalvos().size() < -1) {
                return new ResponseEntity<>(makeMap(Const.KEY_ERROR, Const.MSG_ERROR_SALVOTURN), HttpStatus.FORBIDDEN);
            }
        }
        gamePlayer.get().addSalvo(salvo);
        gamePlayerRepository.save(gamePlayer.get());
        if(gamePlayer.get().getGameStatus()==GameStatus.LOST||gamePlayer.get().getGameStatus()==GameStatus.WON||gamePlayer.get().getGameStatus()==GameStatus.TIED){
            Score scorePlayer = new Score();
            scorePlayer.setGame(gamePlayer.get().getGame());
            scorePlayer.setPlayer(gamePlayer.get().getPlayer());
            scorePlayer.setFinishDate(LocalDateTime.now());
            Score scoreOpponent = new Score();
            scoreOpponent.setGame(gamePlayer.get().getGame());
            scoreOpponent.setPlayer(gamePlayer.get().getOpponent().getPlayer());
            scoreOpponent.setFinishDate(LocalDateTime.now());
            if(gamePlayer.get().getGameStatus()==GameStatus.LOST){
                scorePlayer.setScore(0.0);
                scoreOpponent.setScore(1.0);
            }
            else if(gamePlayer.get().getGameStatus()==GameStatus.WON){
                scorePlayer.setScore(1.0);
                scoreOpponent.setScore(0.0);
            }
            else {
                scorePlayer.setScore(0.5);
                scoreOpponent.setScore(0.5);
            }
            scoreRepository.save(scorePlayer);
            scoreRepository.save(scoreOpponent);
        }
        return new ResponseEntity<>(makeMap(Const.KEY_CREATED, Const.MSG_CREATED_SALVOSAVED), HttpStatus.CREATED);
    }

    private Map<String, Object> makeScoreDTO(Score score){
        if (score == null){
            return null;
        }
        Map<String, Object> scores = new LinkedHashMap<String, Object>();
        scores.put("id",score.getId());
        scores.put("score",score.getScore());
        scores.put("finishDate",score.getFinishDate());
        return scores;
    }

    private Map<String, Object> makePlayerDTO(Player player, Score score){
        Map<String, Object> players = new LinkedHashMap<String, Object>();
        players.put("id", player.getId());
        players.put("email", player.getUserName());
        players.put("score", makeScoreDTO(score));
        return players;
    }

    private Map<String, Object> makeAuthPlayerDTO(Player player){
        Map<String, Object> players = new LinkedHashMap<String, Object>();
        players.put("id", player.getId());
        players.put("name", player.getUserName());
        return players;
    }

    private Map<String, Object> makeGamePlayerDTO(GamePlayer gamePlayer){
        Map<String, Object> gPlayers = new LinkedHashMap<String, Object>();
        gPlayers.put("id", gamePlayer.getId());
        gPlayers.put("player", makePlayerDTO(gamePlayer.getPlayer(), gamePlayer.getScore()));
        return gPlayers;
    }

    private Map<String, Object> makeGameDTO(Game game){
        Map<String, Object> games = new LinkedHashMap<String, Object>();
        games.put("id", game.getId());
        games.put("created", game.getCreationDate());
        games.put("gamePlayers", game.getGameplayer().stream().map(b -> makeGamePlayerDTO(b)).collect(Collectors.toList()));
        return games;
    }

    private boolean isGuest(Authentication authentication) {
        return authentication == null || authentication instanceof AnonymousAuthenticationToken;
    }

    private Map<String, Object> makeShipDTO(Ship ship){
        Map<String, Object> ships = new LinkedHashMap<String, Object>();
        ships.put("type", ship.getType());
        ships.put("Locations", ship.getLocations());
        return ships;
    }

    private Map<String, Object> makeSalvoDTO(Salvo salvo){
        Map<String, Object> salvoes = new LinkedHashMap<String, Object>();
        salvoes.put("turn", salvo.getTurn());
        salvoes.put("player", salvo.getGamePlayer().getPlayer().getId());
        salvoes.put("locations", salvo.getLocations());
        return salvoes;
    }

    private Map<String, Object> makeGameDTOView(GamePlayer gamePlayer){
        Map<String, Object> view = makeGameDTO(gamePlayer.getGame());
        view.put("ships", gamePlayer.getShips().stream().map(this::makeShipDTO));
        view.put("salvoes", gamePlayer.getGame().getGameplayer().stream().flatMap(gp -> gp.getSalvos().stream().map(this::makeSalvoDTO)));
        view.put("hitsOnPlayer", (gamePlayer.getOpponent()==null)?null:gamePlayer.getOpponent().getSalvos().stream().map(b->b.getHitsMade()));
        view.put("hitsOnOpponen", gamePlayer.getSalvos().stream().map(b->b.getHitsMade()));
        view.put("sunkShipsPlayer",(gamePlayer.getSunkShips()==null)?null:gamePlayer.getSunkShips().stream().map(ship->ship.getType()).collect(Collectors.toList()));
        view.put("sunkShipsOpponent",(gamePlayer.getOpponent()==null)?null:gamePlayer.getOpponent().getSunkShips().stream().map(ship->ship.getType()).collect(Collectors.toList()));
        view.put("gameStatus", gamePlayer.getGameStatus());
        return view;
    }

    private Map<String, Object> makeMap(String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

}
