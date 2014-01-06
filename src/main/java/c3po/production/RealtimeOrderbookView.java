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
import c3po.orderbook.OrderBookVolumeByPriceTransformer;
import c3po.orderbook.OrderBookVolumePercentileTransformer;
import c3po.orderbook.OrderPercentile;
import c3po.utils.SignalMath;
import c3po.utils.Time;
import processing.core.*;

public class RealtimeOrderbookView extends PApplet {
	private static final long serialVersionUID = -3659687308548476180L;
	private static final Logger LOGGER = LoggerFactory.getLogger(RealtimeBotRunner.class);
	
	private static final float PRICE_RANGE = 3000.0f;
	private static final float VOLUME_RANGE = 10000f;
	private static final float TIME_RANGE = 1000f; // Minutes
	
	private static final float PRICE_SCALE = 1.0f;
	private static final float VOLUME_SCALE = 0.01f;
	private static final float TIME_SCALE = 5f;
	
	private static final long TIME_UNIT = Time.MINUTES;

	private final static String jsonUrl = "https://www.bitstamp.net/api/order_book/";
	private final static long timestep = 10 * Time.SECONDS;
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
	private OrderBookVolumeByPriceTransformer percentileTransformer;
	private OrderBookPercentileBuffer percentileBuffer;
	
	private PObjectSystem objectSystem;
	private Camera camera;
	private AxisRenderer axisRenderer;
	private OrderBookRenderer orderBookRenderer;
	
	public void setup() {
		size(1280, 720, P3D);
		
		// Signal chain, event based
		
		orderBook = new BitstampOrderBookJsonEventSource(timestep, jsonUrl);
		percentileTransformer = new OrderBookVolumeByPriceTransformer(percentiles, 200d);
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
	
	private float lastMouseX;
	private float lastMouseY;
	
	private float cameraX = 900f;
	private float cameraY = -100f;
	private float cameraTargetX = cameraX;
	private float cameraTargetY = cameraY;
	
	private void updateScene() {
		PVector orbitPoint = new PVector(900f, -50f, -200f);
		
		if (mousePressed) {
			cameraTargetX = cameraX + (mouseX - lastMouseX) * -1f;
			cameraTargetY = cameraY + (mouseY - lastMouseY) * -1f;
			cameraTargetX = SignalMath.clamp(cameraTargetX, 0f, 1200f);
			cameraTargetY = SignalMath.clamp(cameraTargetY, -300f, 0f);
		}
		
		cameraX = cameraTargetX; //SignalMath.interpolate(cameraX, cameraTargetX, 0.33f);
		cameraY = cameraTargetY;//SignalMath.interpolate(cameraY, cameraTargetY, 0.33f);
		
		lastMouseX = mouseX;
		lastMouseY = mouseY;
		
		PVector cameraPosition = new PVector(cameraX, cameraY, 150f);
		
		camera.setPosition(cameraPosition);
		camera.lookAt(orbitPoint);
		
		background(240, 240, 255);
		
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
		
		public void setPosition(PVector position) {
			this.position = position;
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
			final float xRange = PRICE_RANGE * PRICE_SCALE;
			final float yRange = VOLUME_RANGE * VOLUME_SCALE;
			final float zRange = TIME_RANGE * TIME_SCALE;
			
			final float xStep = 10;
			final float yStep = 100f;
			
			app.strokeWeight(2);
			
			app.stroke(255, 0, 0);
			drawLine(new PVector(0f, 0f, 0f), new PVector(xRange, 0f, 0f));
			
			app.stroke(0, 255, 0);
			drawLine(new PVector(0f, 0f, 0f), new PVector(0f, -yRange, 0f));
			
			app.stroke(0, 0, 255);
			drawLine(new PVector(0f, 0f, 0f), new PVector(0f, 0f, -zRange));
			
			app.strokeWeight(1);
			app.stroke(255);
			
			final int xNumLines = (int)(xRange / xStep);
			final int yNumLines = (int)(yRange / yStep);
			
			for (int x = 0; x < xNumLines; x++) {
				drawLine(
						new PVector(x * xStep, 0f, 0f),
						new PVector(x * xStep, 0f, -100f)
				);
				//app.text("" + x, x * xStep, 20f, 0f);
			}
			for (int y = 0; y < yNumLines; y++) {
				drawLine(
					new PVector(0f, y * -yStep, 0f),
					new PVector(0f, y * -yStep, -100f)
				);
			}
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
			
			app.strokeWeight(1);
			
			if (buffer.size() < 2)
				return;
			
			app.stroke(150, 150, 255);
			app.fill(100, 100, 255);
			
			for (int i = 0; i < buffer.size()-1; i++) {
				OrderBookPercentileSnapshot snapshotA = buffer.get(i);
				OrderBookPercentileSnapshot snapshotB = buffer.get(i+1);
				double priceOffsetA = 0f; //-snapshotA.highestBid;
				double priceOffsetB = 0f; //-snapshotB.highestBid;
				double deltaTimeA = (double)(snapshotA.timestamp - clientTime) / TIME_UNIT;
				double deltaTimeB = (double)(snapshotB.timestamp - clientTime) / TIME_UNIT;
				
				app.beginShape(QUAD_STRIP);
				
				for (int j = 0; j < percentiles.length; j++) {
					OrderPercentile bidPercentileA = snapshotA.bids.get(j);
					OrderPercentile bidPercentileB = snapshotB.bids.get(j);
					
					PVector bidPercentileVertexA = orderPercentileToVertex(bidPercentileA, priceOffsetA, deltaTimeA);
					PVector bidPercentileVertexB = orderPercentileToVertex(bidPercentileB, priceOffsetB, deltaTimeB);
					
					app.vertex(bidPercentileVertexA.x, bidPercentileVertexA.y, bidPercentileVertexA.z);
					app.vertex(bidPercentileVertexB.x, bidPercentileVertexB.y, bidPercentileVertexB.z);
				}
				
				app.endShape();
			}
			
			app.stroke(255, 150, 150);
			app.fill(255, 100, 100);
			
			for (int i = 0; i < buffer.size()-1; i++) {
				OrderBookPercentileSnapshot snapshotA = buffer.get(i);
				OrderBookPercentileSnapshot snapshotB = buffer.get(i+1);
				double priceOffsetA = 0f; //-snapshotA.lowestAsk;
				double priceOffsetB = 0f; //-snapshotB.lowestAsk;
				double deltaTimeA = (double)(snapshotA.timestamp - clientTime) / TIME_UNIT;
				double deltaTimeB = (double)(snapshotB.timestamp - clientTime) / TIME_UNIT;
				
				app.beginShape(QUAD_STRIP);
				
				for (int j = 0; j < percentiles.length; j++) {
					OrderPercentile askPercentileA = snapshotA.asks.get(j);
					OrderPercentile askPercentileB = snapshotB.asks.get(j);
					
					PVector askPercentileVertexA = orderPercentileToVertex(askPercentileA, priceOffsetA, deltaTimeA);
					PVector askPercentileVertexB = orderPercentileToVertex(askPercentileB, priceOffsetB, deltaTimeB);
					
					app.vertex(askPercentileVertexA.x, askPercentileVertexA.y, askPercentileVertexA.z);
					app.vertex(askPercentileVertexB.x, askPercentileVertexB.y, askPercentileVertexB.z);
				}
				
				app.endShape();
			}
		}
		
		private PVector orderPercentileToVertex(OrderPercentile orderPercentile, double priceOffset, double deltaTime) {
			return new PVector(
					(float)(orderPercentile.price + priceOffset) * PRICE_SCALE,
					(float)orderPercentile.volume * -VOLUME_SCALE,
					(float)deltaTime * TIME_SCALE
			);
		}
	}
	
	/*
	 *  TODO: Specify maximum item age, prune old entries on every update.
	 *  
	 *  This is needed because buffer length is no longer linearly correlated to update rate.
	 */
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
