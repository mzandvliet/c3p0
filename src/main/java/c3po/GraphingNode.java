package c3po;

import java.awt.Color;

import javax.swing.JPanel;

import org.jfree.chart.*;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.*;
import org.jfree.data.time.*;
import org.jfree.data.xy.*;
import org.jfree.ui.*;
import org.jfree.chart.annotations.XYTextAnnotation;

/*
 * 
 * HOW TO USE
 * 
		// Graph that displays the macdDiff
		GraphingNode macdDiffGraph = new GraphingNode(macdNode.getOutput(4), "MacdDiff", 1000);
		macdDiffGraph.pack();
		macdDiffGraph.setVisible(true);
		
 */
/**
 * Example class of how graphing can work. We can either make this very configurable, or
 * extend it a couple times and configure it exactly how we want to display the data. 
 * I'd rather go with the second option
 */
public class GraphingNode extends ApplicationFrame implements ITickable, ITradeListener {
	private static final long serialVersionUID = 8607592670062359269L;
	
	private final long timestep;
	
	private final String title;
	private final ISignal[] inputs;
	private final TimeSeries[] signalTimeSeries;
	private final XYDataset dataset;
	private final JFreeChart chart;
	
	private long lastTick = -1;
	
	public GraphingNode(long timestep, String title, ISignal ... inputs) {
		super(title);
		
		this.timestep = timestep;
		this.title = title;
		
		this.inputs = inputs;
		
		signalTimeSeries = new TimeSeries[inputs.length];
		for (int i = 0; i < inputs.length; i++) {
			signalTimeSeries[i] = new TimeSeries("Signal_" + i);
		}
		
		dataset = createDatasetFromSeries(signalTimeSeries);
		
		chart = createChart(dataset);
		ChartPanel chartPanel = (ChartPanel) createPanel(chart);
		chartPanel.setPreferredSize(new java.awt.Dimension(1600, 900));
		setContentPane(chartPanel);
	}

	@Override
	public long getTimestep() {
		return timestep;
	}
	
	@Override
	public void tick(long tick) {
		if (tick > lastTick) {
			for (int i = 0; i < inputs.length; i++) {
				Sample newest = inputs[i].peek();
				signalTimeSeries[i].addOrUpdate(new Second(newest.getDate()), newest.value);
			}
		}
		
		lastTick = tick;
	}
	
	@Override
	public long getLastTick() {
		return lastTick;
	}
	
	@Override
	public void onTrade(TradeAction action) {
		XYTextAnnotation annotation = new XYTextAnnotation(action.action.toString(), 10000d, 500d);
		XYPlot plot = (XYPlot) chart.getPlot();
		plot.addAnnotation(annotation);
	}

	/**
     * Creates a panel
     *
     * @return A panel.
     */
    public JPanel createPanel(JFreeChart chart) {
        
        ChartPanel panel = new ChartPanel(chart);
        panel.setFillZoomRectangle(true);
        panel.setMouseWheelEnabled(true);
        return panel;
    }

    /**
     * Creates a chart.
     *
     * @param dataset  a dataset.
     *
     * @return A chart.
     */
    private JFreeChart createChart(final XYDataset dataset) {

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
            title,  // title
            "Date",             // x-axis label
            "Price Per Unit",   // y-axis label
            dataset,            // data
            true,               // create legend?
            true,               // generate tooltips?
            false               // generate URLs?
        );

        chart.setBackgroundPaint(Color.white);

        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.lightGray);
        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinePaint(Color.white);
        plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
        plot.setDomainCrosshairVisible(true);
        plot.setRangeCrosshairVisible(true);

        XYItemRenderer r = plot.getRenderer();
        if (r instanceof XYLineAndShapeRenderer) {
            XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) r;
            renderer.setBaseShapesVisible(false);
            renderer.setBaseShapesFilled(false);
            renderer.setDrawSeriesLineAsPath(true);
        }

        DateAxis axis = (DateAxis) plot.getDomainAxis();
        //axis.setDateFormatOverride(new SimpleDateFormat("MMM-yyyy"));

        return chart;

    }

    /**
     * Creates a dataset. 
     * 
     * TODO: For every signal you would like to have a TimeSeries I presume?
     *
     * @return The dataset.
     */
    private XYDataset createDatasetFromSeries(TimeSeries[] series) {
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        for (TimeSeries timeSeries : series) {
        	dataset.addSeries(timeSeries);
        }
        return dataset;
    }
}