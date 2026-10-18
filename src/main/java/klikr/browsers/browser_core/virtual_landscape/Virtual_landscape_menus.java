// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ../About_klik_stage.java
//SOURCES ../Icon_size.java
//SOURCES ../../image_ml/face_recognition/Python_HTML_Face_recognition_service.java
//SOURCES ../../image_ml/ML_servers_util.java
//SOURCES ../../image_ml/image_similarity/Similarity_cache.java
//SOURCES ../../image_ml/image_similarity/Feature_vector_cache.java
//SOURCES ../../util/files_and_paths/Name_cleaner.java
//SOURCES ../../experimental/metadata/Tag_items_management_stage.java
//SOURCES ../../properties/boolean_features/Preferences_stage.java
//SOURCES ../items/Item_folder.java
//SOURCES ../../look/Look_and_feel_style.java
//SOURCES ../../look/my_i18n/Language.java



package klikr.browsers.browser_core.virtual_landscape;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import klikr.Window_builder;
import klikr.audio.player.The_audio_player;
import klikr.browsers.browser_core.items.Iconifiable_item_type;
import klikr.browsers.browser_core.items.Item_context;
import klikr.machine_learning.ONNX_java_ML.Onnx_face_recognition_service;
import klikr.machine_learning.face_recognition.Face_recognition_service;
import klikr.machine_learning.image_similarity.Onnx_Feature_vector_source_for_image_similarity;
import klikr.path_lists.Files_and_folders;
import klikr.path_lists.Path_list_provider_for_playlist;
import klikr.util.External_application;
import klikr.Window_type;
import klikr.path_lists.Path_list_provider;
import klikr.util.Kontext;
import klikr.util.cache.Cache_folder;
import klikr.util.cache.RAM_caches;
import klikr.util.execute.Execute_result;
import klikr.util.execute.actor.Actor_engine;
import klikr.browsers.browser_core.Icon_size;
import klikr.path_lists.Path_list_provider_for_file_system;
import klikr.browsers.browser_core.items.Item_folder;
import klikr.browsers.browser_core.locator.Folders_with_large_images_locator;
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
import klikr.util.deduplicate.Deduplication_engine;
import klikr.machine_learning.deduplication.Deduplication_by_similarity_engine;
import klikr.machine_learning.feature_vector.Feature_vector_cache;
import klikr.images.Image_context;
import klikr.util.image.decoding.Exif_metadata_extractor;
import klikr.look.Look_and_feel_manager;
import klikr.look.Look_and_feel_style;
import klikr.look.my_i18n.My_I18n;
import klikr.look.my_i18n.Language;
import klikr.machine_learning.feature_vector.Feature_vector_source;
import klikr.machine_learning.song_similarity.Feature_vector_source_for_song_similarity;
import klikr.settings.*;
import klikr.settings.boolean_features.Booleans;
import klikr.settings.boolean_features.Feature;
import klikr.settings.boolean_features.Feature_cache;
import klikr.util.execute.Execute_command;
import klikr.util.execute.System_open_actor;
import klikr.util.files_and_paths.*;
import klikr.change.old_and_new.Command;
import klikr.change.old_and_new.Old_and_new_Path;
import klikr.change.old_and_new.Status;
import klikr.util.info_stage.Info_stage;
import klikr.util.info_stage.Line_for_info_stage;
import klikr.util.log.Logger;
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
    public final Kontext context;
    CheckMenuItem select_all_files_menu_item;
    CheckMenuItem select_all_folders_menu_item;

    private static final double too_far_away_image = 0.14;
    private static final double too_far_away_song = 0.05;//0.04;

    //**********************************************************
    Virtual_landscape_menus(Virtual_landscape virtual_landscape, Change_receiver change_receiver)
    //**********************************************************
    {
        this.virtual_landscape = virtual_landscape;
        this.change_receiver = change_receiver;
        this.context = virtual_landscape.context;
    }


    //**********************************************************
    ContextMenu define_files_ContextMenu()
    //**********************************************************
    {
        ContextMenu files_contextmenu = new ContextMenu();
        Look_and_feel_manager.set_context_menu_look(files_contextmenu,context.logger());

        if ( virtual_landscape.window_type == Window_type.File_system_2D)
        {
            files_contextmenu.getItems().add(make_select_all_folders_menu_item(context));
        }

        {
            String create_string = My_I18n.get_I18n_string("Create",context);
            Menu create = new Menu(create_string);
            Look_and_feel_manager.set_menu_item_look(create,context.logger());

            if ( virtual_landscape.window_type == Window_type.File_system_2D)
            {
                Menu_items.add_menu_item_for_menu("Create_new_empty_directory",true, null,
                        event -> create_new_directory(),create,context);
            }
            /*if (Feature_cache.get(Feature.Enable_image_playlists))
            {
                context.log_with_stack_trace(("not implemented"));
                //Menu_items.add_menu_item2("Create_new_empty_image_playlist",event -> Window_builder.create_new_image_playlist(context)));
            }*/
            Menu_items.add_menu_item_for_menu("Create_PDF_contact_sheet",true, null,event -> create_PDF_contact_sheet(),create,context);
            if ( virtual_landscape.window_type == Window_type.File_system_2D)
            {
                Menu_items.add_menu_item_for_menu("Stash_Files_In_Folders_By_Year",true, null, event -> sort_by_time(Virtual_landscape.Sort_by_time.year),create,context);
                Menu_items.add_menu_item_for_menu("Stash_Files_In_Folders_By_Month",true, null,event -> sort_by_time(Virtual_landscape.Sort_by_time.month),create,context);
                Menu_items.add_menu_item_for_menu("Stash_Files_In_Folders_By_Day",true, null,event -> sort_by_time(Virtual_landscape.Sort_by_time.day),create,context);
                create.getItems().add(make_import_menu());
            }
            files_contextmenu.getItems().add(create);
        }
        {
            String search_string = My_I18n.get_I18n_string("Search",context);
            Menu search = new Menu(search_string);
            Look_and_feel_manager.set_menu_item_look(search,context.logger());

            Menu_items.add_menu_item_for_menu("Search_by_keywords", true,virtual_landscape.find.getDisplayText(), event -> search_files_by_keyworks_fx(),search,context);
            if ( virtual_landscape.window_type == Window_type.File_system_2D)
            {
                Menu_items.add_menu_item_for_menu("Show_Where_Are_Images",true, null,event -> show_where_are_images(),search,context);
            }
            search.getItems().add(make_add_to_Enable_face_recognition_training_set_menu_item());


            files_contextmenu.getItems().add(search);
        }
        if (Feature_cache.get(Feature.Enable_face_recognition))
        {
            Menu face_recognition = new Menu("Face recognition");
            Look_and_feel_manager.set_menu_item_look(face_recognition,context.logger());

            face_recognition.getItems().add(make_load_face_recog_menu_item());
            face_recognition.getItems().add(make_save_face_recog_menu_item());
            face_recognition.getItems().add(make_reset_face_recog_menu_item());
            face_recognition.getItems().add(make_start_auto_face_recog_menu_item());
            Optional<MenuItem> op = make_whole_folder_face_recog_menu_item();
            op.ifPresent((MenuItem mi) -> face_recognition.getItems().add(mi));

            files_contextmenu.getItems().add(face_recognition);
        }
        if ( virtual_landscape.window_type == Window_type.File_system_2D)
        {
            String cleanup = My_I18n.get_I18n_string("Clean_Up",context);
            Menu m = new Menu(cleanup);
            Look_and_feel_manager.set_menu_item_look(m,context.logger());

            Menu_items.add_menu_item_for_menu("Remove_empty_folders",true,null, event -> remove_empty_folders_fx(false),m,context);

            if (Feature_cache.get(Feature.Enable_recursive_empty_folders_removal))
            {
                Menu_items.add_menu_item_for_menu("Remove_empty_folders_recursively",true, null,event -> remove_empty_folders_fx(true),m,context);
            }

            if (Feature_cache.get(Feature.Enable_name_cleaning) )
            {
                Menu_items.add_menu_item_for_menu("Clean_up_names",true, null,event -> clean_up_names_fx(),m,context);
            }
            if ( Feature_cache.get(Feature.Enable_corrupted_images_removal) )
            {
                Menu_items.add_menu_item_for_menu("Remove_corrupted_images",true, null,event -> remove_corrupted_images_fx(),m,context);
            }


            if (Feature_cache.get(Feature.Enable_bit_level_deduplication) )
            {
                create_deduplication_menu(m);
            }

            if (Feature_cache.get(Feature.Enable_image_similarity) )
            {
                create_image_similarity_deduplication_menu(m);
                create_song_similarity_deduplication_menu(m);
            }
            Virtual_landscape.attach_contextmenu_to_browser(context.owner(),files_contextmenu);
            files_contextmenu.getItems().add(m);

            if (Feature_cache.get(Feature.Enable_backup)) {
                files_contextmenu.getItems().add(make_backup_menu());
            }
            if (Feature_cache.get(Feature.Enable_fusk))
            {
                if (Feature_cache.get(Feature.Fusk_is_on))
                {
                    files_contextmenu.getItems().add(make_fusk_menu());
                }
            }
        }


        return files_contextmenu;
    }

    //**********************************************************
    public void show_where_are_images()
    //**********************************************************
    {
        Optional<Path> p = virtual_landscape.path_list_provider.get_folder_path();
        if ( p.isPresent() )
        {
            Folders_with_large_images_locator.locate(virtual_landscape.application, p.get(), 10, 200_000, context);
        }
    }

    //**********************************************************
    public void sort_by_time(Virtual_landscape.Sort_by_time sort_by_time)
    //**********************************************************
    {
        Runnable r = () -> dispatch_by(sort_by_time);
        Actor_engine.execute(r, "Dispatch files by date", context.logger());
    }


    //**********************************************************
    public void dispatch_by(Virtual_landscape.Sort_by_time sort_by)
    //**********************************************************
    {
        List<Path> files = virtual_landscape.path_list_provider.only_file_paths(true,Feature_cache.get(Feature.Show_hidden_files),context.aborter());
        if (files == null) {
            context.log(Logger.error+"ERROR: cannot list files in " + virtual_landscape.path_list_provider.get_key());
        }
        if (files.size() == 0) {
            context.log(Logger.ok+" no file in " + virtual_landscape.path_list_provider.get_key());
        }
        Map<String, Path> folders = new HashMap<>();
        List<Old_and_new_Path> moves = new ArrayList<>();
        for (Path f : files)
        {
            BasicFileAttributes bfa;
            try
            {
                bfa = Files.readAttributes(f, BasicFileAttributes.class);
            }
            catch (IOException e)
            {
                context.log(Logger.error+"BAD " + e);
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
                Path p = virtual_landscape.path_list_provider.resolve( sorter);
                if ( p != null)
                {
                    folder = p;

                    try {
                        Files.createDirectory(folder);
                    } catch (IOException e) {
                        context.log(Logger.error+"BAD " + e);
                        continue;
                    }
                }
            }
            folders.put(sorter, folder);
            {
                // TODO: check if this is useful
                // since perform_safe_moves_in_a_thread will trigger the Chang_gang?
                Optional<Path> displayed_folder_path = virtual_landscape.path_list_provider.get_folder_path();
                if ( displayed_folder_path.isPresent()) {
                    List<Old_and_new_Path> l = new ArrayList<>();
                    l.add(new Old_and_new_Path(displayed_folder_path.get(), displayed_folder_path.get(), Command.command_unknown, Status.move_done, false));
                    Change_gang.report_changes(l, context.owner());
                }
            }
            Old_and_new_Path oanp = new Old_and_new_Path(
                    f,
                    Path.of(folder.toAbsolutePath().toString(), f.getFileName().toString()),
                    Command.command_move,
                    Status.before_command, false);
            moves.add(oanp);

        }

        Moving_files.perform_safe_moves_in_a_thread(moves, true, context);

        // display will be updated because of the Change_gang
    }




    //**********************************************************
    public void create_PDF_contact_sheet()
    //**********************************************************
    {
        Runnable r = this::create_PDF_contact_sheet_in_a_thread;
        Actor_engine.execute(r,"create PDF contact sheet", context.logger());
    }
    //**********************************************************
    public void create_PDF_contact_sheet_in_a_thread()
    //**********************************************************
    {
        Hourglass hourglass = Progress_window.show(
                "Making PDF contact sheet",
                20_000, context).orElse(null);
        List<String> graphicsMagick_command_line = new ArrayList<>();

        {
            graphicsMagick_command_line.add(External_application.GraphicsMagick.get_command(context));
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
            graphicsMagick_command_line.add("jpeg:length=300x200");
            graphicsMagick_command_line.add("-geometry");
            graphicsMagick_command_line.add("300x200+2+2");
            graphicsMagick_command_line.add("*.jpg");
            graphicsMagick_command_line.add(CONTACT_SHEET_FILE_NAME);
        }


        StringBuilder sb = null;
        if ( dbg) sb = new StringBuilder();
        Optional<Path> p = virtual_landscape.path_list_provider.get_folder_path();
        if (p.isEmpty()) return;
        File wd = p.get().toFile();
        Execute_result res = Execute_command.execute_command_list(graphicsMagick_command_line, wd, 2000, sb, context);
        if ( !res.status())
        {
            List<String> verify = new ArrayList<>();
            verify.add(External_application.GraphicsMagick.get_command(context));
            verify.add("--version");
            String home = System.getProperty(String_constants.USER_HOME);
            Execute_result res2 = Execute_command.execute_command_list(verify, new File(home), 20 * 1000, null, context);
            if ( !res2.status())
            {
                Booleans.manage_show_graphicsmagick_install_warning(context);
            }
            return;
        }
        else
        {
            if ( dbg) context.log(Logger.ok+" contact sheet generated "+ sb);
            else
            {
                context.log(Logger.ok+" contact sheet generated : "+ CONTACT_SHEET_FILE_NAME);
                System_open_actor.open_with_system(virtual_landscape.application,Path.of(p.get().toAbsolutePath().toString(), CONTACT_SHEET_FILE_NAME),context);

                Platform.runLater(() ->virtual_landscape.set_status("Contact sheet generated : "+ CONTACT_SHEET_FILE_NAME));
            }
        }
        if ( hourglass != null) hourglass.close();
    }



    //**********************************************************
    public void create_new_directory()
    //**********************************************************
    {
        TextInputDialog dialog = new TextInputDialog(My_I18n.get_I18n_string("New_directory", context));
        Look_and_feel_manager.set_dialog_look(dialog,context.logger());
        dialog.initOwner(context.owner());
        dialog.setWidth(context.getWidth());
        dialog.setTitle(My_I18n.get_I18n_string("New_directory", context));
        dialog.setHeaderText(My_I18n.get_I18n_string("Enter_name_of_new_directory", context));
        dialog.setContentText(My_I18n.get_I18n_string("New_directory_name", context));

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String new_name = result.get();
            for (int i = 0; i < 10; i++)
            {
                try {
                    Optional<Path> p = virtual_landscape.path_list_provider.get_folder_path();
                    if ( p.isPresent()) {
                        Path new_dir = virtual_landscape.path_list_provider.resolve(new_name);
                        if ( new_dir != null) {
                            Files.createDirectory(new_dir);
                            Scroll_position_cache.scroll_position_cache_write(virtual_landscape.path_list_provider.get_key(), new_dir.toAbsolutePath().normalize().toString(),"created new folder", context.logger());
                            virtual_landscape.redraw_fx(true,"created new empty dir", true);
                            break;
                        }

                    }
                }
                catch (IOException e)
                {
                    context.log(Logger.warning+" new directory creation FAILED: " + e);
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
                context
        );

    }
    //**********************************************************
    ContextMenu define_contextmenu_preferences()
    //**********************************************************
    {
        ContextMenu context_menu = new ContextMenu();
        Look_and_feel_manager.set_context_menu_look(context_menu,context.logger());


        context_menu.getItems().add(make_file_sort_method_menu());


        context_menu.getItems().add(make_icon_size_menu());
        if ( Feature_cache.get(Feature.Show_icons_for_folders))
        {
            context_menu.getItems().add(make_folder_icon_size_menu());
        }
        context_menu.getItems().add(make_column_width_menu(virtual_landscape,context));
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
            String s = My_I18n.get_I18n_string(key, context);
            CheckMenuItem menu_item = new CheckMenuItem(s);
            Look_and_feel_manager.set_menu_item_look(menu_item,context.logger());
            menu_item.setMnemonicParsing(false);
            menu_item.setSelected(Feature_cache.get(Feature.Play_music));
            EventHandler<ActionEvent> handler = e ->
            {
                if ( menu_item.isSelected())
                {
                    Feature_cache.update_cached_boolean(Feature.Play_music, true, context);
                    if (The_audio_player.is_running())
                    {
                        context.log("an audio player already exists");
                    }
                    else
                    {
                        context.log("starting new audio player");
                        Path playlist = String_constants.get_playlist_path(context.owner());
                        Path_list_provider playlist_path_list_provider = new Path_list_provider_for_playlist(playlist,context);
                        The_audio_player.play_playlist(virtual_landscape.application, playlist_path_list_provider, context);
                    }
                }
                else
                {
                    Feature_cache.update_cached_boolean(Feature.Play_music, false, context);
                    if ( The_audio_player.is_running())
                    {
                        context.log("killing audio player");
                        The_audio_player.kill_instance();
                    }
                    else
                    {
                        context.log("No audio player to kill?");
                    }
                }

            };
            menu_item.setOnAction(handler);
            context_menu.getItems().add(menu_item);
        }


        Menu_items.add_menu_item_for_context_menu(
                "Clear_Trash_Folder",true,null,
                event -> Static_files_and_paths_utilities.clear_trash(true,context),
                context_menu,context);

        Menu_items.add_menu_item_for_context_menu("Clear_All_RAM_Caches",true,null,
                event -> {
                    RAM_caches.clear_all_RAM_caches(context);
                },
                context_menu,context);

        Menu_items.add_menu_item_for_context_menu("Clear_All_Disk_Caches",true,null,
                event -> {
                    Cache_folder.clear_all_disk_caches(context);
                },
                context_menu,context);


        Menu_items.add_menu_item_for_context_menu(
                "More_Settings",true,null,
                event -> new More_settings_stage("Preferences", context),
                context_menu,context);

        return context_menu;
    }

    //**********************************************************
    private void create_image_similarity_deduplication_menu(Menu clean)
    //**********************************************************
    {
        String txt = My_I18n.get_I18n_string("Image_ML_similarity_deduplication",context);
        Menu menu = new Menu(txt);
        Look_and_feel_manager.set_menu_item_look(menu,context.logger());
        clean.getItems().add(menu);

        // quasi similar means same image dimensions (w & h)
        Menu_items.add_menu_item_for_menu("Deduplicate_with_confirmation_similar_images_of_same_size",true,null,
                event -> {
                    //context.log("Deduplicate manually");
                    Optional<Path> folder_path = virtual_landscape.path_list_provider.get_folder_path();
                    if (folder_path.isPresent()) {
                        Deduplication_by_similarity_engine pbse = new Deduplication_by_similarity_engine(
                                virtual_landscape.application,
                                true,
                                virtual_landscape.path_list_provider,
                                virtual_landscape,
                                too_far_away_image,
                                folder_path.get().toFile(),
                                virtual_landscape.get_image_properties_cache(),
                                get_image_fv_cache,
                                context);
                        pbse.do_your_job();
                    }
                },menu,context);


        Menu_items.add_menu_item_for_menu("Deduplicate_with_confirmation_similar_images",true,null,
                event ->
                {
                    //context.log("Deduplicate manually");
                    Optional<Path> folder_path = virtual_landscape.path_list_provider.get_folder_path();
                    if (folder_path.isPresent())
                    {
                        Deduplication_by_similarity_engine dbse = new Deduplication_by_similarity_engine(
                                virtual_landscape.application,
                                true,
                                virtual_landscape.path_list_provider,
                                virtual_landscape,
                                too_far_away_image,
                                folder_path.get().toFile(),
                                null, // does not check image sizes
                                get_image_fv_cache,
                                context);
                        dbse.do_your_job();
                    }
                },menu,context);


    }


    //**********************************************************
    private void create_song_similarity_deduplication_menu(Menu clean)
    //**********************************************************
    {
        String txt = My_I18n.get_I18n_string("Song_ML_similarity_deduplication",context);
        Menu menu = new Menu(txt);
        Look_and_feel_manager.set_menu_item_look(menu,context.logger());
        clean.getItems().add(menu);

        Menu_items.add_menu_item_for_menu("Deduplicate_with_confirmation_quasi_similar_songs",true,null,
                event -> {
                    //context.log("Deduplicate manually");
                    Optional<Path> folder_path = virtual_landscape.path_list_provider.get_folder_path();
                    if(folder_path.isPresent()) {
                        Deduplication_by_similarity_engine dse = new Deduplication_by_similarity_engine(
                                virtual_landscape.application,
                                false,
                                virtual_landscape.path_list_provider,
                                virtual_landscape,
                                too_far_away_song / 10,
                                folder_path.get().toFile(),
                                null,
                                get_song_fv_cache,
                                context);
                        dse.do_your_job();
                    }
                },menu,context);

        Menu_items.add_menu_item_for_menu("Deduplicate_with_confirmation_songs_sounding_a_bit_the_same",true,null,
                event -> {
                    //context.log("Deduplicate manually");
                    Optional<Path> folder_path = virtual_landscape.path_list_provider.get_folder_path();
                    if(folder_path.isPresent()) {
                        Deduplication_by_similarity_engine dse = new Deduplication_by_similarity_engine(
                                virtual_landscape.application,
                                false,
                                virtual_landscape.path_list_provider,
                                virtual_landscape,
                                too_far_away_song,
                                folder_path.get().toFile(),
                                null,
                                get_song_fv_cache,
                                context);
                        dse.do_your_job();
                    }
                },menu,context);


    }

    //**********************************************************
    public Supplier<Feature_vector_cache> get_image_fv_cache = new Supplier<>()
    //**********************************************************
    {
        public Feature_vector_cache get() {

            //Feature_vector_source fvs = new Server_Feature_vector_source_for_image_similarity(context);
            Feature_vector_source fvs = new Onnx_Feature_vector_source_for_image_similarity(context);

            List<Path> paths = virtual_landscape.path_list_provider.only_image_paths(false, Feature_cache.get(Feature.Show_hidden_files),context.aborter());
            return Feature_vector_cache.preload_all_feature_vector_in_cache(fvs, paths, virtual_landscape.path_list_provider, context);
        }
    };

    //**********************************************************
    public Supplier<Feature_vector_cache> get_song_fv_cache = new Supplier<>()
    //**********************************************************
    {
        public Feature_vector_cache get()
        {
            Feature_vector_source fvs = new Feature_vector_source_for_song_similarity(context.aborter());
            List<Path> paths = virtual_landscape.path_list_provider.only_song_paths( true, Feature_cache.get(Feature.Show_hidden_files),context.aborter());
            return Feature_vector_cache.preload_all_feature_vector_in_cache(fvs, paths, virtual_landscape.path_list_provider, context);
        }
    };

    //**********************************************************
    private void create_deduplication_menu(Menu clean)
    //**********************************************************
    {
        String txt = My_I18n.get_I18n_string("File_bit_exact_deduplication",context);
        Menu menu = new Menu(txt);
        Look_and_feel_manager.set_menu_item_look(menu,context.logger());

        Menu_items.add_menu_item_for_menu("Deduplicate_help",true,null,
                event -> Popups.popup_warning(
                        "Help on deduplication",
                        "The deduplication tool will look recursively down the path starting at:" + virtual_landscape.path_list_provider.get_key() +
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
                        context), menu,context);

        Menu_items.add_menu_item_for_menu("Deduplicate_count",true,null,
                event -> {
                    Optional<Path> folder_path = virtual_landscape.path_list_provider.get_folder_path();
                    if(folder_path.isPresent()) {
                        Deduplication_engine de = new Deduplication_engine(virtual_landscape.application, folder_path.get().toFile(), virtual_landscape.path_list_provider, virtual_landscape, context);
                        de.count(false);
                    }
                },menu,context);
        Menu_items.add_menu_item_for_menu("Deduplicate_manual",true, null,event -> {
            //context.log("Deduplicate manually");
            Optional<Path> folder_path = virtual_landscape.path_list_provider.get_folder_path();
            if(folder_path.isPresent())
            {
                Deduplication_engine de = new Deduplication_engine(virtual_landscape.application, folder_path.get().toFile(), virtual_landscape.path_list_provider,virtual_landscape,context);
                de.do_your_job(false);
            }
        },menu,context);


        Menu_items.add_menu_item_for_menu("Deduplicate_auto",true,null,
                event -> {
                    //context.log("Deduplicate auto");

                    if ( !Popups.popup_ask_for_confirmation( Logger.warning+" EXPERIMENTAL! Are you sure?","Automated deduplication will recurse down this folder and delete (for good = not send them in recycle bin) all duplicate files",context)) return;

                    Optional<Path> folder_path = virtual_landscape.path_list_provider.get_folder_path();
                    if(folder_path.isPresent()) {
                        Deduplication_engine de = new Deduplication_engine(virtual_landscape.application, folder_path.get().toFile(), virtual_landscape.path_list_provider, virtual_landscape, context);
                        de.do_your_job(true);
                    }
                },menu,context);




        clean.getItems().add(menu);
    }



    //**********************************************************
    public Button make_button_that_behaves_like_a_folder(
            Path path,
            String text,
            double height,
            double min_width,
            boolean is_trash_button)
    //**********************************************************
    {
        if ( path == null)
        {
            context.log_with_stack_trace(("❌❌❌ FATAL: cannot make button for null path"));
            return null;
        }
        Path_list_provider path_list_provider =  new Path_list_provider_for_file_system(path,context);

        Item_context item_context = new Item_context(path, path_list_provider,Iconifiable_item_type.folder, is_trash_button, virtual_landscape.shutdown_target,
                null, virtual_landscape.the_Scene,
                virtual_landscape,virtual_landscape.application,virtual_landscape.window_type,virtual_landscape.get_top_left(), context);


        return Item_folder.button_for_a_directory(virtual_landscape, virtual_landscape.shutdown_target,item_context,text, min_width, height);
    }











    //**********************************************************
    public MenuItem make_fusk_check_menu_item()
    //**********************************************************
    {
        String key = Feature.Fusk_is_on.name();
        String text = My_I18n.get_I18n_string(key,context);

        CheckMenuItem item = new CheckMenuItem(text);
        Look_and_feel_manager.set_menu_item_look(item, context.logger());
        item.setSelected(Feature_cache.get(Feature.Fusk_is_on));
        item.setOnAction(actionEvent ->
        {
            boolean val = ((CheckMenuItem) actionEvent.getSource()).isSelected();
            Feature_cache.update_cached_boolean(Feature.Fusk_is_on,val,context);

        });
        Items_with_explanation.add_question_mark_button(key, item, context);
        return item;
    }


    //**********************************************************
    public MenuItem make_add_to_Enable_face_recognition_training_set_menu_item()
    //**********************************************************
    {
        String key = "Add_all_images_to_face_recognition_training_set";
        MenuItem item = Menu_items.make_menu_item(key,true,null,
        event -> {
            Face_recognition_service i = Onnx_face_recognition_service.get_instance(virtual_landscape.application, context);
            context.log(Logger.error+"NOT IMPLEMENTED add_all_pictures_to_training_set for "+virtual_landscape.path_list_provider.get_key());

        },context);
        Items_with_explanation.add_question_mark_button(key, item, virtual_landscape.context);
        return item;
    }

    //**********************************************************
    public MenuItem make_save_face_recog_menu_item()
    //**********************************************************
    {
        return Menu_items.make_menu_item(
                "Save_Face_Recognition",true,null,
                event -> Onnx_face_recognition_service.save(),
                context);
    }

    //**********************************************************
    public MenuItem make_load_face_recog_menu_item()
    //**********************************************************
    {
        return Menu_items.make_menu_item(
                "Load_Face_Recognition",true,null,
                event -> Onnx_face_recognition_service.load(virtual_landscape.application, context),
                context);

    }


    //**********************************************************
    public MenuItem make_reset_face_recog_menu_item()
    //**********************************************************
    {
        return Menu_items.make_menu_item(
                "Start_New_Face_Recognition",true,null,
                event -> Onnx_face_recognition_service.start_new(virtual_landscape.application, context),
                context);
    }


    //**********************************************************
    public MenuItem make_start_auto_face_recog_menu_item()
    //**********************************************************
    {
        return Menu_items.make_menu_item(
                "Auto_Face_Recognition",true,null,
                event -> Onnx_face_recognition_service.auto(virtual_landscape.application, Path.of(virtual_landscape.path_list_provider.get_key()),context),context);

    }

    //**********************************************************
    public Optional<MenuItem> make_whole_folder_face_recog_menu_item()
    //**********************************************************
    {
        Optional<Path> p = virtual_landscape.path_list_provider.get_folder_path();
        if (p.isEmpty()) return Optional.empty();

        return Optional.of(Menu_items.make_menu_item(
                "Do_Face_Recognition_On_Whole_Folder",true,null,
                event -> Onnx_face_recognition_service.do_folder(virtual_landscape.application, p.get(),context),
                context));

    }


    


    //**********************************************************
    public void remove_empty_folders_fx(boolean recursively)
    //**********************************************************
    {
        remove_empty_folders(recursively);
        // can be called from a thread which is NOT the FX event thread
        Jfx_batch_injector.inject(() -> virtual_landscape.redraw_fx(true,"remove empty folder",false),context);
    }

    

    //**********************************************************
    public MenuItem make_select_all_folders_menu_item(Kontext context)
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Select_all_folders_for_drag_and_drop",context);
        select_all_folders_menu_item = new CheckMenuItem(text+" "+virtual_landscape.select_all_folders.getDisplayText());
        Look_and_feel_manager.set_menu_item_look(select_all_folders_menu_item, context.logger());

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
        String text = My_I18n.get_I18n_string("Select_all_files_for_drag_and_drop",context);

        select_all_files_menu_item= new CheckMenuItem(text+ " "+virtual_landscape.select_all_files1.getDisplayText());
        Look_and_feel_manager.set_menu_item_look(select_all_files_menu_item,context.logger());
        select_all_files_menu_item.setSelected(false);
        select_all_files_menu_item.setOnAction(event -> {
            if ( ((CheckMenuItem) event.getSource()).isSelected())
            {
                virtual_landscape.selection_handler.select_all_files_in_folder(virtual_landscape.path_list_provider, context.aborter());
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
            Application application,
            Map<LocalDateTime,String> the_whole_history,
            Path_list_provider path_list_provider,
            Path top_left,
            Shutdown_target shutdown_target,
            Window_type window_type, Kontext context)
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("History",context);

        Menu history_menu = new Menu(text);
        Look_and_feel_manager.set_menu_item_look(history_menu,context.logger());

        populate_history_menu(history_menu, application,the_whole_history,path_list_provider, top_left, shutdown_target, window_type, context);
        return history_menu;
    }

    //**********************************************************
    public static MenuButton make_bookmarks_menubutton(Application application,Path_list_provider path_list_provider, Path top_left, Shutdown_target shutdown_target, Window_type window_type, Kontext context)
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Bookmarks",context);
        MenuButton mb = new MenuButton(text);
        //Look_and_feel_manager.set_menubutton_and_image_look(mb,null,0,context);

        populate_bookmarks_menubutton(application,mb, path_list_provider, top_left, shutdown_target, window_type,context);
        return mb;
    }
    //**********************************************************
    public static Menu make_roots_menu(
            Application application,
            Path path,
            Path top_left,
            Shutdown_target shutdown_target,
            Window_type window_type,
            Kontext context)
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("File_System_Roots",context);
        Menu roots_menu = new Menu(text);
        Look_and_feel_manager.set_menu_item_look(roots_menu,context.logger());

        create_roots_menu(
                application,
                roots_menu,
                path,
                top_left,
                shutdown_target,
                window_type,
                context);
        return roots_menu;
    }
    //**********************************************************
    public static void populate_undo_menubutton(MenuButton mb, Kontext context )
    //**********************************************************
    {

        KeyCodeCombination undo = new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN);

        double x = context.getX()+100;
        double y = context.getY()+100;

        mb.getItems().add(Menu_items.make_menu_item(
                "Undo_LAST_move_or_delete",true, undo.getDisplayText(),
                event -> Undo_for_moves.perform_last_undo_fx(context),context));
        mb.getItems().add(Menu_items.make_menu_item(
                "Show_Undos",true,null,
                event -> pop_up_whole_undo_history(context),context));
        mb.getItems().add(Menu_items.make_menu_item(
                "Clear_Undos",true,null,
                event -> Undo_for_moves.remove_all_undo_items(context),context));
    }



    //**********************************************************
    public static void populate_history_menu(
            Menu history_menu,
            Application application,
            Map<LocalDateTime, String> the_whole_history,
            Path_list_provider path_list_provider,
            Path top_left,
            Shutdown_target shutdown_target,
            Window_type window_type,
            Kontext context)
    //**********************************************************
    {

        Menu_items.add_menu_item_for_menu("Clear_History",true,null,
                    event ->
                    {
                        context.log("clearing history");
                        History_engine.get(context).clear();
                        Window_builder.replace_same_folder(
                                application,
                                shutdown_target,
                                window_type,
                                path_list_provider,
                                path_list_provider.get_key(),
                                top_left,context);
                    },history_menu,context);


        int max_on_screen = 20;
        int on_screen = 0;
        MenuItem more = null;
        Map<String, History_item> path_already_done = new HashMap<>();
        for (History_item hi : History_engine.get(context).get_all_history_items())
        {
            if ( on_screen < max_on_screen)
            {
                if ( path_already_done.get(hi.value) != null)
                {
                    continue;
                }
                String local_displayed_string = hi.value;
                // remove home
                String home = System.getProperty(String_constants.USER_HOME);
                if ( local_displayed_string.startsWith(home) )local_displayed_string = local_displayed_string.substring(home.length());

                if ( local_displayed_string.length() > MAX_MENU_ITEM_STRING_LENGTH)
                {
                    // trick to avoid that the menu is not displayed when items are very wide
                    // which may happens with the largest fonts
                    local_displayed_string = local_displayed_string.substring(0,MAX_MENU_ITEM_STRING_LENGTH)+" ...";
                }
                MenuItem item = new MenuItem(local_displayed_string);
                Look_and_feel_manager.set_menu_item_look(item, context.logger());
                item.setMnemonicParsing(false);
                Optional<Path> folder_path = path_list_provider.get_folder_path();
                if (folder_path.isPresent())
                {
                    if (hi.value.equals(folder_path.get().toAbsolutePath().toString()))
                    {
                        // show the one we are in as inactive
                        item.setDisable(true);
                    }
                }
                if ( !hi.available)
                {
                    item.setDisable(true);
                }
                item.setOnAction(event ->
                {
                    on_history_item_clicked(Path.of(hi.value), application, shutdown_target, window_type,context);
                });
                path_already_done.put(hi.value,hi);
                history_menu.getItems().add(item);
                on_screen++;
            }
            else
            {
                if ( more == null)
                {
                    String text = My_I18n.get_I18n_string("Show_Whole_History",context);
                    more =  new MenuItem(text);
                    Look_and_feel_manager.set_menu_item_look(more, context.logger());

                    history_menu.getItems().add(more);
                    more.setOnAction(actionEvent -> pop_up_whole_history(
                            application,
                            the_whole_history,
                             path_list_provider,
                             top_left,
                             shutdown_target,
                             window_type,
                             context
                    ));
                }
                add_to_whole_history(the_whole_history, hi);
            }
        }
    }

    //**********************************************************
    public static void populate_history_menubutton(
            MenuButton mb,
            Application application,
            Map<LocalDateTime, String> the_whole_history,
            Path_list_provider path_list_provider,
            Path top_left,
            Shutdown_target shutdown_target,
            Window_type window_type,
            Kontext context)
    //**********************************************************
    {

        Menu_items.add_menu_item_for_menubutton("Clear_History",true,null,
                event ->
                {
                    context.log("clearing history");
                    History_engine.get(context).clear();
                    Window_builder.replace_same_folder(
                            application,
                            shutdown_target,
                            window_type,
                            path_list_provider,
                            path_list_provider.get_key(),
                            top_left,context);
                },mb,context);


        int max_on_screen = 20;
        int on_screen = 0;
        MenuItem more = null;
        Map<String, History_item> path_already_done = new HashMap<>();
        for (History_item hi : History_engine.get(context).get_all_history_items())
        {
            if ( on_screen < max_on_screen)
            {
                if ( path_already_done.get(hi.value) != null)
                {
                    continue;
                }
                String local_displayed_string = hi.value;
                // remove home
                String home = System.getProperty(String_constants.USER_HOME);
                if ( local_displayed_string.startsWith(home) )local_displayed_string = local_displayed_string.substring(home.length());

                if ( local_displayed_string.length() > MAX_MENU_ITEM_STRING_LENGTH)
                {
                    // trick to avoid that the menu is not displayed when items are very wide
                    // which may happens with the largest fonts
                    local_displayed_string = local_displayed_string.substring(0,MAX_MENU_ITEM_STRING_LENGTH)+" ...";
                }
                MenuItem item = new MenuItem(local_displayed_string);
                Look_and_feel_manager.set_menu_item_look(item, context.logger());
                item.setMnemonicParsing(false);
                Optional<Path> folder_path = path_list_provider.get_folder_path();
                if (folder_path.isPresent())
                {
                    if (hi.value.equals(folder_path.get().toAbsolutePath().toString()))
                    {
                        // show the one we are in as inactive
                        item.setDisable(true);
                    }
                }
                if ( !hi.available)
                {
                    item.setDisable(true);
                }
                item.setOnAction(event ->
                {
                    on_history_item_clicked(Path.of(hi.value), application, shutdown_target, window_type,  context);
                });
                path_already_done.put(hi.value,hi);
                mb.getItems().add(item);
                on_screen++;
            }
            else
            {
                if ( more == null)
                {
                    String text = My_I18n.get_I18n_string("Show_Whole_History",context);
                    more =  new MenuItem(text);
                    Look_and_feel_manager.set_menu_item_look(more, context.logger());

                    mb.getItems().add(more);
                    more.setOnAction(actionEvent -> pop_up_whole_history(
                            application,
                            the_whole_history,
                            path_list_provider,
                            top_left,
                            shutdown_target,
                            window_type,
                            context
                    ));
                }
                add_to_whole_history(the_whole_history, hi);
            }
        }
    }

    public static void on_history_item_clicked(
            Path target_folder_path,
            Application application,
            Shutdown_target shutdown_target, Window_type window_type,
            Kontext context)
    {
        context.log("on_history_item_clicked "+target_folder_path);
        if ( Guess_file_type.is_this_path_extension_an_audio_playlist(target_folder_path, context.logger()))
        {
            Window_builder.additional_no_past(application,Window_type.Song_playlist,new Path_list_provider_for_playlist(target_folder_path,  context), context);
        }
        Window_builder.replace_different_folder(
                application,
                shutdown_target,
                window_type,
                new Path_list_provider_for_file_system(target_folder_path, context),
                target_folder_path,
                target_folder_path,
                context);
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
            Application application,
            Map<LocalDateTime,String> the_whole_history,
            Path_list_provider path_list_provider,
            Path top_left,
            Shutdown_target shutdown_target,
            Window_type window_type,
            Kontext context)
    //**********************************************************
    {
        Optional<Path> p = path_list_provider.get_folder_path();
        if ( p.isEmpty()) return;
        Active_list_stage_action action = text -> {
            Window_builder.replace_different_folder(
                    application,
                    shutdown_target,
                    window_type,
                    new Path_list_provider_for_file_system(Path.of(text),context),
                    p.get(),
                    top_left,
                    context);
        };
        Datetime_to_signature_source source = new Datetime_to_signature_source() {
            @Override
            public Map<LocalDateTime, String> get_map_of_date_to_signature() {
                return the_whole_history;
            }
        };
        Active_list_stage.show_active_list_stage("Whole history: "+the_whole_history.size()+" items", source, action, context);
    }

    //**********************************************************
    public static void populate_bookmarks_menubutton(
            Application application,
            MenuButton mb,
            Path_list_provider path_list_provider,
            Path top_left,
            Shutdown_target shutdown_target,
            Window_type window_type,
            Kontext context)
    //**********************************************************
    {
        if (path_list_provider.get_folder_path().isEmpty()) return;
        Path current_path = path_list_provider.get_folder_path().get();
        KeyCodeCombination bookmark_this = new KeyCodeCombination(KeyCode.D, KeyCombination.SHORTCUT_DOWN);

        Menu_items.add_menu_item_for_menubutton("Bookmark_this", true,bookmark_this.getDisplayText(),
                event ->
                {
                    Bookmarks.get(context).add(current_path.toAbsolutePath().toString());
                },
                mb,context);
        Menu_items.add_menu_item_for_menubutton("Clear_Bookmarks",true,null,
                event -> Bookmarks.get(context).clear(),
                mb,context);


        for (String hi : Bookmarks.get(context).get_list())
        {

            String s = Path_list_provider_for_file_system.short_path_string(Path.of(hi));
            Menu_items.add_menu_item_for_menubutton(s,false,null,
           (event -> {
                Window_builder.replace_different_folder(
                        application,
                        shutdown_target,
                        window_type,
                        new Path_list_provider_for_file_system(Path.of(hi),context),
                        current_path,
                        top_left,
                        context);
            }),mb,context);


        }
    }


    //**********************************************************
    public static void create_undos_menu(Menu undos_menu,  Kontext context)
    //**********************************************************
    {

        KeyCodeCombination undo = new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN);

        undos_menu.getItems().add(Menu_items.make_menu_item(
                "Undo_LAST_move_or_delete", true,undo.getDisplayText(),
                event -> Undo_for_moves.perform_last_undo_fx(context),context));
        undos_menu.getItems().add(Menu_items.make_menu_item(
                "Show_Undos",true,null,
                event -> pop_up_whole_undo_history(context),context));
        undos_menu.getItems().add(Menu_items.make_menu_item(
                "Clear_Undos",true,null,
                event -> Undo_for_moves.remove_all_undo_items(context),context));
    }

    //**********************************************************
    public static void create_roots_menu(
            Application application,
            Menu roots_menu,
            Path path,
            Path top_left,
            Shutdown_target shutdown_target,
            Window_type window_type,
            Kontext context)
    //**********************************************************
    {
        for ( File f : File.listRoots())
        {
            String text = f.getAbsolutePath().toString();
            MenuItem item = new MenuItem(text);
            Look_and_feel_manager.set_menu_item_look(item, context.logger());

            item.setOnAction(event -> {
                Window_builder.replace_different_folder(
                        application,
                        shutdown_target,
                        window_type,
                        new Path_list_provider_for_file_system(f.toPath(),context),
                        path,
                        top_left,
                        context);
            });
            roots_menu.getItems().add(item);
        }
    }

    //**********************************************************
    private static void pop_up_whole_undo_history(Kontext context)
    //**********************************************************
    {
        Active_list_stage_action action = signature ->
        {
            Map<String, Undo_item> signature_to_undo_item = Undo_for_moves.get_instance(context).get_signature_to_undo_item();
            Undo_item item = signature_to_undo_item.get(signature);
            if ( item == null)
            {
                context.log_with_stack_trace((Logger.error+"item == null for signature="+signature));
                return;
            }
            if ( !Undo_for_moves.check_validity(item, context))
            {
                Popups.popup_warning(Logger.warning+" Invalid undo item ignored","The file was probably moved since?",true,context);
                Undo_for_moves.remove_invalid_undo_item(item, context);
                return;
            }
            context.log(Logger.ok+" undo_item="+item.to_string());

            boolean ok = Popups.popup_ask_for_confirmation(Logger.warning+" Please confirm.",
                    "Undoing this will move the file(s) back to their original location.\n" +
                            item.to_string()+"\n"+
                            item.to_string(),
                    context);

            if ( ok) Undo_for_moves.perform_undo(item, context);
        };
        String title = My_I18n.get_I18n_string("Whole_Undo_History",context);
        Undo_for_moves.undo_stages.add(Active_list_stage.show_active_list_stage(title, Undo_for_moves.get_instance(context), action, context));
    }



    //**********************************************************
    public Menu make_style_menu_item()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Style",context);
        List<CheckMenuItem> all_check_menu_items = new ArrayList<>();
        Menu menu = new Menu(text);
        Look_and_feel_manager.set_menu_item_look(menu,context.logger());

        for( Look_and_feel_style s : Look_and_feel_style.values())
        {
            create_check_menu_item_for_style(menu, s, all_check_menu_items);
        }
        /*Look_and_feel_style current_style = Look_and_feel_manager.get_instance(context).get_look_and_feel_style();
        if ( current_style == Look_and_feel_style.material)
        {
            MenuItem custom_color_item = new MenuItem(My_I18n.get_I18n_string("Choose_Custom_Color",context));
            Look_and_feel_manager.set_menu_item_look(menu_item,context);
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
        context.log(("color picker !"));
        Color default_color = Non_booleans_properties.get_custom_color(virtual_landscape.owner);
        ColorPicker color_picker = new ColorPicker(default_color);
        Look_and_feel_manager.set_region_look(color_picker, context);
        color_picker.setOnAction((ActionEvent e) -> {
            Color new_color = color_picker.getValue();
            context.log(("color picker new color = "+new_color));
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
        context.log(("color picker shown"));
    }
*/
    //**********************************************************
    public void create_check_menu_item_for_style(Menu menu, Look_and_feel_style style, List<CheckMenuItem> all_check_menu_items)
    //**********************************************************
    {
        CheckMenuItem check_menu_item = new CheckMenuItem(style.name());
        Look_and_feel_manager.set_menu_item_look(check_menu_item, context.logger());

        Look_and_feel_style current_style = Look_and_feel_manager.get_instance(context.logger()).get_look_and_feel_style();
        check_menu_item.setSelected(current_style.name().equals(style.name()));
        check_menu_item.setOnAction(actionEvent -> {
            CheckMenuItem local = (CheckMenuItem) actionEvent.getSource();
            if (local.isSelected()) {
                for ( CheckMenuItem cmi : all_check_menu_items)
                {
                    if ( cmi != local) cmi.setSelected(false);
                }
                Look_and_feel_manager.set_look_and_feel(style,  context);
            }
        });
        menu.getItems().add(check_menu_item);
        all_check_menu_items.add(check_menu_item);
    }

    //**********************************************************
    public Menu make_language_menu()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Language",context);
        List<CheckMenuItem> all_check_menu_items = new ArrayList<>();
        Menu menu = new Menu(text);
        Look_and_feel_manager.set_menu_item_look(menu,context.logger());

        String current = Non_booleans_properties.get_language_key();

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
        Look_and_feel_manager.set_menu_item_look(item, context.logger());
        item.setGraphic(new ImageView(language_key.get_icon(context)));
        item.setSelected(current.equals(language_key.name()));
        item.setOnAction(actionEvent -> {
            CheckMenuItem local = (CheckMenuItem) actionEvent.getSource();
            if (local.isSelected())
            {
                for ( CheckMenuItem cmi : all_check_menu_items)
                {
                    if ( cmi != local) cmi.setSelected(false);
                }
                My_I18n.set_new_language(language_key,  context); /// will trigger a repaint via String_setting_change_target
            }
        });
        menu.getItems().add(item);
        all_check_menu_items.add(item);
    }


    //**********************************************************
    public static void create_menu_item_for_one_column_width(Menu menu, int length, List<CheckMenuItem> all_check_menu_items, Virtual_landscape local_virtual_landscape, Kontext context)
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string(String_constants.COLUMN_WIDTH,context);
        CheckMenuItem item = new CheckMenuItem(text + " = " +length);
        Look_and_feel_manager.set_menu_item_look(item, context.logger());
        int actual_size = Non_booleans_properties.get_column_width();
        item.setSelected(actual_size == length);
        item.setOnAction(actionEvent -> {
            CheckMenuItem local = (CheckMenuItem) actionEvent.getSource();
            if (local.isSelected()) {
                for ( CheckMenuItem cmi : all_check_menu_items)
                {
                    if ( cmi != local) cmi.setSelected(false);
                }
                Non_booleans_properties.set_column_width(length);
                local_virtual_landscape.redraw_fx(false,"column width changed",false);
            }
        });
        menu.getItems().add(item);
        all_check_menu_items.add(item);

    }

    //**********************************************************
    public static CheckMenuItem create_menu_item_for_show_details(Virtual_landscape local_virtual_landscape, Kontext context)
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Show_Details",context);
        text += " ("+local_virtual_landscape.show_details.getDisplayText()+")";
        CheckMenuItem item = new CheckMenuItem(text );
        Look_and_feel_manager.set_menu_item_look(item, context.logger());
        item.setSelected(Feature_cache.get(Feature.Show_single_column_with_details));
        item.setOnAction(actionEvent -> {
            CheckMenuItem local = (CheckMenuItem) actionEvent.getSource();
            Feature_cache.update_cached_boolean(Feature.Show_single_column_with_details, local.isSelected(), context);
            local_virtual_landscape.redraw_fx(true,"Show_single_column_with_details",true);
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
            txt = My_I18n.get_I18n_string("Icon_Size",context) + " = " +target_size;
        }
        CheckMenuItem item = new CheckMenuItem(txt);
        Look_and_feel_manager.set_menu_item_look(item, context.logger());
        int actual_size = Non_booleans_properties.get_icon_size();
        item.setSelected(actual_size == target_size);
        item.setOnAction(actionEvent -> {
            CheckMenuItem local = (CheckMenuItem) actionEvent.getSource();
            if (local.isSelected()) {
                for ( CheckMenuItem cmi : all_check_menu_items)
                {
                    if ( cmi != local) cmi.setSelected(false);
                }
                Non_booleans_properties.set_icon_size(target_size);
                context.log("icon size changed to "+target_size);
                virtual_landscape.redraw_fx(true,"icon size changed",true);
            }
        });
        menu.getItems().add(item);
        all_check_menu_items.add(item);
    }


    //**********************************************************
    public void create_menu_item_for_one_folder_icon_size(Menu menu, int target_size, List<CheckMenuItem> all_check_menu_items)
    //**********************************************************
    {
        CheckMenuItem item = new CheckMenuItem(My_I18n.get_I18n_string("Folder_Icon_Size",context) + " = " +target_size);
        Look_and_feel_manager.set_menu_item_look(item, context.logger());
        int actual_size = Non_booleans_properties.get_folder_icon_size();
        item.setSelected(actual_size == target_size);
        item.setOnAction(actionEvent -> {
            CheckMenuItem local = (CheckMenuItem) actionEvent.getSource();
            if (local.isSelected()) {
                for ( CheckMenuItem cmi : all_check_menu_items)
                {
                    if ( cmi != local) cmi.setSelected(false);
                }
                Non_booleans_properties.set_folder_icon_size(target_size);
                virtual_landscape.redraw_fx(true,"folder icon length changed",true);
            }
        });
        menu.getItems().add(item);
        all_check_menu_items.add(item);
    }

    //**********************************************************
    public void create_menu_item_for_one_font_size( Menu menu, int target_size, List<CheckMenuItem> all_check_menu_items)
    //**********************************************************
    {
        CheckMenuItem item = new CheckMenuItem(My_I18n.get_I18n_string("Font_size",context) + " = " +target_size);
        Look_and_feel_manager.set_menu_item_look(item, context.logger());
        double actual_size = Non_booleans_properties.get_font_size();
        item.setSelected(actual_size == target_size);
        item.setOnAction(actionEvent -> {
            CheckMenuItem local = (CheckMenuItem) actionEvent.getSource();
            if (local.isSelected())
            {
                for ( CheckMenuItem cmi : all_check_menu_items)
                {
                    if (cmi != local) cmi.setSelected(false);
                }
                Non_booleans_properties.set_font_size(target_size);
                virtual_landscape.redraw_fx(true,"font size changed",false);
            }
        });
        menu.getItems().add(item);
        all_check_menu_items.add(item);
    }


    //**********************************************************
    public static Menu make_column_width_menu(Virtual_landscape local_virtual_landscape, Kontext context)
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string(String_constants.COLUMN_WIDTH,context);
        Menu menu = new Menu(text);
        Look_and_feel_manager.set_menu_item_look(menu,context.logger());
        List<CheckMenuItem> all_check_menu_items = new ArrayList<>();

        int[] possible_lengths ={Virtual_landscape.MIN_COLUMN_WIDTH,400,500,600,800,1000,2000,4000};
        for ( int l : possible_lengths)
        {
            create_menu_item_for_one_column_width(menu, l, all_check_menu_items,local_virtual_landscape,context);
        }
        return menu;
    }
    //**********************************************************
    public Menu make_file_sort_method_menu()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("File_Sorting_Method",context);
        Menu menu = new Menu(text);
        Look_and_feel_manager.set_menu_item_look(menu,context.logger());
        List<CheckMenuItem> all_check_menu_items = new ArrayList<>();
        for ( Sort_files_by sort_by : Sort_files_by.values())
        {
            /*
            if (( sort_by == Sort_files_by.SIMILARITY_BY_PAIRS)||(sort_by == Sort_files_by.SIMILARITY_BY_PURSUIT))
            {
                if ( !Feature_cache.get(Feature.Enable_image_similarity.name()))
                {
                    continue;
                }
            }*/
            create_menu_item_for_one_file_sort_method(menu, sort_by, all_check_menu_items);
        }
        return menu;
    }

    //**********************************************************
    public void create_menu_item_for_one_file_sort_method(Menu menu, Sort_files_by sort_by, List<CheckMenuItem> all_check_menu_items)
    //**********************************************************
    {
        String key = sort_by.name();
        CheckMenuItem item = new CheckMenuItem(My_I18n.get_I18n_string(key,context));
        Look_and_feel_manager.set_menu_item_look(item, context.logger());
        Sort_files_by actual = Sort_files_by.get_sort_files_by(virtual_landscape.path_list_provider.get_key(),context);
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
                    context.log("new file/image sorting order= "+sort_by);
                    Sort_files_by.set_sort_files_by_for_folder(virtual_landscape.path_list_provider.get_key(),sort_by, true, context);
                    Window_builder.replace_same_folder(
                            virtual_landscape.application,
                            virtual_landscape.shutdown_target,
                            virtual_landscape.window_type,
                            virtual_landscape.path_list_provider,
                            virtual_landscape.path_list_provider.get_key(),
                            virtual_landscape.get_top_left(),
                            context);
                }
            }
        });
        Button explanation = Items_with_explanation.make_explanation_button(key, context);
        item.setGraphic(explanation);
        menu.getItems().add(item);
        all_check_menu_items.add(item);

    }



    //**********************************************************
    public Menu make_icon_size_menu()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Icon_Size",context);
        Menu menu = new Menu(text);
        Look_and_feel_manager.set_menu_item_look(menu,context.logger());

        {
            MenuItem plus = new MenuItem("+ 10%");
            Look_and_feel_manager.set_menu_item_look(plus, context.logger());
            menu.getItems().add(plus);
            plus.setOnAction(actionEvent -> virtual_landscape.increase_icon_size());
        }
        {
            MenuItem minus = new MenuItem("- 10%");
            Look_and_feel_manager.set_menu_item_look(minus, context.logger());
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
            //compute icon length for N icons in a row
            double W = context.getWidth()- virtual_landscape.slider_width;
            int[] possible_dividers = {3,4,5,10};
            for ( int divider : possible_dividers)
            {
                int size = (int) (W/divider);
                icon_sizes.add(new Icon_size(size, true, divider));
            }
        }
        int current_icon_size = Non_booleans_properties.get_icon_size();
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
        String text = My_I18n.get_I18n_string("Folder_Icon_Size",context);
        Menu menu = new Menu(text);
        Look_and_feel_manager.set_menu_item_look(menu,context.logger());

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
        String text = My_I18n.get_I18n_string("Font_size",context);

        Menu menu = new Menu(text);
        Look_and_feel_manager.set_menu_item_look(menu,context.logger());

        List<Integer> possible_sizes = new ArrayList<>();
        possible_sizes.add(Non_booleans_properties.get_font_size());

        int[] possible_font_sizes = {8,10,12,14,16,18,20,22,24,26,28,30,32};
        for (int candidateSize : possible_font_sizes) {
            if (possible_sizes.contains(candidateSize)) continue;
            possible_sizes.add(candidateSize);
        }
        Collections.sort(possible_sizes);
        List<CheckMenuItem> all_check_menu_items = new ArrayList<>();
        for ( int i : possible_sizes)
        {
            create_menu_item_for_one_font_size( menu, i, all_check_menu_items);
        }
        return menu;
    }


    //**********************************************************
    void clean_up_names_fx()
    //**********************************************************
    {
        if ( !Popups.popup_ask_for_confirmation( Logger.warning+" EXPERIMENTAL! Are you sure?","Name cleaning will try to change all names in this folder, which may have very nasty consequences in a home or system folder",context)) return;

        Optional<Path> dir = virtual_landscape.path_list_provider.get_folder_path();
        if ( dir.isEmpty())
        {
            context.log_with_stack_trace((""));
            return;
        }
        File[] files = dir.get().toFile().listFiles();
        if (files == null) return;
        List<Old_and_new_Path> l = new ArrayList<>();
        for (File f : files)
        {

            Path old_path = f.toPath();

            String old_name = old_path.getFileName().toString();

            boolean check_extension = !f.isDirectory();
            String new_name = Name_cleaner.clean(old_name,check_extension, context.logger());
            if (new_name.equals(old_name))
            {
                context.log("skipping " + old_name + " as it is conformant");
                continue;
            }
            context.log("processing "+old_name+" as it is NOT conformant, will try: "+new_name);
            Path new_path = Paths.get(dir.toString(),new_name);
            Old_and_new_Path oandn = new Old_and_new_Path(old_path, new_path, Command.command_rename, Status.before_command,false);
            l.add(oandn);
        }
        Moving_files.perform_safe_moves_in_a_thread(l, true, context);

    }


    //**********************************************************
    void remove_corrupted_images_fx()
    //**********************************************************
    {
        Optional<Path> dir = virtual_landscape.path_list_provider.get_folder_path();
        if ( dir.isEmpty())
        {
            context.log_with_stack_trace((""));
            return;
        }
        Files_and_folders faf = virtual_landscape.path_list_provider.files_and_folders(true, null, true, true, context.aborter());
        List<Path> to_be_deleted =  new ArrayList<>();
        for (Path folder : faf.folders()) {

            if (folder.getFileName().toString().startsWith("._")) {
                // this is 'debatable' since it removes MacOs extended attributes in extFat
                // but is suoer useful e.g when one reloads files from a backup USB drive
                context.log("file name starts with ._, removed " + folder.getFileName());
                to_be_deleted.add(folder);
            }
        }
        for (Path file : faf.folders())
        {
            if ( !Guess_file_type.is_this_path_extension_an_image(file,context)) continue;
            Exif_metadata_extractor e = new Exif_metadata_extractor(file,context);
            e.get_exif_metadata(0, true,context.aborter(), false);
            if( !e.is_image_damaged()) continue;
            to_be_deleted.add(file);
        }
        if (to_be_deleted.isEmpty())
        {
            context.log("no corrupted images found");
            return;
        }

        virtual_landscape.path_list_provider.delete_multiple(to_be_deleted,context);

    }




    //**********************************************************
    public Menu make_backup_menu()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Backup",context);
        Menu menu = new Menu(text);
        Look_and_feel_manager.set_menu_item_look(menu,context.logger());

        menu.getItems().add(Menu_items.make_menu_item("Set_as_backup_source_folder",true,null, event -> virtual_landscape.you_are_backup_source(),context));
        menu.getItems().add(Menu_items.make_menu_item("Set_as_backup_destination_folder",true,null, event -> virtual_landscape.you_are_backup_destination(),context));
        menu.getItems().add(Menu_items.make_menu_item("Start_backup",true,null, event -> virtual_landscape.start_backup(),context));
        menu.getItems().add(Menu_items.make_menu_item("Abort_backup",true,null, event -> virtual_landscape.abort_backup(),context));
        menu.getItems().add(Menu_items.make_menu_item("Backup_help",true,null, event -> show_backup_help(context.logger()),context));
        return menu;
    }
    //**********************************************************
    public Menu make_import_menu()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Import",context);
        Menu menu = new Menu(text);
        menu.getItems().add(Menu_items.make_menu_item("Estimate_Size_Of_Import_Apple_Photos",true,null, event -> virtual_landscape.estimate_size_of_importing_apple_Photos(),context));
        menu.getItems().add(Menu_items.make_menu_item("Import_Apple_Photos",true,null, event -> virtual_landscape.import_apple_Photos(),context));
        return menu;
    }
    //**********************************************************
    public Menu make_fusk_menu()
    //**********************************************************
    {
        String text = "Fusk (experimental!)"; //My_I18n.get_I18n_string("Backup",logger);
        Menu menu = new Menu(text);
        menu.getItems().add(Menu_items.make_menu_item("Enter fusk pin code",false,null, event -> virtual_landscape.enter_fusk_pin_code(),context));
        menu.getItems().add(Menu_items.make_menu_item("Set this folder as fusk source",false,null, event -> virtual_landscape.you_are_fusk_source(),context));
        menu.getItems().add(Menu_items.make_menu_item("Set this folder as fusk destination",false,null, event -> virtual_landscape.you_are_fusk_destination(),context));
        menu.getItems().add(Menu_items.make_menu_item("Start fusk (experimental!)",false,null, event -> virtual_landscape.start_fusk(),context));
        menu.getItems().add( Menu_items.make_menu_item("Abort fusk",false,null, event -> virtual_landscape.abort_fusk(),context));
        menu.getItems().add(Menu_items.make_menu_item("Start defusk (experimental!)",false,null, event -> virtual_landscape.start_defusk(),context));
        menu.getItems().add(Menu_items.make_menu_item("Fusk help",false,null, event -> show_fusk_help(),context));
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
        Info_stage.show_info_stage("Help on backup",l, null,context);
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
        Info_stage.show_info_stage("Help on fusk",l, null,context);
    }



}
