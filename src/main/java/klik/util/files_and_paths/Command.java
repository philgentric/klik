package klik.util.files_and_paths;

//**********************************************************
public enum Command
//**********************************************************
{
	command_move_to_trash, // goes to trash dir
    command_delete_forever, // actual delete : ONLY when clearing the trash
	command_move, // goes to different dir
	command_rename, // same dir, new name
	command_edit, // same name, different content
    command_copy,
	command_restore,
	command_unknown,
	command_remove_for_playlist
}
