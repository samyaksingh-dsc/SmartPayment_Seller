package com.datsea.posseller;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    DatabaseReference posRef;
    SharedPreferences master;
    TextView txtName, txtUpi, txtCustomerBalance;
    LinearLayout lvAmount, lvTapCard, lvRegisterUser;
    EditText txtCName, txtCUpi, txtUserAmount;
    Button btnRegister, btnRequest, btnLogout;
    FirebaseAuth auth;
    FirebaseAuth.AuthStateListener authStateListener;
    String currentbalance = "0";
    String user = "";
    ListView listView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();

        final FirebaseUser userfirebase = FirebaseAuth.getInstance().getCurrentUser();

        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser userfirebase = firebaseAuth.getCurrentUser();
                if (user == null){
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    finish();
                }

            }
        };

        txtName = findViewById(R.id.txtName);
        listView = findViewById(R.id.listView);
        txtUpi = findViewById(R.id.txtUpi);
        lvAmount = findViewById(R.id.lvAmount);
        lvTapCard = findViewById(R.id.lvTapCard);
        lvRegisterUser = findViewById(R.id.lvRegisterUser);
        txtCName = findViewById(R.id.txtCName);
        txtCUpi = findViewById(R.id.txtCUpi);
        txtUserAmount = findViewById(R.id.txtUserAmount);
        txtCustomerBalance = findViewById(R.id.txtCustomerBalance);
        btnRegister = findViewById(R.id.btnRegister);
        btnRequest = findViewById(R.id.button);
        btnLogout = findViewById(R.id.btn_logout);
        btnRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String amount = txtUserAmount.getText().toString();
                if (TextUtils.isEmpty(amount)){
                    Toast.makeText(MainActivity.this, "Amount is needed", Toast.LENGTH_SHORT).show();
                }
                updateBalance(amount);
            }
        });


        master = this.getSharedPreferences("master", 0);

        lvTapCard.setVisibility(View.VISIBLE);

        removeUserId();

        posRef = FirebaseDatabase.getInstance().getReference();
        posRef.addValueEventListener(new ValueEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild("user-id")){
                   final String user_id = Objects.requireNonNull(dataSnapshot.child("user-id").getValue()).toString();
                   user = user_id;
                    posRef = FirebaseDatabase.getInstance().getReference("user_Data");
                    posRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.hasChild(user_id)){
                                getUserData(user_id);
                            }else {
                                lvTapCard.setVisibility(View.GONE);
                                lvAmount.setVisibility(View.GONE);
                                lvRegisterUser.setVisibility(View.VISIBLE);
                                btnRegister.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        String name = txtCName.getText().toString();
                                        String upi = txtCUpi.getText().toString();
                                        if (TextUtils.isEmpty(name)){
                                            Toast.makeText(MainActivity.this, "Please enter name...", Toast.LENGTH_SHORT).show();
                                            return;
                                        }
                                        if (TextUtils.isEmpty(upi)){
                                            Toast.makeText(MainActivity.this, "Please enter upi id...", Toast.LENGTH_SHORT).show();
                                            return;
                                        }
                                        registerUser(user_id, name, upi);
                                    }
                                });
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });

                }

            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        fetchTransactions();

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signOut();
            }
        });

    }

    private void getUserData(String user_id){
        posRef = FirebaseDatabase.getInstance().getReference("user_Data");
        posRef.child(user_id).addValueEventListener(new ValueEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                SharedPreferences.Editor editor = master.edit();
                editor.putString("name", Objects.requireNonNull(dataSnapshot.child("name").getValue()).toString());
                editor.putString("upi", Objects.requireNonNull(dataSnapshot.child("upi").getValue()).toString());
                editor.apply();
                setUserData();
                removeUserId();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        posRef = FirebaseDatabase.getInstance().getReference("request");
        posRef.addValueEventListener(new ValueEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    currentbalance = dataSnapshot.child("amount").getValue().toString();
                    txtCustomerBalance.setText("₹"+currentbalance);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void removeUserId(){
        posRef = FirebaseDatabase.getInstance().getReference();
        posRef.child("user-id").removeValue();
    }

    private void setUserData(){
        lvTapCard.setVisibility(View.GONE);
        lvAmount.setVisibility(View.VISIBLE);
        txtName.setText(master.getString("name", "--"));
        txtUpi.setText(master.getString("upi", "--"));
        txtCustomerBalance.setText(master.getString("balance", "--"));
    }

    private void registerUser(String user_id, String name, String upi){
        posRef = FirebaseDatabase.getInstance().getReference("user_Data");
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("name", name);
        hashMap.put("upi", upi);
        posRef.child(user_id).setValue(hashMap);
        Toast.makeText(this, "User Added!", Toast.LENGTH_SHORT).show();
        lvAmount.setVisibility(View.GONE);
        lvRegisterUser.setVisibility(View.GONE);
        lvTapCard.setVisibility(View.VISIBLE);

    }

    private void updateBalance(String balance_to_adjust){
        int current_balance = Integer.parseInt(currentbalance);
        int add_balance = Integer.parseInt(balance_to_adjust);
        int updated_balance = current_balance-add_balance;
        posRef = FirebaseDatabase.getInstance().getReference("request");
        posRef.child("amount").setValue(updated_balance);
        txtUserAmount.setText("");
        posRef = FirebaseDatabase.getInstance().getReference("transactions");
        HashMap<String, String> hashMap = new HashMap<String, String>();
        hashMap.put("user", master.getString("name", ""));
        hashMap.put("type", "0");
        hashMap.put("amount", balance_to_adjust);
        posRef.push().setValue(hashMap);
        Toast.makeText(MainActivity.this, "Balance Added!", Toast.LENGTH_SHORT).show();
        //txtBal.setText("");
    }

    private void fetchTransactions(){

        final ArrayList<String> txn = new ArrayList<>();

       // txn.clear();

        posRef = FirebaseDatabase.getInstance().getReference("transactions");
        posRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                txn.clear();
                for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()){
                    String user = dataSnapshot1.child("user").getValue().toString();
                    String amount = dataSnapshot1.child("amount").getValue().toString();
                    String type = dataSnapshot1.child("type").getValue().toString();
                    if (type.equals("0")){
                        String txn_list = "Rs. "+amount+" received from user: "+user;
                        txn.add(txn_list);
                    }

                }

                Context context;
                ArrayAdapter arrayAdapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.support_simple_spinner_dropdown_item, txn);
                listView.setAdapter(arrayAdapter);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    public void signOut(){
        auth.signOut();
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

}
