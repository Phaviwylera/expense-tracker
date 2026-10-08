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
 }
}
