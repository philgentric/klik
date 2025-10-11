package klik.machine_learning.feature_vector;

import javafx.stage.Window;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.util.execute.Execute_command;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.log.Logger_factory;
import klik.util.ui.Jfx_batch_injector;
import klik.util.ui.Popups;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

//**********************************************************
public interface Feature_vector_source
//**********************************************************
{
    Feature_vector get_feature_vector(Path path, Window owner, Logger logger);
}
