import in.pocketledger.app.MerchantRules;
import in.pocketledger.app.SmsParser;
public class ParserTest {
 static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);System.out.println("PASS "+name);}
 public static void main(String[]args){
  var h=SmsParser.parse("VM-HDFCBK","Rs.1,250.00 debited from A/c XX1234 to SWIGGY on 06-10-26. Avl Bal Rs.9,000. UPI Ref 123456789012",1000);
  check(h!=null&&h.amountPaise==125000&&h.direction.equals("debit")&&h.category.equals("Food")&&h.account.equals("1234")&&h.reference.equals("123456789012")&&h.status.equals("confirmed"),"HDFC debit with balance and reference");
  var k=SmsParser.parse("AD-KVBBNK","Your account XX5678 credited with INR 67,000.00 from EMPLOYER on 05 Oct.",2000);check(k!=null&&k.amountPaise==6700000&&k.direction.equals("credit"),"KVB income");
  var p=SmsParser.parse("VM-IPPB","INR 300.00 debited from account XX0099 to OLA on 06 Oct.",3000);check(p!=null&&p.bank.equals("India Post")&&p.category.equals("Travel"),"IPPB travel");
  var dop=SmsParser.parse("VD-DOPBNK","Rs.50.00 credited to your account XX9000 on 06 Oct.",4000);check(dop!=null&&dop.direction.equals("credit"),"Post Office sender");
  check(SmsParser.parse("VM-HDFCBK","OTP 987654 for purchase of Rs.500. Do not share.",1)==null,"OTP excluded");
  check(SmsParser.parse("VM-HDFCBK","Your available balance is INR 25,000.",1)==null,"Balance-only alert excluded");
  check(SmsParser.parse("FRIEND","I paid INR 500 to you",1)==null,"Non-bank SMS excluded");
  var f=SmsParser.parse("VM-HDFCBK","INR 500 debited. Transaction failed and will be reversed.",1);check(f!=null&&f.status.equals("review"),"Failed debit requires review");
  var a=SmsParser.parse("VM-HDFCBK","You paid INR 500 and received INR 100.",1);check(a!=null&&a.status.equals("review"),"Ambiguous debit-credit requires review");
  var r=SmsParser.parse("VM-HDFCBK","INR 300 refunded to your account XX1234.",1);check(r!=null&&r.direction.equals("credit"),"Refund is credit");
  var bal=SmsParser.parse("VM-HDFCBK","Available balance INR 9000 after account debited.",1);check(bal!=null&&bal.status.equals("review"),"Balance amount never silently confirmed");
  check(SmsParser.parse("VM-KVB","Rs.0 debited from A/c XX0000",1)==null,"Zero amount rejected");
  String[][] banks={{"SBIUPI","SBI"},{"ICICIB","ICICI"},{"AXISBK","Axis"},{"KOTAKB","Kotak"},{"CANBNK","Canara"},{"PNBSMS","PNB"},{"BOBTXN","Bank of Baroda"},{"UBISMS","Union Bank"},{"INDBNK","Indian Bank"},{"IOBBNK","Indian Overseas Bank"},{"IDFCFB","IDFC FIRST"},{"FEDBNK","Federal Bank"},{"YESBNK","Yes Bank"},{"INDBAK","IndusInd"},{"IDBIBK","IDBI"},{"CUBBNK","City Union Bank"},{"TMBBNK","Tamilnad Mercantile Bank"},{"AUBANK","AU Small Finance Bank"},{"UJJIVN","Ujjivan"},{"HSBCIN","HSBC"}};
  for(String[] bank:banks){var tx=SmsParser.parse("VM-"+bank[0]+"-S","Your A/c XX1010 debited by INR: 250.00 to CAFE on 08 Oct. Avl Bal INR 5000.",6000);check(tx!=null&&tx.bank.equals(bank[1])&&tx.amountPaise==25000&&tx.status.equals("confirmed"),"Bank sender "+bank[0]);}
  var unknown=SmsParser.parse("VM-NEWBNK","INR 120 debited from account XX1234 to STORE on 08 Oct.",7000);check(unknown!=null&&unknown.bank.equals("Other bank · NEWBNK")&&unknown.status.equals("review"),"Unlisted bank retained for review");
  var priority=SmsParser.parse("VM-SBIUPI","INR 400 debited from A/c XX1111 to HDFC account on 08 Oct.",8000);check(priority!=null&&priority.bank.equals("SBI"),"Sender takes precedence over counterparty bank");
  var body=SmsParser.parse("VM-TXNMSG","Canara Bank account XX1111 credited with INR 800.",9000);check(body!=null&&body.bank.equals("Canara")&&body.status.equals("review"),"Body bank identification requires sender review");
  check(SmsParser.parse("+919876543210","Your SBI account XX1234 debited INR 300",1)==null,"Personal phone numbers are not bank senders");
  check(SmsParser.parse("VM-NEWBNK","Offer: you paid INR 500 last month; get a discount",1)==null,"Unfamiliar non-account messages excluded");
  check(SmsParser.parse("VM-ICICIB","INR 600 payment due for card XX1234. Amount credited last month INR 200",1)==null,"Card due reminder excluded");
  var sent=SmsParser.parse("VM-ICICIB","INR 500 sent from account XX1111 to STORE on 08 Oct.",1);check(sent!=null&&sent.direction.equals("debit")&&sent.amountPaise==50000,"Sent payment wording");
  var deposit=SmsParser.parse("VM-AXISBK","INR 600 deposited into account XX1111.",1);check(deposit!=null&&deposit.direction.equals("credit"),"Deposited wording");
  var grocery=SmsParser.parse("VM-HDFCBK","INR 450 debited from account XX1234 to BLINKIT on 08 Oct.",1);check(grocery!=null&&grocery.merchant.equals("BLINKIT")&&grocery.category.equals("Groceries"),"Destination merchant beats source account");
  check(SmsParser.categorise("CHOLAMANDALAM").equals("Uncategorised"),"Ola substring must not tag unrelated merchant");
  check(SmsParser.categorise("LOCAL STORE").equals("Uncategorised"),"Unknown place stays untagged");
  check(SmsParser.categorise("NETFLIX").equals("Entertainment"),"Entertainment merchant");
  check(SmsParser.categorise("TAJ HOTEL").equals("Travel"),"Hotel is travel rather than food");
  check(SmsParser.categorise("SWIGGY.IN").equals("Food"),"Merchant punctuation recognised");

  var info=SmsParser.parse("VM-HDFCBK","INR 450 debited from A/c XX1234. Info: UPI/BLINKIT/123456789012",1);check(info!=null&&info.merchant.equals("BLINKIT")&&info.category.equals("Groceries"),"UPI Info merchant extraction");

  check(SmsParser.categorise("SWIGGYUPI").equals("Food"),"Compact Swiggy UPI descriptor");
  check(SmsParser.categorise("SWIGGY123@paytm").equals("Food"),"Swiggy numbered VPA");
  check(SmsParser.categorise("swiggyinstamart").equals("Groceries"),"Swiggy Instamart is groceries");
  check(MerchantRules.sameBusiness("LENSKART","Lenskart"),"Online identity match ignores case");
  check(!MerchantRules.sameBusiness("John","John Lewis"),"Ambiguous online identity rejected");
  check(MerchantRules.fromPlaceType("indian_restaurant").equals("Food"),"Google restaurant type maps Food");

  check(SmsParser.parse("VM-SWIGGY","Your Swiggy Money is credited with a Gift card of ₹200.00 for Marriott cashback. Total balance is ₹200.00.",1)==null,"Swiggy gift card cashback is not a bank transaction");
  check(SmsParser.parse("VM-SHPHLT","ShopHealthyinRemindertoReOrder Your account (9999999999) Time: 24/09 10:01 Fare at received Rs.3018 Balance Rs.5929. Unsubscribe View Now bit.ly/test",1)==null,"ShopHealthy reorder promotion excluded");
  check(SmsParser.parse("VM-HDFCBK","INR 200 credited to account XX1234 as cashback on 24 Sep.",1)!=null,"Actual bank cashback credit retained");
  var actual=SmsParser.parse("VM-SBIUPI","INR 200 debited from account XX1234 to SWIGGY on 24 Sep.",1);check(actual!=null&&actual.category.equals("Food")&&actual.direction.equals("debit"),"Actual Swiggy bank payment remains Food expense");
  check(SmsParser.parse("VM-STORE","Your account 1234 received Rs.3018 Balance Rs.5929",1)==null,"Generic merchant account and amount are insufficient");
  check(SmsParser.parse("VM-HDFCBK","Buy gift cards! Get Rs.200 coupon credited to your rewards account.",1)==null,"Bank-sender coupon offer excluded");

  check(SmsParser.parse("VM-HDFCBK","INR 200 credited to account XX1234. To stop marketing messages unsubscribe.",1)!=null,"Genuine bank credit with marketing footer retained");

  var full=SmsParser.parse("VM-NEWBNK","Account 123456789012 credited INR 500 via NEFT Ref 123456789012.",1);check(full!=null&&full.status.equals("review"),"Unlisted bank full account number plus NEFT retained for review");

  check(SmsParser.parse("VM-VAANAM","ORDER REMINDER: ID#123 on A/C 9999999999 delivered! And you received Rs.7,232 reward now. Cust Ph View details: example.com/test",1)==null,"Order reward with phone labelled A/C excluded");
  check(SmsParser.parse("VM-OTHER","A/C 9999999999 received Rs.7232. View details example.com/test",1)==null,"Account label and received alone insufficient");
  check(SmsParser.parse("VM-OTHER","Account XX1234 received INR 500",1)==null,"Masked account alone cannot prove received is a bank transfer");
  check(SmsParser.parse("VM-OTHER","A/C 9999999999 credited Rs.7232",1)==null,"Credit word plus unmasked account alone insufficient for unfamiliar source");
  check(SmsParser.parse("VM-OTHER","Account XX1234 credited INR 500",1)!=null,"Masked account explicit credit retained for review");
  check(SmsParser.parse("VM-OTHER","Account 123456789012 received INR 500 via NEFT UTR ABC123456789",1)!=null,"Received transfer with NEFT reference retained for review");

 }
}
