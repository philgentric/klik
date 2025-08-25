#import <Cocoa/Cocoa.h>
#import <jni.h>

static NSApplication *app(void) {
    return [NSApplication sharedApplication];
}


JNIEXPORT void JNICALL Java_klik_look_Macdock_setup(
JNIEnv *env, jclass clazz, jstring label, jbyteArray icon_data)
{
    @autoreleasepool {
    if ( !label) return;
    if ( !icon_data ) return;

    const jchar* chars = (*env)->GetStringChars(env, label, NULL);
    jsize my_length = (*env)->GetStringLength(env, label);
    NSString *badge = [[NSString alloc] initWithCharacters:(const unichar*)chars length:my_length];
    (*env)->ReleaseStringChars(env, label, chars);

    NSDockTile *dockTile = [app() dockTile];
    [dockTile setBadgeLabel:badge];




    jsize my_length2 = (*env)->GetArrayLength(env, icon_data);
    if ( my_length2 <= 0 ) return;
    jbyte* icon_bytes = (*env)->GetByteArrayElements(env, icon_data, NULL);
    NSData* ns_data = [NSData dataWithBytes:icon_bytes length:(NSUInteger)my_length2];
    NSImage* ns_image = [[NSImage alloc] initWithData:ns_data];

    (*env)->ReleaseByteArrayElements(env, icon_data, icon_bytes, JNI_ABORT);

    if ( !ns_image ) return;
    [app() setApplicationIconImage:ns_image];

    [dockTile display];

    return;
    }
}

