package in.pocketledger.app;
import java.util.*;import java.util.regex.*;
/** Only reported deposit-account balances from recognised bank SMS; never derives balance from cash flow. */
public final class BalanceParser {
 public static final class Balance {public String bank,account,sender;public long amount,time;}
 public static Balance parse(String sender,String body,long time){
  if(sender==null||body==null)return null;String bank=SmsParser.identifySender(SmsParser.senderCode(sender));if(bank==null)return null;
  if(Pattern.compile("\\b(?:otp|gift\\s*card|reward\\s+points|unsubscribe|coupon|order\\s+reminder|credit\\s*card|available\\s+credit|credit\\s+limit|outstanding|due\\s+date)\\b",Pattern.CASE_INSENSITIVE).matcher(body).find())return null;
  Matcher account=Pattern.compile("(?:a/c|acct|account)\\s*(?:(?:no\\.?|number|ending(?:\\s+with)?|:)\\s*)?([xX*]*[0-9]{2,20})\\b",Pattern.CASE_INSENSITIVE).matcher(body);if(!account.find())return null;String number=account.group(1).replaceAll("[^0-9]","");String suffix=number.substring(Math.max(0,number.length()-4));
  Matcher m=Pattern.compile("\\b(?:(?:avl|avail|available|aval|closing|current|clear)\\.?\\s*)?(?:bal(?:ance)?)\\.?\\s*(?:is|of|:|=)?\\s*(?:INR|Rs\\.?|₹)?\\s*([0-9]+(?:,[0-9]{2,3})*(?:\\.[0-9]{1,2})?)(?![0-9.])",Pattern.CASE_INSENSITIVE).matcher(body);Long value=null;
  while(m.find()){long next;try{next=new java.math.BigDecimal(m.group(1).replace(",","")).movePointRight(2).longValueExact();}catch(Exception e){return null;}if(value!=null&&value!=next)return null;value=next;}
  if(value==null)return null;Balance b=new Balance();b.bank=bank;b.account=suffix;b.amount=value;b.time=time;b.sender=sender;return b;
 }
}
