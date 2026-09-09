/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.properties;

import io.nut.base.util.As;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PropertiesSettings}.
 *
 * <p>
 * Naming convention: {@code methodUnderTest_scenario_expectedOutcome}.
 */
public class PropertiesSettingsTest
{
    @TempDir
    File tmpDir;

    private File file(String name)
    {
        return new File(tmpDir, name);
    }

    @Test
    public void load_missingFile_throwsFileNotFoundException()
    {
        PropertiesSettings settings = new PropertiesSettings(file("missing.properties"), false);
        assertThrows(FileNotFoundException.class, settings::load);
        assertFalse(settings.isLoaded());
    }

    @Test
    public void saveAndLoad_roundTrip_restoresProperties() throws Exception
    {
        File f = file("roundtrip.properties");

        PropertiesSettings writer = new PropertiesSettings(f, false);
        writer.setProperty("foo", "bar");
        writer.setProperty("num", "42");
        assertTrue(writer.isModified());
        writer.save("my comments");
        assertFalse(writer.isModified());
        assertTrue(f.exists());

        PropertiesSettings reader = new PropertiesSettings(f, false);
        assertFalse(reader.isLoaded());
        reader.load();
        assertTrue(reader.isLoaded());
        assertEquals("bar", reader.getProperty("foo"));
        assertEquals("42", reader.getProperty("num"));
        assertEquals("dflt", reader.getProperty("unknown", "dflt"));
        assertNull(reader.getProperty("unknown"));
    }

    @Test
    public void save_withComments_writesCommentHeader() throws Exception
    {
        File f = file("comments.properties");

        PropertiesSettings settings = new PropertiesSettings(f, false);
        settings.setProperty("k", "v");
        settings.save("hello-settings");

        String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        assertTrue(content.contains("#hello-settings"), content);
        assertTrue(content.contains("k=v") || content.contains("k=v\r"), content);
    }

    @Test
    public void setProperty_marksModified_saveResets() throws Exception
    {
        File f = file("modified.properties");

        PropertiesSettings settings = new PropertiesSettings(f, false);
        assertFalse(settings.isModified());

        settings.setProperty("k", "v");
        assertTrue(settings.isModified());

        settings.save("c");
        assertFalse(settings.isModified());
    }

    @Test
    public void clear_marksModifiedAndRemovesKeys() throws Exception
    {
        File f = file("clear.properties");

        PropertiesSettings settings = new PropertiesSettings(f, false);
        settings.setProperty("k", "v");
        settings.clear();
        assertTrue(settings.isModified());
        assertFalse(settings.containsKey("k"));
    }

    @Test
    public void autoset_getExistingKey_returnsValueWithoutAdding() throws Exception
    {
        File f = file("autoset.properties");

        PropertiesSettings settings = new PropertiesSettings(f, true);
        settings.setProperty("k", "v");
        settings.save("c");

        PropertiesSettings autoset = new PropertiesSettings(f, true);
        autoset.load();
        assertEquals("v", autoset.getProperty("k"));
        assertFalse(autoset.isModified());
    }

    @Test
    public void autoset_getMissingKey_setsEmptyValue() throws Exception
    {
        File f = file("autoset.properties");
        seed(f, "seed", "1");

        PropertiesSettings settings = new PropertiesSettings(f, true);
        settings.load();
        assertEquals("", settings.getProperty("missing"));
        assertTrue(settings.isModified());
        assertTrue(settings.containsKey("missing"));

        PropertiesSettings noAutoset = new PropertiesSettings(f, false);
        noAutoset.load();
        assertNull(noAutoset.getProperty("missing"));
        assertFalse(noAutoset.containsKey("missing"));
    }

    @Test
    public void autoset_getMissingKeyWithDefault_setsAndReturnsDefault() throws Exception
    {
        File f = file("autoset.properties");
        seed(f, "seed", "1");

        PropertiesSettings settings = new PropertiesSettings(f, true);
        settings.load();
        assertEquals("dflt", settings.getProperty("missing", "dflt"));
        assertTrue(settings.isModified());
        assertEquals("dflt", settings.getProperty("missing", "other"));

        PropertiesSettings noAutoset = new PropertiesSettings(f, false);
        noAutoset.load();
        assertEquals("dflt", noAutoset.getProperty("missing", "dflt"));
        assertFalse(noAutoset.containsKey("missing"));
        assertFalse(noAutoset.isModified());
    }

    @Test
    public void stringPropertyNames_returnsAllNames() throws Exception
    {
        PropertiesSettings settings = new PropertiesSettings(file("names.properties"), true);
        settings.setProperty("a", "1");
        settings.setProperty("b", "2");
        settings.setProperty("c", "3");

        assertEquals(3, settings.stringPropertyNames().size());
        assertTrue(settings.stringPropertyNames().containsAll(As.list("a", "b", "c")));
    }

    @Test
    public void sort_afterSave_ordersLinesAlphabetically() throws Exception
    {
        File f = file("sort.properties");

        PropertiesSettings settings = new PropertiesSettings(f, false);
        settings.setProperty("zz", "3");
        settings.setProperty("aa", "1");
        settings.setProperty("mm", "2");
        settings.save("unsorted");
        settings.sort();

        List<String> lines = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
        int indexAa = indexOfKey(lines, "aa");
        int indexMm = indexOfKey(lines, "mm");
        int indexZz = indexOfKey(lines, "zz");
        assertTrue(indexAa >= 0 && indexMm >= 0 && indexZz >= 0, lines.toString());
        assertTrue(indexAa < indexMm && indexMm < indexZz, lines.toString());
    }

    @Test
    public void exists_reflectsFileSystemState()
    {
        File f = file("nope.properties");
        PropertiesSettings settings = new PropertiesSettings(f, false);
        assertFalse(settings.exists());
    }

    private static void seed(File f, String key, String value) throws Exception
    {
        PropertiesSettings writer = new PropertiesSettings(f, false);
        writer.setProperty(key, value);
        writer.save("seed");
    }

    private static int indexOfKey(List<String> lines, String key)
    {
        for (int i = 0; i < lines.size(); i++)
        {
            if (lines.get(i).startsWith(key + "="))
            {
                return i;
            }
        }
        return -1;
    }
}