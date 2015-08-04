package hive.android.hebron.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import hive.android.hebron.R;


public class StartActivity extends Activity {

    @Bind(R.id.passwordInput)
    EditText password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start_activity);
        ButterKnife.bind(this);
    }

    @OnClick(R.id.checkPassword)
    public void onPasswordCheckClick(View view) {
        if (password.getText().toString().equals(generatePasswordGen())) {
            final Intent intent = new Intent(this, BeaconPlayerActivity.class);
            view.getContext().startActivity(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private String generatePasswordGen() {
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(new Date());
        int currentDate = calendar.get(Calendar.DATE);
        if (currentDate < 10) return "heb0" + currentDate + "ron";
        else  return "heb" + currentDate + "ron";
    }
}
