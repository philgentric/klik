package klik.change;

public class After_move_handler_message
{
    After_move_handler originator;
    After_move_handler_message_type type;

    public After_move_handler_message(After_move_handler amh, After_move_handler_message_type type_)
    {
        originator = amh;
        type = type_;
    }
}
