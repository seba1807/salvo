package com.codeoftheweb.salvo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.Arrays;


@SpringBootApplication
public class SalvoApplication {

	@Autowired
	PasswordEncoder passwordEncoder;

	public static void main(String[] args) {
		SpringApplication.run(SalvoApplication.class, args);
	}


	@Bean
	public CommandLineRunner initData(PlayerRepository playerRepository, GameRepository gameRepository, GamePlayerRepository gamePlayerRepository, ScoreRepository scoreRepository){
		return (args) -> {

			Player player1 = new Player("j.bauer@ctu.gov",passwordEncoder.encode("password1"));
			Player player2 = new Player("c.obrian@ctu.gov", passwordEncoder.encode("password2"));
			Player player3 = new Player("t.almeida@ctu.gov", passwordEncoder.encode("password3"));
			Player player4 = new Player("d.palmer@whitehouse.gov", passwordEncoder.encode("password4"));

			Game game1 = new Game(LocalDateTime.now());
			Game game2 = new Game(LocalDateTime.now().plusHours(1));
			Game game3 = new Game(LocalDateTime.now().plusHours(2));

			Score score1 = new Score(game1, player1, 1.0, LocalDateTime.now());
			Score score2 = new Score(game1, player2, 0.0, LocalDateTime.now());

			GamePlayer gamePlayer1 = new GamePlayer(player1, game1,LocalDateTime.now().plusHours(1));
			GamePlayer gamePlayer2 = new GamePlayer(player2, game1,LocalDateTime.now().plusHours(1));
			GamePlayer gamePlayer3 = new GamePlayer(player3, game2,LocalDateTime.now().plusHours(2));
			GamePlayer gamePlayer4 = new GamePlayer(player4, game3,LocalDateTime.now().plusHours(3));


			Ship ship1 = new Ship("Destroyer", Arrays.asList("H1", "H2", "H3") );
			Ship ship2 = new Ship("Submarine", Arrays.asList("A1", "A2", "A3") );
			Ship ship3 = new Ship("Patroal", Arrays.asList("B1", "B2", "B3") );
			Ship ship4 = new Ship("Destroyer", Arrays.asList("C1", "C2", "C3") );

			Salvo salvo1 = new Salvo(5, Arrays.asList("H2","A1","B3","C1","H6"));
			Salvo salvo2 = new Salvo(2, Arrays.asList("A1","H4"));
			Salvo salvo3 = new Salvo(1, Arrays.asList("B3"));
			Salvo salvo4 = new Salvo(1, Arrays.asList("C1"));
			Salvo salvo5 = new Salvo(1, Arrays.asList("H6"));

			gamePlayer1.addShip(ship1);
			gamePlayer1.addShip(ship2);
			gamePlayer1.addShip(ship3);
			gamePlayer1.addShip(ship4);

			gamePlayer1.addSalvo(salvo2);
			gamePlayer2.addSalvo(salvo1);

			playerRepository.save(player1);
			playerRepository.save(player2);
			playerRepository.save(player3);
			playerRepository.save(player4);

			gameRepository.save(game1);
			gameRepository.save(game2);
			gameRepository.save(game3);

			gamePlayerRepository.save(gamePlayer1);
			gamePlayerRepository.save(gamePlayer2);
			gamePlayerRepository.save(gamePlayer3);
			gamePlayerRepository.save(gamePlayer4);

			scoreRepository.save(score1);
			scoreRepository.save(score2);
		};
	}
}

@Configuration
class WebSecurityConfiguration extends GlobalAuthenticationConfigurerAdapter {

	@Autowired
	PlayerRepository playerRepository;

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Override
	public void init(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(inputUserName-> {
			Player player = playerRepository.findByUserName(inputUserName);
			if (player != null) {
				return new User(player.getUserName(), player.getPassword(),
						AuthorityUtils.createAuthorityList("PLAYER"));
			} else {
				throw new UsernameNotFoundException("Unknown player: " + inputUserName);
			}
		});
	}

}

@EnableWebSecurity
@Configuration
class WebSecurityConfig extends WebSecurityConfigurerAdapter {

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.authorizeRequests()
				.antMatchers("/api/game_view/**","/web/game.html").hasAnyAuthority("PLAYER")
				.antMatchers("/**").permitAll()
				.and()
			.formLogin()
				.usernameParameter("userName")
				.passwordParameter("password")
				.loginPage("/api/login");

		http.logout().logoutUrl("/api/logout");

		// turn off checking for CSRF tokens
		http.csrf().disable();

		// if user is not authenticated, just send an authentication failure response
		http.exceptionHandling().authenticationEntryPoint((req, res, exc) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED));

		// if login is successful, just clear the flags asking for authentication
		http.formLogin().successHandler((req, res, auth) -> clearAuthenticationAttributes(req));

		// if login fails, just send an authentication failure response
		http.formLogin().failureHandler((req, res, exc) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED));

		// if logout is successful, just send a success response
		http.logout().logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler());
	}

	private void clearAuthenticationAttributes(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
		}
	}

}