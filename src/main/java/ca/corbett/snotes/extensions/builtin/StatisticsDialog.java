package ca.corbett.snotes.extensions.builtin;

import ca.corbett.snotes.Resources;
import ca.corbett.snotes.ui.MainWindow;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.SymbolAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.LookupPaintScale;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.xy.DefaultXYZDataset;
import org.jfree.data.xy.XYZDataset;
import org.knowm.xchart.HeatMapChart;
import org.knowm.xchart.HeatMapChartBuilder;
import org.knowm.xchart.XChartPanel;

import javax.swing.JDialog;
import java.awt.Color;
import java.awt.Dimension;

public class StatisticsDialog extends JDialog {

    public StatisticsDialog() {
        super(MainWindow.getInstance(), "Statistics", true);
        setSize(600, 300);
        setLocationRelativeTo(MainWindow.getInstance());
        setIconImage(Resources.getIconStats().getImage());

        addJFreeChartTest();
    }

    private void addJFreeChartTest() {
        // Let's see what JFreeChart can do...
        // We'll build a simple heatmap with 2 rows and 2 columns, with values in ascending order from 1 to 4:

        // --- Data ---
        // JFreeChart's DefaultXYZDataset wants three parallel double[] arrays: x, y, z
        // One entry per cell:
        double[] xValues = {0, 1, 2, 3, 4, 5, 6};
        double[] yValues = {0, 0, 0, 0, 0, 0, 0};
        double[] zValues = {1, 2, 3, 4, 3, 2, 1};  // zData[row][col] flattened, y-first

        DefaultXYZDataset dataset = new DefaultXYZDataset();
        dataset.addSeries("Test series", new double[][]{xValues, yValues, zValues});

        // --- Color scale ---
        // Map z values to colors. Define your range (min=1, max=4 here)
        double minZ = 1, maxZ = 4;
        LookupPaintScale paintScale = new LookupPaintScale(minZ, maxZ, Color.BLACK);

        // Build a smooth gradient across your desired color range
        int steps = 256;
        for (int i = 0; i < steps; i++) {
            float t = (float)i / (steps - 1);
            // Blue → White → Red (adjust to taste)
            Color c = blendColors(t, Color.LIGHT_GRAY, new Color(96, 96, 212));
            paintScale.add(minZ + t * (maxZ - minZ), c);
        }

        // --- Renderer ---
        XYBlockRenderer renderer = new XYBlockRenderer();
        renderer.setBlockWidth(1.0);   // width of each cell in data units
        renderer.setBlockHeight(1.0);
        renderer.setPaintScale(paintScale);

        // Tooltip generator — called automatically on mouseover
        String[] dayNames = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        renderer.setDefaultToolTipGenerator((ds, series, item) -> {
            XYZDataset xyzDs = (XYZDataset)ds;  // safe cast — we know what we put in
            int x = (int)ds.getXValue(series, item);
            double z = xyzDs.getZValue(series, item);
            return String.format("Day=%s, Value=%.0f", dayNames[x], z);
        });

        // --- Axes ---
        NumberAxis xAxis = new SymbolAxis("X Axis", dayNames);
        //xAxis.setRange(1d, 2d); // Limit to our data range
        xAxis.setRange(-0.5, 6.5); // cells are centered on integer coordinates?
        xAxis.setLabel(null);
        xAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        NumberAxis yAxis = new NumberAxis("Y Axis");
        //yAxis.setRange(1d, 2d); // Limit to our data range
        yAxis.setRange(-0.5, 0.5); // cells are centered on integer coordinates?
        yAxis.setLabel(null);
        yAxis.setTickLabelsVisible(false);
        yAxis.setMinorTickMarksVisible(false);
        yAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        // --- Plot and chart ---
        XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);
        plot.setBackgroundPaint(Color.DARK_GRAY);

        JFreeChart chart = new JFreeChart("Test Heatmap", plot);
        chart.setTitle("");
        chart.getLegend().setVisible(false);

        // --- Color scale legend (sidebar) ---
        NumberAxis scaleAxis = new NumberAxis("Value");
        scaleAxis.setRange(minZ, maxZ);
        PaintScaleLegend legend = new PaintScaleLegend(paintScale, scaleAxis);
        legend.setPosition(RectangleEdge.RIGHT);
        legend.setMargin(4, 4, 4, 4);
        //chart.addSubtitle(legend);

        // --- Panel (tooltips work automatically) ---
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(400, 320));
        add(chartPanel);
    }

    private void addXChartTest() {
        // Let's see what XChart can do...
        // We'll create a simple heatmap in the form of a square with 2 rows and 2 columns,
        // with values in ascending order from 1 to 4:
        HeatMapChartBuilder builder = new HeatMapChartBuilder()
            .width(160)
            .height(160)
            .title("Test Heatmap")
            .xAxisTitle("X Axis")
            .yAxisTitle("Y Axis");
        HeatMapChart testChart = builder.build();
        testChart.addSeries("Test series", new int[]{1, 2}, new int[]{1, 2}, new int[][]{
            {1, 2},
            {3, 4}
        });

        add(new XChartPanel<HeatMapChart>(testChart));
    }


    private Color blendColors(float t, Color... colors) {
        if (colors.length == 1) { return colors[0]; }
        // t is in [0,1]; map it across the color stops
        float scaled = t * (colors.length - 1);
        int idx = (int)scaled;
        if (idx >= colors.length - 1) { return colors[colors.length - 1]; }
        float local = scaled - idx;
        Color a = colors[idx], b = colors[idx + 1];
        return new Color(
            (int)(a.getRed() + local * (b.getRed() - a.getRed())),
            (int)(a.getGreen() + local * (b.getGreen() - a.getGreen())),
            (int)(a.getBlue() + local * (b.getBlue() - a.getBlue()))
        );
    }
}
