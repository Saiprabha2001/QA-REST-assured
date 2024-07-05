import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import org.testng.asserts.SoftAssert;


import static io.restassured.RestAssured.given;

public class BookingTest {

  @Test(dataProvider = "bookingData")
  public void testBookingWorkflow(String firstName, String lastName, int totalPrice, boolean depositPaid, String checkinDate, String checkoutDate, String additionalNeeds) {
    String baseUrl = "https://restful-booker.herokuapp.com/booking/";

    //Create Booking

    String updateFirstName = "UpdatedF";
    String updatedLastName = "UpdatedL";

    JSONObject jsonBody = new JSONObject();
    JSONObject bookingdatesObject = new JSONObject();
    jsonBody.put("firstname", firstName);
    jsonBody.put("lastname", lastName);
    jsonBody.put("totalprice", totalPrice);
    jsonBody.put("depositpaid", depositPaid);
    jsonBody.put("additionalneeds", additionalNeeds);

    bookingdatesObject.put("checkin", checkinDate);
    bookingdatesObject.put("checkout", checkoutDate);
    jsonBody.put("bookingdates", bookingdatesObject);

    Response response = given()
        .contentType(ContentType.JSON)
        .body(jsonBody.toString())
        .post("https://restful-booker.herokuapp.com/booking");

    response.then()
        .assertThat()
        .statusCode(200);

    int bookingId = response.jsonPath().getInt("bookingid");

    //Get booking
    Response response1 = given()
        .get(baseUrl+bookingId);

    response1.then()
        .assertThat()
        .statusCode(200);
    response1.print();

    String responseBody = response1.getBody().asString();
    Assert.assertTrue(responseBody.contains(firstName), "First name should be " + firstName);
    Assert.assertTrue(responseBody.contains(lastName), "Last name should be " + lastName);
    Assert.assertTrue(responseBody.contains(String.valueOf(totalPrice)), "Total price should be " + totalPrice);
    Assert.assertTrue(responseBody.contains(String.valueOf(depositPaid)), "Deposit paid should be " + depositPaid);
    Assert.assertTrue(responseBody.contains(checkinDate), "Checkin date should be " + checkinDate);
    Assert.assertTrue(responseBody.contains(checkoutDate), "Checkout date should be " + checkoutDate);
    Assert.assertTrue(responseBody.contains(additionalNeeds), "Additional needs should be " + additionalNeeds);

//  //Update booking
    JSONObject jsonBodyU = new JSONObject();
    JSONObject bookingdatesObjectU = new JSONObject();
    jsonBodyU.put("firstname", updateFirstName);
    jsonBodyU.put("lastname", updatedLastName);
    jsonBodyU.put("totalprice", totalPrice);
    jsonBodyU.put("depositpaid", depositPaid);
    jsonBodyU.put("additionalneeds", additionalNeeds);

    bookingdatesObjectU.put("checkin", checkinDate);
    bookingdatesObjectU.put("checkout", checkoutDate);
    jsonBodyU.put("bookingdates", bookingdatesObjectU);

    Response response3 = RestAssured.given().auth().preemptive().basic("admin", "password123").contentType(ContentType.JSON).body(jsonBodyU.toString())
        .put(baseUrl + bookingId);

    response3.then()
        .assertThat()
        .statusCode(200);

    response3.print();

    String responseBody3 = response3.getBody().asString();
    Assert.assertTrue(responseBody3.contains(updateFirstName), "First name should be " + updateFirstName);
    Assert.assertTrue(responseBody3.contains(updatedLastName), "Last name should be " + updatedLastName);

    SoftAssert softAssert = new SoftAssert();
    String actualFirstName = response3.jsonPath().getString("firstname");
    softAssert.assertEquals(actualFirstName, updateFirstName, "firstname in response is not expected");

    String actualLastName = response3.jsonPath().getString("lastname");
    softAssert.assertEquals(actualLastName, updatedLastName, "lastname in response is not expected");

    int price = response3.jsonPath().getInt("totalprice");
    softAssert.assertEquals(price, 150, "totalprice in response is not expected");

    boolean depositpaid = response3.jsonPath().getBoolean("depositpaid");
    softAssert.assertTrue(depositpaid, "depositpaid should be true, but it's not");

    String actualCheckin = response3.jsonPath().getString("bookingdates.checkin");
    softAssert.assertEquals(actualCheckin, "2024-08-10", "checkin in response is not expected");

    String actualCheckout = response3.jsonPath().getString("bookingdates.checkout");
    softAssert.assertEquals(actualCheckout, "2024-08-15", "checkout in response is not expected");

    softAssert.assertAll();

   //Delete booking
    Response response4 = RestAssured.given().auth().preemptive().basic("admin", "password123")
        .delete(baseUrl + bookingId);

    response4.print();
    Assert.assertEquals(response4.getStatusCode(), 201, "Status code should be 201, but it's not.");

    Response responseGet = RestAssured.get(baseUrl + bookingId);
    responseGet.print();

    response3.then()
        .assertThat()
        .statusCode(200);

    Assert.assertEquals(responseGet.getBody().asString(), "Not Found", "Body should be 'Not Found', but it's not.");

  }

  @DataProvider(name = "bookingData")
  public Object[][] getBookingData() throws IOException, CsvException {
    String csvFile = "src/test/test-QA.csv";
    CSVReader reader = new CSVReader(new FileReader(csvFile));
    List<String[]> rows = reader.readAll();
    reader.close();

    Object[][] data = new Object[rows.size()][7];

    for (int i = 0; i < rows.size(); i++) {
      String[] row = rows.get(i);
      data[i] = new Object[]{
          row[0],
          row[1],
          Integer.parseInt(row[2]),
          Boolean.parseBoolean(row[3]),
          row[4],
          row[5],
          row[6]
      };
    }

    return data;
  }
}
