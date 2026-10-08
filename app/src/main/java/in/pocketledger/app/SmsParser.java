package in.pocketledger.app;
import java.util.*;
import java.util.regex.*;
/** Pure Java parser. Only recognised banks and explicit transaction verbs are accepted. */
public final class SmsParser {
 public static final class Entry {
  public String bank, account="Unknown", direction, merchant="Unknown", category="Uncategorised", reference="", raw, sender, status="confirmed", reason="";
  public long amountPaise, time;
 }
 private static final String MONEY="(?:INR|Rs\\.?|₹)\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)";
 private static final Pattern DEBIT=Pattern.compile("\\b(debited|spent|withdrawn|paid|purchase(?:d)?)\\b",Pattern.CASE_INSENSITIVE);
 private static final Pattern CREDIT=Pattern.compile("\\b(credited|received|refunded|refund)\\b",Pattern.CASE_INSENSITIVE);
 public static Entry parse(String sender,String body,long time) {
  if(sender==null||body==null)return null;
  String u=(sender+" "+body).toUpperCase(Locale.ROOT), b=body.toLowerCase(Locale.ROOT);
  String bank=u.matches("(?s).*\\b(?:HDFC|HDFCBK|HDFCBN)\\w*.*")?"HDFC":u.matches("(?s).*\\b(?:KVB|KVBBNK|KARUR)\\w*.*")?"KVB":u.matches("(?s).*\\b(?:IPPB|DOPBNK|INDIA POST|INDIAN POST|POST OFFICE)\\w*.*")?"India Post":null;
  if(bank==null || b.matches("(?s).*\\b(otp|one.time password|verification code|available offer|pre.approved)\\b.*"))return null;
  Matcher debit=DEBIT.matcher(body),credit=CREDIT.matcher(body);
  boolean d=debit.find(),c=credit.find(); if(!d&&!c)return null;
  Entry e=new Entry();e.bank=bank;e.sender=sender;e.raw=body;e.time=time;e.direction=c&&!d?"credit":"debit";
  if(b.matches("(?s).*\\b(failed|declined|unsuccessful|not debited|will be|will get|shall be|scheduled)\\b.*")) {e.status="review";e.reason="Failed, future or conditional transaction; excluded until reviewed.";}
  if(d&&c){e.status="review";e.reason="Both debit and credit language; confirm direction.";}
  Matcher near=Pattern.compile("(?:"+MONEY+")\\s*(?:has been |is |was )?(?:debited|credited|spent|withdrawn|paid|received|refunded)",Pattern.CASE_INSENSITIVE).matcher(body);
  Matcher after=Pattern.compile("(?:debited|credited|spent|withdrawn|paid|received|refunded)(?:\\s+(?:by|with|for|of|an|amount|is|:)){0,4}\\s*"+MONEY,Pattern.CASE_INSENSITIVE).matcher(body);
  String amount=null;
  if(near.find())amount=near.group(1);else if(after.find())amount=after.group(1);
  else {Matcher m=Pattern.compile(MONEY,Pattern.CASE_INSENSITIVE).matcher(body);if(m.find()){amount=m.group(1);int pos=m.start();String before=b.substring(Math.max(0,pos-28),pos);if(before.matches("(?s).*(bal|balance|limit).*")){e.status="review";e.reason="Amount may be a balance; confirm transaction amount.";}else if(m.find()){e.status="review";e.reason="Multiple amounts; verify extracted transaction amount.";}}}
  if(amount==null)return null;
  try{e.amountPaise=new java.math.BigDecimal(amount.replace(",","")).movePointRight(2).longValueExact();}catch(Exception ex){return null;}
  if(e.amountPaise<=0)return null;
  Matcher a=Pattern.compile("(?:a/c|acct|account)(?:\\s*(?:no\\.?|ending|number|:))?\\s*([Xx*0-9]{2,})",Pattern.CASE_INSENSITIVE).matcher(body);
  if(a.find()){String acc=a.group(1).replaceAll("[^0-9]","");if(!acc.isEmpty())e.account=acc.length()>4?acc.substring(acc.length()-4):acc;}
  Matcher merchant=Pattern.compile("\\b(?:to|at|from)\\s+([A-Za-z][A-Za-z0-9@._ /-]{1,55}?)(?=\\s+(?:on|via|using|ref|UPI|Avl|bal|a/c|account)\\b|[.;]|$)",Pattern.CASE_INSENSITIVE).matcher(body);
  if(merchant.find())e.merchant=merchant.group(1).trim();
  Matcher ref=Pattern.compile("(?:UPI(?:\\s+Ref)?|UTR|RRN|Ref(?:erence)?(?:\\s+No\\.?)?)[\\s:#.-]*(\\d{8,24})",Pattern.CASE_INSENSITIVE).matcher(body);if(ref.find())e.reference=ref.group(1);
  String name=e.merchant.toLowerCase(Locale.ROOT);
  if(name.matches(".*(swiggy|zomato|restaurant|cafe|bakery|hotel).*"))e.category="Food";
  else if(name.matches(".*(uber|ola|rapido|irctc|metro|railway).*"))e.category="Travel";
  else if(name.matches(".*(amazon|flipkart|myntra|ajio).*"))e.category="Shopping";
  else if(name.matches(".*(apollo|pharmacy|hospital|clinic).*"))e.category="Health";
  else if(name.matches(".*(jio|airtel|electric|tneb|broadband).*"))e.category="Bills";
  else if(name.matches(".*(bigbasket|blinkit|zepto|grocery|supermarket).*"))e.category="Groceries";
  else if(e.direction.equals("credit"))e.category="Income";
  return e;
 }
}
