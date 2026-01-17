package klikr.util;

import klikr.util.log.Logger;

//**********************************************************
public class Simple_json_parser
//**********************************************************
{
    // example of json content:
    // {"name": "MobileNet_embeddings_server",
    // "port": 54225,
    // "uuid": "072ff019-5884-44f9-8b40-fe4ea967d4f8"}

    //**********************************************************
    public static String read_key(String content, String key, Logger logger)
    //**********************************************************
    {
        if (content == null || content.isEmpty()) {
            logger.log("Error: Empty JSON content");
            return null;
        }

        try {
            // Find the key in the JSON
            int key_start = content.indexOf("\"" + key + "\"");
            if (key_start == -1) {
                //logger.log("Warning: Key '" + key + "' not found");
                return null;
            }

            // Find the value after the key
            int start = content.indexOf(":", key_start) + 1;
            if (start == -1) {
                logger.log("Error: Malformed JSON (missing colon)");
                return null;
            }

            // Skip whitespace
            while (start < content.length() &&
                    Character.isWhitespace(content.charAt(start))) {
                start++;
            }

            // Check if we're at the end of the string
            if (start >= content.length()) {
                logger.log("Error: Unexpected end of JSON after key '" + key + "'");
                return null;
            }

            // Check if value is quoted
            if (content.charAt(start) == '"')
            {
                start++; // Skip opening quote
                int end = content.indexOf("\"", start);
                if (end == -1) {
                    logger.log("Error: Unclosed quote for key '" + key + "'");
                    return null;
                }
                return content.substring(start, end);
            }
            else
            {
                if (is_naked(content, start))
                {
                    int valueEnd = content.indexOf(",", start);
                    if (valueEnd == -1)
                    {
                        valueEnd = content.indexOf("}", start);
                        if (valueEnd == -1) valueEnd = content.length();
                    }
                    return content.substring(start, valueEnd).trim();
                }
                else
                {
                    int valueEnd = find_matching_brace(content, start);
                    if (valueEnd == -1)
                    {
                        logger.log("Error: Unclosed object/array for key '" + key + "'");
                        return null;
                    }
                    return content.substring(start, valueEnd + 1).trim();
                }
            }
        } catch (Exception e) {
            logger.log("Error parsing JSON: " + e.getMessage());
            return null;
        }
    }

    //**********************************************************
    private static int find_matching_brace(String content, int start)
    //**********************************************************
    {
        int depth = 0;
        for (int i = start; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '{' || c == '[') depth++;
            if (c == '}' || c == ']') {
                if (--depth == 0) return i;
            }
        }
        return -1;
    }

    //**********************************************************
    private static boolean is_naked(String content, int start)
    //**********************************************************
    {
        if (start >= content.length()) return false;

        char firstChar = content.charAt(start);
        // Numbers (digits or minus)
        if (Character.isDigit(firstChar) || firstChar == '-') return true;
        // Booleans/null
        if (content.startsWith("true", start) ||
                content.startsWith("false", start) ||
                content.startsWith("null", start)) return true;
        return false;
    }
}
