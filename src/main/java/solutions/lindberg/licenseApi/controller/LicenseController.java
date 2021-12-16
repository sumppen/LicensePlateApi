/*
 *
 *  Copyright (c) 2018-2020 Givantha Kalansuriya, This source is a part of
 *   Staxrt - sample application source code.
 *   http://staxrt.com
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package solutions.lindberg.licenseApi.controller;

import kong.unirest.json.JSONObject;
import org.imgscalr.Scalr;
import org.springframework.web.multipart.MultipartFile;
import solutions.lindberg.licenseApi.exception.ResourceNotFoundException;
import solutions.lindberg.licenseApi.model.LicensePlate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import kong.unirest.Unirest;
import kong.unirest.HttpResponse;
import solutions.lindberg.licenseApi.model.LicensePlateResponse;

import javax.imageio.ImageIO;

/**
 * The type LicensePlate controller.
 *
 * @author Kim Lindberg
 */
@RestController
@RequestMapping("/api/v1")
public class LicenseController {

  private String token = "e999af752976bb4f7ce1cd9279c020bf35989faf";
  private boolean emulated = false;

  /**
   * Gets license plate by number.
   *
   * @param licensePlate the license plate number
   * @return the users by id
   * @throws ResourceNotFoundException the resource not found exception
   */
  @GetMapping("/licensePlate/{id}")
  public ResponseEntity<LicensePlate> getUsersById(@PathVariable(value = "id") String licensePlate)
      throws ResourceNotFoundException {
    return ResponseEntity.ok().body(new LicensePlate());
  }

  /**
   * Create user user.
   *
   * @param image Image from which to detect the license plate
   * @return Detection result(s)
   */
  @PostMapping("/licensePlate")
  public LicensePlateResponse detectLicensePlate(@RequestPart MultipartFile image, @RequestPart String originalUri) {
    LicensePlateResponse response = new LicensePlateResponse();
    response.setOriginalUri(originalUri);
    try {
      String responseString = null;
      String lookupResponseString = null;
      File tempFile = File.createTempFile("image", "jpg");
      tempFile.deleteOnExit();
      BufferedImage src = ImageIO.read(image.getInputStream());
      int originalWidth = src.getWidth();
      int targetWidth = 1024;
      double ratio = originalWidth/targetWidth;
      if(!emulated) {
        responseString = detectLicensePlate(tempFile, src, targetWidth);
      } else {
        responseString = "{'processing_time':228.471,'results':[{'box':{'xmin':336,'ymin':787,'xmax':471,'ymax':834},'plate':'mji260','region':{'code':'fi','score':0.961},'score':0.902,'candidates':[{'score':0.902,'plate':'mji260'},{'score':0.901,'plate':'mji26o'},{'score':0.759,'plate':'mj1260'},{'score':0.757,'plate':'mj126o'}],'dscore':0.99,'vehicle':{'score':0.579,'type':'Sedan','box':{'xmin':0,'ymin':479,'xmax':605,'ymax':951}}}],'filename':'1003_oK69F_image7211659229191648598jpg.jpg','version':1,'camera_id':null,'timestamp':'2021-11-03T10:03:58.364826Z'}";
      }
      tempFile.delete();
      JSONObject obj = new JSONObject(responseString).getJSONArray("results").getJSONObject(0);
      // TODO error handlind
      String plate = obj.getString("plate");
      String parts[] = plate.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
      LicensePlate licensePlate = new LicensePlate();
      licensePlate.setLicensePlateNumber(parts[0].toUpperCase()+"-"+parts[1].toUpperCase());
      JSONObject box = obj.getJSONObject("box");
      licensePlate.setX((int) (box.getInt("xmin")*ratio));
      licensePlate.setY((int) (box.getInt("ymin")*ratio));
      if(!emulated) {
        lookupResponseString = lookupCar(licensePlate);
      } else {
        lookupResponseString = "{'json_status':'success','json_message':null,'json_array':{'response':{'action':'lookupLicense','technical':{'chassies':'WDB2030071F835164','type':'Sedan','cylinder':'4','cylinderCapacity':'2148','engineCode':'OM646.963','fuelType':'Diesel','fuelTypeProcess':'Suoraruiskutus','gearBox':'Automaattinen','groupCode':'MB Fi0243','imported':false,'impulsionType':'Takaveto','lenght':'4530','maxWeightKg':'1995','monthAndYearManufactured':'','motorType':'Diesel','powerHp':'122','powerKw':'90','registrationDate':'20070702','tireDimensions':'','typeOfVehicle':'PB','weightKg':'1535'},'vehicleName':'MERCEDES-BENZ C-SARJA (W203) C 200 CDI (203.007)','licensePlate':'MJI-260','color':''},'state':''}}";
      }
      // TODO error handlind
      obj = new JSONObject(lookupResponseString).getJSONObject("json_array").getJSONObject("response");
      licensePlate.setModel(obj.getString("vehicleName"));
      licensePlate.setRegistrationDate(obj.getJSONObject("technical").getString("registrationDate"));
      licensePlate.setPower(obj.getJSONObject("technical").getInt("powerKw"));
      ArrayList<LicensePlate> list = new ArrayList<>();
      list.add(licensePlate);
      response.setLicensePlates(list);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return response;
  }

  private String lookupCar(LicensePlate licensePlate) {
    String lookupResponseString;
    HttpResponse<String> lookupResponse = Unirest.get("https://autopalvelut.net/v2/license/lookup/" + licensePlate.getLicensePlateNumber())
            .asString();
    System.out.println(lookupResponse.getBody());
    lookupResponseString = lookupResponse.getBody();
    return lookupResponseString;
  }

  private String detectLicensePlate(File tempFile, BufferedImage src, int targetWidth) throws IOException {
    String responseString;
    BufferedImage rotated = Scalr.rotate(src, Scalr.Rotation.CW_90, Scalr.OP_ANTIALIAS);
    BufferedImage resizedImage = Scalr.resize(rotated, targetWidth);
    ImageIO.write(resizedImage, "JPG", tempFile);
    HttpResponse<String> response = Unirest.post("https://api.platerecognizer.com/v1/plate-reader/")
            .header("Authorization", "Token " + token)
            .field("upload", tempFile)
            .asString();
    System.out.println(response.getBody());
    responseString = response.getBody();
    return responseString;
  }

}
