package in.pocketledger.app;
import java.util.*;
public final class MerchantRules {
 public static String clean(String name){return name.replaceAll("@.*$", "").replaceAll("[0-9]{5,}", "").replaceAll("(?i)\\b(?:upi|pos|pvt|private|ltd|limited)\\b", "").replaceAll("[^\\p{L} ]", " ").replaceAll("\\s+", " ").trim();}
 public static boolean sameBusiness(String query,String title){String q=clean(query).toLowerCase(Locale.ROOT),t=clean(title.replaceAll("\\([^)]*\\)", "")).toLowerCase(Locale.ROOT);return q.length()>=3&&q.equals(t);}
 public static String fromPlaceType(String type){
  if(type.equals("supermarket")||type.equals("grocery_store"))return "Groceries";
  if(type.endsWith("restaurant")||Arrays.asList("cafe","coffee_shop","bakery","meal_delivery","meal_takeaway","food_delivery").contains(type))return "Food";
  if(Arrays.asList("hotel","lodging","resort_hotel","travel_agency","airport","train_station","bus_station","taxi_stand","gas_station").contains(type))return "Travel";
  if(Arrays.asList("shopping_mall","clothing_store","shoe_store","electronics_store","department_store","book_store","jewelry_store").contains(type))return "Shopping";
  if(Arrays.asList("hospital","pharmacy","drugstore","doctor","dental_clinic").contains(type))return "Health";
  if(Arrays.asList("movie_theater","amusement_park","bowling_alley").contains(type))return "Entertainment";
  return "Uncategorised";
 }
}
