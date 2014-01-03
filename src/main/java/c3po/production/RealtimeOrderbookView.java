package c3po.production;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.CircularArrayList;
import c3po.bitstamp.BitstampOrderBookJsonSource;
import c3po.bitstamp.BitstampTickerJsonSource;
import c3po.bitstamp.BitstampTickerSource;
import c3po.orderbook.IOrderBookSource;
import c3po.orderbook.OrderBookPercentileTransformer;
import c3po.orderbook.OrderBookPricePercentileTransformer;
import c3po.orderbook.OrderBookVolumePercentileTransformer;
import c3po.orderbook.OrderPercentile;
import c3po.utils.Time;
import processing.core.*;

public class RealtimeOrderbookView extends PApplet {
	private static final long serialVersionUID = -3659687308548476180L;
	private static final float PERCENTILE_SCALE = 10.0f;
	private static final float VOLUME_SCALE = 1f;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(RealtimeBotRunner.class);

	private final static long interpolationTime = 10 * Time.SECONDS;
	private final static long timestep = 5 * Time.SECONDS;
	private final static long timespan = 1 * Time.HOURS;
	
	private static final double[] percentiles = { 99, 98, 97, 96, 95, 94, 93, 92, 91, 90, 89, 88, 87, 86, 85, 84, 83, 82, 81, 80 };
	
	private PObjectSystem objectSystem;
	private Camera camera;
	private OrderBookRenderer orderBook;
	
	public void setup() {
		size(1280, 720, P3D);
		
		objectSystem = new PObjectSystem();
		
		camera = new Camera(this);
		orderBook = new OrderBookRenderer(this, new OrderBookPercentileBuffer((int)(timespan / timestep)));
		
		objectSystem.add(camera);
		objectSystem.add(orderBook);
	}

	public void draw() {
		updateScene();
	}
	
	private void updateScene() {
		camera.setPosition(850f, -200f, 200f);
		camera.lookAt(new PVector(900f, 0f, 0f));
		
		background(104, 118, 212);
		
		objectSystem.draw();
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
		private final OrderBookPercentileBuffer buffer;
		
		public OrderBookRenderer(PApplet app, OrderBookPercentileBuffer buffer) {
			super(app);
			
			this.buffer = buffer;
		}
		
		@Override
		public void draw() {
			buffer.update();
			
			long time = new Date().getTime() - interpolationTime;
			
			stroke(255);
			strokeWeight(1);
			noFill();
			
			for (int i = 0; i < buffer.size(); i++) {
				OrderBookPercentileSnapshot snapshot = buffer.get(i);
				
				double delta = (double)(snapshot.timestamp - time) / (double)Time.MINUTES;
				float z = (float)delta * 100f;
				
				beginShape();
				for (int j = 0; j < percentiles.length; j++) {
					OrderPercentile percentile = snapshot.bids[j];
					float x = (float)percentiles[j] * PERCENTILE_SCALE; // This does not have a value, currently. Volume is incorrect.
					float y = (float)percentile.volume * VOLUME_SCALE;
					
					vertex(x, y, z);
				}
				endShape();
			}
		}
	}
	
	private class OrderBookPercentileBuffer {
		private final OrderBookPercentileTransformer percentileTransformer;
		private final CircularArrayList<OrderBookPercentileSnapshot> buffer;
		
		public OrderBookPercentileBuffer(int size) {
			// Set up global signal tree
			//final BitstampTickerSource ticker = new BitstampTickerJsonSource(timestep, interpolationTime, "https://www.bitstamp.net:443/api/ticker/");
			final IOrderBookSource orderBook = new BitstampOrderBookJsonSource(timestep, "https://www.bitstamp.net/api/order_book/");
			
			percentileTransformer = new OrderBookPricePercentileTransformer(timestep, interpolationTime, percentiles, orderBook);
			buffer = new CircularArrayList<OrderBookPercentileSnapshot>(size);
		}
		
		public void update() {
			long time = new Date().getTime() - interpolationTime;
			
			// Get latest percentiles, store in buffer.
			// TODO: To achieve that currently you need to iterate over all the fucking outputs and group them together in a new datatype. That's a fucked up interface.
			
			// ERROROROROROR: You are writing to the buffer each frame, instead of when a new sample is actually available.
			
			OrderPercentile[] bids = new OrderPercentile[percentiles.length];
			OrderPercentile[] asks = new OrderPercentile[percentiles.length];
			
			for (int i = 0; i < percentiles.length; i++) {
				double bidPercentileVolume = percentileTransformer.getOutputBidPercentile(i).getSample(time).value;
				//double askPercentileVolume = percentileTransformer.getOutputAskPercentile(i).getSample(time).value;
				
				bids[i] = new OrderPercentile(percentiles[i], -1f, bidPercentileVolume);
				//asks[i] = new OrderPercentile(percentiles[i], -1f, askPercentileVolume);
			}
			
			buffer.add(new OrderBookPercentileSnapshot(time, bids, asks));
		}
		
		public int size() {
			return buffer.size();
		}
		
		public OrderBookPercentileSnapshot get(int i) {
			return buffer.get(i);
		}
	}
	
	private class OrderBookPercentileSnapshot {
		public final long timestamp;
		public final OrderPercentile[] bids;
		public final OrderPercentile[] asks;
		
		public OrderBookPercentileSnapshot(long timestamp, OrderPercentile[] bids, OrderPercentile[] asks) {
			this.timestamp = timestamp;
			this.bids = bids;
			this.asks = asks;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + Arrays.hashCode(asks);
			result = prime * result + Arrays.hashCode(bids);
			result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			OrderBookPercentileSnapshot other = (OrderBookPercentileSnapshot) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (!Arrays.equals(asks, other.asks))
				return false;
			if (!Arrays.equals(bids, other.bids))
				return false;
			if (timestamp != other.timestamp)
				return false;
			return true;
		}
		
		private RealtimeOrderbookView getOuterType() {
			return RealtimeOrderbookView.this;
		}
	}
}
