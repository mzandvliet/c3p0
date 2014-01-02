package c3po.production;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.CircularArrayList;
import c3po.bitstamp.BitstampOrderBookJsonSource;
import c3po.bitstamp.BitstampTickerJsonSource;
import c3po.bitstamp.BitstampTickerSource;
import c3po.clock.IRealtimeClock;
import c3po.orderbook.IOrderBookSource;
import c3po.orderbook.OrderBookPercentileSource;
import c3po.orderbook.OrderBookPercentileTransformer;
import c3po.orderbook.OrderBookVolumePercentileParser;
import c3po.orderbook.OrderBookVolumePercentileTransformer;
import c3po.utils.Time;
import processing.core.*;
import processing.opengl.*;

public class RealtimeOrderbookView extends PApplet {
	private static final long serialVersionUID = -3659687308548476180L;
	private static final float SCALE = 50.0f;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(RealtimeBotRunner.class);

	private final static long interpolationTime = 10 * Time.SECONDS;
	private final static long timestep = 5 * Time.SECONDS;
	private final static long timespan = 4 * Time.HOURS;
	
	private static final double[] percentiles = { 99.5, 99.0, 98.5, 98.0, 97.5 , 97.0, 96.5, 96, 95.5, 95 };
	
	PObjectSystem objectSystem;
	private Camera camera;
	
	public void setup() {
		size(1280, 720, P3D);
		
		objectSystem = new PObjectSystem();
		
		camera = new Camera(this);
		
		objectSystem.add(camera);
	}

	public void draw() {
		updateScene();
		renderScene();
	}
	
	private void updateScene() {
		camera.setPosition(0f, -100f, 0f);
		camera.lookAt(new PVector(0f, 0f, -100f));
		
		objectSystem.draw();
	}
	
	private void renderScene() {
		background(0);
		
		stroke(255);
		
		for (int i = 0; i < 10; i++) {
			beginShape(QUAD_STRIP);
			for (int j = 0; j < 10; j++) {
				vertex(i * SCALE, 0f, j * -SCALE);
				vertex(i * SCALE, 0f, (j+1) * -SCALE);
			}
			endShape();
		}
	}
	
	private abstract class PObject {
		protected PApplet app;
		
		public PObject(PApplet app) {
			this.app = app;
		}
		
		public abstract void draw();
	}
	
	private class PObjectSystem {
		private List<PObject> objects;
		
		public PObjectSystem() {
			this.objects = new ArrayList<PObject>();
		}
		
		public void add(PObject object) {
			objects.add(object);
		}
		
		public void remove(PObject object) {
			objects.remove(object);
		}
		
		public void draw() {
			for (PObject object : objects) {
				object.draw();
			}
		}
	}
	
	private class Camera extends PObject {
		private final PVector sceneOffset;
		private PVector position = new PVector(0f, 0f, 0f);
		private PVector lookTarget = new PVector(0f, 0f, -1f);
		private PVector up = new PVector (0f, 1f, 0f);
		
		public Camera(PApplet app) {
			super(app);
			sceneOffset = new PVector(-app.width * 0.5f, -app.height * 0.5f, 0f);
		}
		
		public void setPosition(float x, float y, float z) {
			position.x = x;
			position.y = y;
			position.z = z;
		}
		
		public void translate(float x, float y, float z) {
			position.x += x;
			position.y += y;
			position.z += z;
		}
		
		public void lookAt(PVector targetPosition) {
			this.lookTarget = targetPosition;
		}
		
		@Override
		public void draw() {
			app.camera(
				position.x, position.y, position.z,
				lookTarget.x, lookTarget.y, lookTarget.z,
				up.x, up.y, up.z);
		}
	}
	
	private class OrderBookRenderer extends PObject {
		private final OrderBookPercentileTransformer percentileTransformer;
		
		private final CircularArrayList<OrderBookPercentileSnapshot> buffer;
		
		public OrderBookRenderer(PApplet app) {
			super(app);

			// Set up global signal tree
			final BitstampTickerSource ticker = new BitstampTickerJsonSource(timestep, interpolationTime, "https://www.bitstamp.net:443/api/ticker/");
			final IOrderBookSource orderBook = new BitstampOrderBookJsonSource(timestep, "https://www.bitstamp.net:443/api/order_book/");
			percentileTransformer = new OrderBookVolumePercentileTransformer(timestep, interpolationTime, percentiles, orderBook);
		}
		
		@Override
		public void draw() {
			long time = new Date().getTime();
			
			// Todo: hook stuff up and render it
		}

		private class OrderBookPercentileSnapshot {
			
		}
	}
}
