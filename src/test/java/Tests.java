import io.restassured.RestAssured;
import io.restassured.config.SSLConfig;
import io.restassured.http.ContentType;
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
    private Integer orderId = 0;
    private Integer orderQuantity = 1;
    private String shipDate = "2022-06-11T08:01:50.696Z";
    private String orderStatus = "placed";
    private boolean orderComplete = true;
    private String userSession;
    JSONObject createUserJson = new JSONObject();
    JSONArray usersJsonArray = new JSONArray();
    JSONObject createPetJson = new JSONObject();
    JSONObject createOrderJson = new JSONObject();

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
        RestAssured.given().contentType(ContentType.JSON).body(usersJsonArray.toString())
                .when().post(userUrl + "/createWithArray")
                .then().statusCode(HttpStatus.SC_OK)
                .and().contentType(ContentType.JSON)
                .and().body("type", equalTo("unknown"))
                .and().body("message", equalTo("ok"));

        Awaitility.await().atMost(2, TimeUnit.SECONDS);

        userId = RestAssured.given()
                .when().get(userUrl + "/" + userName)
                .then().statusCode(HttpStatus.SC_OK)
                .and().contentType(ContentType.JSON)
                .and().body("username", equalTo(userName))
                .and().body("firstName", equalTo(userFirstName))
                .and().body("lastName", equalTo(userLastName))
                .and().body("email", equalTo(userEmail))
                .and().body("password", equalTo(userPassword))
                .and().body("phone", equalTo(userPhone))
                .and().body("userStatus", equalTo((int) userStatus))
                .extract().path("id");

        login();
        logout();
    }

    private void login() {
        userSession = RestAssured.given()
                .queryParam("username", userName, "password", userPassword)
                .when().get(userUrl + "/login")
                .then().statusCode(HttpStatus.SC_OK)
                .and().contentType(ContentType.JSON)
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
        RestAssured.given().contentType(ContentType.JSON).body(createUserJson.toString())
                .when().put(userUrl + "/" + userName)
                .then().statusCode(HttpStatus.SC_OK)
                .and().contentType(ContentType.JSON)
                .and().body("type", equalTo("unknown"));

        Awaitility.await().atMost(2, TimeUnit.SECONDS);

        RestAssured.given()
                .when().get(userUrl + "/" + userName)
                .then().statusCode(HttpStatus.SC_OK)
                .and().contentType(ContentType.JSON)
                .and().body("username", equalTo(userName))
                .and().body("firstName", equalTo(userFirstName))
                .and().body("lastName", equalTo(userLastName))
                .and().body("email", equalTo(userEmail))
                .and().body("password", equalTo(userPassword))
                .and().body("phone", equalTo(userUpdatedPhone))
                .and().body("userStatus", equalTo((int) userStatus));
    }

    @Test
    @Order(3)
    public void createPet() {
        login();

        petId = RestAssured.given().contentType(ContentType.JSON).body(createPetJson.toString())
                .when().post(petUrl)
                .then().statusCode(HttpStatus.SC_OK)
                .and().contentType(ContentType.JSON)
                .extract().path("id");

        RestAssured.given()
                .when().get(petUrl + "/" + petId)
                .then().statusCode(HttpStatus.SC_OK)
                .and().contentType(ContentType.JSON)
                .and().body("category.id", equalTo((int) petCategoryId))
                .and().body("category.name", equalTo(petCategoryName))
                .and().body("name", equalTo(petName))
                .and().body("photoUrls[0]", equalTo(petPhotoUrl))
                .and().body("tags[0].id", equalTo((int) petTagId))
                .and().body("tags[0].name", equalTo(petTagName))
                .and().body("status", equalTo(petStatus));

        logout();
    }

    @Test
    @Order(4)
    public void createOrder() {
        login();

        createOrderJson.put("petId", petId);

        orderId = RestAssured.given().contentType(ContentType.JSON).body(createOrderJson.toString())
                .when().post(storeUrl + "/order")
                .then().statusCode(HttpStatus.SC_OK)
                .and().contentType(ContentType.JSON)
                .extract().path("id");

        RestAssured.given()
                .when().get(storeUrl + "/order/" + orderId)
                .then().statusCode(HttpStatus.SC_OK)
                .and().contentType(ContentType.JSON)
                .and().body("petId", equalTo(petId))
                .and().body("quantity", equalTo(orderQuantity))
                .and().body("shipDate", containsString(shipDate.substring(0, 23)))
                .and().body("status", equalTo(orderStatus))
                .and().body("complete", equalTo(orderComplete));

        logout();
    }

    @Test
    @Order(5)
    public void deletePetAndOrder() {
        RestAssured.given()
                .when().delete(storeUrl + "/order/" + orderId)
                .then().statusCode(HttpStatus.SC_OK)
                .and().contentType(ContentType.JSON)
                .and().body("type", equalTo("unknown"));

        RestAssured.given()
                .when().delete(petUrl + "/" + petId)
                .then().statusCode(HttpStatus.SC_OK)
                .and().contentType(ContentType.JSON)
                .and().body("type", equalTo("unknown"));
    }

    @Test
    @Order(6)
    public void deleteUser() {
        RestAssured.given()
                .when().delete(userUrl + "/" + userName)
                .then().statusCode(HttpStatus.SC_OK)
                .and().contentType(ContentType.JSON)
                .and().body("type", equalTo("unknown"))
                .and().body("message", equalTo(userName));

        Awaitility.await().atMost(2, TimeUnit.SECONDS);

        RestAssured.given()
                .when().get(userUrl + "/" + userName)
                .then().statusCode(HttpStatus.SC_NOT_FOUND)
                .and().contentType(ContentType.JSON)
                .and().body("code", equalTo(1))
                .and().body("type", equalTo("error"))
                .and().body("message", equalTo("User not found"));
    }

}
