import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.config.SSLConfig;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.apache.http.HttpStatus;
import org.awaitility.Awaitility;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.*;

import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.config;
import static org.hamcrest.Matchers.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Tests {

    private final String baseUrl = "https://petstore.swagger.io";
    private final String version = "/v2";
    private final String userUrl = "/user";
    private final String petUrl = "/pet";
    private final String storeUrl = "/store";
    private long userId = 0;
    private String userName = "test_user_name";
    private String userFirstName = "test_user_first_name";
    private String userLastName = "test_user_last_name";
    private String userEmail = "test_user_email@gmail.com";
    private String userPassword = "test_user_password";
    private String userPhone = "+7(987)65-43-21";
    private Integer userStatus = 0;
    private String userUpdatedPhone = "+7(123)45-67-89";
    private long petId = 0;
    private Integer petCategoryId = 0;
    private String petCategoryName = "test_pet_category_name";
    private String petName = "test_pet_name";
    private String petPhotoUrl = "test_pet_photo_url.com";
    private Integer petTagId = 0;
    private String petTagName = "test_pet_tag_name";
    private String petStatus = "test_pet_status";
    private long orderId = 0;
    private Integer orderQuantity = 1;
    private String shipDate = "2022-06-11T08:01:50.696Z";
    private String orderStatus = "placed";
    private boolean orderComplete = true;
    private String userSession;
    JSONObject createUserJson = new JSONObject();
    JSONArray usersJsonArray = new JSONArray();
    JSONObject createPetJson = new JSONObject();
    JSONObject createOrderJson = new JSONObject();
    RequestSpecification createUserSpecification;
    RequestSpecification createPetSpecification;
    RequestSpecification createOrderSpecification;
    RequestSpecification loginSpecification;

    ResponseSpecification okResponse = new ResponseSpecBuilder()
            .expectStatusCode(HttpStatus.SC_OK)
            .expectContentType(ContentType.JSON)
            .build();

    ResponseSpecification notFoundResponse = new ResponseSpecBuilder()
            .expectStatusCode(HttpStatus.SC_NOT_FOUND)
            .expectContentType(ContentType.JSON)
            .build();


    SSLConfig sslConfig = config.getSSLConfig();

    @BeforeAll
    public void init() {
        RestAssured.baseURI = baseUrl;
        RestAssured.basePath = version;

        config = config().sslConfig(sslConfig.with().keystoreType(sslConfig.getKeyStoreType())
                .trustStore(sslConfig.getTrustStore()).and().strictHostnames());

        createUserJson.put("id", userId)
                .put("username", userName)
                .put("firstName", userFirstName)
                .put("lastName", userLastName)
                .put("email", userEmail)
                .put("password", userPassword)
                .put("phone", userPhone)
                .put("userStatus", userStatus);
        usersJsonArray.put(createUserJson);

        JSONArray petTagsJsonArray = new JSONArray();
        petTagsJsonArray.put(
                new JSONObject()
                        .put("id", petTagId)
                        .put("name", petTagName));
        createPetJson.put("id", petId)
                .put("category", new JSONObject()
                        .put("id", petCategoryId)
                        .put("name", petCategoryName))
                .put("name", petName)
                .put("photoUrls", new JSONArray()
                        .put(petPhotoUrl))
                .put("tags", petTagsJsonArray)
                .put("status", petStatus);

        createOrderJson.put("id", orderId)
                .put("quantity", orderQuantity)
                .put("shipDate", shipDate)
                .put("status", orderStatus)
                .put("complete", orderComplete);

        createUserSpecification = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setBody(usersJsonArray.toString())
                .build();

        createPetSpecification = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setBody(createPetJson.toString())
                .build();

        createOrderSpecification = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .build();

        loginSpecification = new RequestSpecBuilder()
                .addQueryParam("username", userName)
                .addQueryParam( "password", userPassword)
                .build();
    }

    @AfterAll
    public void release() {
        usersJsonArray.clear();
        createUserJson.clear();
        createPetJson.clear();
        createOrderJson.clear();
    }

    @Test
    @Order(1)
    public void createAndLoginUser() {
        RestAssured.given().spec(createUserSpecification)
                .when().post(userUrl + "/createWithArray")
                .then().spec(okResponse)
                .and().body("type", equalTo("unknown"))
                .and().body("message", equalTo("ok"));

        Awaitility.await().atLeast(1, TimeUnit.SECONDS);

        JsonPath getUserJsonResponse = RestAssured.given()
                .when().get(userUrl + "/" + userName)
                .then().spec(okResponse)
                .extract().jsonPath();

        Assertions.assertEquals(userName, getUserJsonResponse.get("username"));
        Assertions.assertEquals(userFirstName, getUserJsonResponse.get("firstName"));
        Assertions.assertEquals(userLastName, getUserJsonResponse.get("lastName"));
        Assertions.assertEquals(userEmail, getUserJsonResponse.get("email"));
        Assertions.assertEquals(userPassword, getUserJsonResponse.get("password"));
        Assertions.assertEquals(userPhone, getUserJsonResponse.get("phone"));
        Assertions.assertEquals(userStatus, getUserJsonResponse.get("userStatus"));

        userId = getUserJsonResponse.getLong("id");

        login();
        logout();
    }

    private void login() {
        userSession = RestAssured.given().spec(loginSpecification)
                .when().get(userUrl + "/login")
                .then().spec(okResponse)
                .and().body("type", equalTo("unknown"))
                .and().body("message", containsString("logged in user session"))
                .extract().path("message");
        userSession = userSession.substring(userSession.indexOf(":") + 1);
    }

    private void logout() {
        RestAssured.given()
                .when().get(userUrl + "/logout")
                .then().statusCode(HttpStatus.SC_OK)
                .and().body("type", equalTo("unknown"))
                .and().body("message", equalTo("ok"));
    }

    @Test
    @Order(2)
    public void updateUser() {
        createUserJson.put("phone", userUpdatedPhone);
        createUserSpecification.body(createUserJson.toString());

        RestAssured.given().spec(createUserSpecification)
                .when().put(userUrl + "/" + userName)
                .then().spec(okResponse)
                .and().body("type", equalTo("unknown"));

        Awaitility.await().atLeast(1, TimeUnit.SECONDS);

        JsonPath getUserJsonResponse = RestAssured.given()
                .when().get(userUrl + "/" + userName)
                .then().spec(okResponse)
                .extract().jsonPath();

        Assertions.assertEquals(userName, getUserJsonResponse.get("username"));
        Assertions.assertEquals(userFirstName, getUserJsonResponse.get("firstName"));
        Assertions.assertEquals(userLastName, getUserJsonResponse.get("lastName"));
        Assertions.assertEquals(userEmail, getUserJsonResponse.get("email"));
        Assertions.assertEquals(userPassword, getUserJsonResponse.get("password"));
        Assertions.assertEquals(userUpdatedPhone, getUserJsonResponse.get("phone"));
        Assertions.assertEquals(userStatus, getUserJsonResponse.get("userStatus"));
    }

    @Test
    @Order(3)
    public void createPet() {
        login();

        JsonPath createPetJsonResponse = RestAssured.given().spec(createPetSpecification)
                .when().post(petUrl)
                .then().spec(okResponse)
                .extract().jsonPath();

        petId = createPetJsonResponse.getLong("id");

        JsonPath getPetJsonResponse = RestAssured.given()
                .when().get(petUrl + "/" + petId)
                .then().spec(okResponse)
                .extract().jsonPath();

        Assertions.assertEquals(petCategoryId, getPetJsonResponse.get("category.id"));
        Assertions.assertEquals(petCategoryName, getPetJsonResponse.get("category.name"));
        Assertions.assertEquals(petName, getPetJsonResponse.get("name"));
        Assertions.assertEquals(petPhotoUrl, getPetJsonResponse.get("photoUrls[0]"));
        Assertions.assertEquals(petTagId, getPetJsonResponse.get("tags[0].id"));
        Assertions.assertEquals(petTagName, getPetJsonResponse.get("tags[0].name"));
        Assertions.assertEquals(petStatus, getPetJsonResponse.get("status"));

        logout();
    }

    @Test
    @Order(4)
    public void createOrder() {
        login();

        createOrderJson.put("petId", petId);
        createOrderSpecification.body(createOrderJson.toString());

        JsonPath createOrderJsonResponse = RestAssured.given().spec(createOrderSpecification)
                .when().post(storeUrl + "/order")
                .then().spec(okResponse)
                .extract().jsonPath();
        orderId = createOrderJsonResponse.getLong("id");

        JsonPath getOrderJsonResponse = RestAssured.given()
                .when().get(storeUrl + "/order/" + orderId)
                .then().spec(okResponse)
                .extract().jsonPath();

        Assertions.assertEquals(petId, getOrderJsonResponse.getLong("petId"));
        Assertions.assertEquals(orderQuantity, getOrderJsonResponse.get("quantity"));
        Assertions.assertTrue(getOrderJsonResponse.getString("shipDate").contains(shipDate.substring(0, 23)));
        Assertions.assertEquals(orderStatus, getOrderJsonResponse.get("status"));
        Assertions.assertEquals(orderComplete, getOrderJsonResponse.get("complete"));

        logout();
    }

    @Test
    @Order(5)
    public void deletePetAndOrder() {
        RestAssured.given()
                .when().delete(storeUrl + "/order/" + orderId)
                .then().spec(okResponse)
                .and().body("type", equalTo("unknown"));

        RestAssured.given()
                .when().delete(petUrl + "/" + petId)
                .then().spec(okResponse)
                .and().body("type", equalTo("unknown"));
    }

    @Test
    @Order(6)
    public void deleteUser() {
        RestAssured.given()
                .when().delete(userUrl + "/" + userName)
                .then().spec(okResponse)
                .and().body("type", equalTo("unknown"))
                .and().body("message", equalTo(userName));

        Awaitility.await().atLeast(1, TimeUnit.SECONDS);

        RestAssured.given()
                .when().get(userUrl + "/" + userName)
                .then().spec(notFoundResponse)
                .and().body("code", equalTo(1))
                .and().body("type", equalTo("error"))
                .and().body("message", equalTo("User not found"));
    }

}
