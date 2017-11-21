package com.pujhones.bicita.view;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.pujhones.bicita.R;
import com.pujhones.bicita.model.BiciEmpresa;
import com.pujhones.bicita.model.Lugar;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import de.hdodenhof.circleimageview.CircleImageView;

public class CrearLugarActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    EditText nombre;
    EditText descripcion;
    RadioButton promocionar;

    double longitud;
    double latitud;

    FirebaseDatabase database;
    DatabaseReference myRef;
    StorageReference storageRef;

    Button registro;
    Button elegir;
    CircleImageView cam;
    Button galeria;

    public final static String TAG = "REGISTRO_EMPRESAS";
    public final static String PATH_BICIUSUARIOS = "biciempresas/";
    public final static String PATH_STORAGE_FOTOSPRFIL = "fotosPerfil/";
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crear_lugar);
        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();
        nombre = (EditText) findViewById(R.id.nombreLugar);

        descripcion = (EditText) findViewById(R.id.descripcionLugar);
        promocionar = (RadioButton) findViewById(R.id.promocionar);

        registro = (Button) findViewById(R.id.botonRegistro_lugar);
        elegir = (Button) findViewById(R.id.botonElegir_lugar);
        galeria = (Button) findViewById(R.id.btnCamara_lugar);
        cam = (CircleImageView) findViewById(R.id.btnCamara_lugar);


        galeria.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cargarGaleriaConPermiso();
            }
        });

        cam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                abrirCamaraConPermiso();
            }
        });
        registro.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if (!nombre.getText().toString().equals("") &&
                        !descripcion.getText().toString().equals("")
                       ) {
                    mAuth.createUserWithEmailAndPassword(nombre.getText().toString(), descripcion.getText().toString())
                            .addOnCompleteListener(CrearLugarActivity.this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) {
                                        Log.d(TAG, "createUserWithEmail:onComplete:" + task.isSuccessful());
                                        FirebaseUser user = mAuth.getCurrentUser();
                                        if (user != null) {
                                            UserProfileChangeRequest.Builder upcrb = new UserProfileChangeRequest.Builder();
                                            upcrb.setDisplayName(nombre.getText().toString());
                                            //upcrb.setPhotoUri(Uri.parse("path/to/pic"));//fake	 uri,	real	one	coming	soon
                                            user.updateProfile(upcrb.build());

                                            String urlPath = PATH_STORAGE_FOTOSPRFIL + "URL::" + user.getUid() + ".jpg";
                                            StorageReference url = storageRef.child(urlPath);

                                            // Get the data from an ImageView as bytes
                                            cam.setDrawingCacheEnabled(true);
                                            cam.buildDrawingCache();
                                            Bitmap bitmap = cam.getDrawingCache();
                                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                                            byte[] data = baos.toByteArray();

                                            UploadTask uploadTask = url.putBytes(data);
                                            uploadTask.addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception exception) {
                                                    // Handle unsuccessful uploads
                                                }
                                            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                                @Override
                                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                                    // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                                                    Uri downloadUrl = taskSnapshot.getDownloadUrl();
                                                    String fullURL = "";
                                                    if (downloadUrl != null) {
                                                        fullURL = downloadUrl.toString();
                                                    }
                                                    Log.i(TAG, fullURL);
                                                    // Lugar(String uid, String nombre, String descripción, String photoURL, double longitud, double latitud, double altitud, boolean prom)
                                                   /* Lugar bu = new Lugar(
                                                            mAuth.getCurrentUser().getUid(),
                                                            nombre.getText().toString(),
                                                            descripcion.getText().toString(),
                                                            fullURL,

                                                            promocionar.isEnabled());
                                                    Log.i("URL", bu.getPhotoURL());*/

                                                    myRef = database.getReference(PATH_BICIUSUARIOS + FirebaseAuth.getInstance().getCurrentUser().getUid());
                                                   // myRef.setValue(bu);

                                                    Toast.makeText(CrearLugarActivity.this, "Registro exitoso", Toast.LENGTH_SHORT).show();
                                                    startActivity(new Intent(CrearLugarActivity.this, ActivityPerfilEmpresa.class));
                                                }
                                            });
                                        }
                                    }
                                    if (!task.isSuccessful()) {
                                        Toast.makeText(CrearLugarActivity.this, R.string.auth_failed + task.getException().toString(), Toast.LENGTH_SHORT).show();
                                        Log.e(TAG, task.getException().getMessage());
                                    }
                                }
                            });
                } else {
                    if (nombre.getText().toString().equals("")) {
                        Toast.makeText(CrearLugarActivity.this, "El nombre no puede estar vacio", Toast.LENGTH_SHORT).show();
                    }
                    if (descripcion.getText().toString().equals("")) {
                        Toast.makeText(CrearLugarActivity.this, "La descripción no puede estar vacia", Toast.LENGTH_SHORT).show();
                    }


                }
            }
        });
    }

    protected void cargarGaleriaConPermiso() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // Show an expanation to the user *asynchronously*
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            }
        } else {
            cargarGaleria();
        }
    }

    protected void abrirCamaraConPermiso() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                // Show an expanation to the user *asynchronously*
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
            }
        } else {
            abrirCamara();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, continue with task related to permission
                    cargarGaleria();
                } else {
                    // permission denied, disable functionality that depends on this permission.
                }
                return;
            }
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, continue with task related to permission
                    abrirCamara();
                } else {
                    // permission denied, disable functionality that depends on this permission.
                }
                return;
            }

        }
    }

    protected void cargarGaleria() {
        Intent pickImage = new Intent(Intent.ACTION_PICK);
        pickImage.setType("image/*");
        startActivityForResult(pickImage, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
    }

    protected void abrirCamara() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(takePictureIntent, MY_PERMISSIONS_REQUEST_CAMERA);

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:
                if (resultCode == RESULT_OK) {
                    try {
                        final Uri imageUri = data.getData();
                        final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                        final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                        cam.setImageBitmap(selectedImage);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case MY_PERMISSIONS_REQUEST_CAMERA:
                if (resultCode == RESULT_OK) {
                    Bundle extras = data.getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    cam.setImageBitmap(imageBitmap);
                }
                break;
        }
    }

    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        // An item was selected. You can retrieve the selected item using
        parent.getItemAtPosition(pos);
        Toast.makeText(view.getContext(), parent.getItemAtPosition(pos).toString(), Toast.LENGTH_SHORT).show();
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }

}
