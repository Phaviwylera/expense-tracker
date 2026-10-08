package in.pocketledger.app;
import java.net.*;import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;import org.json.*;
/** Online lookup sends only the cleaned merchant name. No bank/SMS/amount data is sent. */
public final class MerchantLookup {
 public static final class Result {public String category="Uncategorised",source="",message="No confident business match. Kept Untagged.";}
 private static JSONObject request(String address,String apiKey,String body)throws Exception{
  HttpURLConnection c=(HttpURLConnection)new URL(address).openConnection();c.setConnectTimeout(4000);c.setReadTimeout(6000);c.setInstanceFollowRedirects(false);c.setRequestProperty("User-Agent","PocketLedger/1.3 (personal expense categorisation)");
  try{if(body!=null){c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json; charset=UTF-8");c.setRequestProperty("X-Goog-Api-Key",apiKey);c.setRequestProperty("X-Goog-FieldMask","places.displayName,places.primaryType");try(OutputStream o=c.getOutputStream()){o.write(body.getBytes(StandardCharsets.UTF_8));}}
   if(c.getResponseCode()!=200)throw new IOException("Lookup provider unavailable");try(InputStream in=c.getInputStream();ByteArrayOutputStream bytes=new ByteArrayOutputStream()){byte[] chunk=new byte[4096];int n;while((n=in.read(chunk))!=-1){if(bytes.size()+n>150000)throw new IOException("Response too large");bytes.write(chunk,0,n);}return new JSONObject(bytes.toString("UTF-8"));}
  }finally{c.disconnect();}
 }
 public static Result lookup(String merchant,String key)throws Exception{
  Result result=new Result();if(key.isEmpty()){result.message="Google Places key is required for online lookup.";return result;}String query=MerchantRules.clean(merchant);if(query.length()<3||query.length()>80||query.equalsIgnoreCase("Unknown"))return result;
  if(!key.isEmpty()){
   JSONObject body=new JSONObject();body.put("textQuery",query+" India");body.put("languageCode","en");body.put("regionCode","IN");body.put("pageSize",5);JSONArray places=request("https://places.googleapis.com/v1/places:searchText",key,body.toString()).optJSONArray("places");Set<String> options=new HashSet<>();
   if(places!=null)for(int i=0;i<places.length();i++){JSONObject p=places.getJSONObject(i);if(MerchantRules.sameBusiness(query,p.optJSONObject("displayName")==null?"":p.getJSONObject("displayName").optString("text")))options.add(MerchantRules.fromPlaceType(p.optString("primaryType")));}
   if(options.size()==1&&!options.contains("Uncategorised")){result.category=options.iterator().next();result.source="Google Maps";result.message="Google Maps business type matched.";return result;}
  }
  return result;
 }
}
