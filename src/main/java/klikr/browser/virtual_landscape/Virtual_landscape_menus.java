// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ../About_klik_stage.java
//SOURCES ../Icon_size.java
//SOURCES ../../image_ml/face_recognition/Face_recognition_service.java
//SOURCES ../../image_ml/ML_servers_util.java
//SOURCES ../../image_ml/image_similarity/Similarity_cache.java
//SOURCES ../../image_ml/image_similarity/Feature_vector_cache.java
//SOURCES ../../util/files_and_paths/Name_cleaner.java
//SOURCES ../../experimental/metadata/Tag_items_management_stage.java
//SOURCES ../../properties/boolean_features/Preferences_stage.java
//SOURCES ../items/Item_folder.java
//SOURCES ../../look/Look_and_feel_style.java
//SOURCES ../../look/my_i18n/Language.java



package klikr.browser.virtual_landscape;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Window;
import klikr.util.External_application;
import klikr.Klikr_application;
import klikr.Window_type;
import klikr.Instructions;
import klikr.audio.Audio_player;
import klikr.path_lists.Path_list_provider;
import klikr.util.cache.Cache_folder;
import klikr.util.cache.RAM_caches;
import klikr.util.execute.Execute_result;
import klikr.util.execute.actor.Actor_engine;
import klikr.browser.Icon_size;
import klikr.path_lists.Path_list_provider_for_file_system;
import klikr.browser.items.Item_folder;
import klikr.browser.locator.Folders_with_large_images_locator;
import klikr.change.Change_gang;
import klikr.change.Change_receiver;
import klikr.change.active_list_stage.Active_list_stage;
import klikr.change.active_list_stage.Active_list_stage_action;
import klikr.change.active_list_stage.Datetime_to_signature_source;
import klikr.change.history.History_engine;
import klikr.change.bookmarks.Bookmarks;
import klikr.change.history.History_item;
import klikr.change.undo.Undo_for_moves;
import klikr.change.undo.Undo_item;
import klikr.experimental.deduplicate.Deduplication_engine;
import klikr.machine_learning.face_recognition.Face_recognition_service;
import klikr.machine_learning.deduplication.Deduplication_by_similarity_engine;
import klikr.machine_learning.feature_vector.Feature_vector_cache;
import klikr.images.Image_context;
import klikr.util.image.decoding.Exif_metadata_extractor;
import klikr.look.Look_and_feel_manager;
import klikr.look.Look_and_feel_style;
import klikr.look.my_i18n.My_I18n;
import klikr.look.my_i18n.Language;
import klikr.machine_learning.feature_vector.Feature_vector_source;
import klikr.machine_learning.image_similarity.Feature_vector_source_for_image_similarity;
import klikr.machine_learning.song_similarity.Feature_vector_source_for_song_similarity;
import klikr.properties.*;
import klikr.properties.boolean_features.Booleans;
import klikr.properties.boolean_features.Feature;
import klikr.properties.boolean_features.Feature_cache;
import klikr.util.execute.Execute_command;
import klikr.util.execute.System_open_actor;
import klikr.util.files_and_paths.*;
import klikr.util.files_and_paths.old_and_new.Command;
import klikr.util.files_and_paths.old_and_new.Old_and_new_Path;
import klikr.util.files_and_paths.old_and_new.Status;
import klikr.util.info_stage.Info_stage;
import klikr.util.info_stage.Line_for_info_stage;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;
import klikr.util.ui.*;
import klikr.util.ui.progress.Hourglass;
import klikr.util.ui.progress.Progress_window;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Supplier;

//**********************************************************
public class Virtual_landscape_menus
//**********************************************************
{
    private static final boolean dbg = false;
    public static final String CONTACT_SHEET_FILE_NAME = "contact_sheet.pdf";
    private static final int MAX_MENU_ITEM_STRING_LENGTH = 150;
    public final Virtual_landscape virtual_landscape;
    public final Change_receiver change_receiver;
    public final Window owner;
    public final Logger logger;
    CheckMenuItem select_all_files_menu_item;
    CheckMenuItem select_all_folders_menu_item;

    private static final double too_far_away_image = 0.14;
    private static final double too_far_away_song = 0.05;//0.04;

    //**********************************************************
    Virtual_landscape_menus(Virtual_landscape virtual_landscape, Change_receiver change_receiver, Window owner)
    //**********************************************************
    {
        this.virtual_landscape = virtual_landscape;
        this.change_receiver = change_receiver;
        this.owner = owner;
        this.logger = virtual_landscape.logger;
    }


    //**********************************************************
    ContextMenu define_files_ContextMenu()
    //**********************************************************
    {
        ContextMenu files_menu = new ContextMenu();
        Look_and_feel_manager.set_context_menu_look(files_menu,owner,logger);

        files_menu.getItems().add(make_select_all_files_menu_item(logger));
        if ( virtual_landscape.context_type == Window_type.File_system_2D) files_menu.getItems().add(make_select_all_folders_menu_item(logger));

        {
            String create_string = My_I18n.get_I18n_string("Create",owner,logger);
            Menu create = new Menu(create_string);
            Look_and_feel_manager.set_menu_item_look(create,owner,logger);

            if ( virtual_landscape.context_type == Window_type.File_system_2D)
            {
                Menu_items.add_menu_item_for_menu("Create_new_empty_directory", null,
                        event -> create_new_directory(),create,owner,logger);
            }
            /*if (Feature_cache.get(Feature.Enable_image_playlists))
            {
                logger.log(Stack_trace_getter.get_stack_trace("not implemented"));
                //Menu_items.add_menu_item2("Create_new_empty_image_playlist",event -> Instructions.create_new_image_playlist(owner, logger)));
            }*/
            Menu_items.add_menu_item_for_menu("Create_PDF_contact_sheet", null,event -> create_PDF_contact_sheet(),create,owner,logger);
            if ( virtual_landscape.context_type == Window_type.File_system_2D)
            {
                Menu_items.add_menu_item_for_menu("Stash_Files_In_Folders_By_Year", null, event -> sort_by_time(Virtual_landscape.Sort_by_time.year),create,owner,logger);
                Menu_items.add_menu_item_for_menu("Stash_Files_In_Folders_By_Month", null,event -> sort_by_time(Virtual_landscape.Sort_by_time.month),create,owner,logger);
                Menu_items.add_menu_item_for_menu("Stash_Files_In_Folders_By_Day", null,event -> sort_by_time(Virtual_landscape.Sort_by_time.day),create,owner,logger);
                create.getItems().add(make_import_menu());
            }
            files_menu.getItems().add(create);
        }
        {
            String search_string = My_I18n.get_I18n_string("Search",owner,logger);
            Menu search = new Menu(search_string);
            Look_and_feel_manager.set_menu_item_look(search,owner,logger);

            Menu_items.add_menu_item_for_menu("Search_by_keywords", virtual_landscape.find.getDisplayText(), event -> search_files_by_keyworks_fx(),search,owner,logger);
            if ( virtual_landscape.context_type == Window_type.File_system_2D)
            {
                Menu_items.add_menu_item_for_menu("Show_Where_Are_Images", null,event -> show_where_are_images(),search,owner,logger);
            }
            search.getItems().add(make_add_to_Enable_face_recognition_training_set_menu_item());


            files_menu.getItems().add(search);
        }
        if (Booleans.get_boolean_defaults_to_false(Feature.Enable_face_recognition.name()))
        {
            Menu face_recognition = new Menu("Face recognition");
            Look_and_feel_manager.set_menu_item_look(face_recognition,owner,logger);

            face_recognition.getItems().add(make_load_face_recog_menu_item());
            face_recognition.getItems().add(make_save_face_recog_menu_item());
            face_recognition.getItems().add(make_reset_face_recog_menu_item());
            face_recognition.getItems().add(make_start_auto_face_recog_menu_item());
            face_recognition.getItems().add(make_whole_folder_face_recog_menu_item());

            files_menu.getItems().add(face_recognition);
        }
        if ( virtual_landscape.context_type == Window_type.File_system_2D)
        {
            String cleanup = My_I18n.get_I18n_string("Clean_Up",owner,logger);
            Menu menu = new Menu(cleanup);
            Look_and_feel_manager.set_menu_item_look(menu,owner,logger);


            Menu_items.add_menu_item_for_menu("Remove_empty_folders",null,
                    event -> remove_empty_folders_fx(false),menu,owner,logger);

            if (Booleans.get_boolean_defaults_to_false(Feature.Enable_recursive_empty_folders_removal.name()))
            {
                Menu_items.add_menu_item_for_menu("Remove_empty_folders_recursively", null,event -> remove_empty_folders_fx(true),menu,owner,logger);
            }
            if (Booleans.get_boolean_defaults_to_false(Feature.Enable_name_cleaning.name()) )
            {
                Menu_items.add_menu_item_for_menu("Clean_up_names", null,event -> clean_up_names_fx(),menu,owner,logger);
            }
            if ( Booleans.get_boolean_defaults_to_false(Feature.Enable_corrupted_images_removal.name()) )
            {
                Menu_items.add_menu_item_for_menu("Remove_corrupted_images", null,event -> remove_corrupted_images_fx(),menu,owner,logger);
            }


            if (Booleans.get_boolean_defaults_to_false(Feature.Enable_bit_level_deduplication.name()) )
            {
                create_deduplication_menu(menu);
            }

            if (Booleans.get_boolean_defaults_to_false(Feature.Enable_image_similarity.name()) )
            {
                create_image_similarity_deduplication_menu(menu);
                create_song_similarity_deduplication_menu(menu);
            }
            files_menu.getItems().add(menu);
        }

        if ( virtual_landscape.context_type == Window_type.File_system_2D)
        {
            if (Booleans.get_boolean_defaults_to_false(Feature.Enable_backup.name())) {
                files_menu.getItems().add(make_backup_menu());
            }
            if (Feature_cache.get(Feature.Enable_fusk))
            {
                if (Feature_cache.get(Feature.Fusk_is_on))
                {
                    files_menu.getItems().add(make_fusk_menu());
                }
            }
        }


        return files_menu;
    }

    //**********************************************************
    public void show_where_are_images()
    //**********************************************************
    {
        Path top = virtual_landscape.path_list_provider.get_folder_path();
        Folders_with_large_images_locator.locate(top, 10, 200_000, owner, logger);
    }

    //**********************************************************
    public void sort_by_time(Virtual_landscape.Sort_by_time sort_by_time)
    //**********************************************************
    {
        Runnable r = () -> dispatch_by(sort_by_time);
        Actor_engine.execute(r, "Dispatch files by date",logger);
    }


    //**********************************************************
    public void dispatch_by(Virtual_landscape.Sort_by_time sort_by)
    //**********************************************************
    {
        List<File> files = virtual_landscape.path_list_provider.only_files(Feature_cache.get(Feature.Show_hidden_files));
        if (files == null) {
            logger.log("❌ ERROR: cannot list files in " + virtual_landscape.path_list_provider.get_name());
        }
        if (files.size() == 0) {
            logger.log("✅ no file in " + virtual_landscape.path_list_provider.get_name());
        }
        Map<String, Path> folders = new HashMap<>();
        List<Old_and_new_Path> moves = new ArrayList<>();
        for (File f : files)
        {
            BasicFileAttributes bfa;
            try
            {
                bfa = Files.readAttributes(f.toPath(), BasicFileAttributes.class);
            }
            catch (IOException e)
            {
                logger.log("❌ BAD " + e);
                continue;
            }

            FileTime ft = bfa.creationTime();
            LocalDateTime ldt = ft.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            String sorter = null;
            switch ( sort_by)
            {
                case year -> sorter = ""+ldt.getYear();
                case month -> sorter = ldt.getMonth().toString();
                case day -> sorter = ""+ldt.getDayOfYear();
            }
            Path folder = folders.get(sorter);
            if (folder == null)
            {
                folder = virtual_landscape.path_list_provider.resolve( sorter);
                try {
                    Files.createDirectory(folder);
                } catch (IOException e) {
                    logger.log("❌ BAD " + e);
                    continue;
                }
            }
            folders.put(sorter, folder);
            {
                // TODO: check if this is useful
                // since perform_safe_moves_in_a_thread will trigger the Chang_gang?
                List<Old_and_new_Path> l = new ArrayList<>();
                Path displayed_folder_path = virtual_landscape.path_list_provider.get_folder_path();
                l.add(new Old_and_new_Path(displayed_folder_path, displayed_folder_path, Command.command_unknown, Status.move_done, false));
                Change_gang.report_changes(l, owner);
            }
            Old_and_new_Path oanp = new Old_and_new_Path(
                    f.toPath(),
                    Path.of(folder.toAbsolutePath().toString(), f.getName()),
                    Command.command_move,
                    Status.before_command, false);
            moves.add(oanp);

        }

        double x = this.owner.getX()+100;
        double y = this.owner.getY()+100;
        Moving_files.perform_safe_moves_in_a_thread(moves, true, x,y, this.owner, virtual_landscape.aborter, logger);

        // display will be updated because of the Change_gang
    }




    //**********************************************************
    public void create_PDF_contact_sheet()
    //**********************************************************
    {
        Runnable r = this::create_PDF_contact_sheet_in_a_thread;
        Actor_engine.execute(r,"create PDF contact sheet",logger);
    }
    //**********************************************************
    public void create_PDF_contact_sheet_in_a_thread()
    //**********************************************************
    {
        double x = this.owner.getX()+100;
        double y = this.owner.getY()+100;

        Hourglass hourglass = Progress_window.show(
                false,
                "Making PDF contact sheet",
                20_000,
                x,
                y,
                owner,
                logger).orElse(null);
        List<String> graphicsMagick_command_line = new ArrayList<>();

        boolean formula1 = false;
        if ( formula1)
        {
            graphicsMagick_command_line.add(External_application.GraphicsMagick.get_command(owner,logger));
            graphicsMagick_command_line.add("convert");
            graphicsMagick_command_line.add("vid:*.jpg");
            graphicsMagick_command_line.add("contact_sheet.pdf");

        }
        else {
            graphicsMagick_command_line.add(External_application.GraphicsMagick.get_command(owner,logger));
            graphicsMagick_command_line.add("montage");
            graphicsMagick_command_line.add("-label");
            graphicsMagick_command_line.add("'%f'");
            graphicsMagick_command_line.add("-font");
            graphicsMagick_command_line.add("Helvetica");
            graphicsMagick_command_line.add("-pointsize");
            graphicsMagick_command_line.add("10");
            graphicsMagick_command_line.add("-background");
            graphicsMagick_command_line.add("#000000");
            graphicsMagick_command_line.add("-fill");
            graphicsMagick_command_line.add("#ffffff");
            graphicsMagick_command_line.add("-define");
            graphicsMagick_command_line.add("jpeg:size=300x200");
            graphicsMagick_command_line.add("-geometry");
            graphicsMagick_command_line.add("300x200+2+2");
            graphicsMagick_command_line.add("*.jpg");
            graphicsMagick_command_line.add(CONTACT_SHEET_FILE_NAME);
        }


        StringBuilder sb = null;
        if ( dbg) sb = new StringBuilder();
        File wd = (virtual_landscape.path_list_provider.get_folder_path()).toFile();
        Execute_result res = Execute_command.execute_command_list(graphicsMagick_command_line, wd, 2000, sb, logger);
        if ( !res.status())
        {
            List<String> verify = new ArrayList<>();
            verify.add(External_application.GraphicsMagick.get_command(owner,logger));
            verify.add("--version");
            String home = System.getProperty(String_constants.USER_HOME);
            Execute_result res2 = Execute_command.execute_command_list(verify, new File(home), 20 * 1000, null, logger);
            if ( !res2.status())
            {
                Booleans.manage_show_graphicsmagick_install_warning(owner,logger);
            }
            return;
        }
        else
        {
            if ( dbg) logger.log("✅ contact sheet generated "+ sb);
            else
            {
                logger.log("✅ contact sheet generated : "+ CONTACT_SHEET_FILE_NAME);
                System_open_actor.open_with_system(Path.of(virtual_landscape.path_list_provider.get_folder_path().toAbsolutePath().toString(), CONTACT_SHEET_FILE_NAME),owner,virtual_landscape.aborter,logger);

                Platform.runLater(() ->virtual_landscape.set_status("Contact sheet generated : "+ CONTACT_SHEET_FILE_NAME));
            }
        }
        if ( hourglass != null) hourglass.close();
    }



    //**********************************************************
    public void create_new_directory()
    //**********************************************************
    {
        TextInputDialog dialog = new TextInputDialog(My_I18n.get_I18n_string("New_directory", owner,logger));
        Look_and_feel_manager.set_dialog_look(dialog,owner,logger);
        dialog.initOwner(owner);
        dialog.setWidth(owner.getWidth());
        dialog.setTitle(My_I18n.get_I18n_string("New_directory", owner,logger));
        dialog.setHeaderText(My_I18n.get_I18n_string("Enter_name_of_new_directory", owner,logger));
        dialog.setContentText(My_I18n.get_I18n_string("New_directory_name", owner,logger));

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String new_name = result.get();
            for (int i = 0; i < 10; i++)
            {
                try {
                    Path new_dir = virtual_landscape.path_list_provider.resolve(new_name);
                    Files.createDirectory(new_dir);
                    Scroll_position_cache.scroll_position_cache_write(virtual_landscape.path_list_provider.get_folder_path(), new_dir);
                    virtual_landscape.redraw_fx("created new empty dir");
                    break;
                }
                catch (IOException e)
                {
                    logger.log("❗ new directory creation FAILED: " + e);
                    // n case the issue is the name, we just addd "_" at the end and retry
                    new_name += "_";
                }
            }

        }
    }




    //**********************************************************
    public void remove_empty_folders(boolean recursively)
    //**********************************************************
    {
        virtual_landscape.paths_holder.remove_empty_folders(recursively);
    }

    //**********************************************************
    public void clear_scroll_position_cache()
    //**********************************************************
    {
        Scroll_position_cache.scroll_position_cache_clear();
    }

    //**********************************************************
    void search_files_by_keyworks_fx()
    //**********************************************************
    {
        List<String> given = new ArrayList<>();
        Image_context.ask_user_and_find(
                virtual_landscape.path_list_provider,
                virtual_landscape,
                given,
                false,
                virtual_landscape.aborter,
                owner,
                logger
        );

    }
    //**********************************************************
    ContextMenu define_contextmenu_preferences()
    //**********************************************************
    {
        ContextMenu context_menu = new ContextMenu();
        Look_and_feel_manager.set_context_menu_look(context_menu,owner,logger);


        context_menu.getItems().add(make_file_sort_method_menu());

        context_menu.getItems().add(create_menu_item_for_show_details(virtual_landscape,owner,logger));

        context_menu.getItems().add(make_icon_size_menu());
        if ( Feature_cache.get(Feature.Show_icons_for_folders))
        {
            context_menu.getItems().add(make_folder_icon_size_menu());
        }
        context_menu.getItems().add(make_column_width_menu(virtual_landscape,owner,logger));
        context_menu.getItems().add(make_font_size_menu_item());
        context_menu.getItems().add(make_style_menu_item());
        context_menu.getItems().add(make_language_menu());
        //context_menu.getItems().add(make_video_length_menu());


        if (Feature_cache.get(Feature.Enable_fusk))
        {
            context_menu.getItems().add(make_fusk_check_menu_item());
        }

        {
            String key = "Launch_Music_Player";
            String s = My_I18n.get_I18n_string(key, owner,logger);
            CheckMenuItem menu_item = new CheckMenuItem(s);
            Look_and_feel_manager.set_menu_item_look(menu_item,owner,logger);
            menu_item.setMnemonicParsing(false);
            menu_item.setSelected(Feature_cache.get(Feature.Play_music));
            EventHandler<ActionEvent> handler = e ->
            {
                if ( menu_item.isSelected())
                {
                    Feature_cache.update_cached_boolean_and_save(Feature.Play_music, true, owner);
                    if ( Klikr_application.audio_player != null)
                    {
                        logger.log("an audio player already exists");
                    }
                    else
                    {
                        logger.log("starting new audio player");
                        Klikr_application.audio_player = new Audio_player(null, logger);
                    }
                }
                else
                {
                    Feature_cache.update_cached_boolean_and_save(Feature.Play_music, false, owner);
                    if ( Klikr_application.audio_player != null)
                    {
                        logger.log("killing audio player");
                        Klikr_application.audio_player.die();
                        Klikr_application.audio_player = null;
                    }
                    else
                    {
                        logger.log("No audio player to kill?");
                    }
                }

            };
            menu_item.setOnAction(handler);
            context_menu.getItems().add(menu_item);
        }


        Menu_items.add_menu_item_for_context_menu(
                "Clear_Trash_Folder",null,
                event -> Static_files_and_paths_utilities.clear_trash(true,owner, virtual_landscape.aborter, logger),
                context_menu,owner,logger);

        Menu_items.add_menu_item_for_context_menu("Clear_All_RAM_Caches",null,
                event -> {
                    RAM_caches.clear_all_RAM_caches(owner, logger);
                },
                context_menu,owner,logger);

        Menu_items.add_menu_item_for_context_menu("Clear_All_Disk_Caches",null,
                event -> {
                    Cache_folder.clear_all_disk_caches(owner,virtual_landscape.aborter,logger);
                },
                context_menu,owner,logger);


        Menu_items.add_menu_item_for_context_menu(
                "More_Settings",null,
                event -> new More_settings_stage("Preferences", owner,logger),
                context_menu,owner,logger);

        return context_menu;
    }

    //**********************************************************
    private void create_image_similarity_deduplication_menu(Menu clean)
    //**********************************************************
    {
        String txt = My_I18n.get_I18n_string("File_ML_similarity_deduplication",owner,logger);
        Menu menu = new Menu(txt);
        Look_and_feel_manager.set_menu_item_look(menu,owner,logger);
        clean.getItems().add(menu);

        // quasi similar means same image size
        Menu_items.add_menu_item_for_menu("Deduplicate_with_confirmation_quasi_similar_images",null,
                event -> {
                    //logger.log("Deduplicate manually");
                    (new Deduplication_by_similarity_engine(
                            true,
                            virtual_landscape.path_list_provider,
                            virtual_landscape,
                            too_far_away_image,
                            virtual_landscape.path_list_provider.get_folder_path().toFile(),
                            virtual_landscape.get_image_properties_cache(),
                            get_image_fv_cache,
                            owner,
                            logger)).do_your_job();
                },menu,owner,logger);

        Menu_items.add_menu_item_for_menu("Deduplicate_with_confirmation_images_looking_a_bit_the_same",null,
                event -> {
                    //logger.log("Deduplicate manually");
                    (new Deduplication_by_similarity_engine(
                            true,
                            virtual_landscape.path_list_provider,
                            virtual_landscape,
                            too_far_away_image,
                            virtual_landscape.path_list_provider.get_folder_path().toFile(),
                            null, // does not check image size
                            get_image_fv_cache,
                            owner,
                            logger)).do_your_job();
                },menu,owner,logger);


    }


    //**********************************************************
    private void create_song_similarity_deduplication_menu(Menu clean)
    //**********************************************************
    {
        String txt = "Song similarity";//My_I18n.get_I18n_string("File_ML_similarity_deduplication",owner,logger);
        Menu menu = new Menu(txt);
        Look_and_feel_manager.set_menu_item_look(menu,owner,logger);
        clean.getItems().add(menu);

        Menu_items.add_menu_item_for_menu("Deduplicate_with_confirmation_quasi_similar_songs",null,
                event -> {
                    //logger.log("Deduplicate manually");
                    (new Deduplication_by_similarity_engine(
                            false,
                            virtual_landscape.path_list_provider,
                            virtual_landscape,
                            too_far_away_song/10,
                            virtual_landscape.path_list_provider.get_folder_path().toFile(),
                            null,
                            get_song_fv_cache,
                            owner,
                            logger)).do_your_job();
                },menu,owner,logger);

        Menu_items.add_menu_item_for_menu("Deduplicate_with_confirmation_songs_sounding_a_bit_the_same",null,
                event -> {
                    //logger.log("Deduplicate manually");
                    (new Deduplication_by_similarity_engine(
                            false,
                            virtual_landscape.path_list_provider,
                            virtual_landscape,
                            too_far_away_song,
                            virtual_landscape.path_list_provider.get_folder_path().toFile(),
                            null,
                            get_song_fv_cache,
                            owner,
                            logger)).do_your_job();
                },menu,owner,logger);


    }

    //**********************************************************
    public Supplier<Feature_vector_cache> get_image_fv_cache = new Supplier<>()
    //**********************************************************
    {
        public Feature_vector_cache get() {

            Feature_vector_source fvs = new Feature_vector_source_for_image_similarity(owner, logger);

            List<Path> paths = virtual_landscape.path_list_provider.only_image_paths(Feature_cache.get(Feature.Show_hidden_files));
            Feature_vector_cache.Paths_and_feature_vectors local =
                    Feature_vector_cache.preload_all_feature_vector_in_cache(fvs, paths, virtual_landscape.path_list_provider, owner,owner.getX()+100, owner.getY()+100, virtual_landscape.aborter, logger);
            return local.fv_cache();
        }
    };

    //**********************************************************
    public Supplier<Feature_vector_cache> get_song_fv_cache = new Supplier<>()
    //**********************************************************
    {
        public Feature_vector_cache get()
        {
            Feature_vector_source fvs = new Feature_vector_source_for_song_similarity(virtual_landscape.aborter);
            List<Path> paths = virtual_landscape.path_list_provider.only_song_paths(Feature_cache.get(Feature.Show_hidden_files));
            Feature_vector_cache.Paths_and_feature_vectors local =
                    Feature_vector_cache.preload_all_feature_vector_in_cache(fvs, paths, virtual_landscape.path_list_provider, owner,owner.getX()+100, owner.getY()+100, virtual_landscape.aborter, logger);
            return local.fv_cache();
        }
    };

    //**********************************************************
    private void create_deduplication_menu(Menu clean)
    //**********************************************************
    {
        String txt = My_I18n.get_I18n_string("File_bit_exact_deduplication",owner,logger);
        Menu menu = new Menu(txt);
        Look_and_feel_manager.set_menu_item_look(menu,owner,logger);

        Menu_items.add_menu_item_for_menu("Deduplicate_help",null,
                event -> Popups.popup_warning(
                        "Help on deduplication",
                        "The deduplication tool will look recursively down the path starting at:" + virtual_landscape.path_list_provider.get_name() +
                                "\nLooking for identical files in terms of file content i.e. names/path are different but it IS the same file" +
                                " Then you will be able to either:" +
                                "\n  1. Review each pair of duplicate files one by one" +
                                "\n  2. Or ask for automated deduplication (DANGER!)" +
                                "\n  Beware: automated de-duplication may give unexpected results" +
                                " since you do not choose which file in the pair is deleted." +
                                "\n  However, the files are not actually deleted: they are MOVED to the klik_trash folder," +
                                " which you can visit by clicking on the trash button." +
                                "\n\n WARNING: On folders containing a lot of data, the search can take a long time!",
                        false,
                        owner,logger), menu,owner,logger);

        Menu_items.add_menu_item_for_menu("Deduplicate_count",null,
                event -> {
                    (new Deduplication_engine(owner, virtual_landscape.path_list_provider.get_folder_path().toFile(), virtual_landscape.path_list_provider,virtual_landscape,logger)).count(false);
                },menu,owner,logger);
        Menu_items.add_menu_item_for_menu("Deduplicate_manual", null,event -> {
            //logger.log("Deduplicate manually");
            (new Deduplication_engine(owner, virtual_landscape.path_list_provider.get_folder_path().toFile(), virtual_landscape.path_list_provider,virtual_landscape,logger)).do_your_job(false);
        },menu,owner,logger);


        Menu_items.add_menu_item_for_menu("Deduplicate_auto",null,
                event -> {
                    //logger.log("Deduplicate auto");

                    if ( !Popups.popup_ask_for_confirmation( "❗ EXPERIMENTAL! Are you sure?","Automated deduplication will recurse down this folder and delete (for good = not send them in recycle bin) all duplicate files",owner,logger)) return;
                    (new Deduplication_engine(owner, (virtual_landscape.path_list_provider.get_folder_path()).toFile(), virtual_landscape.path_list_provider,virtual_landscape,logger)).do_your_job(true);
                },menu,owner,logger);




        clean.getItems().add(menu);
    }



    //**********************************************************
    public Button make_button_that_behaves_like_a_folder(
            Path path,
            String text,
            double height,
            double min_width,
            boolean is_trash_button,
            Path is_parent_of,
            Logger logger)
    //**********************************************************
    {
        // we make a Item_button but are only interested in the button...
        Item_folder dummy = new Item_folder(
                virtual_landscape.the_Scene,
                virtual_landscape.selection_handler,
                virtual_landscape.icon_factory_actor,
                null,
                text,
                height,
                is_trash_button,
                is_parent_of,
                virtual_landscape.get_image_properties_cache(),
                virtual_landscape.shutdown_target,
                new Path_list_provider_for_file_system(path,owner,logger),
                virtual_landscape,
                virtual_landscape,
                
                owner,
                virtual_landscape.aborter,
                logger);
        dummy.button_for_a_directory(text, is_trash_button, min_width, height, null);
        return dummy.button;
    }











    //**********************************************************
    public MenuItem make_fusk_check_menu_item()
    //**********************************************************
    {
        String key = Feature.Fusk_is_on.name();
        String text = My_I18n.get_I18n_string(key,virtual_landscape.owner,logger);

        CheckMenuItem item = new CheckMenuItem(text);
        Look_and_feel_manager.set_menu_item_look(item, virtual_landscape.owner, logger);
        item.setSelected(Feature_cache.get(Feature.Fusk_is_on));
        item.setOnAction(actionEvent ->
        {
            boolean val = ((CheckMenuItem) actionEvent.getSource()).isSelected();
            Feature_cache.update_cached_boolean_and_save(Feature.Fusk_is_on,val,owner);

        });
        Items_with_explanation.add_question_mark_button(key, item, virtual_landscape.owner ,logger);
        return item;
    }


    //**********************************************************
    public MenuItem make_add_to_Enable_face_recognition_training_set_menu_item()
    //**********************************************************
    {
        String key = "Add_all_images_to_face_recognition_training_set";
        MenuItem item = Menu_items.make_menu_item(key,null,
        event -> {
            Face_recognition_service i = Face_recognition_service.get_instance(owner,logger);
            logger.log("❌ NOT IMPLEMENTED add_all_pictures_to_training_set for "+virtual_landscape.path_list_provider.get_name());

        },owner,logger);
        Items_with_explanation.add_question_mark_button(key, item, virtual_landscape.owner, logger);
        return item;
    }

    //**********************************************************
    public MenuItem make_save_face_recog_menu_item()
    //**********************************************************
    {
        return Menu_items.make_menu_item(
                "Save_Face_Recognition",null,
                event -> Face_recognition_service.save(),
                owner,logger);
    }

    //**********************************************************
    public MenuItem make_load_face_recog_menu_item()
    //**********************************************************
    {
        return Menu_items.make_menu_item(
                "Load_Face_Recognition",null,
                event -> Face_recognition_service.load(owner,logger),
                owner,logger);

    }


    //**********************************************************
    public MenuItem make_reset_face_recog_menu_item()
    //**********************************************************
    {
        return Menu_items.make_menu_item(
                "Start_New_Face_Recognition",null,
                event -> Face_recognition_service.start_new(owner,logger),
                owner,logger);
    }


    //**********************************************************
    public MenuItem make_start_auto_face_recog_menu_item()
    //**********************************************************
    {
        return Menu_items.make_menu_item(
                "Auto_Face_Recognition",null,
                event -> Face_recognition_service.auto(Path.of(virtual_landscape.path_list_provider.get_name()),owner,logger),
                owner,logger);

    }

    //**********************************************************
    public MenuItem make_whole_folder_face_recog_menu_item()
    //**********************************************************
    {
        return Menu_items.make_menu_item(
                "Do_Face_Recognition_On_Whole_Folder",null,
                event -> Face_recognition_service.do_folder(virtual_landscape.path_list_provider.get_folder_path(),owner,logger),
                owner,logger);
    }


    


    //**********************************************************
    public void remove_empty_folders_fx(boolean recursively)
    //**********************************************************
    {
        remove_empty_folders(recursively);
        // can be called from a thread which is NOT the FX event thread
        Jfx_batch_injector.inject(() -> virtual_landscape.redraw_fx("remove empty folder"),logger);
    }

    

    //**********************************************************
    public MenuItem make_select_all_folders_menu_item(Logger logger)
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Select_all_folders_for_drag_and_drop",virtual_landscape.owner,logger);
        select_all_folders_menu_item = new CheckMenuItem(text+" "+virtual_landscape.select_all_folders.getDisplayText());
        Look_and_feel_manager.set_menu_item_look(select_all_folders_menu_item, virtual_landscape.owner, logger);

        select_all_folders_menu_item.setSelected(false);
        select_all_folders_menu_item.setOnAction(event -> {
            if ( ((CheckMenuItem) event.getSource()).isSelected())
            {
                virtual_landscape.selection_handler.reset_selection();
                virtual_landscape.selection_handler.add_into_selected_files(virtual_landscape.get_folder_list());
                virtual_landscape.selection_handler.set_select_all_folders(true);
            }
            else
            {
                virtual_landscape.selection_handler.reset_selection();
                virtual_landscape.selection_handler.set_select_all_folders(false);
            }
        });
        return select_all_folders_menu_item;
    }

    //**********************************************************
    public MenuItem make_select_all_files_menu_item(Logger logger)
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Select_all_files_for_drag_and_drop",virtual_landscape.owner,logger);

        select_all_files_menu_item= new CheckMenuItem(text+ " "+virtual_landscape.select_all_files.getDisplayText());
        Look_and_feel_manager.set_menu_item_look(select_all_files_menu_item,virtual_landscape.owner,logger);
        select_all_files_menu_item.setSelected(false);
        select_all_files_menu_item.setOnAction(event -> {
            if ( ((CheckMenuItem) event.getSource()).isSelected())
            {
                virtual_landscape.selection_handler.select_all_files_in_folder(virtual_landscape.path_list_provider);
            }
            else
            {
                virtual_landscape.selection_handler.reset_selection();
                virtual_landscape.selection_handler.set_select_all_files(false);
            }
        });
        return select_all_files_menu_item;
    }


    //**********************************************************
    public void reset_all_files_and_folders()
    //**********************************************************
    {
        if(select_all_files_menu_item != null) select_all_files_menu_item.setSelected(false);
        if(select_all_folders_menu_item != null) select_all_folders_menu_item.setSelected(false);
    }

    //**********************************************************
    public static Menu make_history_menu(
            Map<LocalDateTime,String> the_whole_history,
            Path_list_provider path_list_provider,
            Path top_left,
            Shutdown_target shutdown_target,
            Window_type context_type, Window owner, Logger logger)
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("History",owner,logger);

        Menu history_menu = new Menu(text);
        Look_and_feel_manager.set_menu_item_look(history_menu,owner, logger);

        create_history_menu(the_whole_history,path_list_provider, top_left, shutdown_target, history_menu, context_type, owner,logger);
        return history_menu;
    }

    //**********************************************************
    public static Menu make_bookmarks_menu(Path path, Path top_left, Shutdown_target shutdown_target, Window_type context_type, Window owner, Logger logger)
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Bookmarks",owner,logger);
        Menu bookmarks_menu = new Menu(text);
        Look_and_feel_manager.set_menu_item_look(bookmarks_menu,owner, logger);

        create_bookmarks_menu(bookmarks_menu, path, top_left, shutdown_target, context_type,owner,logger);
        return bookmarks_menu;
    }
    //**********************************************************
    public static Menu make_roots_menu(
            Path_list_provider path_list_provider,
            Path top_left,
            Shutdown_target shutdown_target,
            Window_type window_type,
            Window owner, Logger logger)
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("File_System_Roots",owner,logger);
        Menu roots_menu = new Menu(text);
        Look_and_feel_manager.set_menu_item_look(roots_menu,owner, logger);

        create_roots_menu(
                roots_menu,
                path_list_provider,
                top_left,
                shutdown_target,
                window_type,
                owner,
                logger);
        return roots_menu;
    }
    //**********************************************************
    public static Menu make_undos_menu(Window owner,Logger logger)
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Undo",owner,logger);
        Menu undos_menu = new Menu(text);
        Look_and_feel_manager.set_menu_item_look(undos_menu,owner, logger);

        create_undos_menu(undos_menu,owner,logger);
        return undos_menu;
    }



    //**********************************************************
    public static void create_history_menu(
            Map<LocalDateTime,String> the_whole_history,
            Path_list_provider path_list_provider,
            Path top_left,
            Shutdown_target shutdown_target,
            Menu history_menu,
            Window_type window_type,
            Window owner, Logger logger)
    //**********************************************************
    {

        Menu_items.add_menu_item_for_menu("Clear_History",null,
                    event -> {
                        logger.log("clearing history");
                        History_engine.get(owner).clear();
                        Instructions.replace_same_folder(shutdown_target, window_type,path_list_provider,top_left,owner,logger);
                    },history_menu,owner,logger);


        int max_on_screen = 20;
        int on_screen = 0;
        MenuItem more = null;
        Map<String, History_item> path_already_done = new HashMap<>();
        for (History_item hi : History_engine.get(owner).get_all_history_items())
        {
            if ( on_screen < max_on_screen)
            {
                if ( path_already_done.get(hi.value) != null)
                {
                    continue;
                }
                String displayed_string = hi.value;

                if ( displayed_string.length() > MAX_MENU_ITEM_STRING_LENGTH)
                {
                    // trick to avoid that the menu is not displayed when items are very wide
                    // which may happens for the largest fonts
                    displayed_string = displayed_string.substring(0,MAX_MENU_ITEM_STRING_LENGTH)+" ...";
                }
                MenuItem item = new MenuItem(displayed_string);
                Look_and_feel_manager.set_menu_item_look(item, owner, logger);
                item.setMnemonicParsing(false);
                if ( hi.value.equals(path_list_provider.get_folder_path().toAbsolutePath().toString()))
                {
                    // show the one we are in as inactive
                    item.setDisable(true);
                }
                if ( !hi.get_available())
                {
                    item.setDisable(true);
                }
                item.setOnAction(event ->
                {
                    Path path = Path.of(hi.value);
                    Path old_folder_path = path_list_provider.get_folder_path();
                    Scroll_position_cache.scroll_position_cache_write(old_folder_path,top_left);
                    Instructions.replace_different_folder( shutdown_target, window_type,new Path_list_provider_for_file_system(path,owner,logger), owner,logger);
                });
                path_already_done.put(hi.value,hi);
                history_menu.getItems().add(item);
                on_screen++;
            }
            else
            {
                if ( more == null)
                {
                    String text = My_I18n.get_I18n_string("Show_Whole_History",owner,logger);
                    more =  new MenuItem(text);
                    Look_and_feel_manager.set_menu_item_look(more, owner, logger);

                    history_menu.getItems().add(more);
                    more.setOnAction(actionEvent -> pop_up_whole_history(
                            the_whole_history,
                             path_list_provider,
                             top_left,
                             shutdown_target,
                             window_type,
                             owner, logger
                    ));
                }
                add_to_whole_history(the_whole_history, hi);
            }
        }
    }

    //**********************************************************
    private static void add_to_whole_history(Map<LocalDateTime, String> the_whole_history, History_item hi)
    //**********************************************************
    {
        if ( the_whole_history == null) the_whole_history = new HashMap<>();
        the_whole_history.put(hi.time_stamp,hi.value);
    }


    //**********************************************************
    private static void pop_up_whole_history(
            Map<LocalDateTime,String> the_whole_history,
            Path_list_provider path_list_provider,
            Path top_left,
            Shutdown_target shutdown_target,
            Window_type window_type,
            Window owner,Logger logger)
    //**********************************************************
    {
        Active_list_stage_action action = text -> {
            Scroll_position_cache.scroll_position_cache_write(path_list_provider.get_folder_path(),top_left);
            Instructions.replace_different_folder( shutdown_target, window_type, new Path_list_provider_for_file_system(Path.of(text),owner,logger), owner, logger);
        };
        Datetime_to_signature_source source = new Datetime_to_signature_source() {
            @Override
            public Map<LocalDateTime, String> get_map_of_date_to_signature() {
                return the_whole_history;
            }
        };
        Active_list_stage.show_active_list_stage("Whole history: "+the_whole_history.size()+" items", source, action, owner, logger);
    }

    //**********************************************************
    public static void create_bookmarks_menu(
            Menu bookmarks_menu,
            Path path,
            Path top_left,
            Shutdown_target shutdown_target,
            Window_type context_type,
            Window owner, Logger logger)
    //**********************************************************
    {
        KeyCodeCombination bookmark_this = new KeyCodeCombination(KeyCode.D, KeyCombination.SHORTCUT_DOWN);

        Menu_items.add_menu_item_for_menu("Bookmark_this", bookmark_this.getDisplayText(),
                event -> Bookmarks.get(owner).add(path.toAbsolutePath().toString()),
                bookmarks_menu,owner,logger);
        Menu_items.add_menu_item_for_menu("Clear_Bookmarks",null,
                event -> Bookmarks.get(owner).clear(),
                bookmarks_menu,owner,logger);


        for (String hi : Bookmarks.get(owner).get_list())
        {
            MenuItem item = new MenuItem(hi);
            Look_and_feel_manager.set_menu_item_look(item, owner, logger);
            item.setOnAction(event -> {
                Scroll_position_cache.scroll_position_cache_write(path,top_left);
                Instructions.replace_different_folder( shutdown_target, context_type, new Path_list_provider_for_file_system(Path.of(hi),owner,logger), owner,logger);
            });
            bookmarks_menu.getItems().add(item);

        }
    }


    //**********************************************************
    public static void create_undos_menu(Menu undos_menu,  Window owner, Logger logger)
    //**********************************************************
    {
        double x = owner.getX()+100;
        double y = owner.getY()+100;

        KeyCodeCombination undo = new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN);

        undos_menu.getItems().add(Menu_items.make_menu_item(
                "Undo_LAST_move_or_delete", undo.getDisplayText(),
                event -> Undo_for_moves.perform_last_undo_fx(owner, x, y, logger),owner,logger));
        undos_menu.getItems().add(Menu_items.make_menu_item(
                "Show_Undos",null,
                event -> pop_up_whole_undo_history(owner,logger),owner,logger));
        undos_menu.getItems().add(Menu_items.make_menu_item(
                "Clear_Undos",null,
                event -> Undo_for_moves.remove_all_undo_items(owner, logger),owner,logger));
    }

    //**********************************************************
    public static void create_roots_menu(
            Menu roots_menu,
            Path_list_provider path_list_provider,
            Path top_left,
            Shutdown_target shutdown_target,
            Window_type window_type,
            Window owner, Logger logger)
    //**********************************************************
    {
        for ( File f : File.listRoots())
        {
            String text = f.getAbsolutePath().toString();
            MenuItem item = new MenuItem(text);
            Look_and_feel_manager.set_menu_item_look(item, owner, logger);

            item.setOnAction(event -> {
                Scroll_position_cache.scroll_position_cache_write(path_list_provider.get_folder_path(),top_left);
                Instructions.replace_different_folder( shutdown_target,window_type, new Path_list_provider_for_file_system(f.toPath(),owner,logger),owner,logger);
            });
            roots_menu.getItems().add(item);
        }
    }

    //**********************************************************
    private static void pop_up_whole_undo_history(Window owner,Logger logger)
    //**********************************************************
    {
        Active_list_stage_action action = signature ->
        {
            Map<String, Undo_item> signature_to_undo_item = Undo_for_moves.get_instance(owner, logger).get_signature_to_undo_item();
            Undo_item item = signature_to_undo_item.get(signature);
            if ( item == null)
            {
                logger.log(Stack_trace_getter.get_stack_trace("❌ item == null for signature="+signature));
                return;
            }
            if ( !Undo_for_moves.check_validity(item, owner,logger))
            {
                Popups.popup_warning("❗ Invalid undo item ignored","The file was probably moved since?",true,owner,logger);
                Undo_for_moves.remove_invalid_undo_item(item, owner,logger);
                return;
            }
            logger.log("✅ undo_item="+item.to_string());
            double x = owner.getX()+100;
            double y = owner.getY()+100;

            boolean ok = Popups.popup_ask_for_confirmation("❗ Please confirm.",
                    "Undoing this will move the file(s) back to their original location.\n" +
                            item.to_string()+"\n"+
                            item.to_string(),
                    owner, logger);

            if ( ok) Undo_for_moves.perform_undo(item, owner, x, y, logger);
        };
        String title = My_I18n.get_I18n_string("Whole_Undo_History",owner,logger);
        Undo_for_moves.undo_stages.add(Active_list_stage.show_active_list_stage(title, Undo_for_moves.get_instance(owner,logger), action, owner,logger));
    }



    //**********************************************************
    public Menu make_style_menu_item()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Style",virtual_landscape.owner,logger);
        List<CheckMenuItem> all_check_menu_items = new ArrayList<>();
        Menu menu = new Menu(text);
        Look_and_feel_manager.set_menu_item_look(menu,virtual_landscape.owner, logger);

        for( Look_and_feel_style s : Look_and_feel_style.values())
        {
            create_check_menu_item_for_style(menu, s, all_check_menu_items);
        }
        /*Look_and_feel_style current_style = Look_and_feel_manager.get_instance(virtual_landscape.owner,logger).get_look_and_feel_style();
        if ( current_style == Look_and_feel_style.material)
        {
            MenuItem custom_color_item = new MenuItem(My_I18n.get_I18n_string("Choose_Custom_Color",virtual_landscape.owner,logger));
            Look_and_feel_manager.set_menu_item_look(menu_item,owner,logger);
            custom_color_item.setOnAction((ActionEvent e) -> invoke_color_picker());
            menu.getItems().add(custom_color_item);
        }*/
        return menu;
    }

    /*
    //**********************************************************
    private void invoke_color_picker()
    //**********************************************************
    {
        logger.log(("color picker !"));
        Color default_color = Non_booleans_properties.get_custom_color(virtual_landscape.owner);
        ColorPicker color_picker = new ColorPicker(default_color);
        Look_and_feel_manager.set_region_look(color_picker, virtual_landscape.owner,logger);
        color_picker.setOnAction((ActionEvent e) -> {
            Color new_color = color_picker.getValue();
            logger.log(("color picker new color = "+new_color));
            Non_booleans_properties.set_custom_color(new_color, virtual_landscape.owner);
            virtual_landscape.redraw_fx("custom color changed");
        });
        Stage color_picker_stage = new Stage();
        color_picker_stage.setTitle("Custom color picker (requires restart!)");
        color_picker_stage.initOwner(virtual_landscape.owner); // Set owner window
        // Create layout container
        VBox layout = new VBox(10); // 10 pixels spacing
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(10));
        layout.getChildren().add(color_picker);
        Scene color_picker_scene = new Scene(layout, 400, 200);
        color_picker_stage.setScene(color_picker_scene);
        color_picker_stage.show();
        logger.log(("color picker shown"));
    }
*/
    //**********************************************************
    public void create_check_menu_item_for_style(Menu menu, Look_and_feel_style style, List<CheckMenuItem> all_check_menu_items)
    //**********************************************************
    {
        CheckMenuItem check_menu_item = new CheckMenuItem(style.name());
        Look_and_feel_manager.set_menu_item_look(check_menu_item, virtual_landscape.owner, logger);

        Look_and_feel_style current_style = Look_and_feel_manager.get_instance(virtual_landscape.owner,logger).get_look_and_feel_style();
        check_menu_item.setSelected(current_style.name().equals(style.name()));
        check_menu_item.setOnAction(actionEvent -> {
            CheckMenuItem local = (CheckMenuItem) actionEvent.getSource();
            if (local.isSelected()) {
                for ( CheckMenuItem cmi : all_check_menu_items)
                {
                    if ( cmi != local) cmi.setSelected(false);
                }
                Look_and_feel_manager.set_look_and_feel(style,  virtual_landscape.owner,logger);
            }
        });
        menu.getItems().add(check_menu_item);
        all_check_menu_items.add(check_menu_item);
    }

    //**********************************************************
    public Menu make_language_menu()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Language",virtual_landscape.owner,logger);
        List<CheckMenuItem> all_check_menu_items = new ArrayList<>();
        Menu menu = new Menu(text);
        Look_and_feel_manager.set_menu_item_look(menu,virtual_landscape.owner, logger);

        String current = Non_booleans_properties.get_language_key(owner);

        for( Language language_key : Language.values())
        {
            create_check_menu_item_for_language(menu, language_key, current, all_check_menu_items);
        }
        return menu;
    }

    //**********************************************************
    public void create_check_menu_item_for_language(Menu menu, Language language_key, String current, List<CheckMenuItem> all_check_menu_items)
    //**********************************************************
    {
        CheckMenuItem item = new CheckMenuItem(language_key.name());
        Look_and_feel_manager.set_menu_item_look(item, virtual_landscape.owner, logger);
        item.setGraphic(new ImageView(language_key.get_icon(owner,logger)));
        item.setSelected(current.equals(language_key.name()));
        item.setOnAction(actionEvent -> {
            CheckMenuItem local = (CheckMenuItem) actionEvent.getSource();
            if (local.isSelected())
            {
                for ( CheckMenuItem cmi : all_check_menu_items)
                {
                    if ( cmi != local) cmi.setSelected(false);
                }
                My_I18n.set_new_language(language_key,  virtual_landscape.owner,logger); /// will trigger a repaint via String_change_target
            }
        });
        menu.getItems().add(item);
        all_check_menu_items.add(item);
    }


    //**********************************************************
    public static void create_menu_item_for_one_column_width(Menu menu, int length, List<CheckMenuItem> all_check_menu_items, Virtual_landscape local_virtual_landscape, Window owner, Logger logger)
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string(String_constants.COLUMN_WIDTH,owner,logger);
        CheckMenuItem item = new CheckMenuItem(text + " = " +length);
        Look_and_feel_manager.set_menu_item_look(item, owner, logger);
        int actual_size = Non_booleans_properties.get_column_width(owner);
        item.setSelected(actual_size == length);
        item.setOnAction(actionEvent -> {
            CheckMenuItem local = (CheckMenuItem) actionEvent.getSource();
            if (local.isSelected()) {
                for ( CheckMenuItem cmi : all_check_menu_items)
                {
                    if ( cmi != local) cmi.setSelected(false);
                }
                Non_booleans_properties.set_column_width(length,owner);
                local_virtual_landscape.redraw_fx("column width changed");
            }
        });
        menu.getItems().add(item);
        all_check_menu_items.add(item);

    }

    //**********************************************************
    public static CheckMenuItem create_menu_item_for_show_details(Virtual_landscape local_virtual_landscape, Window owner, Logger logger)
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Show_Details",owner,logger);
        text += " ("+local_virtual_landscape.show_details.getDisplayText()+")";
        CheckMenuItem item = new CheckMenuItem(text );
        Look_and_feel_manager.set_menu_item_look(item, owner, logger);
        item.setSelected(Feature_cache.get(Feature.Show_single_column_with_details));
        item.setOnAction(actionEvent -> {
            CheckMenuItem local = (CheckMenuItem) actionEvent.getSource();
            Feature_cache.update_cached_boolean_and_dont_save(Feature.Show_single_column_with_details, local.isSelected(), owner);
            local_virtual_landscape.redraw_fx("Show_single_column_with_details");
        });
        return item;

    }
    //**********************************************************
    public void create_menu_item_for_one_icon_size(Menu menu, Icon_size icon_size, List<CheckMenuItem> all_check_menu_items)
    //**********************************************************
    {
        int target_size = icon_size.size();
        String txt = "";
        if (icon_size.is_divider())
        {
            txt = icon_size.divider()+" icons per row";
        }
        else
        {
            txt = My_I18n.get_I18n_string("Icon_Size",virtual_landscape.owner,logger) + " = " +target_size;
        }
        CheckMenuItem item = new CheckMenuItem(txt);
        Look_and_feel_manager.set_menu_item_look(item, virtual_landscape.owner, logger);
        int actual_size = Non_booleans_properties.get_icon_size(owner);
        item.setSelected(actual_size == target_size);
        item.setOnAction(actionEvent -> {
            CheckMenuItem local = (CheckMenuItem) actionEvent.getSource();
            if (local.isSelected()) {
                for ( CheckMenuItem cmi : all_check_menu_items)
                {
                    if ( cmi != local) cmi.setSelected(false);
                }
                Non_booleans_properties.set_icon_size(target_size,owner);
                logger.log("icon size changed to "+target_size);
                virtual_landscape.redraw_fx("icon size changed");
            }
        });
        menu.getItems().add(item);
        all_check_menu_items.add(item);
    }


    //**********************************************************
    public void create_menu_item_for_one_folder_icon_size(Menu menu, int target_size, List<CheckMenuItem> all_check_menu_items)
    //**********************************************************
    {
        CheckMenuItem item = new CheckMenuItem(My_I18n.get_I18n_string("Folder_Icon_Size",virtual_landscape.owner,logger) + " = " +target_size);
        Look_and_feel_manager.set_menu_item_look(item, virtual_landscape.owner, logger);
        int actual_size = Non_booleans_properties.get_folder_icon_size(owner);
        item.setSelected(actual_size == target_size);
        item.setOnAction(actionEvent -> {
            CheckMenuItem local = (CheckMenuItem) actionEvent.getSource();
            if (local.isSelected()) {
                for ( CheckMenuItem cmi : all_check_menu_items)
                {
                    if ( cmi != local) cmi.setSelected(false);
                }
                Non_booleans_properties.set_folder_icon_size(target_size,owner);
                virtual_landscape.redraw_fx("folder icon size changed");
            }
        });
        menu.getItems().add(item);
        all_check_menu_items.add(item);
    }

    //**********************************************************
    public void create_menu_item_for_one_font_size( Menu menu, double target_size, List<CheckMenuItem> all_check_menu_items)
    //**********************************************************
    {
        CheckMenuItem item = new CheckMenuItem(My_I18n.get_I18n_string("Font_size",virtual_landscape.owner,logger) + " = " +target_size);
        Look_and_feel_manager.set_menu_item_look(item, virtual_landscape.owner, logger);
        double actual_size = Non_booleans_properties.get_font_size(owner,logger);
        item.setSelected(actual_size == target_size);
        item.setOnAction(actionEvent -> {
            CheckMenuItem local = (CheckMenuItem) actionEvent.getSource();
            if (local.isSelected())
            {
                for ( CheckMenuItem cmi : all_check_menu_items)
                {
                    if (cmi != local) cmi.setSelected(false);
                }
                Non_booleans_properties.set_font_size(target_size, owner);
                virtual_landscape.redraw_fx("font size changed");
            }
        });
        menu.getItems().add(item);
        all_check_menu_items.add(item);
    }


    //**********************************************************
    public static Menu make_column_width_menu(Virtual_landscape local_virtual_landscape, Window owner, Logger logger)
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string(String_constants.COLUMN_WIDTH,owner,logger);
        Menu menu = new Menu(text);
        Look_and_feel_manager.set_menu_item_look(menu,owner, logger);
        List<CheckMenuItem> all_check_menu_items = new ArrayList<>();

        int[] possible_lengths ={Virtual_landscape.MIN_COLUMN_WIDTH,400,500,600,800,1000,2000,4000};
        for ( int l : possible_lengths)
        {
            create_menu_item_for_one_column_width(menu, l, all_check_menu_items,local_virtual_landscape,owner, logger);
        }
        return menu;
    }
    //**********************************************************
    public Menu make_file_sort_method_menu()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("File_Sorting_Method",virtual_landscape.owner,logger);
        Menu menu = new Menu(text);
        Look_and_feel_manager.set_menu_item_look(menu,virtual_landscape.owner, logger);
        List<CheckMenuItem> all_check_menu_items = new ArrayList<>();
        for ( Sort_files_by sort_by : Sort_files_by.values())
        {
            if (( sort_by == Sort_files_by.SIMILARITY_BY_PAIRS)||(sort_by == Sort_files_by.SIMILARITY_BY_PURSUIT))
            {
                if ( !Booleans.get_boolean_defaults_to_false(Feature.Enable_image_similarity.name()))
                {
                    continue;
                }
            }
            create_menu_item_for_one_file_sort_method(menu, sort_by, all_check_menu_items);
        }
        return menu;
    }

    //**********************************************************
    public void create_menu_item_for_one_file_sort_method(Menu menu, Sort_files_by sort_by, List<CheckMenuItem> all_check_menu_items)
    //**********************************************************
    {
        String key = sort_by.name();
        CheckMenuItem item = new CheckMenuItem(My_I18n.get_I18n_string(key,virtual_landscape.owner,logger));
        Look_and_feel_manager.set_menu_item_look(item, virtual_landscape.owner, logger);
        Sort_files_by actual = Sort_files_by.get_sort_files_by(virtual_landscape.path_list_provider.get_folder_path(),owner);
        item.setSelected(actual == sort_by);
        item.setOnAction(actionEvent -> {
            CheckMenuItem local = (CheckMenuItem) actionEvent.getSource();
            if (local.isSelected())
            {
                for ( CheckMenuItem cmi : all_check_menu_items)
                {
                    if ( cmi != local) cmi.setSelected(false);
                }
                if ( actual != sort_by)
                {
                    Sort_files_by.set_sort_files_by(virtual_landscape.path_list_provider.get_folder_path(),sort_by,owner,logger);
                    logger.log("new file/image sorting order= "+sort_by);
                    Instructions.replace_same_folder(virtual_landscape.shutdown_target, Window_type.File_system_2D,virtual_landscape.path_list_provider,virtual_landscape.get_top_left(),owner,logger);

                    //virtual_landscape.replace_same(this.virtual_landscape.context_type);
                }
            }
        });
        Button explanation = Items_with_explanation.make_explanation_button(key, owner, logger);
        item.setGraphic(explanation);
        menu.getItems().add(item);
        all_check_menu_items.add(item);

    }



    //**********************************************************
    public Menu make_icon_size_menu()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Icon_Size",virtual_landscape.owner,logger);
        Menu menu = new Menu(text);
        Look_and_feel_manager.set_menu_item_look(menu,virtual_landscape.owner, logger);

        {
            MenuItem plus = new MenuItem("+ 10%");
            Look_and_feel_manager.set_menu_item_look(plus, virtual_landscape.owner, logger);
            menu.getItems().add(plus);
            plus.setOnAction(actionEvent -> virtual_landscape.increase_icon_size());
        }
        {
            MenuItem minus = new MenuItem("- 10%");
            Look_and_feel_manager.set_menu_item_look(minus, virtual_landscape.owner, logger);
            menu.getItems().add(minus);
            minus.setOnAction(actionEvent -> virtual_landscape.decrease_icon_size());
        }
        List<CheckMenuItem> all_check_menu_items = new ArrayList<>();
        List<Icon_size> icon_sizes = get_icon_sizes();
        for ( Icon_size icon_size : icon_sizes)
        {
            create_menu_item_for_one_icon_size(menu, icon_size, all_check_menu_items);
        }
        return menu;
    }

    //**********************************************************
    private List<Icon_size> get_icon_sizes()
    //**********************************************************
    {
        List<Icon_size> icon_sizes = new ArrayList<>();
        {
            int[] possible_sizes = {32, 64, 128, Non_booleans_properties.DEFAULT_ICON_SIZE, 512, 1024};
            for (int size : possible_sizes)
            {
                icon_sizes.add(new Icon_size(size, false, 0));
            }
        }
        {
            //compute icon size for N icons in a row
            double W = owner.getWidth()- virtual_landscape.slider_width;
            int[] possible_dividers = {3,4,5,10};
            for ( int divider : possible_dividers)
            {
                int size = (int) (W/divider);
                icon_sizes.add(new Icon_size(size, true, divider));
            }
        }
        int current_icon_size = Non_booleans_properties.get_icon_size(owner);
        Icon_size cur = new Icon_size(current_icon_size,false,0);
        if ( !icon_sizes.contains(cur)) icon_sizes.add(cur);
        Comparator<? super Icon_size> comp = new Comparator<Icon_size>() {
            @Override
            public int compare(Icon_size o1, Icon_size o2) {
                return Integer.valueOf(o1.size()).compareTo(Integer.valueOf(o2.size()));
            }
        };
        Collections.sort(icon_sizes,comp);
        return icon_sizes;
    }

    //**********************************************************
    public Menu make_folder_icon_size_menu()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Folder_Icon_Size",virtual_landscape.owner,logger);
        Menu menu = new Menu(text);
        Look_and_feel_manager.set_menu_item_look(menu,virtual_landscape.owner, logger);

        List<CheckMenuItem> all_check_menu_items = new ArrayList<>();
        int[] possible_folder_icon_sizes ={Non_booleans_properties.DEFAULT_FOLDER_ICON_SIZE,64,128,256, 300,400,512};
        for ( int size : possible_folder_icon_sizes)
        {
            create_menu_item_for_one_folder_icon_size(menu, size, all_check_menu_items);
        }
        return menu;
    }
    //**********************************************************
    public Menu make_font_size_menu_item()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Font_size",virtual_landscape.owner,logger);

        Menu menu = new Menu(text);
        Look_and_feel_manager.set_menu_item_look(menu,virtual_landscape.owner, logger);

        List<Double> possible_sizes = new ArrayList<>();
        possible_sizes.add(Double.valueOf(Non_booleans_properties.get_font_size(owner,logger)));

        double[] possible_font_sizes = {8,10,12,14,16,18,20,22,24,26,28,30};
        for (double candidateSize : possible_font_sizes) {
            if (possible_sizes.contains((Double)candidateSize)) continue;
            possible_sizes.add((Double)candidateSize);
        }
        Collections.sort(possible_sizes);
        List<CheckMenuItem> all_check_menu_items = new ArrayList<>();
        for ( Double i : possible_sizes)
        {
            create_menu_item_for_one_font_size( menu, i, all_check_menu_items);
        }
        return menu;
    }


    //**********************************************************
    void clean_up_names_fx()
    //**********************************************************
    {
        if ( !Popups.popup_ask_for_confirmation( "❗ EXPERIMENTAL! Are you sure?","Name cleaning will try to change all names in this folder, which may have very nasty consequences in a home or system folder",owner,logger)) return;

        Path dir = virtual_landscape.path_list_provider.get_folder_path();
        File[] files = dir.toFile().listFiles();
        if (files == null) return;
        List<Old_and_new_Path> l = new ArrayList<>();
        for (File f : files)
        {

            Path old_path = f.toPath();

            String old_name = old_path.getFileName().toString();

            boolean check_extension = !f.isDirectory();
            String new_name = Name_cleaner.clean(old_name,check_extension, logger);
            if (new_name.equals(old_name))
            {
                logger.log("skipping " + old_name + " as it is conformant");
                continue;
            }
            logger.log("processing "+old_name+" as it is NOT conformant, will try: "+new_name);
            Path new_path = Paths.get(dir.toString(),new_name);
            Old_and_new_Path oandn = new Old_and_new_Path(old_path, new_path, Command.command_rename, Status.before_command,false);
            l.add(oandn);
        }
        double x = owner.getX()+100;
        double y = owner.getY()+100;

        Moving_files.perform_safe_moves_in_a_thread(l, true, x,y, owner,virtual_landscape.aborter, logger);

    }


    //**********************************************************
    void remove_corrupted_images_fx()
    //**********************************************************
    {
        Path dir = virtual_landscape.path_list_provider.get_folder_path();
        File[] files = dir.toFile().listFiles();
        if ( files == null) return;
        List<Path> to_be_deleted =  new ArrayList<>();
        for (File f : files)
        {
            if ( f.isDirectory()) continue;

            if ( f.getName().startsWith("._"))
            {
                // this is 'debatable' since it removes MacOs extended attributes in extFat
                // but is suoer useful e.g when one reloads files from a backup USB drive
                logger.log("file name starts with ._, removed "+f.getName());
                to_be_deleted.add(f.toPath());
                continue;
            }

            if ( !Guess_file_type.is_this_file_an_image(f,owner,logger)) continue;
            Exif_metadata_extractor e = new Exif_metadata_extractor(f.toPath(),virtual_landscape.owner,logger);
            e.get_exif_metadata(0, true,virtual_landscape.aborter, false);
            if( !e.is_image_damaged()) continue;
            to_be_deleted.add(f.toPath());
        }
        if ( to_be_deleted.size() == 0)
        {
            logger.log("no corrupted images found");
            return;
        }
        double x = owner.getX()+100;
        double y = owner.getY()+100;

        virtual_landscape.path_list_provider.delete_multiple(to_be_deleted,owner,x,y,virtual_landscape.aborter,logger);

    }




    //**********************************************************
    public Menu make_backup_menu()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Backup",virtual_landscape.owner,logger);
        Menu menu = new Menu(text);
        Look_and_feel_manager.set_menu_item_look(menu,virtual_landscape.owner, logger);

        menu.getItems().add(Menu_items.make_menu_item("Set_as_backup_source_folder",null,event -> virtual_landscape.you_are_backup_source(),owner,logger));
        menu.getItems().add(Menu_items.make_menu_item("Set_as_backup_destination_folder",null,event -> virtual_landscape.you_are_backup_destination(),owner,logger));
        menu.getItems().add(Menu_items.make_menu_item("Start_backup",null,event -> virtual_landscape.start_backup(),owner,logger));
        menu.getItems().add(Menu_items.make_menu_item("Abort_backup",null,event -> virtual_landscape.abort_backup(),owner,logger));
        menu.getItems().add(Menu_items.make_menu_item("Backup_help",null,event -> show_backup_help(logger),owner,logger));
        return menu;
    }
    //**********************************************************
    public Menu make_import_menu()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Import",virtual_landscape.owner,logger);
        Menu menu = new Menu(text);
        menu.getItems().add(Menu_items.make_menu_item("Estimate_Size_Of_Import_Apple_Photos",null,event -> virtual_landscape.estimate_size_of_importing_apple_Photos(),owner,logger));
        menu.getItems().add(Menu_items.make_menu_item("Import_Apple_Photos",null,event -> virtual_landscape.import_apple_Photos(),owner,logger));
        return menu;
    }
    //**********************************************************
    public Menu make_fusk_menu()
    //**********************************************************
    {
        String text = "Fusk (experimental!)"; //My_I18n.get_I18n_string("Backup",logger);
        Menu menu = new Menu(text);
        menu.getItems().add(Menu_items.make_menu_item("Enter fusk pin code",null,event -> virtual_landscape.enter_fusk_pin_code(),owner,logger));
        menu.getItems().add(Menu_items.make_menu_item("Set this folder as fusk source",null,event -> virtual_landscape.you_are_fusk_source(),owner,logger));
        menu.getItems().add(Menu_items.make_menu_item("Set this folder as fusk destination",null,event -> virtual_landscape.you_are_fusk_destination(),owner,logger));
        menu.getItems().add(Menu_items.make_menu_item("Start fusk (experimental!)",null,event -> virtual_landscape.start_fusk(),owner,logger));
        menu.getItems().add( Menu_items.make_menu_item("Abort fusk",null,event -> virtual_landscape.abort_fusk(),owner,logger));
        menu.getItems().add(Menu_items.make_menu_item("Start defusk (experimental!)",null,event -> virtual_landscape.start_defusk(),owner,logger));
        menu.getItems().add(Menu_items.make_menu_item("Fusk help",null,event -> show_fusk_help(),owner,logger));
        return menu;
    }

    //**********************************************************
    private void show_backup_help(Logger logger)
    //**********************************************************
    {
        List<Line_for_info_stage> l = new ArrayList<>();
        l.add(new Line_for_info_stage(false,"The backup tool will copy recursively down the paths starting in the SOURCE folder"));
        l.add(new Line_for_info_stage(false,"into the DESTINATION folder"));
        l.add(new Line_for_info_stage(false,"It detects if identical names designate identical files in terms of file content"));
        l.add(new Line_for_info_stage(false,"If names and content are the same, the file is not copied (it is not a brute force copy)"));
        l.add(new Line_for_info_stage(false,"If names are matching but content is different, the source file is copied"));
        l.add(new Line_for_info_stage(false,"and the previous file in the destination is renamed"));
        Info_stage.show_info_stage("Help on backup",l, null, null);
    }
    //**********************************************************
    private void show_fusk_help()
    //**********************************************************
    {
        List<Line_for_info_stage> l = new ArrayList<>();
        l.add(new Line_for_info_stage(false,"Fusk tool: create obsfuscated files that can only be decoded by Klik"));
        l.add(new Line_for_info_stage(false,"The fusk tool will copy recursively down the paths starting in the SOURCE folder"));
        l.add(new Line_for_info_stage(false,"into the DESTINATION folder"));
        l.add(new Line_for_info_stage(false,"It obfuscates all files in the destination"));
        l.add(new Line_for_info_stage(false,"You will be asked for a pin code"));
        l.add(new Line_for_info_stage(false,"You can have multiple pin codes, but at any point of time, klik uses only one"));
        l.add(new Line_for_info_stage(false,"If the pin code is not the good one the images are not displayed"));
        l.add(new Line_for_info_stage(false,"WARNING: this is encryption, if you forget your pin code, recovering your files will be painful"));
        l.add(new Line_for_info_stage(false,"(recovery: someone will have to make a brute force attack code i.e. try all possible pin codes!)"));
        Info_stage.show_info_stage("Help on fusk",l, null, null);
    }



}
