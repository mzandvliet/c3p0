package c3po;

import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.JPanel;

import org.jfree.chart.*;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.*;
import org.jfree.data.time.*;
import org.jfree.data.xy.*;
import org.jfree.ui.*;

/**
 * Example class of how graphing can work. We can either make this very configurable, or
 * extend it a couple times and configure it exactly how we want to display the data. 
 * I'd rather go with the second option
 */
public class GraphingNode extends ApplicationFrame implements INode {
	private static final long serialVersionUID = 8607592670062359269L;
	private ISignal input;
	private int kernelSize;
	private CircularArrayList<Sample> buffer;
	private OutputSignal output;
	private long lastTick = -1;
	private String title;
	
	// Used timeseries
	private TimeSeries s1;
	
	public GraphingNode(ISignal input, String title, int kernelSize) {
		super(title);
		this.title = title;
		this.input = input;
		this.kernelSize = kernelSize;
		this.buffer = new CircularArrayList<Sample>(kernelSize * 2);
		this.output = new OutputSignal(this);
		
		ChartPanel chartPanel = (ChartPanel) createDemoPanel();
		chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
		setContentPane(chartPanel);
	}

	@Override
	public int getNumOutputs() {
		return 1;
	}

	@Override
	public ISignal getOutput(int i) {
		return output;
	}

	@Override
	public void tick(long tick) {
		if (tick > lastTick) {
			Sample newest = input.getSample(tick);
			buffer.add(newest);

			s1.addOrUpdate(new Second(newest.getDate()), newest.value);
			
			output.setSample(newest);
			lastTick = tick;
		}
	}

    /**
     * Creates a chart.
     *
     * @param dataset  a dataset.
     *
     * @return A chart.
     */
    private JFreeChart createChart(XYDataset dataset) {

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
            renderer.setBaseShapesVisible(true);
            renderer.setBaseShapesFilled(true);
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
    private XYDataset createDataset() {
        s1 = new TimeSeries("Ticker");

        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(s1);

        return dataset;
    }

    /**
     * Creates a panel for the demo (used by SuperDemo.java).
     *
     * @return A panel.
     */
    public JPanel createDemoPanel() {
        JFreeChart chart = createChart(createDataset());
        ChartPanel panel = new ChartPanel(chart);
        panel.setFillZoomRectangle(true);
        panel.setMouseWheelEnabled(true);
        return panel;
    }
}