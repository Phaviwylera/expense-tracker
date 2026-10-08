package in.pocketledger.app;
import java.util.*;
import java.util.regex.*;
/** Generic INR transaction parser. Known bank senders are labelled; unfamiliar sources require review. */
public final class SmsParser {
 public static final class Entry {
  public String bank, account="Unknown", direction, merchant="Unknown", category="Uncategorised", reference="", raw, sender, status="confirmed", reason="";
  public long amountPaise, time;
 }
 private static final String MONEY="(?:INR|Rs\\.?|₹)\\s*:?\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)";
 private static final Pattern DEBIT=Pattern.compile("\\b(debited|spent|withdrawn|paid|sent|deducted|purchase(?:d)?)\\b",Pattern.CASE_INSENSITIVE);
 private static final Pattern CREDIT=Pattern.compile("\\b(credited|received|refunded|refund|deposited)\\b",Pattern.CASE_INSENSITIVE);
 private static final String[][] BANKS={
  {"HDFC","HDFC,HDFCBK,HDFCBN","HDFC"},
  {"KVB","KVB,KVBBNK","Karur Vysya,KVB"},
  {"India Post","IPPB,DOPBNK","India Post,Indian Post,Post Office,IPPB"},
  {"SBI","SBI,SBIPSG,SBIINB,SBIUPI,SBITXN,SBICRD","State Bank of India,SBI"},
  {"ICICI","ICICI,ICICIB,ICICIS","ICICI"},
  {"Axis","AXIS,AXISBK,AXISBN,AXSBNK","Axis Bank"},
  {"Kotak","KOTAK,KOTAKB,KOTKBK","Kotak"},
  {"Canara","CANBNK,CANARA,CANBnk","Canara"},
  {"PNB","PNB,PNBSMS,PNBBNK","Punjab National,PNB"},
  {"Bank of Baroda","BOB,BOBTXN,BOBSMS,BARODA","Bank of Baroda,Baroda"},
  {"Union Bank","UBI,UNIONB,UNIONBK,UBISMS","Union Bank"},
  {"Indian Bank","INDBNK,INDBAN,INDIANB","Indian Bank"},
  {"Indian Overseas Bank","IOB,IOBBNK,IOBSMS","Indian Overseas,IOB"},
  {"IDFC FIRST","IDFC,IDFCFB,IDFCBK","IDFC FIRST,IDFC"},
  {"IDBI","IDBI,IDBIBK,IDBIBN","IDBI"},
  {"Federal Bank","FEDBNK,FEDERAL,FEDBK","Federal Bank"},
  {"South Indian Bank","SIB,SIBSMS,SIBBNK","South Indian Bank"},
  {"Karnataka Bank","KARNBK,KARBNK,KBLBNK,KBL","Karnataka Bank"},
  {"Yes Bank","YESBNK,YESBANK,YESBK","Yes Bank"},
  {"IndusInd","INDBAK,INDUSB,INDUS,INDSBK","IndusInd"},
  {"Bank of India","BOI,BOIIND,BOISMS","Bank of India"},
  {"Bank of Maharashtra","MAHABK,BOMBNK,MAHBNK","Bank of Maharashtra"},
  {"Central Bank of India","CENTBK,CBI,CBISMS","Central Bank of India"},
  {"UCO Bank","UCO,UCOBNK,UCOBK","UCO Bank"},
  {"Punjab & Sind Bank","PSB,PSBANK,PSBBNK","Punjab & Sind,Punjab and Sind"},
  {"RBL","RBL,RBLBNK,RBLBANK","RBL Bank"},
  {"AU Small Finance Bank","AUBANK,AUBNK,AUSFB","AU Small Finance,AU Bank"},
  {"Equitas","EQUITAS,EQTSBN,EQBANK","Equitas"},
  {"Ujjivan","UJJIVN,UJJIVAN,UJSFB","Ujjivan"},
  {"Bandhan","BANDHN,BANDHAN,BDBANK","Bandhan"},
  {"DCB","DCB,DCBBNK,DCBANK","DCB Bank"},
  {"City Union Bank","CUB,CUBBNK,CITYUB","City Union Bank"},
  {"Tamilnad Mercantile Bank","TMB,TMBBNK","Tamilnad Mercantile,TMB"},
  {"Dhanlaxmi Bank","DLXBNK,DHANBK","Dhanlaxmi"},
  {"CSB Bank","CSB,CSBBNK","CSB Bank,Catholic Syrian"},
  {"HSBC","HSBC,HSBCIN","HSBC"},
  {"Standard Chartered","SCBANK,SCB,STANCH","Standard Chartered"},
  {"DBS","DBS,DBSBNK,DIGIBK","DBS Bank,digibank"},
  {"Citibank","CITI,CITIBK,CITIBN","Citibank,Citi Bank"},
  {"Bank of America","BOFA,BOABNK","Bank of America"},
  {"Airtel Payments Bank","AIRBNK,AIRPAY","Airtel Payments Bank"},
  {"Fino Payments Bank","FINOBK,FINOBN,FINO","Fino Payments Bank"},
  {"Paytm Payments Bank","PAYTMB,PTMBNK","Paytm Payments Bank"},
  {"Jana Small Finance Bank","JANABK,JANASF","Jana Small Finance"},
  {"Suryoday Small Finance Bank","SURYBK,SURYOD","Suryoday"},
  {"Utkarsh Small Finance Bank","UTKBNK,UTKSFB","Utkarsh"},
  {"ESAF","ESAFBK,ESAFSF","ESAF"},
  {"North East Small Finance Bank","NESFB,NESBNK","North East Small Finance"},
  {"Slice","SLICE,SLICEB","Slice Bank"}
 };
 private static String senderCode(String sender){return sender.toUpperCase(Locale.ROOT).replaceFirst("^[A-Z]{2}-", "").replaceFirst("-[STPG]$", "").trim();}
 private static final Map<String,String> SENDERS=new HashMap<>();
 private static final List<Pattern> BANK_NAMES=new ArrayList<>();
 static{for(String[] bank:BANKS){for(String alias:bank[1].toUpperCase(Locale.ROOT).split(","))SENDERS.put(alias,bank[0]);StringJoiner names=new StringJoiner("|");for(String name:bank[2].split(","))names.add(Pattern.quote(name));BANK_NAMES.add(Pattern.compile("(?<![A-Za-z])(?:"+names+")(?![A-Za-z])",Pattern.CASE_INSENSITIVE));}}
 private static String identifySender(String code){String bank=SENDERS.get(code);return bank!=null?bank:SENDERS.get(code.replaceFirst("[0-9]{1,3}$",""));}
 private static String identifyBody(String body){for(int i=0;i<BANKS.length;i++)if(BANK_NAMES.get(i).matcher(body).find())return BANKS[i][0];return null;}
 private static final Pattern MONEY_PATTERN=Pattern.compile(MONEY,Pattern.CASE_INSENSITIVE);
 private static final Pattern PROMOTIONAL=Pattern.compile("\\b(?:unsubscribe|reminder\\s*to\\s*re\\s*order|remindertoreorder|reorder|shophealthy|shophealthyin|coupon|promo\\s*code|offer\\s*code|discount\\s*code|claim\\s*(?:now|reward)|redeem\\s*(?:now|reward|points))\\b",Pattern.CASE_INSENSITIVE);
 private static final Pattern ACCOUNT_NUMBER=Pattern.compile("(?:a/c|acct|account|card)\\s*(?:(?:no\\.?|number|ending|ending\\s+with|:)[ \\t]*)?(?:[xX*]+[0-9]{2,}|[0-9]{2,20}\\b)",Pattern.CASE_INSENSITIVE);
 private static final Pattern BANK_MARKER=Pattern.compile("\\b(?:bank|neft|imps|rtgs|upi|utr|rrn|atm|pos)\\b|a/c|\\bacct\\b",Pattern.CASE_INSENSITIVE);
 /** Reject non-bank alerts before extracting a value, and reuse for cleaning historical false matches. */
 public static String rejectionReason(String sender,String body){
  if(sender==null||body==null)return "Missing SMS source";String code=senderCode(sender);boolean known=identifySender(code)!=null;
  boolean account=ACCOUNT_NUMBER.matcher(body).find();
  if(PROMOTIONAL.matcher(body).find()&&!(known&&account&&Pattern.compile("\\b(?:debited|credited|withdrawn|spent)\\b",Pattern.CASE_INSENSITIVE).matcher(body).find()))return "Promotion, coupon or reorder reminder";
  boolean reward=Pattern.compile("(?:gift\\s*card|swiggy\\s+money|reward\\s+points|loyalty\\s+points|store\\s+credit)",Pattern.CASE_INSENSITIVE).matcher(body).find();
  if(reward&&(!known||!account))return "Merchant wallet, gift-card or loyalty reward; not a bank transaction";
  if(!known){
   if(!categorise(code).equals("Uncategorised")||code.matches("(?i)(?:SHPHLT|SHOPHLT|SHOPHEALTHY|AMAZON|FLPKRT|SWIGGY|ZOMATO)[0-9]*"))return "Merchant notification; not a bank alert";
   if(!account)return "No bank account or card transaction evidence";
   boolean masked=Pattern.compile("[xX*]+[0-9]{2,}").matcher(body).find();
   if(!masked&&!BANK_MARKER.matcher(body).find()&&identifyBody(body)==null&&!code.matches(".*(?:BNK|BANK).*"))return "Unfamiliar source without banking context";
  }
  return "";
 }
 public static Entry parse(String sender,String body,long time) {
  if(sender==null||body==null)return null;
  String b=body.toLowerCase(Locale.ROOT), code=senderCode(sender);
  if(!MONEY_PATTERN.matcher(body).find()||(!DEBIT.matcher(body).find()&&!CREDIT.matcher(body).find()))return null;
  if(!rejectionReason(sender,body).isEmpty())return null;
  if(b.matches("(?s).*\\b(otp|one.time password|verification code|available offer|pre.approved|minimum amount due|total amount due|payment due|due date|collect request|request to pay)\\b.*"))return null;
  String bank=identifySender(code);boolean knownSender=bank!=null;

  // Do not interpret messages from personal phone numbers as bank alerts.
  if(sender.replaceAll("[+\\s-]", "").matches("[0-9]{9,}"))return null;
  boolean accountContext=Pattern.compile("(?:a/c|acct|account|card)\\b",Pattern.CASE_INSENSITIVE).matcher(body).find();
  if(!knownSender&&!accountContext)return null;
  if(bank==null)bank=identifyBody(body);
  if(bank==null)bank="Other bank · "+code;
  Matcher debit=DEBIT.matcher(body),credit=CREDIT.matcher(body);
  boolean d=debit.find(),c=credit.find(); if(!d&&!c)return null;
  Entry e=new Entry();if(!knownSender){e.status="review";e.reason="Unfamiliar sender; verify the bank and transaction before including.";}e.bank=bank;e.sender=sender;e.raw=body;e.time=time;e.direction=c&&!d?"credit":"debit";
  if(b.matches("(?s).*\\b(failed|declined|unsuccessful|not debited|will be|will get|shall be|scheduled)\\b.*")) {e.status="review";e.reason="Failed, future or conditional transaction; excluded until reviewed.";}
  if(d&&c){e.status="review";e.reason="Both debit and credit language; confirm direction.";}
  Matcher near=Pattern.compile("(?:"+MONEY+")\\s*(?:has been |is |was )?(?:debited|credited|spent|withdrawn|paid|sent|deducted|received|refunded|deposited)",Pattern.CASE_INSENSITIVE).matcher(body);
  Matcher after=Pattern.compile("(?:debited|credited|spent|withdrawn|paid|sent|deducted|received|refunded|deposited)(?:\\s+(?:by|with|for|of|an|amount|is|:)){0,4}\\s*"+MONEY,Pattern.CASE_INSENSITIVE).matcher(body);
  String amount=null;
  if(near.find())amount=near.group(1);else if(after.find())amount=after.group(1);
  else {Matcher m=Pattern.compile(MONEY,Pattern.CASE_INSENSITIVE).matcher(body);if(m.find()){amount=m.group(1);int pos=m.start();String before=b.substring(Math.max(0,pos-28),pos);if(before.matches("(?s).*(bal|balance|limit).*")){e.status="review";e.reason="Amount may be a balance; confirm transaction amount.";}else if(m.find()){e.status="review";e.reason="Multiple amounts; verify extracted transaction amount.";}}}
  if(amount==null)return null;
  try{e.amountPaise=new java.math.BigDecimal(amount.replace(",","")).movePointRight(2).longValueExact();}catch(Exception ex){return null;}
  if(e.amountPaise<=0)return null;
  Matcher a=Pattern.compile("(?:a/c|acct|account)(?:\\s*(?:no\\.?|ending|number|:))?\\s*([Xx*0-9]{2,})",Pattern.CASE_INSENSITIVE).matcher(body);
  if(a.find()){String acc=a.group(1).replaceAll("[^0-9]","");if(!acc.isEmpty())e.account=acc.length()>4?acc.substring(acc.length()-4):acc;}
  // Prefer payment destinations over the source account; masked accounts are not merchants.
  for(String preposition:new String[]{"at","to","from"}) {
   Matcher merchant=Pattern.compile("\\b"+preposition+"\\s+([A-Za-z][A-Za-z0-9@._ /&-]{1,70}?)(?=\\s+(?:on|via|using|ref|UPI|Avl|bal|a/c|account|Info|txn|transaction)\\b|[.;]|$)",Pattern.CASE_INSENSITIVE).matcher(body);
   while(merchant.find()){String candidate=merchant.group(1).trim();if(!candidate.matches("(?i)(?:your\\s+)?(?:a/c|account|acct|card)\\b.*")&&!candidate.matches("(?i)[x*0-9 /-]+")){e.merchant=candidate;break;}}
   if(!e.merchant.equals("Unknown"))break;
  }
  if(e.merchant.equals("Unknown")){
   Matcher info=Pattern.compile("\\bInfo\\s*:?\\s*(?:UPI[/-])?([A-Za-z][A-Za-z0-9 .&@_]{2,60})(?=[/-]|$)",Pattern.CASE_INSENSITIVE).matcher(body);
   if(info.find())e.merchant=info.group(1).trim();
  }
  Matcher ref=Pattern.compile("(?:UPI(?:\\s+Ref)?|UTR|RRN|Ref(?:erence)?(?:\\s+No\\.?)?)[\\s:#.-]*(\\d{8,24})",Pattern.CASE_INSENSITIVE).matcher(body);if(ref.find())e.reference=ref.group(1);
  e.category=categorise(e.merchant);
  return e;
 }
 private static boolean matches(String merchant,String words){return Pattern.compile("(?<![a-z])(?:"+words+")(?![a-z])").matcher(merchant.toLowerCase(Locale.ROOT).replaceAll("[._@/-]"," ")).find();}
 public static String categorise(String merchant){
  if(merchant==null)return "Uncategorised";
  if(matches(merchant,"(?:swiggy)?instamart|swiggy instamart"))return "Groceries";
  if(matches(merchant,"swiggy(?:upi|food|online|payments)?|zomato|restaurant|cafe|bakery|a2b|adyar ananda bhavan|adyar anandha bhavan|saravana bhavan|sangeetha|dominos|domino's|pizza hut|mcdonalds|kfc|burger king|starbucks"))return "Food";
  if(matches(merchant,"uber|ola|rapido|irctc|metro|railway|redbus|makemytrip|goibibo|indigo|air india|hotel|resort"))return "Travel";
  if(matches(merchant,"bigbasket|blinkit|zepto|grocery|groceries|supermarket|dmart|d mart"))return "Groceries";
  if(matches(merchant,"amazon|flipkart|myntra|ajio|nykaa|meesho"))return "Shopping";
  if(matches(merchant,"apollo|pharmacy|hospital|clinic|netmeds|pharmeasy"))return "Health";
  if(matches(merchant,"jio|airtel|electricity|tneb|bescom|broadband|recharge"))return "Bills";
  if(matches(merchant,"netflix|spotify|bookmyshow|pvr|inox|cinema"))return "Entertainment";
  if(matches(merchant,"salary|payroll"))return "Income";
  return "Uncategorised";
 }

}
