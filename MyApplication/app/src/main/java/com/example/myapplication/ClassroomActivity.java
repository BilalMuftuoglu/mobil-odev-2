package com.example.myapplication;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ClassroomActivity extends AppCompatActivity {

    RecyclerView postRecyclerView;
    PostRecyclerViewAdapter postRecyclerViewAdapter;
    ArrayList<Map<String,Object>> postList;

    FirebaseFirestore db;
    FirebaseAuth mAuth;
    FirebaseStorage storage;
    String accountType;
    String courseId;
    String email;
    String myProfileImageUrl;
    String myUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classroom);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        Bundle extras = getIntent().getExtras();
        accountType = extras.getString("accountType");
        courseId = extras.getString("courseId");

        setTitle(courseId + " Sınıfı");

        email = mAuth.getCurrentUser().getEmail();
        db.collection(accountType).whereEqualTo("email",email).get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (DocumentSnapshot doc: queryDocumentSnapshots.getDocuments()) {
                myProfileImageUrl = doc.getString("profileImageUrl");
                myUsername = doc.getString("nameSurname");
                postRecyclerViewAdapter = new PostRecyclerViewAdapter(this, postList,myProfileImageUrl,myUsername);
                postRecyclerView.setAdapter(postRecyclerViewAdapter);
            }
        });

        postList = new ArrayList<>();

        postRecyclerView = findViewById(R.id.postRecyclerView);
        postRecyclerView.setHasFixedSize(true);
        postRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        fetchPosts();

    }

    public void fetchPosts(){
        postList.clear();
        /*db.collection("posts").whereEqualTo("courseId",courseId).orderBy("date", Query.Direction.DESCENDING).get().addOnSuccessListener(queryDocumentSnapshots -> {

            for (DocumentSnapshot doc: queryDocumentSnapshots.getDocuments()) {
                Map<String, Object> post = doc.getData();
                Date date = doc.getTimestamp("date").toDate();
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy   HH:mm", Locale.getDefault());
                String formattedDate = sdf.format(date);
                post.put("date",formattedDate);

                db.collection("instructors").whereEqualTo("email",post.get("email").toString()).get().addOnSuccessListener(query2DocumentSnapshots -> {
                    for (DocumentSnapshot doc2: query2DocumentSnapshots.getDocuments()) {
                        String username = doc2.getString("nameSurname");
                        post.put("username",username);
                        String profileImageUrl = doc2.getString("profileImageUrl");
                        post.put("profileImageUrl",profileImageUrl);

                        postList.add(post);
                        postRecyclerViewAdapter.notifyDataSetChanged();
                    }
                });
            }
        });*/
        db.collection("posts").whereEqualTo("courseId", courseId).orderBy("date", Query.Direction.DESCENDING).get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<Map<String, Object>> tempPostList = new ArrayList<>();

            for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                Map<String, Object> post = doc.getData();
                Date date = doc.getTimestamp("date").toDate();
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy   HH:mm", Locale.getDefault());
                String formattedDate = sdf.format(date);
                post.put("date", formattedDate);
                post.put("postId", doc.getId());

                tempPostList.add(post);
            }

            // Tüm instructor sorguları için tamamlandıktan sonra çalışacak Task listesi
            List<Task<QuerySnapshot>> tasks = new ArrayList<>();

            for (Map<String, Object> post : tempPostList) {
                String email = post.get("email").toString();
                Task<QuerySnapshot> task = db.collection("instructors").whereEqualTo("email", email).get();
                tasks.add(task);
            }

            Tasks.whenAllSuccess(tasks).addOnSuccessListener(taskResults -> {
                for (int i = 0; i < taskResults.size(); i++) {
                    QuerySnapshot querySnapshot = (QuerySnapshot) taskResults.get(i);
                    Map<String, Object> post = tempPostList.get(i);

                    for (DocumentSnapshot doc2 : querySnapshot.getDocuments()) {
                        String username = doc2.getString("nameSurname");
                        post.put("username", username);
                        String profileImageUrl = doc2.getString("profileImageUrl");
                        post.put("profileImageUrl", profileImageUrl);
                    }

                    postList.add(post);
                    postRecyclerViewAdapter.notifyDataSetChanged();
                }
            });
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.classroom_page_menu, menu);

        if(!accountType.equals("instructors")){
            menu.removeItem(R.id.addPostButton);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        int id = item.getItemId();

        if(id == R.id.addPostButton){
            popUpEditText();
        }
        if(id == R.id.pollsPageButton){
            Intent intent = new Intent(ClassroomActivity.this, PollMainActivity.class);
            intent.putExtra("courseId",courseId);
            intent.putExtra("accountType",accountType);
            startActivity(intent);

        }

        return super.onOptionsItemSelected(item);
    }

    private void popUpEditText() {
        /*AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Duyuru Ekle");
        alert.setMessage("Duyurunuzu buraya yazınız");
        alert.setCancelable(false);

        final EditText input = new EditText(this);
        input.setSingleLine(false);
        input.setGravity(Gravity.LEFT|Gravity.TOP);
        input.setBackground(getResources().getDrawable(R.drawable.my_edit_text));
        input.setMinLines(5);
        input.setPadding(30,0,30,0);
        alert.setView(input,50,0,50,0);

        alert.setPositiveButton("Gönder", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                if(input.getText().toString().trim().isEmpty()){
                    Toast.makeText(getApplicationContext(),"Lütfen bir duyuru giriniz!",Toast.LENGTH_LONG).show();
                }else{
                    String postText = input.getText().toString().trim();
                    addPost(postText,false);
                }
            }
        });

        alert.setNegativeButton("İptal",null);
        alert.show();*/

        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Duyuru Ekle");
        alert.setMessage("Duyurunuzu buraya yazınız");
        alert.setCancelable(false);

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_with_edittext_and_checkbox, null);
        alert.setView(dialogView);

        final EditText input = dialogView.findViewById(R.id.editTextAnnouncement);
        final CheckBox alertCheckBox = dialogView.findViewById(R.id.alertCheckBox);

        alert.setPositiveButton("Gönder", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                if (input.getText().toString().trim().isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Lütfen bir duyuru giriniz!", Toast.LENGTH_LONG).show();
                } else {
                    String postText = input.getText().toString().trim();
                    boolean alert = alertCheckBox.isChecked();
                    addPost(postText, alert);
                }
            }
        });

        alert.setNegativeButton("İptal", null);
        alert.show();

    }

    public void addPost(String postText, boolean alert){
        Map<String,Object> post = new HashMap<>();
        post.put("email",email);
        post.put("date", FieldValue.serverTimestamp());
        post.put("courseId",courseId);
        post.put("post",postText);

        db.collection("posts").add(post).addOnSuccessListener(documentReference -> {
            fetchPosts();
        });
    }
}