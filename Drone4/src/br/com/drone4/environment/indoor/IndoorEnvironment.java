package br.com.drone4.environment.indoor;

import static javax.media.opengl.GL.GL_LINEAR;
import static javax.media.opengl.GL.GL_TEXTURE_2D;
import static javax.media.opengl.GL.GL_TEXTURE_MAG_FILTER;
import static javax.media.opengl.GL.GL_TEXTURE_MIN_FILTER;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;

import br.com.drone4.control.Sensitivity;
import br.com.drone4.drone.PhantomDJI;
import br.com.drone4.drone.Roomba;
import br.com.drone4.model.sensor.camera.StandardCamera;
import br.com.etyllica.core.event.GUIEvent;
import br.com.etyllica.core.event.KeyEvent;
import br.com.etyllica.core.event.PointerEvent;
import br.com.etyllica.core.graphics.Graphic;
import br.com.luvia.grid.GridApplication;
import br.com.luvia.util.CameraGL;

import com.jogamp.opengl.util.awt.Screenshot;

public class IndoorEnvironment extends GridApplication {

	private StandardCamera droneCamera;

	protected CameraGL cameraGL;

	protected Roomba drone;

	protected double angleX = 0;

	protected double angleY = 0;

	protected double angleZ = 0;

	private boolean[][] floor;
	
	private final int tileSize = 10;

	public IndoorEnvironment(int w, int h) {
		super(w, h);
	}

	@Override
	public void init(GLAutoDrawable drawable) {

		GL2 gl = drawable.getGL().getGL2();

		// Global settings
		gl.glEnable(GL.GL_DEPTH_TEST);

		gl.glDepthFunc(GL.GL_LEQUAL);

		gl.glShadeModel(GL2.GL_SMOOTH);

		gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST);
		
	}
	
	@Override
	public void load() {

		//Size in meters		
		drone = new Roomba(1, 0, 0);

		droneCamera = drone.getCamera();

		cameraGL = new CameraGL(0, 20, -10);

		cameraGL.setTarget(drone);

		floor = new boolean[4][3];		
	
		updateAtFixedRate(300);

	}
	
	private void drawFloor(GL2 gl) {

		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

		drawFloor(gl, 200, 120);

	}

	private void drawFloor(GL2 gl, double x, double y) {

		double tileSize = 10;

		for(int j = 0; j < 3; j++) {

			for(int i = 0; i < 4; i++) {

				if(floor[i][j]) {
					
					gl.glColor3d(0, 0, 1);
					
				} else {
					
					gl.glColor3d(1, 0, 0);
					
				}				

				drawTile(gl, i, j, tileSize);

			}
		}

	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {

		GL2 gl = drawable.getGL().getGL2();

		gl.glViewport (x, y, width, height);

		gl.glMatrixMode(GL2.GL_PROJECTION);

		gl.glLoadIdentity();

		float aspect = (float)width / (float)height; 

		glu.gluPerspective(60, aspect, 1, 100);

	}

	@Override
	public GUIEvent updateKeyboard(KeyEvent event) {
		
		return GUIEvent.NONE;
	}

	public GUIEvent updateMouse(PointerEvent event) {

		return GUIEvent.NONE;
	}

	@Override
	public void display(GLAutoDrawable drawable) {

		GL2 gl = drawable.getGL().getGL2();

		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
		gl.glClearColor(1f, 1f, 1f, 1);

		captureCamera(gl, droneCamera);

		gl.glViewport(x, y, w, h);

		//Update Camera View
		updateCamera(gl, cameraGL);

		gl.glRotated(angleX, 1, 0, 0);
		gl.glRotated(angleY, 0, 1, 0);
		gl.glRotated(angleZ, 0, 0, 1);

		drawScene(gl);


	}

	private void captureCamera(GL2 gl, StandardCamera camera) {

		int w = camera.getWidth();

		int h = camera.getHeight();

		gl.glViewport(0, 0, w, h);

		//Update Camera View
		aimCamera(gl, camera);

		gl.glRotated(angleX, 1, 0, 0);
		gl.glRotated(angleY, 0, 1, 0);
		gl.glRotated(angleZ, 0, 0, 1);

		drawScene(gl);

		//Draw Drone Camera
		camera.setBufferedImage(Screenshot.readToBufferedImage(0, 0, w, h, false));

		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
		gl.glClearColor(1f, 1f, 1f, 1);

		gl.glLoadIdentity();
	}

	private void drawScene(GL2 gl) {

		//Draw Scene
		drawFloor(gl);

		drone.getModel().draw(gl);

		gl.glFlush();

	}
		
	private void verifyProjection() {
				
		final int tx = (int)(drone.getX()/tileSize);
		
		final int tz = (int)(drone.getZ()/tileSize);
		
		if((tx >= 0 && tx <= 3) && (tz >= 0 && tz <= 4)) {
			floor[tx][tz] = true;
		}
		
	}
	
	public void timeUpdate(long now) {
		
		verifyProjection();
		
		final int tx = (int)(drone.getX()/tileSize);
		
		final int tz = (int)(drone.getZ()/tileSize);

		if(tx<=3) {
			drone.goForward(Sensitivity.FULL_POSITIVE);
		}
		
	}

	@Override
	public void draw(Graphic g) {

		//Draw PipCamera
		g.drawImage(drone.getCamera().getBufferedImage(), 0, 60);

		//Draw Information
		g.setColor(Color.WHITE);
		g.drawShadow(20,20, "Scene",Color.BLACK);

		g.drawShadow(20,40, "AngleX: "+(angleX-5),Color.BLACK);

		g.drawShadow(20,60, "AngleY: "+(angleY),Color.BLACK);

		g.drawShadow(20,100, "DroneX: "+(drone.getX()),Color.BLACK);
		g.drawShadow(20,120, "DroneY: "+(drone.getY()),Color.BLACK);
		g.drawShadow(20,140, "DroneZ: "+(drone.getZ()),Color.BLACK);		

	}

	protected void drawPipCamera(Graphic g, BufferedImage pipCamera) {

		//AffineTransform transform = AffineTransform.getScaleInstance(640/w, 480/h);
		AffineTransform transform = AffineTransform.getScaleInstance(0.2, 0.2);

		AffineTransformOp op = new AffineTransformOp(transform, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);

		BufferedImage camera = op.filter(pipCamera, null);

		g.drawImage(camera, 50, 200);

	}

}