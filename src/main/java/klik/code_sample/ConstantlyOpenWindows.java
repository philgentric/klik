package klik.code_sample;

import java.util.Random;
        import java.util.concurrent.atomic.AtomicInteger;

        import javafx.animation.Animation;
        import javafx.animation.KeyFrame;
        import javafx.animation.Timeline;
        import javafx.application.Application;
        import javafx.beans.binding.NumberBinding;
        import javafx.beans.property.LongProperty;
        import javafx.beans.property.ObjectProperty;
        import javafx.beans.property.SimpleLongProperty;
        import javafx.beans.property.SimpleObjectProperty;
        import javafx.concurrent.ScheduledService;
        import javafx.concurrent.Task;
        import javafx.geometry.Pos;
        import javafx.scene.Scene;
        import javafx.scene.chart.LineChart;
        import javafx.scene.chart.NumberAxis;
        import javafx.scene.chart.XYChart.Data;
        import javafx.scene.chart.XYChart.Series;
        import javafx.scene.control.ContentDisplay;
        import javafx.scene.control.Label;
        import javafx.scene.image.ImageView;
        import javafx.scene.image.PixelWriter;
        import javafx.scene.image.WritableImage;
        import javafx.scene.layout.BorderPane;
        import javafx.scene.layout.Pane;
        import javafx.scene.layout.StackPane;
        import javafx.scene.layout.VBox;
        import javafx.scene.paint.Color;
        import javafx.stage.Screen;
        import javafx.stage.Stage;
        import javafx.util.Duration;

public class ConstantlyOpenWindows extends Application {

    @Override
    public void start(Stage primaryStage) {

        ScheduledService<?> service = periodicallyShowNewWindow();

        Pane root = createMemoryMonitor();
        primaryStage.setScene(new Scene(root, 800, 600));

        primaryStage.show();

        service.start();

    }


    private ScheduledService<Integer> periodicallyShowNewWindow() {

        Screen screen = Screen.getPrimary();
        double maxX = screen.getBounds().getMaxX();

        AtomicInteger count = new AtomicInteger();
        ObjectProperty<Stage> visibleStage = new SimpleObjectProperty<>();
        ScheduledService<Integer> service = new ScheduledService<Integer>() {
            @Override
            protected Task<Integer> createTask() {
                Task<Integer> task = new Task<Integer>() {
                    @Override
                    public Integer call() {
                        return count.incrementAndGet();
                    }
                };
                return task ;
            }
        };

        service.setOnSucceeded(event -> {
            Stage lastStage = visibleStage.get();
            Stage stage = createWindowWithImage(service.getValue());
            visibleStage.set(stage);
            stage.setX(maxX - 480);
            stage.show();
            if (lastStage != null) {
                lastStage.close();
            }
//            System.gc();
        });

        service.setPeriod(Duration.seconds(1));
        return service;
    }


    private Stage createWindowWithImage(int count) {
        Stage stage = new Stage();
        ImageView image = createImage();
        Label label = new Label("Window "+count);
        label.setGraphic(image);
        label.setContentDisplay(ContentDisplay.BOTTOM);
        stage.setScene( new Scene(new StackPane(label), 480, 500) );
        return stage;
    }

    private ImageView createImage() {
        WritableImage img = new WritableImage(400, 400);
        Random rng = new Random();
        int x = rng.nextInt(40);
        int y = rng.nextInt(40);
        PixelWriter pw = img.getPixelWriter();
        for (int i = 0; i < 400; i++) {
            for (int j = 0 ; j < 400 ; j++) {
                if (i >= x*10 && i < (x+1)*10 && j >= y*10 && j < (y+1) * 10) {
                    pw.setColor(i, j, Color.CORNFLOWERBLUE);
                } else {
                    pw.setColor(i, j, Color.ANTIQUEWHITE);
                }
            }
        }
        return new ImageView(img);
    }

    private Pane createMemoryMonitor() {
        LongProperty totalMemory = new SimpleLongProperty(Runtime.getRuntime().totalMemory());
        LongProperty freeMemory = new SimpleLongProperty(Runtime.getRuntime().freeMemory());
        LongProperty maxMemory = new SimpleLongProperty(Runtime.getRuntime().maxMemory());
        NumberBinding usedMemory = totalMemory.subtract(freeMemory);

        Label usedMemoryLabel = new Label();
        usedMemoryLabel.textProperty().bind(usedMemory.asString("Used memory: %,d"));
        Label freeMemoryLabel = new Label();
        freeMemoryLabel.textProperty().bind(freeMemory.asString("Free memory: %,d"));
        Label totalMemoryLabel = new Label();
        totalMemoryLabel.textProperty().bind(totalMemory.asString("Total memory: %,d"));
        Label maxMemoryLabel = new Label();
        maxMemoryLabel.textProperty().bind(maxMemory.asString("Max memory: %,d"));

        Series<Number, Number> series = new Series<>();
        series.setName("Used memory");

        AtomicInteger time = new AtomicInteger();

        Timeline updateMemory = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            totalMemory.set(Runtime.getRuntime().totalMemory());
            freeMemory.set(Runtime.getRuntime().freeMemory());
            maxMemory.set(Runtime.getRuntime().maxMemory());
            series.getData().add(new Data<>(time.incrementAndGet(), usedMemory.getValue()));
            if (series.getData().size() > 100) {
                series.getData().subList(0, series.getData().size() - 100).clear();
            }
        }));
        updateMemory.setCycleCount(Animation.INDEFINITE);
        updateMemory.play();

        VBox labels = new VBox(usedMemoryLabel, freeMemoryLabel, totalMemoryLabel, maxMemoryLabel);
        labels.setAlignment(Pos.CENTER);

        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Time");
        xAxis.setForceZeroInRange(false);
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Memory");
        LineChart<Number, Number> chart = new LineChart<Number, Number>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.getData().add(series);

        BorderPane root = new BorderPane(chart, labels, null, null, null);

        return root;
    }

    public static void main(String[] args) {
        launch(args);
    }
}