package org.acme.quarkussocial.rest;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.acme.quarkussocial.domain.model.Follower;
import org.acme.quarkussocial.domain.model.Post;
import org.acme.quarkussocial.domain.model.User;
import org.acme.quarkussocial.domain.repository.FollowerRepository;
import org.acme.quarkussocial.domain.repository.PostRepository;
import org.acme.quarkussocial.domain.repository.UserRepository;
import org.acme.quarkussocial.rest.dto.CreatePostRequest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.transaction.Transactional;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestHTTPEndpoint(PostResource.class)
class PostResourceTest {

    @Inject
    UserRepository userRepository;
    @Inject
    FollowerRepository followerRepository;
    @Inject
    PostRepository postRepository;

    Long userId;
    Long userNotFollowerId;
    Long userFollowerId;

    @BeforeEach
    @Transactional
    public void setUP(){
        //usuário padão dos testes
        var user = new User();
        user.setAge(20);
        user.setName("Brubs");
        userRepository.persist(user);
        userId = user.getId();

        //criada a postagem para o usuário
        Post post = new Post();
        post.setText("Hello");
        post.setUser(user);
        post.setUser(user);
        postRepository.persist(post);

        //usuário que não segue
        var userNotFollower = new User();
        userNotFollower.setAge(22);
        userNotFollower.setName("Rapha");
        userRepository.persist(userNotFollower);
        userNotFollowerId = userNotFollower.getId();


        //usuário seguidor
        var userFollower = new User();
        userFollower.setAge(22);
        userFollower.setName("Rapha");
        userRepository.persist(userFollower);
        userFollowerId = userFollower.getId();

        Follower follower = new Follower();
        follower.setUser(user);
        follower.setFollower(userFollower);
        followerRepository.persist(follower);
    }

    @Test
    @DisplayName("should create a post for a user")
    public void createPostTest(){
        var postRequest = new CreatePostRequest();
        postRequest.setText("Some text");

        given()
                .contentType(ContentType.JSON)
                .body(postRequest)
                .pathParams("userId", userId)
                .when()
                .post()
                .then()
                .statusCode(201);
    }

    @Test
    @DisplayName("should return 404 when trying to make a post for an inexistent user")
    public void postForAnInexistentUserTest(){
        var postRequest = new CreatePostRequest();
        postRequest.setText("Some text");

        var inexistentUserId = 999;

        given()
                .contentType(ContentType.JSON)
                .body(postRequest)
                .pathParams("userId", inexistentUserId)
                .when()
                .post()
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("should return 404 when user doesn´t exist")
    public void listPostUserNotFoundTest(){
        var inexistentUserId = 999;
        given()
                .pathParams("userId", inexistentUserId)
        .when()
                .get()
        .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("should return 404 when followerId header is not present")
    public void listPostFollowerHeaderNotSendTest(){
        given()
                .pathParams("userId", userId)
                .when()
                .get()
                .then()
                .statusCode(400)
                .body(Matchers.is("You forgot the header followerId"));
    }

    @Test
    @DisplayName("should return 404 when followerId doesn´t exist")
    public void listPostFollowerNotFoundTest(){

        var inexistentFollowerId = 999;

        given()
                .pathParams("userId", userId)
                .header("followerId", inexistentFollowerId)
                .when()
                .get()
                .then()
                .statusCode(400)
                .body(Matchers.is("Inexistent followerId"));
    }

    @Test
    @DisplayName("should return 403 when followerId isn´t a follower")
    public void listPostNotAFollower(){
        given()
                .pathParams("userId", userId)
                .header("followerId", userNotFollowerId)
                .when()
                .get()
                .then()
                .statusCode(403)
                .body(Matchers.is("You can´t see these posts"));
    }

    @Test
    @DisplayName("should return posts")
    public void listPostTest(){
        given()
                .pathParams("userId", userId)
                .header("followerId", userFollowerId)
                .when()
                .get()
                .then()
                .statusCode(200)
                .body("size()", Matchers.is(1));
    }

}