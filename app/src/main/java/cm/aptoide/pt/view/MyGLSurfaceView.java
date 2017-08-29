package cm.aptoide.pt.view;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import cm.aptoide.pt.AptoideApplication;

/**
 * Created by jdandrade on 14/11/2016.
 */
public class MyGLSurfaceView extends GLSurfaceView {

  private MyGLRenderer myGLRenderer;

  public MyGLSurfaceView(Context context, AttributeSet attrs) {
    super(context, attrs);
    setTransparent(context);
  }

  public MyGLSurfaceView(Context context) {
    super(context);
    setTransparent(context);
  }

  private void setTransparent(Context context) {
    setEGLContextClientVersion(2);

    myGLRenderer = new MyGLRenderer(context,
        ((AptoideApplication) context.getApplicationContext()).getQManager());
    setRenderer(myGLRenderer);

    setZOrderOnTop(true);
    SurfaceHolder surfaceHolder = getHolder();
    surfaceHolder.setFormat(PixelFormat.TRANSPARENT);
  }
}
