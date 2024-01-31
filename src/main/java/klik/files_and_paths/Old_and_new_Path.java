package klik.files_and_paths;


import java.nio.file.Path;

//**********************************************************
public 	class Old_and_new_Path
//**********************************************************
{
	private static final boolean dbg = false;
	public final Path old_Path;
	public final Path new_Path;
	public final Command_old_and_new_Path cmd;
	public final Status_old_and_new_Path status;
    public Runnable run_after;
	public final boolean is_a_restore;


    //**********************************************************
	public Old_and_new_Path(Path old_Path_, Path new_Path_,
							//boolean is_move_to_trash_,
							Command_old_and_new_Path cmd_, Status_old_and_new_Path status_, boolean is_a_restore_)
	//**********************************************************
	{
		old_Path = old_Path_;
		new_Path = new_Path_;
		is_a_restore = is_a_restore_;
		//is_move_to_trash = is_move_to_trash_;
		status = status_;
		cmd = cmd_;
		if ( dbg)
		{
			if ( old_Path_ == null)
			{
				System.out.println("old Path is null");
			}
			else
			{
				System.out.println("old_Path="+old_Path_);
			}
			if ( new_Path_ == null)
			{
				System.out.println("new Path is null");
			}
			else
			{
				System.out.println("new_Path="+new_Path_);
			}			
		}

	}
	/*
	// the new and old Path are in the same dir: this is a rename 
	//**********************************************************
	public boolean is_a_rename()
	//**********************************************************
	{
		return old_Path.getParent().toAbsolutePath().equals(
				new_Path.getParent().toAbsolutePath());
	}*/
	public Old_and_new_Path reverse_for_restore()
	{
		return new Old_and_new_Path(this.new_Path, this.old_Path, this.cmd, Status_old_and_new_Path.before_command, true);
	}
	public Path get_new_Path() 
	{
		return new_Path;
	}
	public Path get_old_Path()
	{
		return old_Path;
	}
	public Command_old_and_new_Path get_cmd()
	{
		return cmd;
	}
	
	public String get_string()
	{
		return "old Path="+old_Path+" new Path="+new_Path+" cmd="+cmd+" status="+status+"\n";
	}


}