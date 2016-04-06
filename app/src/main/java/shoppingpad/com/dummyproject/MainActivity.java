package shoppingpad.com.dummyproject;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView messageText;
    private Button uploadButton, btnselectpic,decodeImg;
    private ImageView imageview,decodeImgView;
    private ProgressDialog dialog;
    Bitmap bitmap;

    private String imagepath = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        uploadButton = (Button) findViewById(R.id.buttonUpload);
        btnselectpic = (Button) findViewById(R.id.buttonChoose);
        decodeImg = (Button) findViewById(R.id.decodeImg);
        messageText = (TextView) findViewById(R.id.editText);
        imageview = (ImageView) findViewById(R.id.imageView);
        decodeImgView = (ImageView) findViewById(R.id.imageView2);

        btnselectpic.setOnClickListener(this);
        uploadButton.setOnClickListener(this);
        decodeImg.setOnClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        if (v == btnselectpic) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_PICK);
            startActivityForResult(Intent.createChooser(intent, "Complete action using"), 1);
        } else if (v == uploadButton) {

            dialog = ProgressDialog.show(MainActivity.this, "", "Uploading file...", true);
            messageText.setText("uploading started.....");
            new Thread(new Runnable() {
                public void run() {
                    Log.e("image path",imagepath);
                    new DemoAsync().execute();
                }
            }).start();
        }
        else if(v == decodeImg){
            Bitmap bit = decodeImage();
            decodeImgView.setImageBitmap(bit);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && resultCode == RESULT_OK) {
            Uri selectedImageUri = data.getData();
            imagepath = getPath(selectedImageUri);
            bitmap = BitmapFactory.decodeFile(imagepath);
            imageview.setImageBitmap(bitmap);
            messageText.setText("Uploading file path:" + imagepath);
        }
    }
    String encodedImage;

    public String getStringImage(Bitmap bmp){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageBytes = baos.toByteArray();
         encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);
        return encodedImage;
    }
    private Bitmap decodeImage() {
        byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
        return decodedByte;
    }

    public String getPath(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    private String uploadFile(String imagePath) {

        URL mProfilePicUrl;
        String response1 = "";

        String fname= imagePath.substring(imagePath.lastIndexOf("/")+1);

        //Prepare data passed to server...
        try {

            //Url on which data is to be posted...
            mProfilePicUrl = new URL("http://54.86.64.100:3000/api/v1/test/postdata");

            String img=getStringImage(bitmap);

            //Establish connection to url...
            HttpURLConnection urlConnection = (HttpURLConnection) mProfilePicUrl.openConnection();

            urlConnection.setDoOutput(true);
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoInput(true);
            urlConnection.setRequestProperty("Connection", "Keep-Alive");
            urlConnection.setRequestProperty("Cache-Control", "no-cache");
            urlConnection.setRequestProperty("Content-Type", "image/jpeg");

            OutputStreamWriter os = new OutputStreamWriter(urlConnection.getOutputStream());

            os.write(String.valueOf(img));
            os.flush();
            int code= urlConnection.getResponseCode();

            InputStream in = urlConnection.getInputStream();
            BufferedReader responseStreamReader = new BufferedReader(new InputStreamReader(in));
            String line = "";
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = responseStreamReader.readLine()) != null)
                stringBuilder.append(line).append("\n");
            responseStreamReader.close();

            response1 = stringBuilder.toString();
            System.out.println(response1);

            os.close();
            urlConnection.disconnect();

            }catch (Exception e){
            e.printStackTrace();
        }
        finally {
            dialog.dismiss();
        }
        return response1;
    }

    public String getPostDataString(ContentValues params) throws UnsupportedEncodingException{
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for(Map.Entry<String, Object> entry : params.valueSet()){
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue().toString(), "UTF-8"));
        }
        return result.toString();
    }

    class DemoAsync extends AsyncTask<String,String,String>{

        String res=null;
        @Override
        protected String doInBackground(String... params) {
            res = uploadFile(imagepath);
            return res;
        }

        @Override
        protected void onPostExecute(String s) {
            Toast.makeText(MainActivity.this, "response is: "+res, Toast.LENGTH_SHORT).show();
            super.onPostExecute(s);
        }
    }
}