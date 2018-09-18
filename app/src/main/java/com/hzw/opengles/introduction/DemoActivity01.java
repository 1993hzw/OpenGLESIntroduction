package com.hzw.opengles.introduction;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.hzw.opengles.introduction.util.OpenGlUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class DemoActivity01 extends Activity {
    static final float CUBE[] = {
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f, 1.0f,
            1.0f, 1.0f,
    };
    public static final float TEXTURE_NO_ROTATION[] = {
            0.0f, 1.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,
    };

    private GLSurfaceView mGLSurfaceView;
    private Bitmap mBitmap;
    private int mGLTextureId = OpenGlUtils.NO_TEXTURE;
    private GPUImageFilter mGPUImageFilter = new GPUImageFilter();

    private FloatBuffer mGLCubeBuffer;
    private FloatBuffer mGLTextureBuffer;
    private int mOutputWidth, mOutputHeight;
    private int mImageWidth, mImageHeight;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_01);
        mGLSurfaceView = findViewById(R.id.gl_surfaceview);

        mBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.thelittleprince);
        mImageWidth = mBitmap.getWidth();
        mImageHeight = mBitmap.getHeight();

        mGLCubeBuffer = ByteBuffer.allocateDirect(CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLCubeBuffer.put(CUBE).position(0);

        mGLTextureBuffer = ByteBuffer.allocateDirect(TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();

        mGLSurfaceView.setRenderer(new MyRender());
        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    private class MyRender implements GLSurfaceView.Renderer {

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            GLES20.glClearColor(0, 0, 0, 1);
            GLES20.glDisable(GLES20.GL_DEPTH_TEST); // 当我们需要绘制透明图片时，就需要关闭它
            mGPUImageFilter.init();

            Bitmap resizedBitmap = null;
            if (mBitmap.getWidth() % 2 == 1) {
                resizedBitmap = Bitmap.createBitmap(mBitmap.getWidth() + 1, mBitmap.getHeight(),
                        Bitmap.Config.ARGB_8888);
                Canvas can = new Canvas(resizedBitmap);
                can.drawARGB(0x00, 0x00, 0x00, 0x00);
                can.drawBitmap(mBitmap, 0, 0, null);
            } else {
            }

            mGLTextureId = OpenGlUtils.loadTexture(
                    resizedBitmap != null ? resizedBitmap : mBitmap, mGLTextureId, false);
            mGLSurfaceView.requestRender();
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            GLES20.glViewport(0, 0, width, height); // 设置窗口大小
            mOutputWidth = width;
            mOutputHeight = height;
            GLES20.glViewport(0, 0, width, height);
            GLES20.glUseProgram(mGPUImageFilter.getProgram());
            mGPUImageFilter.onOutputSizeChanged(width, height);
            adjustImageScaling();
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            mGPUImageFilter.onDraw(mGLTextureId, mGLCubeBuffer, mGLTextureBuffer);
        }

        private void adjustImageScaling() {
            float outputWidth = mOutputWidth;
            float outputHeight = mOutputHeight;

            float ratio1 = outputWidth / mImageWidth;
            float ratio2 = outputHeight / mImageHeight;
            float ratioMax = Math.max(ratio1, ratio2);
            int imageWidthNew = Math.round(mImageWidth * ratioMax);
            int imageHeightNew = Math.round(mImageHeight * ratioMax);

            float ratioWidth = imageWidthNew / outputWidth;
            float ratioHeight = imageHeightNew / outputHeight;

            float[] cube = CUBE;
            float[] textureCords = TEXTURE_NO_ROTATION;
            cube = new float[]{
                    CUBE[0] / ratioHeight, CUBE[1] / ratioWidth,
                    CUBE[2] / ratioHeight, CUBE[3] / ratioWidth,
                    CUBE[4] / ratioHeight, CUBE[5] / ratioWidth,
                    CUBE[6] / ratioHeight, CUBE[7] / ratioWidth,
            };

            mGLCubeBuffer.clear();
            mGLCubeBuffer.put(cube).position(0);
            mGLTextureBuffer.clear();
            mGLTextureBuffer.put(textureCords).position(0);
        }
    }
}
