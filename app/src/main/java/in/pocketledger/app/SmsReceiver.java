package in.pocketledger.app;
import android.content.*;import android.provider.Telephony;import android.telephony.SmsMessage;
public final class SmsReceiver extends BroadcastReceiver {
 public void onReceive(Context c,Intent intent){if(!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction()))return;SmsMessage[] parts=Telephony.Sms.Intents.getMessagesFromIntent(intent);if(parts.length==0)return;StringBuilder text=new StringBuilder();for(SmsMessage m:parts)text.append(m.getMessageBody());SmsParser.Entry e=SmsParser.parse(parts[0].getOriginatingAddress(),text.toString(),parts[0].getTimestampMillis());try(Store s=new Store(c)){s.recordBalance(BalanceParser.parse(parts[0].getOriginatingAddress(),text.toString(),parts[0].getTimestampMillis()));if(e!=null)s.insert(e);}}
}
