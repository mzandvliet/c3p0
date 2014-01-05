package c3po.production;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.AbstractTickable;
import c3po.CircularArrayList;
import c3po.bitstamp.BitstampOrderBookJsonEventSource;
import c3po.bitstamp.BitstampOrderBookJsonSource;
import c3po.bitstamp.BitstampTickerJsonSource;
import c3po.bitstamp.BitstampTickerSource;
import c3po.events.IEventListener;
import c3po.orderbook.IOrderBookSource;
import c3po.orderbook.OrderBookPercentileSnapshot;
import c3po.orderbook.OrderBookPercentileTransformer;
import c3po.orderbook.OrderBookPricePercentileTransformer;
import c3po.orderbook.OrderBookVolumePercentileTransformer;
import c3po.orderbook.OrderPercentile;
import c3po.utils.Time;
import processing.core.*;

public class RealtimeOrderbookView extends PApplet {
	private static final long serialVersionUID = -3659687308548476180L;
	
	private static final float PRICE_SCALE = 1.0f;
	private static final float VOLUME_SCALE = 0.1f;
	private static final float TIME_SCALE = 100f;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(RealtimeBotRunner.class);

	private final static String jsonUrl = "https://www.bitstamp.net/api/order_book/";
	private final static long timestep = 5 * Time.SECONDS;
	private final static long timespan = 30 * Time.MINUTES;
	
	private static final double[] percentiles; 
	
	static {
		/*
		 *  Create the list of percentiles used in the orderbook transformation stage
		 *  
		 *  Note that these currently *need* to be in high-to-low order, like this:
		 *  
		 *  percentiles = new double[] { 99, 98, 97, 96, 95 };
		 */
		
		final double percentileStep = 0.5;
		final int numPercentiles = (int)(100d / percentileStep);		
		
		percentiles = new double[numPercentiles];
		for (int i = 0; i < numPercentiles; i++) {
			percentiles[i] = 100d-(i*percentileStep);
		}
	}
	
	private BitstampOrderBookJsonEventSource orderBook;
	private OrderBookPricePercentileTransformer percentileTransformer;
	private OrderBookPercentileBuffer percentileBuffer;
	
	private PObjectSystem objectSystem;
	private Camera camera;
	private AxisRenderer axisRenderer;
	private OrderBookRenderer orderBookRenderer;
	
	public void setup() {
		size(1280, 720, P3D);
		
		// Signal chain, event based
		
		orderBook = new BitstampOrderBookJsonEventSource(timestep, jsonUrl);
		percentileTransformer = new OrderBookPricePercentileTransformer(percentiles);
		percentileBuffer =  new OrderBookPercentileBuffer((int)(timespan / timestep));
		
		orderBook.addListener(percentileTransformer);
		percentileTransformer.addListener(percentileBuffer);
		
		// Rendering components
		
		objectSystem = new PObjectSystem();
		
		camera = new Camera(this);
		axisRenderer = new AxisRenderer(this);
		orderBookRenderer = new OrderBookRenderer(this, percentileBuffer);
		
		objectSystem.add(camera);
		objectSystem.add(axisRenderer);
		objectSystem.add(orderBookRenderer);
	}

	public void draw() {
		long clientTime = new Date().getTime();
		orderBook.tick(clientTime);
		
		updateScene();
	}
	
	private void updateScene() {
		camera.setPosition(1000f, -300f, 400f);
		camera.lookAt(new PVector(500f, 0f, -400f));
		
		background(104, 118, 212);
		
		objectSystem.update();
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
		
		public void update() {
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
	
	private class AxisRenderer extends PObject {
		public AxisRenderer(PApplet app) {
			super(app);
		}

		@Override
		public void draw() {
			app.strokeWeight(2);
			
			app.stroke(255, 0, 0);
			drawLine(new PVector(0f, 0f, 0f), new PVector(1000f, 0f, 0f));
			
			app.stroke(0, 255, 0);
			drawLine(new PVector(0f, 0f, 0f), new PVector(0f, -1000f, 0f));
			
			app.stroke(0, 0, 255);
			drawLine(new PVector(0f, 0f, 0f), new PVector(0f, 0f, -1000f));
		}
		
		private void drawLine(PVector a, PVector b) {
			app.line(a.x, a.y, a.z, b.x, b.y, b.z);
		}
	}
	
	private class OrderBookRenderer extends PObject {
		private final OrderBookPercentileBuffer buffer;
		
		public OrderBookRenderer(PApplet app, OrderBookPercentileBuffer buffer) {
			super(app);
			
			this.buffer = buffer;
		}
		
		@Override
		public void draw() {
			long clientTime = new Date().getTime();

			app.fill(200);
			app.stroke(255);
			app.strokeWeight(1);
			
			if (buffer.size() < 2)
				return;
			
			for (int i = 0; i < buffer.size()-1; i++) {
				OrderBookPercentileSnapshot snapshotA = buffer.get(i);
				OrderBookPercentileSnapshot snapshotB = buffer.get(i+1);
				
				app.beginShape(QUAD_STRIP);
				
				for (int j = 0; j < percentiles.length; j++) {
					OrderPercentile orderPercentileA = snapshotA.bids.get(j);
					OrderPercentile orderPercentileB = snapshotB.bids.get(j);
					
					PVector percentileVertexA = orderPercentileToVertex(orderPercentileA, clientTime, snapshotA.timestamp);
					PVector percentileVertexB = orderPercentileToVertex(orderPercentileB, clientTime, snapshotB.timestamp);
					
					app.vertex(percentileVertexA.x, percentileVertexA.y, percentileVertexA.z);
					app.vertex(percentileVertexB.x, percentileVertexB.y, percentileVertexB.z);
				}
				
				app.endShape();
			}
		}
		
		private PVector orderPercentileToVertex(OrderPercentile orderPercentile, long currentTime, long snapshotTime) {
			double delta = (double)(snapshotTime - currentTime) / (double)Time.MINUTES;
			return new PVector(
					(float)orderPercentile.price * PRICE_SCALE, // TODO: use order.price, of course
					(float)orderPercentile.volume * -VOLUME_SCALE,
					(float)delta * TIME_SCALE
			);
		}
	}
	
	private class OrderBookPercentileBuffer implements IEventListener<OrderBookPercentileSnapshot> {
		
		private final CircularArrayList<OrderBookPercentileSnapshot> buffer;
		
		public OrderBookPercentileBuffer(int length) {
			buffer = new CircularArrayList<OrderBookPercentileSnapshot>(length);
		}
		
		public int size() {
			return buffer.size();
		}
		
		public OrderBookPercentileSnapshot get(int i) {
			return buffer.get(i);
		}

		@Override
		public void onEvent(OrderBookPercentileSnapshot snapshot) {
			buffer.add(snapshot);
		}
	}
}
