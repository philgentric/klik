# Javalin History & Undo Viewer - Strategy Options

## Problem
Klikr needs to display large lists of history paths and undo records. Current JavaFX SceneGraph-based implementations don't scale for 1000+ items.

## Solution Options

### Option 1: Javalin-based Browser Gallery (RECOMMENDED - Current Implementation)
**Approach**: Serve HTML/JS pages in browser via Javalin server with WebSocket communication.

**Pros**:
- ✅ Scales to 100k+ items (browser DOM handles rendering)
- ✅ Simple implementation using existing javalin infrastructure
- ✅ Consistent with existing javalin modules (Monaco, list view)
- ✅ Better UX for text-heavy data (search, filtering, right-click menus)
- ✅ Separate concerns (UI in HTML/JS, logic in Java)
- ✅ Single HTML template for both history and undo

**Cons**:
- ❌ Requires browser display (may not fit all use cases)
- ❌ Slightly more complex architecture

**Implementation**:
- `Javalin_for_history_and_undo.java` - Server management
- `index.html` - Single-page UI (sidebar + scrollable list)
- WebSocket `/history-undo-ws` for communication

**Usage**:
```java
// For history (folder paths)
Javalin_for_history_and_undo.show_history(
    application,
    List<Path> paths,
    Consumer<Path> on_click,
    logger
);

// For undo (move operations)
Javalin_for_history_and_undo.show_undo(
    application,
    List<Undo_item> undo_items,
    Consumer<Undo_item> on_click,
    logger
);
```

**WebSocket Protocol**:
- Client → Server: `REQUEST_INIT`, `TYPE_CHANGED:<history|undo>`, `CLICK:<id>`
- Server → Client: JSON array `[{"id":"0","text":"/path","timestamp":"...","type":"history","details":null},...]`

---

### Option 2: Virtual-Landscape Multiline Item
**Approach**: Extend `Item` class to create multiline button in Virtual-Landscape view.

**When to use**:
- History: simple folder path strings (single line)
- Undo: need to show "original path → destination path" with filename changes

**Design**:
- Create new item type (e.g., `Item_undo_move`) extending `Item`
- Button contains multiline `Label` or `TextArea`:
  - Line 1: destination path (or original path for undo)
  - Line 2: original path → destination path (for undo)
  - Optional: filename change indicator
- Override `get_Height()` to return text-measured height
- Keep Virtual-Landscape integration unchanged

**Pros**:
- ✅ Keeps everything in JavaFX
- ✅ No browser dependency
- ✅ Reuses existing Virtual-Landscape infrastructure

**Cons**:
- ❌ Doesn't scale well for 1000+ items (SceneGraph limitation)
- ❌ Complex multiline button implementation
- ❌ More JavaFX-specific code
- ❌ Harder to add advanced UI features (search, filtering)

**Example Data Structure for Undo**:
```
Item content:
┌─────────────────────────────────────────────┐
│ /Users/user/documents/work/project          │
│ /Users/user/downloads/old → /a/b/new.txt    │
└─────────────────────────────────────────────┘
```

---

## Recommendation

**Use Option 1 (Javalin-based)** for most cases because:
- Proven scalability for large text-heavy lists
- Better UX with built-in scrolling, search, and filtering
- Consistent with existing javalin architecture
- Simpler to maintain (single HTML file for both use cases)

**Use Option 2 (Multiline Item)** only if:
- You need a JavaFX-only solution
- Lists are expected to stay small (< 500 items)
- You want to avoid browser display

## Current Status

✅ **Option 1 is implemented** and ready to use.
❌ Option 2 is not yet implemented.