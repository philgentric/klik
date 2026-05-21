# Javalin History & Undo Viewer - Test Application

## Overview
This is a JavaFX test application for the `Javalin_for_history_and_undo` class. It demonstrates how to use the javalin-based history and undo viewer with sample data.

## Files Created

### Main Test App
- **`Javalin_history_and_undo_app.java`** - JavaFX application with test buttons

### Helper Files
- **`Javalin_for_history_and_undo.java`** - Main class for managing javalin server
- **`index.html`** - Single-page UI for browser display
- **`STRATEGY_OPTIONS.md`** - Documents both implementation options

## Running the Test App

### Option 1: Direct from IDE
1. Open `Javalin_history_and_undo_app.java` in your IDE
2. Right-click and run as JavaFX application
3. Click test buttons in the app window

### Option 2: From Command Line
```bash
cd klikr
gradle run --args="klikr.javalin.Javalin_history_and_undo_app"
```

### Option 3: With Custom Args
```bash
gradle run --args="klikr.javalin.Javalin_history_and_undo_app"
```

## Test Features

### Test 1: History Viewer
**Action**: Click "🚀 Show History"
**What happens**:
- Browser opens at `http://localhost:PORT/index.html`
- Displays 50 sample folder paths
- Each path can be clicked
- Sidebar shows "History (50)"

**Sample Data**:
```
/Users/test/documents/work/project_0/file_0.txt
/Users/test/documents/work/project_1/file_1.txt
...
/Users/test/documents/work/project_49/file_49.txt
```

### Test 2: Undo History Viewer
**Action**: Click "↩️ Show Undo History"
**What happens**:
- Browser opens with undo operations
- Displays 30 sample undo items
- Shows original path → destination path
- Indicates filename changes
- Sidebar shows "Undo History (30)"

**Sample Data**:
```
[Undo #0] file_0.txt → file_0_moved_0.txt
[Undo #1] file_1.txt → file_1_moved_1.txt
...
[Undo #29] file_29.txt → file_29_moved_29.txt
```

## Browser Features

### UI Components
- **Header**: Title and status indicator
- **Sidebar**: Type toggle (History/Undo counts)
- **Main Content**: Scrollable list of items
- **List Item**: Badge, timestamp, path, details

### Interactions
1. **Scroll**: Smooth scrolling through list
2. **Click**: Trigger action in JavaFX app (logged to TextArea)
3. **Switch View**: Toggle between History/Undo in sidebar
4. **Right-click**: Context menu (browser native)

### WebSocket Communication
- Client → Server: `REQUEST_INIT`, `TYPE_CHANGED`, `CLICK:<id>`
- Server → Client: JSON array of items

## Technical Details

### Port Assignment
- Test app finds first available port automatically
- Server runs in background (singleton pattern)
- Same port reused for all test runs

### Thread Safety
- All WebSocket messages handled on JavaFX Application Thread
- Thread-safe singleton for javalin instance
- CountDownLatch for server startup synchronization

### Error Handling
- Invalid path conversions logged to logger
- WebSocket disconnection handled gracefully
- Graceful shutdown on test app close

## Integration

### Using in Your Code

```java

import javafx.application.Application;

// In your JavaFX application
Application myApp = this;

        // For history
        List<Path> paths = new ArrayList<>();
paths.

        add(Paths.get("/path/to/file.txt"));
        Javalin_for_history_and_undo.

        show_history(
                myApp,
                paths,
    (Path p) ->{
        // Handle click
        System.out.

        println("Clicked: "+p);
    },
        logger
);

        // For undo
        List<Undo_item> undoItems = new ArrayList<>();
// populate undoItems
Javalin_for_history_and_undo.

        show_undo(
                myApp,
                undoItems,
    (Undo_item ui) ->{
        // Handle click
        System.out.

        println("Clicked undo: "+ui.signature());
        },
        logger
);
```

## Testing Checklist

- [x] History viewer displays correctly
- [x] Undo viewer displays correctly
- [x] Click handlers work
- [x] Switching views works
- [x] Scrolling smooth
- [x] Browser stays open after test app closes
- [x] Multiple test runs reuse same port
- [x] Error handling works
- [x] Thread safety maintained

## Troubleshooting

### Browser doesn't open
- Check firewall settings
- Verify port is available
- Check logger output

### WebSocket connection fails
- Ensure browser allows localhost connections
- Check javalin server is running
- Verify port matches between server and browser

### Memory issues with large lists
- Browser DOM handles rendering automatically
- 100k+ items should be fine
- Close browser tab to stop WebSocket

## Related Files

- `Javalin_for_history_and_undo.java` - Main server class
- `Javalin_monaco_app.java` - Similar test app for Monaco editor
- `index.html` - Browser UI
- `STRATEGY_OPTIONS.md` - Implementation strategy documentation