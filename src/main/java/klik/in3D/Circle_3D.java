package klik.in3D;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.*;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.PickResult;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.stage.Window;
import klik.Context_type;
import klik.New_context;
import klik.Shared_services;
import klik.Window_provider;
import klik.browser.virtual_landscape.Shutdown_target;
import klik.browser.virtual_landscape.Virtual_landscape;
import klik.change.history.History_engine;
import klik.properties.Sort_files_by;
import klik.properties.boolean_features.Feature;
import klik.properties.boolean_features.Feature_cache;
import klik.util.execute.actor.Aborter;
import klik.path_lists.Path_list_provider_for_file_system;
import klik.images.Image_window;
import klik.look.Look_and_feel_manager;
import klik.util.execute.actor.Actor_engine;
import klik.util.log.Logger;
import klik.util.ui.Hourglass;
import klik.util.ui.Progress_window;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

//*******************************************************
public class Circle_3D implements Window_provider, Shutdown_target
//*******************************************************
{
    public static final boolean dbg = false;
    final double CORRIDOR_HEIGHT = 2048;
    final double CORRIDOR_WIDTH = 2*CORRIDOR_HEIGHT;

    final PerspectiveCamera perspective_camera = new PerspectiveCamera(true);
    double camera_look_angle_Y = 0; // Left/right look (mouse)
    double camera_path_angle = 0;   // Position around circle i.e. in the corridor

    double circle_radius = 5000; // Radius of circular corridor
    int num_segments; // Number of wall segments i.e. pictures displayed

    double mouse_old_X, mouse_pos_X, mouse_delta_X;

    private boolean boxes_are_facing_camera = false;
    // Keep track of all boxes to update their orientation
    private List<Box_and_angle> inner_boxes = new ArrayList<>();
    private List<Box_and_angle> outer_boxes = new ArrayList<>();

    final PhongMaterial grey_material = new PhongMaterial(Color.LIGHTGRAY);

    private final Image_source item_source;
    private final Logger logger;
    private final Stage stage;
    private final Path  the_path;
    double inner_box_size;
    double outer_box_size;

    Group floor_group;
    Group position_indicator_group;
    Group inner_boxes_group = new Group();
    Group outer_boxes_group = new Group();
    private List<Box> allFloorTiles = new ArrayList<>();
    private final int large_icon_size;
    private final int small_icon_size;
    Map<Image_and_path,PhongMaterial> material_cache_small = new HashMap<>();
    Map<Image_and_path,PhongMaterial> material_cache_large = new HashMap<>();


    //*******************************************************
    public Circle_3D(New_context context, Logger logger)
    //*******************************************************
    {
        this.the_path = context.path_list_provider.get_folder_path();
        History_engine.get(get_owner()).add(the_path.toAbsolutePath().toString());

        this.large_icon_size = (int) CORRIDOR_HEIGHT;
        this.small_icon_size = 64;
        this.stage = (Stage)context.originator;
        this.logger = logger;
        //image_source = new Dummy_text_image_source(icon_size,30000);

        this.item_source = new Image_source_from_files( the_path,small_icon_size,large_icon_size,stage,logger);

        String s = Shared_services.main_properties().get(Sort_files_by.SORT_FILES_BY);
        if ( !s.equals(Sort_files_by.NAME.name()))
        {
            Shared_services.main_properties().set(Sort_files_by.SORT_FILES_BY, Sort_files_by.NAME.name());
        }
        Hourglass hourglass = Circle_3D.get_hourglass(stage,logger);

        Scene scene = get_scene();
        stage.setScene(scene);

        stage.show();
        stage.setTitle(context.path_list_provider.get_folder_path().toAbsolutePath().toString());
        hourglass.close();

    }

    //*******************************************************
    public Scene get_scene()
    //*******************************************************
    {
        int number_of_items = item_source.how_many_items();

        // Calculate segments needed (2 walls per segment + max 3 blanks)
        int wallsNeeded = number_of_items + 3;
        if (wallsNeeded < 30) wallsNeeded = 30;
        int segmentsNeeded = (wallsNeeded) / 2; // Round up, 2 walls per segment

        // Use calculated segments instead of fixed NUM_SEGMENTS
        num_segments = segmentsNeeded;


        // Calculate circle radius based on desired box width
        double desiredBoxWidth = CORRIDOR_HEIGHT; // Adjust this for preferred image size
        double angleStep = 360.0 / num_segments;
        double angleStepRad = Math.toRadians(angleStep);

        // For inner wall: arcLength = radius * angleRad, so radius = arcLength / angleRad
        double calculatedRadius = desiredBoxWidth / angleStepRad;
        circle_radius = calculatedRadius;

        logger.log("Using " + num_segments + " segments with radius " + circle_radius);

        double boxDepth = 10;



        double innerRadius = circle_radius - CORRIDOR_WIDTH / 2;
        double outerRadius = circle_radius + CORRIDOR_WIDTH / 2;
        double innerArcLength = innerRadius * angleStepRad;
        inner_box_size = innerArcLength;
        double outerArcLength = outerRadius * angleStepRad;
        outer_box_size = outerArcLength;


        init_boxes(angleStep,
                  boxDepth,
                  innerRadius,
                  outerRadius);


        create_floor(stage,logger);

        double dome_radius = circle_radius * 20;
        Group ceilingGroup = create_sky_ceiling(dome_radius,stage,logger);

        AmbientLight ambientLight = new AmbientLight(Color.LIGHTGRAY);

        Group corridor = new Group(floor_group, ceilingGroup, inner_boxes_group, outer_boxes_group, ambientLight);

        SubScene subScene = new SubScene(corridor, 800, 600, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.DARKGRAY);
        position_indicator_group = createCircularPositionIndicator();


        StackPane stackPane = new StackPane();
        stackPane.getChildren().addAll(subScene, position_indicator_group);
        // Bind SubScene size to StackPane
        subScene.widthProperty().bind(stackPane.widthProperty());
        subScene.heightProperty().bind(stackPane.heightProperty());
        // Position indicator at top-center
        StackPane.setAlignment(position_indicator_group, javafx.geometry.Pos.TOP_CENTER);
        StackPane.setMargin(position_indicator_group, new javafx.geometry.Insets(20, 0, 0, 0));

        // Camera setup
        perspective_camera.setNearClip(1.0);
        perspective_camera.setFarClip(dome_radius*1.5);
        logger.log("camera FarClip = " + perspective_camera.getFarClip());
        perspective_camera.setFieldOfView(60);

        subScene.setCamera(perspective_camera);
        resetCamera();

        Scene scene = new Scene(stackPane, 800, 600);

        HBox buttons_box = new HBox();
        buttons_box.setPickOnBounds(false); // otherwise double click on Boxes does not work
        stackPane.getChildren().add(buttons_box);
        StackPane.setAlignment(buttons_box, Pos.TOP_LEFT);
        StackPane.setMargin(buttons_box, new Insets(10));         // 10‑pixel margin from the edges
        if ( the_path.getParent() != null)
        {
            Button up = new Button("Up");
            Look_and_feel_manager.set_button_look(up, true, stage, logger);
            up.setOnAction(event -> {
                New_context.replace_different_folder(this, Context_type.File_system_3D,new Path_list_provider_for_file_system(the_path.getParent()),stage,logger);
            });
            buttons_box.getChildren().add(up);
        }

        {
            Button undo_and_bookmark_and_history = Virtual_landscape.make_button_undo_and_bookmark_and_history(Context_type.File_system_3D,20, stage, logger);
            buttons_box.getChildren().add(undo_and_bookmark_and_history);
        }

        setupClickHandling(scene,logger);

        scene.setOnMousePressed(me -> mouse_old_X = me.getSceneX());
        scene.setOnMouseDragged(me -> {
            mouse_pos_X = me.getSceneX();
            mouse_delta_X = mouse_pos_X - mouse_old_X;
            camera_look_angle_Y -= mouse_delta_X * 0.3;
            update_camera_and_boxes();
            mouse_old_X = mouse_pos_X;
        });

        scene.setOnScroll(e -> on_scroll(e));

        //scene.setOnKeyPressed(e ->on_key_pressed(e));
        scene.addEventFilter(KeyEvent.KEY_PRESSED,e->on_key_pressed(e));

        // Initial update of box orientations
        update_boxes();

        return scene;
    }


    //*******************************************************
    private void init_boxes(
            double angleStep,
            double boxDepth,
            double innerRadius,
            double outerRadius)
    //*******************************************************
    {
        for (int i = 0; i < num_segments; i++)
        {
            double angle = Math.toRadians(i * angleStep);

            Box innerBox = new Box(boxDepth, inner_box_size, inner_box_size);
            innerBox.setCullFace(CullFace.BACK);
            innerBox.setMaterial(grey_material);
            innerBox.setTranslateX(innerRadius * Math.cos(angle));
            innerBox.setTranslateY(0);
            innerBox.setTranslateZ(innerRadius * Math.sin(angle));
            innerBox.getTransforms().add(new Rotate(-Math.toDegrees(angle), Rotate.Y_AXIS));
            inner_boxes.add(new Box_and_angle(innerBox, angle));

            Box outerBox = new Box(boxDepth, outer_box_size, outer_box_size);
            outerBox.setCullFace(CullFace.BACK);
            outerBox.setMaterial(grey_material);
            outerBox.setTranslateX(outerRadius * Math.cos(angle));
            outerBox.setTranslateY(0);
            outerBox.setTranslateZ(outerRadius * Math.sin(angle));
            outerBox.getTransforms().add(new Rotate(-Math.toDegrees(angle), Rotate.Y_AXIS));
            outer_boxes.add(new Box_and_angle(outerBox, angle));
        }
    }


    //*******************************************************
    private void updateCamera()
    //*******************************************************
    {
        double angleRad = Math.toRadians(camera_path_angle);
        double camX = circle_radius * Math.cos(angleRad);
        double camZ = circle_radius * Math.sin(angleRad);

        perspective_camera.getTransforms().setAll(
                new javafx.scene.transform.Translate(camX, 0, camZ),
                new Rotate(camera_look_angle_Y, Rotate.Y_AXIS)
                                     );

        if (position_indicator_group != null) {
            updateCircularPositionIndicator(position_indicator_group);
        }
    }

    //*******************************************************
    private void resetCamera()
    //*******************************************************
    {
        camera_look_angle_Y = 0; // No default offset
        camera_path_angle = 0;
        update_camera_and_boxes();
    }


    private long last_update_time = 0;
    private static final long UPDATE_INTERVAL_MS = 50; // Update every 50ms max
    List<Box_and_angle> boxes_to_update = new ArrayList<>();
    private List<PhongMaterial> materials_to_apply = new ArrayList<>();

    //*******************************************************
    private void update_boxes()
    //*******************************************************
    {
        long now = System.currentTimeMillis();
        if (now - last_update_time < UPDATE_INTERVAL_MS) {
            return; // Skip update
        }
        last_update_time = now;

        boxes_to_update.clear();
        materials_to_apply.clear();

        // Get camera position
        double cameraAngleRad = Math.toRadians(camera_path_angle);
        double camX = circle_radius * Math.cos(cameraAngleRad);
        double camZ = circle_radius * Math.sin(cameraAngleRad);


        // Cull floor tiles
        double maxFloorDistance = CORRIDOR_WIDTH * 8;
        for (Box tile : allFloorTiles) {
            double tileX = tile.getTranslateX();
            double tileZ = tile.getTranslateZ();
            double distance = Math.sqrt((camX - tileX) * (camX - tileX) + (camZ - tileZ) * (camZ - tileZ));

            if (distance > maxFloorDistance) {
                if (tile.getParent() != null) {
                    floor_group.getChildren().remove(tile);
                }
            } else {
                if (tile.getParent() == null) {
                    floor_group.getChildren().add(tile);
                }
            }
        }


        // Update each box to face directly toward the camera
        int item_index = 0;
        for (int i = 0; i < num_segments; i++)
        {
            double max_distance = CORRIDOR_WIDTH*20;
            Box_and_angle baa = outer_boxes.get(i);
            update_one_box(item_index++, baa, camX, camZ, max_distance, outer_boxes_group);
        }
        for (int i = 0; i < num_segments; i++)
        {
            double max_distance = CORRIDOR_WIDTH * 10;
            Box_and_angle baa = inner_boxes.get(i);
            update_one_box(item_index++, baa, camX, camZ, max_distance, inner_boxes_group);
        }

        // Apply all material changes at once
        for (int i = 0; i < boxes_to_update.size(); i++)
        {
            Box box = boxes_to_update.get(i).box();
            box.setMaterial(materials_to_apply.get(i));
            double angle = boxes_to_update.get(i).angle();
            if(boxes_are_facing_camera)
            {
                double innerBoxX = box.getTranslateX();
                double innerBoxZ = box.getTranslateZ();

                double boxToCamX = camX - innerBoxX;
                double boxToCamZ = camZ - innerBoxZ;
                angle = Math.atan2(boxToCamZ, boxToCamX);
            }

            // Apply rotation to make box face the camera
            box.getTransforms().clear();
            box.getTransforms().add(new Rotate(-Math.toDegrees(angle), Rotate.Y_AXIS));

        }
    }

    //*******************************************************
    private boolean update_one_box(
            int image_index,
            Box_and_angle baa,
            double camX, double camZ,
            double max_distance,
            Group parent_group)
    //*******************************************************
    {
        Box box = baa.box();
        double innerBoxX = box.getTranslateX();
        double innerBoxZ = box.getTranslateZ();

        double distance_to_camera = Math.sqrt( (camX - innerBoxX)*(camX - innerBoxX) + (camZ - innerBoxZ)*(camZ - innerBoxZ));

        if ( distance_to_camera > max_distance)
        {
            parent_group.getChildren().remove(box);
            //box.setMaterial(null);//greyMaterial);
            return false;
        }

        if ( !parent_group.getChildren().contains(box))
        {
            parent_group.getChildren().add(box);
        }

        boxes_to_update.add(baa);

        // load image
        Image_and_path iap = item_source.get(image_index);
        if ( iap == null)
        {
            materials_to_apply.add(grey_material);
        }
        else
        {
            if ( distance_to_camera < 2*CORRIDOR_WIDTH)
            {
                //logger.log(iap.path +" IS CLOSE => large image");
                materials_to_apply.add(get_phong_large(iap));
            }
            else
            {
                //logger.log(iap.path +" IS far => large image");
                materials_to_apply.add(get_phong_small(iap));
            }
            box.setUserData(iap.path);
            if (Feature_cache.get(Feature.Show_file_names_as_tooltips))
            {
                Tooltip.install(box, new Tooltip(iap.path.getFileName().toString()));
            }
        }


        return true;
    }

    //*******************************************************
    private PhongMaterial get_phong_small(Image_and_path iap)
    //*******************************************************
    {
        if ( material_cache_small.containsKey(iap))
        {
            return material_cache_small.get(iap);
        }
        material_cache_small.put(iap, new PhongMaterial(){
            {
            Image local_image = iap.small_image;
            setDiffuseMap(local_image);}
        });
        return material_cache_small.get(iap);
    }

    //*******************************************************
    private PhongMaterial get_phong_large(Image_and_path iap)
    //*******************************************************
    {
        if ( material_cache_large.containsKey(iap))
        {
            return material_cache_large.get(iap);
        }
        material_cache_large.put(iap, new PhongMaterial(){
            {
                Image local_image = iap.get_large_image(large_icon_size);
                setDiffuseMap(local_image);
            }
        });
        return material_cache_large.get(iap);
    }




    //*******************************************************
    private void setupClickHandling(Scene scene, Logger logger)
    //*******************************************************
    {
        scene.setOnMouseClicked(event -> {
            //if ( dbg)

                logger.log(event.toString());
            if (event.getClickCount() < 2) return; // Only handle double-clicks
            if (event.getClickCount() == 3)
            {
                toggle_boxes_are_facing_camera();
                return;
            }
            logger.log(" double click !");
            PickResult pickResult = event.getPickResult();
            Node clickedNode = pickResult.getIntersectedNode();

            if (clickedNode instanceof Box)
            {
                logger.log(" clicked item is a Box");
                Box clickedBox = (Box) clickedNode;
                Path p = (Path) clickedBox.getUserData();
                if ( p == null)
                {
                    //if ( dbg)
                        logger.log("No user data!");
                    return;
                }

                logger.log("Clicked on: " + p);

                if (Files.isDirectory(p))
                {
                    logger.log("is folder: "+p);
                    New_context.replace_different_folder(this, Context_type.File_system_3D,new Path_list_provider_for_file_system(p),stage,logger);
                }
                else 
                {
                    logger.log("is not folder : "+p);
                    Image_window image_stage = Image_window.get_Image_window(p, new Path_list_provider_for_file_system(p.getParent()), Optional.empty(),scene.getWindow(),new Aborter("dummy",logger),logger);
                }
            }
            else
            {
                logger.log("clicked item is not a Box but a "+clickedNode.getClass().getCanonicalName());
            }
        });
    }

    public static Hourglass get_hourglass(Stage stage, Logger logger) {
        Hourglass hourglass = Progress_window.show(
                true,
                "Wait, loading in 3D",
                20000,
                100,
                100,
                stage,
                logger);
        return hourglass;
    }

    //*******************************************************
    private void toggle_boxes_are_facing_camera()
    //*******************************************************
    {
        boxes_are_facing_camera = !boxes_are_facing_camera;
        //updateCamera();
        update_boxes();
    }


    //*******************************************************
    private void create_floor(Window owner, Logger logger)
    //*******************************************************
    {
        floor_group = new Group();
        allFloorTiles.clear();
        PhongMaterial redMaterial = new PhongMaterial(Color.RED);

        Image floor_image = Look_and_feel_manager.get_floor_icon(small_icon_size,owner,logger);

        PhongMaterial floorMaterial = null;
        if ( floor_image != null)
        {
            Image finalFloor_image = floor_image;
            floorMaterial =new PhongMaterial() {{setDiffuseMap(finalFloor_image); }};
        }
        else {
            floorMaterial = new PhongMaterial(Color.LIGHTGRAY);
        }

        // Tile size - adjust based on your texture resolution
        double tileSize = 2000;//Math.max(200, circle_radius / 50); // Square tiles
        double tileThickness = 10;

        // Calculate grid bounds to cover the circular corridor area
        // We need to cover from -outerRadius to +outerRadius
        double outerRadius = circle_radius + CORRIDOR_WIDTH / 2;
        double innerRadius = circle_radius - CORRIDOR_WIDTH / 2;

        int tilesCreated = 0;

        // Camera starts at angle 0, which is position (CIRCLE_RADIUS, 0, 0)
        double cameraStartX = circle_radius;
        double cameraStartZ = 0;

// Only render floor tiles within visible corridor range
        double maxFloorDistance = CORRIDOR_WIDTH * 8; // Much less than dome radius
        double gridStart = -(outerRadius + tileSize);
        double gridEnd = outerRadius + tileSize;

        for (double x = gridStart; x < gridEnd; x += tileSize) {
            for (double z = gridStart; z < gridEnd; z += tileSize) {
                // Check if this tile intersects with the corridor ring
                double tileCenterX = x + tileSize / 2;
                double tileCenterZ = z + tileSize / 2;
                double distanceFromCenter = Math.sqrt(tileCenterX * tileCenterX + tileCenterZ * tileCenterZ);

                //double outerRadiusCheck = circle_radius + CORRIDOR_WIDTH / 2;

                // Only create tiles that are within the corridor ring
                if (distanceFromCenter >= innerRadius - tileSize &&
                        distanceFromCenter <= outerRadius + tileSize) {

                    Box floorTile = new Box(tileSize, tileThickness, tileSize);
                    allFloorTiles.add(floorTile);
                    floorTile.setMaterial(floorMaterial);
                    floorTile.setTranslateX(tileCenterX);
                    floorTile.setTranslateY(CORRIDOR_HEIGHT / 2 - tileThickness / 2);
                    floorTile.setTranslateZ(tileCenterZ);

                    //floorGroup.getChildren().add(floorTile);
                    tilesCreated++;
                }
            }
        }


        // Create separate red origin marker with fixed size
        double markerSize = Math.min(tileSize * 0.3, 600);
        Box redMarker = new Box(markerSize, tileThickness *2, markerSize);
        redMarker.setMaterial(redMaterial);
        redMarker.setTranslateX(cameraStartX);
        redMarker.setTranslateY(CORRIDOR_HEIGHT / 2 - tileThickness / 2 + 1); // Slightly above floor
        redMarker.setTranslateZ(cameraStartZ);
        floor_group.getChildren().add(redMarker);



        logger.log("Created " + tilesCreated + " floor tiles");
    }

    //*******************************************************
    private Group create_ceiling()
    //*******************************************************
    {
        PhongMaterial ceilingMaterial = new PhongMaterial(Color.SKYBLUE);

        Group ceilingGroup = new Group();

        double tileSize = Math.max(2000, circle_radius / 50); // Square tiles
        double tileThickness = 10;

        double outerRadius = circle_radius + CORRIDOR_WIDTH / 2;
        double gridStart = -outerRadius - tileSize;
        double gridEnd = outerRadius + tileSize;

        int tilesCreated = 0;

        for (double x = gridStart; x < gridEnd; x += tileSize) {
            for (double z = gridStart; z < gridEnd; z += tileSize) {
                double tileCenterX = x + tileSize / 2;
                double tileCenterZ = z + tileSize / 2;
                double distanceFromCenter = Math.sqrt(tileCenterX * tileCenterX + tileCenterZ * tileCenterZ);

                double innerRadius = circle_radius - CORRIDOR_WIDTH / 2;
                double outerRadiusCheck = circle_radius + CORRIDOR_WIDTH / 2;

                if (distanceFromCenter >= innerRadius - tileSize &&
                        distanceFromCenter <= outerRadiusCheck + tileSize) {

                    Box ceilingTile = new Box(tileSize, tileThickness, tileSize);
                    ceilingTile.setMaterial(ceilingMaterial);
                    ceilingTile.setTranslateX(tileCenterX);
                    ceilingTile.setTranslateY(-CORRIDOR_HEIGHT / 2 + tileThickness / 2);
                    ceilingTile.setTranslateZ(tileCenterZ);

                    ceilingGroup.getChildren().add(ceilingTile);
                    tilesCreated++;
                }
            }
        }

        logger.log("Created " + tilesCreated + " ceiling tiles");
        return ceilingGroup;
    }


    long start_shift_down = -1;
    //*******************************************************
    private void on_scroll(ScrollEvent se)
    //*******************************************************
    {
        mouse_delta_X = se.getDeltaY();
        if (mouse_delta_X == 0) return;
        double stepAngle = 360.0 / (num_segments * 50.0);
        if ( se.isShiftDown())
        {
            long now  = System.currentTimeMillis();
            if ( start_shift_down < 0)
            {
                start_shift_down = now;
            }
            else if ( now-start_shift_down> 3000)
            {
                stepAngle *= 40000;
                if ( dbg) logger.log("40000 stepAngle="+stepAngle);

            }
            else if ( now-start_shift_down> 1000)
            {
                stepAngle *= 1000;
                if ( dbg) logger.log("1000 stepAngle="+stepAngle);
            }
            else
            {
                stepAngle *= 100;
                if ( dbg) logger.log("100 stepAngle="+stepAngle);
            }
        }
        else
        {
            start_shift_down = -1;
            if ( dbg) logger.log("zero stepAngle="+stepAngle);
        }
        if ( se.isControlDown()) stepAngle /= 10;
        if ( mouse_delta_X < 0)
        {
            camera_path_angle += stepAngle;
            camera_look_angle_Y -= stepAngle;
        }
        else
        {
            camera_path_angle -= stepAngle;
            camera_look_angle_Y += stepAngle;
        }
        update_camera_and_boxes();

        mouse_old_X = mouse_pos_X;
    }

    //*******************************************************
    private void update_camera_and_boxes()
    //*******************************************************
    {
        updateCamera();
        update_boxes();
    }


    final long[] last_time = {System.currentTimeMillis()};
    final int[] count = {0};
    //*******************************************************
    private void on_key_pressed(KeyEvent event)
    //*******************************************************
    {
        //logger.log("on_key_pressed="+event);

        if (event.getCode() == KeyCode.ESCAPE)
        {
            logger.log("✅ 3D Window RECEIVED ESCAPE");
            event.consume();

            if (Feature_cache.get(Feature.Use_escape_to_close_windows))
            {
                shutdown();
            }
            else
            {
                logger.log("✅ ESCAPE ignored by user preference");
            }
            return;
        }

        long now = System.currentTimeMillis();
        double stepAngle =  360.0 / (num_segments * 10.0);
        //logger.log("stepAngle="+stepAngle);

        if (now - last_time[0] < 100)
        {
            stepAngle *= 3;
            count[0]++;
            //logger.log("stepAngle, count="+count[0]);
            if (count[0] > 30) // 3 seconds
            {
                stepAngle *= 1000;
                if ( dbg)
                    logger.log("1000 stepAngle="+stepAngle);
            }
            else if (count[0] > 10) // 1 seconds
            {
                stepAngle *= 100;
                if ( dbg)
                    logger.log("100 stepAngle="+stepAngle);
            }
            else if (count[0] > 5)
            {
                stepAngle *= 10;
                if ( dbg)
                    logger.log("10 stepAngle="+stepAngle);
            }
            else
            {
                if ( dbg)
                    logger.log("stepAngle="+stepAngle);
            }

        }
        else
        {
            if ( dbg) logger.log("stepAngle, count="+0);
            count[0] = 0;
        }

        last_time[0] = now;


        double delta_camera_path_angle;
        double delta_camera_look_angle_Y;
        switch (event.getCode())
        {
            case UP:
            case RIGHT:
                delta_camera_path_angle = stepAngle;
                delta_camera_look_angle_Y = -stepAngle;
                break;

            case DOWN:
            case LEFT:
                delta_camera_path_angle = -stepAngle;
                delta_camera_look_angle_Y = stepAngle;
                break;
            default:
                return;
        }
        event.consume();

        // Keep angle in 0-360 range

        double small_andle = 360.0 / (num_segments * 50.0);
        int iterations = (int)(delta_camera_path_angle/small_andle)*3;
        logger.log("iterations ="+ iterations);
        if ( iterations <0) iterations = -iterations;
        if ( iterations == 0) iterations = 1;
        if ( iterations > 100) iterations = 100;


        double camera_path_small_angle;
        if (delta_camera_path_angle>0)
        {
            camera_path_small_angle = small_andle;
        }
        else
        {
            camera_path_small_angle = -small_andle;
        }
        double camera_look_small_angle_Y;
        if (delta_camera_look_angle_Y>0)
        {
            camera_look_small_angle_Y = small_andle;
        }
        else
        {
            camera_look_small_angle_Y = -small_andle;
        }

        int finalIterations = iterations;
        Actor_engine.execute(()-> {
            for (int i = 0; i < finalIterations; i++)
            {
                camera_path_angle += camera_path_small_angle;
                camera_look_angle_Y += camera_look_small_angle_Y;
                camera_path_angle = camera_path_angle % 360;
                camera_look_angle_Y = camera_look_angle_Y % 360;
                Platform.runLater(() -> update_camera_and_boxes());
                try {
                    Thread.sleep(20/(i+1));
                } catch (InterruptedException e) {
                    logger.log(""+e);
                }
            }
        },"smoother",logger);
    }

    //*******************************************************
    private Group create_sky_ceiling(double dome_radius,Window owner, Logger logger)
    //*******************************************************
    {
        Group ceilingGroup = new Group();

        // Create a sphere for the dome
        Sphere dome = new Sphere(dome_radius,128);
        dome.setCullFace(CullFace.FRONT); // Render inside only

        // Load night sky texture
        Image nightSkyImage = Look_and_feel_manager.get_sky_icon(small_icon_size,owner,logger);

        PhongMaterial domeMaterial;
        if (nightSkyImage != null) {
            Image finalNightSkyImage = nightSkyImage;
            domeMaterial = new PhongMaterial() {{
                setDiffuseMap(finalNightSkyImage);
                setSpecularColor(Color.BLACK); // No reflections
            }};
        } else {
            // Fallback to dark blue if no texture
            logger.log("falling back to solid color for dome");
            domeMaterial = new PhongMaterial(Color.MIDNIGHTBLUE);
        }

        dome.setMaterial(domeMaterial);

        // Position dome high above corridor
        dome.setTranslateY(-10*CORRIDOR_HEIGHT);//-domeRadius + CORRIDOR_HEIGHT); // Bottom of dome at corridor top

        ceilingGroup.getChildren().add(dome);

        logger.log("Created dome ceiling with radius: " + dome_radius);
        return ceilingGroup;
    }


    //*******************************************************
    private Group createCircularPositionIndicator()
    //*******************************************************
    {
        Group indicator = new Group();

        // Outer circle (track)
        javafx.scene.shape.Circle outerCircle = new javafx.scene.shape.Circle(50);
        outerCircle.setFill(Color.TRANSPARENT);
        outerCircle.setStroke(Color.WHITE);
        outerCircle.setStrokeWidth(2);

        // Inner dot (position marker)
        javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(5);
        dot.setFill(Color.RED);

        indicator.getChildren().addAll(outerCircle, dot);
        indicator.setUserData(dot); // Store dot reference for updates

        return indicator;
    }

    //*******************************************************
    private void updateCircularPositionIndicator(Group indicator)
    //*******************************************************
    {
        javafx.scene.shape.Circle dot = (javafx.scene.shape.Circle) indicator.getUserData();

        // Convert camera angle to radians (0° = top of circle)
        double angleRad = Math.toRadians(camera_path_angle - 90); // -90 to start at top

        // Position dot on circle circumference
        double radius = 45; // Slightly less than outer circle radius
        double dotX = radius * Math.cos(angleRad);
        double dotY = radius * Math.sin(angleRad);

        dot.setTranslateX(dotX);
        dot.setTranslateY(dotY);
    }

    //*******************************************************
    @Override
    public Window get_owner()
    //*******************************************************
    {
        return stage;
    }

    @Override
    public void shutdown() {
        stage.close();
    }
}
