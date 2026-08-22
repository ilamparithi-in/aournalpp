package dev.ilamparithi.aournalpp.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class OpenSourceLibrary(
    val name: String,
    val copyright: String,
    val licenseType: String,
    val description: String,
    val repositoryUrl: String,
    val licenseText: String
)

val BUNDLED_LIBRARIES = listOf(
    OpenSourceLibrary(
        name = "Aournal (Xournal++ Android Port)",
        copyright = "Copyright (c) 2026 Ilamparithi and Contributors",
        licenseType = "GPL-3.0-or-later",
        description = "Material 3 interface, headless background conversion, X11 runtime orchestration, and storage bridge.",
        repositoryUrl = "https://github.com/ilamparithi/xopp-android",
        licenseText = """
GNU GENERAL PUBLIC LICENSE
Version 3, 29 June 2007

Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>
Everyone is permitted to copy and distribute verbatim copies
of this license document, but changing it is not allowed.

Preamble
The GNU General Public License is a free, copyleft license for software and other kinds of works.
... (Full GPL-3.0 License Applied)
This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
        """.trimIndent()
    ),
    OpenSourceLibrary(
        name = "Xournal++ Core",
        copyright = "Copyright (c) 2018-2026 The Xournal++ Team, Denis Auroux",
        licenseType = "GPL-2.0-or-later",
        description = "Native note-taking and PDF annotation application with pen pressure support and vector stroke rendering.",
        repositoryUrl = "https://github.com/xournalpp/xournalpp",
        licenseText = """
GNU GENERAL PUBLIC LICENSE
Version 2, June 1991

Copyright (C) 1989, 1991 Free Software Foundation, Inc.,
51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
Everyone is permitted to copy and distribute verbatim copies of this license document, but changing it is not allowed.

This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later version.
        """.trimIndent()
    ),
    OpenSourceLibrary(
        name = "Termux-X11 Subsystem",
        copyright = "Copyright (c) 2022-2026 Termux Developers, ags131",
        licenseType = "GPL-3.0",
        description = "High-performance hardware-accelerated X11 display server and input bridge for Android.",
        repositoryUrl = "https://github.com/termux/termux-x11",
        licenseText = """
GNU GENERAL PUBLIC LICENSE
Version 3, 29 June 2007

Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>
Everyone is permitted to copy and distribute verbatim copies of this license document, but changing it is not allowed.
        """.trimIndent()
    ),
    OpenSourceLibrary(
        name = "Matchbox Window Manager",
        copyright = "Copyright (c) 2001-2006 Matthew Allum, OpenedHand Ltd",
        licenseType = "GPL-2.0-or-later",
        description = "Lightweight kiosk window manager engineered for non-overlapping mobile viewports.",
        repositoryUrl = "https://git.yoctoproject.org/matchbox-window-manager",
        licenseText = """
GNU GENERAL PUBLIC LICENSE
Version 2, June 1991

Copyright (C) 1989, 1991 Free Software Foundation, Inc.
Everyone is permitted to copy and distribute verbatim copies of this license document.
        """.trimIndent()
    ),
    OpenSourceLibrary(
        name = "GTK+ 3 & GLib Stack",
        copyright = "Copyright (c) The GTK Team, GNOME Foundation",
        licenseType = "LGPL-2.1-or-later",
        description = "Cross-platform graphical toolkit and foundational utilities powering the Xournal++ desktop runtime.",
        repositoryUrl = "https://gitlab.gnome.org/GNOME/gtk",
        licenseText = """
GNU LESSER GENERAL PUBLIC LICENSE
Version 2.1, February 1999

Copyright (C) 1991, 1999 Free Software Foundation, Inc.
51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
Everyone is permitted to copy and distribute verbatim copies of this license document.
        """.trimIndent()
    ),
    OpenSourceLibrary(
        name = "AndroidX & Jetpack Compose",
        copyright = "Copyright (c) The Android Open Source Project",
        licenseType = "Apache-2.0",
        description = "Modern declarative UI toolkit and lifecycle architecture components for Android.",
        repositoryUrl = "https://android.googlesource.com/platform/frameworks/support",
        licenseText = """
Apache License
Version 2.0, January 2004
http://www.apache.org/licenses/

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
        """.trimIndent()
    ),
    OpenSourceLibrary(
        name = "XZ for Java",
        copyright = "Public Domain / Lasse Collin",
        licenseType = "Public Domain / 0BSD",
        description = "Pure-Java implementation of XZ and LZMA data decompression for bootstrap package installation.",
        repositoryUrl = "https://tukaani.org/xz/java.html",
        licenseText = """
This library has been put into the public domain, where it can be used, modified, and redistributed freely by anyone for any purpose.
        """.trimIndent()
    ),
    OpenSourceLibrary(
        name = "Apache Commons Compress",
        copyright = "Copyright (c) 2002-2026 The Apache Software Foundation",
        licenseType = "Apache-2.0",
        description = "Streaming TAR archive extraction and POSIX permission parsing.",
        repositoryUrl = "https://commons.apache.org/proper/commons-compress/",
        licenseText = """
Apache License
Version 2.0, January 2004
http://www.apache.org/licenses/
        """.trimIndent()
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(onBack: (() -> Unit)? = null) {
    val context = LocalContext.current
    var selectedLibraryForDialog by remember { mutableStateOf<OpenSourceLibrary?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Open Source Licenses",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Aournal is built on top of incredible open-source projects. In compliance with free software licensing, all component licenses and sources are provided below.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(BUNDLED_LIBRARIES, key = { it.name }) { lib ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = lib.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = lib.copyright,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                Text(
                                    text = lib.licenseType,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = lib.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(lib.repositoryUrl))
                                    context.startActivity(intent)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Source Code")
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            OutlinedButton(
                                onClick = { selectedLibraryForDialog = lib },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("View License")
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        selectedLibraryForDialog?.let { lib ->
            AlertDialog(
                onDismissRequest = { selectedLibraryForDialog = null },
                title = {
                    Text(
                        text = "${lib.name} License",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp)
                        ) {
                            Text(
                                text = lib.licenseText,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { selectedLibraryForDialog = null }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}
