# Boolean Preferences Web UI

This package implements a web-based UI for managing Klikr's boolean preferences using Javalin and JavaScript.

## Overview

Replaces the JavaFX ListView approach with a modern web interface that provides:
- **Checkboxes** for clear visual indication of current state
- **Grouped by topics** for better organization
- **Search functionality** (native to web pages) for quick filtering
- **Real-time updates** via WebSocket synchronization
- **i18n support** using the existing `My_I18n` system

## Components

### Backend (Java)

**`Javalin_boolean_preferences.java`**
- Main server class that manages the web UI
- Provides WebSocket endpoint (`/preferences-ws`) for real-time communication
- Automatically categorizes features into logical groups:
  - UI Display
  - File & Folder Display
  - Image Features
  - Processing & Advanced
  - Text Editing
  - Audio & Video
  - Installation & Warnings
  - Logging & Debug
  - Security & Obfuscation
  - Other

**Key Methods:**
- `show(Application, Logger)` - Opens the preferences UI in the default browser
- `get_all_preferences()` - Retrieves all features with i18n labels and current values
- `categorize_feature(Feature)` - Groups features into categories
- `broadcast_update(String, boolean)` - Syncs changes to all connected clients

### Frontend (HTML/JS)

**`index.html`**
- Modern, responsive web interface
- Features:
  - Live search/filter box in the header
  - Collapsible category sections
  - Checkbox controls for each preference
  - Explanatory text under each preference
  - Auto-reconnect on WebSocket disconnection
  - Visual feedback for connection status

### WebSocket Protocol

**Client → Server:**
- `REQUEST_INIT` - Request all preferences
- `TOGGLE:<feature_name>` - Toggle a specific feature

**Server → Client:**
- `[{...}, {...}]` - Array of preference items (initial load)
- `{"type":"UPDATE","id":"<feature_name>","value":true/false}` - Single preference update

## Usage

### From Your Code

```java
import klikr.javalin.boolean_preferences.Javalin_boolean_preferences;

// Open the preferences UI
Javalin_boolean_preferences.show(application, logger);
```

### Testing

Run the test application:
```bash
# From the project root
./gradlew run --args="klikr.javalin.boolean_preferences.Test_boolean_preferences"
```

Or run `Test_boolean_preferences.main()` from your IDE.

## Integration with Existing Code

The implementation reuses existing infrastructure:

1. **Feature Management:** Uses `Feature` enum and `Feature_cache` for storage
2. **i18n:** Uses `My_I18n.get_I18n_string()` for translations
3. **Server Setup:** Follows the pattern from `Javalin_for_history_and_undo`
4. **Browser Opening:** Uses `Javalin_common.open_browser()`

## Feature Categories

Features are automatically categorized based on naming patterns:

| Category | Pattern | Examples |
|----------|---------|----------|
| UI Display | `Show_*`, `Hide_*`, `Display_*` | Show_icons_for_files |
| File & Folder Display | Contains `_files`, `_folders` | Show_hidden_folders |
| Image Features | Contains `image`, `Image` | Enable_image_similarity |
| Processing & Advanced | `Enable_*` (processing) | Enable_face_recognition |
| Text Editing | Contains `monaco`, `browser`, `text` | Use_monaco_for_text_edition |
| Audio & Video | `Play_*` | Play_music |
| Installation & Warnings | Contains `install_warning` | Show_ffmpeg_install_warning |
| Logging & Debug | `Log_*`, contains `debug` | Log_to_file |
| Security & Obfuscation | Contains `Fusk`, `fusk` | Enable_fusk |
| Other | Everything else | - |

## Adding New Features

1. Add the feature to `Feature.java` enum
2. Add translations to `MessagesBundle_*.properties` files:
   - `Feature_name=Display Label`
   - `Feature_name_Explanation=Detailed explanation`
3. The UI will automatically pick it up and categorize it

## Advantages Over JavaFX ListView

1. **Better UX:** Checkboxes show current state at a glance
2. **Search:** Native web search built-in
3. **Grouping:** Features organized by topic
4. **Responsive:** Works on any screen size
5. **Extensible:** Easy to add filtering, sorting, bulk operations
6. **No JavaFX complexity:** Simpler UI code
