function createPlayerPosObject(data){
    var position = [];
    for(var i=0; i<data.length; i++){
        for(var j=0; j<data[i].gamePlayers.length; j++){
            var auxOb = position.find(function(player){return player["name"] == this}, data[i].gamePlayers[j].player.email);
            if(auxOb == undefined && data[i].gamePlayers[j].player.score){
                var playerObj = {"name": data[i].gamePlayers[j].player.email,
                                 "total": data[i].gamePlayers[j].player.score.score,
                                 "won": (data[i].gamePlayers[j].player.score.score == 1)? 1:0,
                                 "lost": (data[i].gamePlayers[j].player.score.score == 0)? 1:0,
                                 "tied": (data[i].gamePlayers[j].player.score.score == 0.5)? 1:0
                };
                position.push(playerObj);
            }else if(!data[i].gamePlayers[j].player.score){
                var playerObj = {"name": data[i].gamePlayers[j].player.email,
                                 "total": 0,
                                 "won": 0,
                                 "lost": 0,
                                 "tied": 0
                };
                position.push(playerObj);
            }
            else{
                var indice = position.indexOf(auxOb);
                auxOb["total"] += data[i].gamePlayers[j].player.score.score;
                auxOb["won"] += (data[i].gamePlayers[j].player.score.score == 1)? 1:0;
                auxOb["lost"] += (data[i].gamePlayers[j].player.score.score == 0)? 1:0;
                auxOb["tied"] += (data[i].gamePlayers[j].player.score.score == 0.5)? 1:0;
                position[indice] = auxOb;
            }
        }
    }
    return position;
}

function renderPosTable(positionObjArray){
    var html = positionObjArray.map(function(obj){return "<tr><td>"+obj.name+"</td><td>"+obj.total+"</td><td>"+
                                                  obj.won+"</td><td>"+obj.lost+"</td><td>"+obj.tied+"</td><td></tr>"});
    document.getElementById("tabla").innerHTML = html.join("");
}

function signIn(){
    var user = $("#userName").val();
    var pass = $("#password").val();
     $.post("/api/login", { userName: user, password: pass })
     .done(function() {
             alert("logged in!");
             location.reload();
             })
     .error(function (error) {
             alert("Error! try again or create new user.");
             location.reload();
             });
 }

function signUp(){
    var user = $("#userName").val();
    var pass = $("#password").val();
    $.post("/api/players", { username: user, password: pass })
    .done(function() {
            alert("New Player Created!!");
            $.post("/api/login", { userName: user, password: pass })
                .done(function() {
                    alert("logged in!");
                    location.reload();
                    });
            })
    .error(function (error) {
            alert(error.responseJSON.error);
            location.reload();
            });
}

function signOut(){
    $.post("/api/logout")
    .done(function() {
            alert("logged out");
            location.reload();
            });
}

function renderWelcomeMessage(data){
    var player = (data == "guest")? "Welcome! Please sign in or create new user.":"Welcome "+data.name;
    if(data != "guest"){
        $("#login").hide();
        $("#logout").show();
    }
    document.getElementById("welcome").innerHTML = "<p>"+player+"</p>";
}

function renderGames(games){
    var html = games.map(function(game){return "<div id='game"+game.id+"'><h3>Game "+game.id+"</h3></div>"});
    html.push("<p>Please login to join the games or:</p>")
    document.getElementById("game-list").innerHTML = html.join("");
}

function renderJoinGames(data){
    if(data.player == "guest"){
        renderGames(data.games);
    }else{
        var html = "";
        for(var i=0; i<data.games.length; i++){
            html += "<div id='game"+data.games[i].id+"'><h3>Game "+data.games[i].id+"</h3>";
            html += "<button type='button' data-target='"+data.games[i].id+"' onclick=joinGame(event)>Join Game</button>";
            for(var j=0; j<data.games[i].gamePlayers.length; j++){
                if(data.games[i].gamePlayers[j].player.id == data.player.id&&data.games[i].gamePlayers[j].player){
                    html += "<a href=/web/game.html?gp="+data.games[i].gamePlayers[j].id+" class='loggedinUser'>Return to Game</a>";
                }
            }
            html += "</div>";
        }
        document.getElementById("game-list").innerHTML = html;
        $(".loggedinUser").parent().find("button").hide();
    }
}

function createGame(){
    $.post("/api/games")
        .done(function(response) {
                alert("New Game Created! Redirecting...");
                var gamePlayerId = response.gpid;
                window.location.href="/web/game.html?gp="+gamePlayerId;
                }
        )
        .error(function (error) {
                alert(error.responseJSON.error);
        });
}

function joinGame(event){
    $.post("/api/game/"+event.target.dataset.target+"/players")
     .done(function(response) {
            alert("You joined the game! Redirecting...");
            var gamePlayerId = response.gpid;
            window.location.href="/web/game.html?gp="+gamePlayerId;
            })
    .error(function (error) {
            alert(error.responseJSON.error);
            });
}

$(function () {

    $("#logout").hide();

    var games;

    var players;

    $(function() {

        fetch("/api/games")
        .then(function(response){
            if(response.ok){
                return response.json();
                }
            })
            .then(function(response){
                games = response;
                players = createPlayerPosObject(games.games);
                renderWelcomeMessage(games.player);
                renderJoinGames(games);
                renderPosTable(players);
                })
        .catch(function (error) { console.log("Request failed: " + error.message); });
    });

}
);