package in.pocketledger.app;
import android.app.*;import android.os.*;import android.content.*;import android.database.sqlite.*;import org.json.*;
/** Isolated test database only. Never inserts synthetic SMS or production ledger entries. */
public final class LedgerInstrumentation extends Instrumentation {
 public void onCreate(Bundle args){super.onCreate(args);start();}
 private void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 private JSONObject row(Store store)throws Exception{return store.snapshot().getJSONArray("transactions").getJSONObject(0);}
 public void onStart(){Bundle result=new Bundle();Context c=getTargetContext();String name="ledger-regression-only.db";c.deleteDatabase(name);try{
  // Reproduce an older version's stored Untagged Swiggy entry and migrate it.
  SQLiteDatabase old=c.openOrCreateDatabase(name,0,null);old.execSQL("CREATE TABLE tx(id TEXT PRIMARY KEY,time INTEGER,bank TEXT,account TEXT,amount INTEGER,direction TEXT,merchant TEXT,category TEXT,reference TEXT,raw TEXT,sender TEXT,status TEXT,reason TEXT)");old.execSQL("CREATE TABLE rules(merchant TEXT PRIMARY KEY,category TEXT)");old.execSQL("CREATE TABLE plans(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT,amount INTEGER,date TEXT)");old.execSQL("INSERT INTO tx VALUES ('test-only',1,'SBI','TEST',50000,'debit','SWIGGYUPI','Uncategorised','','unit-test fixture','VM-SBIUPI','confirmed','isolated test')");old.setVersion(2);old.close();
  try(Store s=new Store(c,name)){check(row(s).getString("category").equals("Food"),"Existing Swiggy must repair to Food");s.edit("test-only","SBI","Travel","SWIGGYUPI","confirmed","debit",false);check(row(s).getString("category").equals("Travel"),"Manual edit persisted immediately");check(s.applyAutoCategory("SWIGGYUPI","Food")==0,"Online lookup cannot overwrite manual tag");s.repairCategories();check(row(s).getString("category").equals("Travel"),"Repair cannot overwrite manual tag");s.edit("test-only","SBI","Uncategorised","SWIGGYUPI","confirmed","debit",false);s.repairCategories();check(row(s).getString("category").equals("Uncategorised"),"Explicit manual untag respected");}
  result.putString("stream","PASS isolated native DB migration, Swiggy repair, persisted category edits and manual tag preservation.\n");finish(Activity.RESULT_OK,result);
 }catch(Throwable e){result.putString("stream","FAIL native ledger regression: "+e.toString());finish(Activity.RESULT_CANCELED,result);}finally{c.deleteDatabase(name);}}
}
