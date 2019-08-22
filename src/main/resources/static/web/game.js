var grid = { "columnasTitulos": [1,2,3,4,5,6,7,8,9,10],
             "filasTitulos": ["A","B","C","D","E","F","G","H","I","J"],
             "rows": []
}

function createEle(filaLetra, colList){
    return colList.map(function(e){ return filaLetra+e})
}

function createObj(filaLetra, colList){
    var obj = {};
    obj.elements = createEle(filaLetra,colList);
    return obj;
}

function getHeadersHtml(data) {
  return "<tr><th></th>" + data.columnasTitulos.map(function(dest) {
    return "<th>" + dest + "</th>";
  }).join("") + "</tr>";
}

function renderHeaders(data, headerID) {
  var html = getHeadersHtml(data);
  document.getElementById(headerID).innerHTML = html;
}

function getColumnsHtml(row) {
  return row.elements.map(function(element) {
    return "<td id="+element+" class='inactive'>"+element+"</td>";
  }).join("")
}

function getRowsHtml(data) {
  return data.rows.map(function(row, i) {
    return "<tr><th>" + data.filasTitulos[i] + "</th>" +
      getColumnsHtml(row) + "</tr>";
    }).join("");
}

function renderRows(data, rowID) {
  var html = getRowsHtml(data);
  document.getElementById(rowID).innerHTML = html;
}

function renderTable(data, headerID, rowID) {
  renderHeaders(data, headerID);
  renderRows(data, rowID);
}

function paramObj(search) {
  var obj = {};
  var reg = /(?:[?&]([^?&#=]+)(?:=([^&#]*))?)(?:#.*)?/g;

  search.replace(reg, function(match, param, val) {
    obj[decodeURIComponent(param)] = val === undefined ? "" : decodeURIComponent(val);
  });

  return obj;
}

function renderGameInfo(player, opponent){
    var html = [];
    if(opponent.length>0){
        html.push("<p><strong>Currently Playing:</strong> "+player[0].player.email + "  vs  " + opponent[0].player.email +"</p>"+
                  "<p><strong>Player:</strong> "+player[0].player.email+"</p>"+
                  "<p><strong>Opponent:</strong> "+opponent[0].player.email+"</p>");
    }else{
        html.push("<p><strong>Currently Playing:</strong> "+player[0].player.email + "  vs  No opponent yet.</p>"+
                          "<p><strong>Player:</strong> "+player[0].player.email+"</p>"+
                          "<p><strong>Opponent:</strong> No opponent yet.");
    }
    document.getElementById("gameinfo").innerHTML = html.join("");
}

function signOut(){
    $.post("/api/logout")
    .done(function() {
            alert("logged out");
            window.location.href='/web/games.html';
            });
}

function getLocation(shipId){
    var filas = ["A","B","C","D","E","F","G","H","I","J"];
    var location = [];
    var x = parseInt($(shipId).attr("data-gs-x"))+1;
    var y = parseInt($(shipId).attr("data-gs-y"));
    var altura = $(shipId).attr("data-gs-height");
    var base = $(shipId).attr("data-gs-width");
    if(altura == 1){
        for(var i = 0; i<base; i++){
            location.push(filas[y]+(x+i));
        }
    }else{
        for(var i=0; i<altura; i++){
            location.push(filas[y+i]+x);
        }
    }
    return location
}

function addShips(){
    var alerta = confirm("Are you sure you want to place the ships?")
    if(alerta == true){
        var queryStrg = paramObj(window.location.href);
        var gamePlayerId = queryStrg.gp;
        $.post({
          url: "/api/games/players/"+gamePlayerId+"/ships",
          data: JSON.stringify([
                          {type: "Carrier", locations: getLocation("#Carrier")},
                          {type: "Battleship", locations: getLocation("#Battleship")},
                          {type: "Submarine", locations: getLocation("#Submarine")},
                          {type: "Destroyer", locations: getLocation("#Destroyer")},
                          {type: "Patroal", locations: getLocation("#Patroal")}
                          ]),
          dataType: "text",
          contentType: "application/json"
        })
        .done(function (response) {
          alert("Ships were saved");
          location.reload();
        })
        .error(function (error) {
          alert(error.responseText);
        })
    }
}

function renderSalvo(salvoes){
    //Renderiza salvoes de turnos anteriores
    var locations = salvoes.map(function(salvo){ return salvo.locations.map(function(location){ return "#"+location})});
    $(locations.join(",")).addClass("active");
}

function selectSalvo(){
    //Selecciona los salvos a disparar
    $("td").click(function(){
        var shots = parseInt($("#shots").text());
        if($(this).attr("class")=="inactive" && shots > 0){
            $(this).removeClass("inactive");
            $(this).addClass("active");
            shots -= 1;
        }
        else if($(this).attr("class")=="active"){
            $(this).removeClass("active");
            $(this).addClass("inactive");
            shots += 1;
        }
        $("#shots").text(shots);
    });
}

function getSalvoLocation(){
    //Genera un array con las locations de los salvos seleccionados
    var salvoes = $("td.active").toArray();
    var salvoesToSave = salvoes.filter(function(salvo){return salvo.className == "active"});
    return salvoesToSave.map(function(salvo){return salvo.id});
}

function addSalvo(){
    if(parseInt($("#shots").text())==0){
        var queryStrg = paramObj(window.location.href);
        var gamePlayerId = queryStrg.gp;
        $.post({
          url: "/api/games/players/"+gamePlayerId+"/salvos",
          data: JSON.stringify(
                          {turn: 0, locations: getSalvoLocation()}
                          ),
          dataType: "text",
          contentType: "application/json"
        })
        .done(function (response) {
          alert("You have shot.");
          location.reload();
        })
        .error(function (error) {
          alert(error.responseText);
        })
    }else{
        alert("You have not made all the shots.");
    }
}

function createObjHits(data){
    var ships = {"Carrier": 5,
                 "Battleship": 4,
                 "Submarine": 3,
                 "Destroyer": 3,
                 "Patroal": 2
                };
    var html = [];
    for(var i=0; i<data.length; i++){
        if(data[i].hit.length > 0){
            var obj = {"turno":data[i].turn}
            for(var j=0; j<data[i].hit.length; j++){
                if(data[i].hit[j].hits.length > 0){
                    ships[data[i].hit[j].ship] -= data[i].hit[j].hits.length;
                    if(!obj[data[i].hit[j].ship]){
                        obj[data[i].hit[j].ship]= {"hits": data[i].hit[j].hits.map(() => "*").join(""),
                                                   "left": ships[data[i].hit[j].ship],
                                                   "sink": (ships[data[i].hit[j].ship] == 0)?true:false
                                                   }
                    }else{
                        obj[data[i].hit[j].ship]["hits"] += data[i].hit[j].hits.map(() => "*").join("");
                        obj[data[i].hit[j].ship]["left"] = ships[data[i].hit[j].ship];
                        obj[data[i].hit[j].ship]["sink"] = (ships[data[i].hit[j].ship] == 0)?true:false;
                    }
                }
            }
        html.push(obj);
        }
    }
    return html;
}

function renderHits(obj, tableId){
    var html = "";
    for(var i=0; i<obj.length; i++){
        html += (!obj[i]["Destroyer"])?"":"<tr><td>"+obj[i].turno+"</td><td>Destroyer "+obj[i].Destroyer.hits+((obj[i].Destroyer.sink)?" Sunk!":"")+"</td><td>"+obj[i].Destroyer.left+"</td></tr>";
        html += (!obj[i]["Carrier"])?"":"<tr><td>"+obj[i].turno+"</td><td>Carrier "+obj[i].Carrier.hits+((obj[i].Carrier.sink)?" Sunk!":"")+"</td><td>"+obj[i].Carrier.left+"</td></tr>";
        html += (!obj[i]["Submarine"])?"":"<tr><td>"+obj[i].turno+"</td><td>Submarine "+obj[i].Submarine.hits+((obj[i].Submarine.sink)?" Sunk!":"")+"</td><td>"+obj[i].Submarine.left+"</td></tr>";
        html += (!obj[i]["Battleship"])?"":"<tr><td>"+obj[i].turno+"</td><td>Battleship "+obj[i].Battleship.hits+((obj[i].Battleship.sink)?" Sunk!":"")+"</td><td>"+obj[i].Battleship.left+"</td></tr>";
        html += (!obj[i]["Patroal"])?"":"<tr><td>"+obj[i].turno+"</td><td>Patroal "+obj[i].Patroal.hits+((obj[i].Patroal.sink)?" Sunk!":"")+"</td><td>"+obj[i].Patroal.left+"</td></tr>";
    }
    document.getElementById(tableId).innerHTML = html;
}

function renderSunkShips(ships, placeID){
    html = ships.map(function(ship){return "<tr><td>"+ship+"</td></tr>"});
    document.getElementById(placeID).innerHTML = html.join("");
}

for(var i=0; i<grid.filasTitulos.length; i++){
    grid.rows.push(createObj(grid.filasTitulos[i],grid.columnasTitulos));
}

renderTable(grid, "table-headers-salvo", "table-rows-salvo")

var queryStrg = paramObj(window.location.href);

var gamePlayerId = queryStrg.gp;

var games;

var player;

var opponent;

$("#waiting").hide()

$(function() {

    fetch("/api/game_view/"+gamePlayerId)
    .then(function(response){
        if(response.ok){
            return response.json();
            }
        })
        .then(function(response){

            games = response;

            if(games.gameStatus != "PLACE_SHIPS"){
                placeShips(games.ships, true);
                $("#add-ships,#add-ships-tittle").hide();

                if(games.gameStatus == "WAIT"){
                    $("#waiting").show();
                    $("#shoot-salvo,#shots-left").hide();
                    setInterval(function(){location.reload();},10000);
                }else if(games.gameStatus == "SHOOT"){
                    $("#shots").text(games.ships.length - ((games.sunkShipsPlayer==null)?0:games.sunkShipsPlayer.length));
                    selectSalvo();
                }else{
                    alert("You have "+games.gameStatus);
                    window.location.href='/web/games.html';
                }

                player = games.gamePlayers.filter(function(array){ return array.id == this},gamePlayerId);
                opponent = games.gamePlayers.filter(function(array){ return array.id != this},gamePlayerId);

                renderGameInfo(player, opponent);

                var playerSalvoes = games.salvoes.filter(function(salvo){return salvo.player == this},player[0].player.id);

                renderSalvo(playerSalvoes);

                var hitPlayer = createObjHits(games.hitsOnPlayer);
                var hitOppo = createObjHits(games.hitsOnOpponen);

                renderHits(hitPlayer, "hit-player");
                renderHits(hitOppo, "hit-opponent");

                renderSunkShips(games.sunkShipsPlayer, "player-sunk-ships");
                renderSunkShips(games.sunkShipsOpponent, "opponent-sunk-ships");

            }else{
                $("#salvo-column,#hits-sinks").hide();
                placeShips([
                             {type: "Carrier", Locations: ["A1","A2","A3","A4","A5"]},
                             {type: "Battleship", Locations: ["B1","B2","B3","B4"]},
                             {type: "Submarine", Locations: ["C1","C2","C3"]},
                             {type: "Destroyer", Locations: ["D1","D2","D3"]},
                             {type: "Patroal", Locations: ["E1","E2"]}
                             ], false);
                addEvents();
            }

        })
    .catch(function (error) { console.log("Request failed: " + error.message); });
});

function placeShips(ships, permiteMover){
    initializerGrid(permiteMover);
    ships.forEach(function(ship){addWidget(ship)});
}

function addWidget(ship){
       var searchChar = ship.Locations[0].slice(0, 1);
       var secondChar = ship.Locations[1].slice(0, 1);
       if ( searchChar === secondChar ) {
           ship.position = "Horizontal";
       } else {
           ship.position = "Vertical";
       }
       for (var i=0; i < ship.Locations.length; i++) {
           ship.Locations[i] = ship.Locations[i].replace(/A/g, '0');
           ship.Locations[i] = ship.Locations[i].replace(/B/g, '1');
           ship.Locations[i] = ship.Locations[i].replace(/C/g, '2');
           ship.Locations[i] = ship.Locations[i].replace(/D/g, '3');
           ship.Locations[i] = ship.Locations[i].replace(/E/g, '4');
           ship.Locations[i] = ship.Locations[i].replace(/F/g, '5');
           ship.Locations[i] = ship.Locations[i].replace(/G/g, '6');
           ship.Locations[i] = ship.Locations[i].replace(/H/g, '7');
           ship.Locations[i] = ship.Locations[i].replace(/I/g, '8');
           ship.Locations[i] = ship.Locations[i].replace(/J/g, '9');
       }
       var yInGrid = parseInt(ship.Locations[0].slice(0, 1));
       var xInGrid = parseInt(ship.Locations[0].slice(1, 3)) - 1;


       if (ship.position === "Horizontal") {
             grid.addWidget($('<div id="'+ship.type+'"><div class="grid-stack-item-content '+ship.type+'Horizontal"></div><div/>'),
             xInGrid, yInGrid, ship.Locations.length, 1, false);
       } else if (ship.position === "Vertical") {
             grid.addWidget($('<div id="'+ship.type+'"><div class="grid-stack-item-content '+ship.type+'Vertical"></div><div/>'),
             xInGrid, yInGrid, 1, ship.Locations.length, false);
       }
}

function initializerGrid(permiteMover){
    var options = {
        //grilla de 10 x 10
        width: 10,
        height: 10,
        //separacion entre elementos (les llaman widgets)
        verticalMargin: 0,
        //altura de las celdas
        cellHeight: 40,
        //desabilitando el resize de los widgets
        disableResize: true,
        //widgets flotantes
        float: true,
        //removeTimeout: 100,
        //permite que el widget ocupe mas de una columna
        disableOneColumnMode: true,
        //false permite mover, true impide
        staticGrid: permiteMover,
        //activa animaciones (cuando se suelta el elemento se ve más suave la caida)
        animate: true
    }
    //se inicializa el grid con las opciones
    $('.grid-stack').gridstack(options);

    grid = $('#grid').data('gridstack');
}


function addEvents(){
     $("#Patroal,#Destroyer,#Submarine,#Carrier,#Battleship").click(function(){
     grid = $('#grid').data('gridstack');
     var h = parseInt($(this).attr("data-gs-height"));
     var w = parseInt($(this).attr("data-gs-width"));
     var posX = parseInt($(this).attr("data-gs-x"));
     var posY = parseInt($(this).attr("data-gs-y"));
     // Rotate Ships Mechanics...
     if (w>h) {
        if ( grid.isAreaEmpty(posX, posY+1, h, w-1) && posY+w<=10 ) {
            grid.update($(this), posX, posY, h, w);
            $(this).children('.grid-stack-item-content').removeClass($(this).attr('id')+"Horizontal");
            $(this).children('.grid-stack-item-content').addClass($(this).attr('id')+"Vertical");
        }
     }else {
        if ( grid.isAreaEmpty(posX+1, posY-1, w-1, h) && posX+h<=10 ) {
            grid.update($(this), posX, posY, h, w);
            $(this).children('.grid-stack-item-content').addClass($(this).attr('id')+"Horizontal");
            $(this).children('.grid-stack-item-content').removeClass($(this).attr('id')+"Vertical");
        }
     }
     });
}