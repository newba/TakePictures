package br.com.darksite.takepictures;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Main extends AppCompatActivity {

    //lembrar de aplicar as permissoes no AndroidManifest

    private ImageView imgView;
    private final int GALLERYIMG = 1;
    private final int PERMISSAO_REQUEST = 2;
    private final int CAMERA = 3;
    private final int TAKEPICTURE = 4;
    Button btnGallery, btnCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        /* em tempo de execução (runtime), definir a permissão
        Isto vai permitir que o usuário possa autorizar que o aplicado tenha
        acesso às fotos que estão no dispositivo. Note que o usuário pode permitir ou
        negar. Isto deve ser tratado pelo aplicativo
        */
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSAO_REQUEST);
            }
        }

       /* Permissao de gravação
        */

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSAO_REQUEST);
            }
        }

        imgView = (ImageView)findViewById(R.id.imgView);
        btnGallery = (Button)findViewById(R.id.btnGalley);

        btnGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Action_Pick associa a intent da galeria de imagens
                Intent i = new Intent (Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                // para retornar ele testa o codigo que foi enviado com o código passado, geralmente um int = 1
                // aqui mudamos o 1 por uma variavel GALLERYIMG
                startActivityForResult(i, GALLERYIMG);
            }
        });

        btnCamera = (Button)findViewById(R.id.btnCamera);
        /*
        Como estamos usando startActivityForResult o retorno da
        chamada da câmera será no método onActivityResult
         */
        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                Método anterior que chama a camera, mas nao grava na galeria
                Intent takePictureIntent = new
                        Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(takePictureIntent, CAMERA);
                }*/

                // método que pode ser usado para disparar a intent de utilização da câmera
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    try {
                        arquivoFoto = criarArquivo();
                    } catch (IOException ex) {
                        // Manipulação em caso de falha de criação do arquivo
                    }
                    if (arquivoFoto != null) {
                        Uri photoURI = FileProvider.getUriForFile(getBaseContext(),
                                getBaseContext().getApplicationContext().getPackageName() +
                                        ".provider", arquivoFoto);
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                        startActivityForResult(takePictureIntent, TAKEPICTURE);
                    }
                }
            }
        });
    }
    // implementação do método de retorno
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // aqui é onde testa-se o código de retorno (onde geralmente se usa uma constant numerica e usamos uma variavel)
        if (resultCode == RESULT_OK && requestCode == GALLERYIMG) {
            Uri selectedImage = data.getData();
            String[] filePath = { MediaStore.Images.Media.DATA };
            Cursor c = getContentResolver().query(selectedImage,filePath, null, null, null);
            c.moveToFirst();
            int columnIndex = c.getColumnIndex(filePath[0]);
            String picturePath = c.getString(columnIndex);
            c.close();
            Bitmap thumbnail = (BitmapFactory.decodeFile(picturePath));
            imgView.setImageBitmap(thumbnail);
        }

        //lembrando que CAMERA é uma constante que indica de qual activity estamos retornando
        if (requestCode == CAMERA && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            imgView.setImageBitmap(imageBitmap);
        }

        if (requestCode == TAKEPICTURE && resultCode == RESULT_OK) {
            sendBroadcast(new Intent(
                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                    Uri.fromFile(arquivoFoto))
            );
            exibirImagem();
        }
    }

    //O código abaixo faz o tratamento da resposta do usuário referente à permissao

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if (requestCode == PERMISSAO_REQUEST) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // A permissão foi concedida. Pode continuar
            } else {
            // A permissão foi negada. Precisa ver o que deve ser desabilitado
            }
            return;
        }
    }
    /* método para criar o nome do arquivo
    tem que ser um nome que não entre em conflito com algum nome já existente
        O mais comum é usar um formato onde seja utilizada uma variável data-hora
        para diferenciar os nomes das fotos
     */
    private File arquivoFoto = null;
    private File criarArquivo() throws IOException {
        String timeStamp = new
                SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File pasta = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File img = new File(pasta.getPath() + File.separator
                + "APKTAKEPICTURES_" + timeStamp + ".jpg");
        return img;
    }

    private void exibirImagem() {
        int targetW = imgView.getWidth();
        int targetH = imgView.getHeight();
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(arquivoFoto.getAbsolutePath(), bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        Bitmap bitmap =
                BitmapFactory.decodeFile(arquivoFoto.getAbsolutePath(), bmOptions);
        imgView.setImageBitmap(bitmap);
    }

}
