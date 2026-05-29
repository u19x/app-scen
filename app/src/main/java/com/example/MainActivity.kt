package com.example

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.request.SuccessResult
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.zIndex
import com.example.ui.MainViewModel
import com.example.ui.Translations
import com.example.ui.categories
import com.example.ui.TranslationPack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val langState by viewModel.lang.collectAsStateWithLifecycle()
            val layoutDirection = if (langState == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0C0C0E) // Premium deep cinematic grey
                ) {
                    AIRetouchProApp(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIRetouchProApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val lang by viewModel.lang.collectAsStateWithLifecycle()
    val t = Translations.get(lang)

    val history by viewModel.history.collectAsStateWithLifecycle()
    val historyIndex by viewModel.historyIndex.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val activeCategory by viewModel.activeCategory.collectAsStateWithLifecycle()
    val toast by viewModel.toast.collectAsStateWithLifecycle()

    val currentBitmap = if (historyIndex in history.indices) history[historyIndex] else null
    val canUndo = historyIndex > 0
    val canRedo = historyIndex < history.size - 1

    var isAddingSample by remember { mutableStateOf<String?>(null) }

    // Image Picker Launcher
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val bitmap = loadAndScaleBitmap(context, uri)
            if (bitmap != null) {
                viewModel.setInitialImage(bitmap)
            } else {
                viewModel.showToast("Failed to load image. Try another one.", "error")
            }
        }
    }

    // Camera Launcher
    val takePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            viewModel.setInitialImage(bitmap)
        }
    }

    // Toast logic
    LaunchedEffect(toast) {
        toast?.let {
            kotlinx.coroutines.delay(3500)
            viewModel.clearToast()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = t.appTitle,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            style = TextStyle(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFFEC4899), Color(0xFF8B5CF6))
                                )
                            )
                        )
                        Box(
                            modifier = Modifier
                                .border(1.dp, Color(0xFF374151), RoundedCornerShape(4.dp))
                                .clickable {
                                    viewModel.setLang(if (lang == "ar") "en" else "ar")
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (lang == "ar") "EN" else "عربي",
                                color = Color(0xFF9CA3AF),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("language_toggle_btn")
                            )
                        }
                    }
                },
                actions = {
                    // Undo
                    IconButton(
                        onClick = { viewModel.handleUndo() },
                        enabled = canUndo && !isProcessing,
                        modifier = Modifier.testTag("undo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Undo,
                            contentDescription = "Undo",
                            tint = if (canUndo) Color.White else Color(0xFF374151)
                        )
                    }

                    // Redo
                    IconButton(
                        onClick = { viewModel.handleRedo() },
                        enabled = canRedo && !isProcessing,
                        modifier = Modifier.testTag("redo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Redo,
                            contentDescription = "Redo",
                            tint = if (canRedo) Color.White else Color(0xFF374151)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Save
                    Button(
                        onClick = {
                            currentBitmap?.let { bitmap ->
                                val savedUri = saveBitmapToGallery(context, bitmap)
                                if (savedUri != null) {
                                    viewModel.showToast(t.saveSuccess, "success")
                                } else {
                                    viewModel.showToast("Failed to save image.", "error")
                                }
                            }
                        },
                        enabled = currentBitmap != null && !isProcessing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black,
                            disabledContainerColor = Color(0xFF1F2937),
                            disabledContentColor = Color(0xFF4B5563)
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("save_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = t.save, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF030303),
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            if (currentBitmap != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF030303))
                        .navigationBarsPadding()
                        .padding(bottom = 8.dp)
                ) {
                    // Sub-menu for active category
                    AnimatedVisibility(
                        visible = activeCategory != null,
                        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
                    ) {
                        activeCategory?.let { categoryId ->
                            val category = categories.firstOrNull { it.id == categoryId }
                            if (category != null) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF0F0F12))
                                        .padding(vertical = 12.dp, horizontal = 16.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.setActiveCategory(null) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowBack,
                                                contentDescription = "Back",
                                                tint = Color(0xFF9CA3AF),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (lang == "ar") category.titleAr else category.titleEn,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }

                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(category.options) { option ->
                                            val label = if (lang == "ar") option.labelAr else option.labelEn
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFF1E1E24), RoundedCornerShape(24.dp))
                                                    .border(1.dp, Color(0xFF374151), RoundedCornerShape(24.dp))
                                                    .clickable(enabled = !isProcessing) {
                                                        viewModel.applyAIFeature(
                                                            promptText = option.prompt,
                                                            processingMsg = t.processingToast,
                                                            successMsg = t.successToast,
                                                            errorMsg = t.errorToast,
                                                            serverErrorMsg = t.serverError
                                                        )
                                                    }
                                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                                                    .testTag("option_${option.id}")
                                            ) {
                                                Text(
                                                    text = label,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Main Categories Navigation Row
                    HorizontalDivider(color = Color(0xFF1F2937))
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        items(categories) { category ->
                            val isSelected = activeCategory == category.id
                            val label = if (lang == "ar") category.titleAr else category.titleEn

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .clickable {
                                        viewModel.setActiveCategory(
                                            if (isSelected) null else category.id
                                        )
                                    }
                                    .testTag("category_${category.id}"),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color(0xFFDB2777) else Color(0xFF1F2937))
                                        .padding(10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = category.icon,
                                        contentDescription = label,
                                        tint = if (isSelected) Color.White else Color(0xFF9CA3AF),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) Color(0xFFEC4899) else Color(0xFF9CA3AF)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF09090B)),
            contentAlignment = Alignment.Center
        ) {
            // Absolute Toast Alert Popup Overlay
            AnimatedVisibility(
                visible = toast != null,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .zIndex(10f)
            ) {
                toast?.let { toastModel ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF18181C)),
                        shape = RoundedCornerShape(32.dp),
                        elevation = CardDefaults.cardElevation(8.dp),
                        border = BorderStroke(
                            1.dp,
                            when (toastModel.type) {
                                "success" -> Color(0xFF10B981)
                                "error" -> Color(0xFFEF4444)
                                else -> Color(0xFF3B82F6)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = when (toastModel.type) {
                                    "success" -> Icons.Default.CheckCircle
                                    "error" -> Icons.Default.ErrorOutline
                                    else -> Icons.Default.Info
                                },
                                contentDescription = null,
                                tint = when (toastModel.type) {
                                    "success" -> Color(0xFF10B981)
                                    "error" -> Color(0xFFEF4444)
                                    else -> Color(0xFF3B82F6)
                                },
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = toastModel.message,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (currentBitmap == null) {
                // Landing Screen / Empty Workspace
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1F2937))
                            .clickable { pickImageLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Upload icon",
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = t.startEditing,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = t.uploadDesc,
                        color = Color(0xFF9CA3AF),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 320.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { pickImageLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFDB2777),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(24.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                            modifier = Modifier.testTag("choose_image_btn")
                        ) {
                            Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = t.chooseImage, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Button(
                            onClick = { takePhotoLauncher.launch(null) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1F2937),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(24.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Camera", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(36.dp))

                    // Portrait Samples Selector
                    Text(
                        text = t.orUseSample,
                        color = Color(0xFFE5E7EB),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    SampleSelectorGrid(
                        currentAddingId = isAddingSample,
                        onSampleSelected = { label, url ->
                            isAddingSample = label
                            coroutineScope.launch {
                                val fetched = fetchBitmapFromUrl(context, url)
                                if (fetched != null) {
                                    viewModel.setInitialImage(fetched)
                                    viewModel.showToast("Loaded $label!", "success")
                                } else {
                                    viewModel.showToast("Failed to load sample. Check internet connection.", "error")
                                }
                                isAddingSample = null
                            }
                        },
                        t = t
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Security Prompt Message as required by android-secret-management skill
                    SecurityWarningCard()
                }
            } else {
                // Workspace display
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = currentBitmap.asImageBitmap(),
                        contentDescription = "Editing image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.85f)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF374151), RoundedCornerShape(12.dp))
                            .testTag("workspace_image"),
                        contentScale = ContentScale.Fit
                    )

                    if (isProcessing) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.65f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F23)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator(color = Color(0xFFEC4899))
                                    Text(
                                        text = t.applyingMagic,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SampleSelectorGrid(
    currentAddingId: String?,
    onSampleSelected: (String, String) -> Unit,
    t: TranslationPack
) {
    val samples: List<Pair<String, String>> = listOf(
        Pair(t.sampleMale, "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&auto=format&fit=crop&q=80"),
        Pair(t.sampleFemale, "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=400&auto=format&fit=crop&q=80"),
        Pair(t.sampleNeutral, "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&auto=format&fit=crop&q=80")
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        samples.forEach { (label, url) ->
            val isSelected = currentAddingId == label
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable(enabled = currentAddingId == null) {
                        onSampleSelected(label, url)
                    }
                    .testTag("sample_${label.lowercase(Locale.ROOT).replace(" ", "_")}")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(url)
                                .crossfade(true)
                                .build()
                        ),
                        contentDescription = label,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .border(2.dp, if (isSelected) Color(0xFFEC4899) else Color(0xFF374151), CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    color = Color(0xFFD1D5DB),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun SecurityWarningCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24).copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFBBF24),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Security Warning",
                    color = Color(0xFFFBBF24),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
            Text(
                text = "I have included your API keys in the generated APK file for this prototype. Please be aware that Android APKs can be easily decompiled, and these keys can be extracted by anyone who has access to the file. Do not share this APK file publicly or with unauthorized individuals to prevent potential misuse.",
                color = Color(0xFF9CA3AF),
                fontSize = 10.sp,
                lineHeight = 13.sp
            )
        }
    }
}

// --- Utilities ---

fun loadAndScaleBitmap(context: Context, uri: Uri, maxDimension: Int = 1024): Bitmap? {
    return try {
        var inputStream: InputStream? = context.contentResolver.openInputStream(uri) ?: return null
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeStream(inputStream, null, options)
        inputStream?.close()

        val width = options.outWidth
        val height = options.outHeight
        var scale = 1
        if (width > maxDimension || height > maxDimension) {
            val largest = maxOf(width, height)
            scale = Math.ceil(largest.toDouble() / maxDimension.toDouble()).toInt()
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = scale
        }
        inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
        inputStream?.close()
        bitmap
    } catch (e: Exception) {
        Log.e("AIRetouchPro", "Error loading scaled bitmap", e)
        null
    }
}

suspend fun fetchBitmapFromUrl(context: Context, url: String): Bitmap? {
    return withContext(Dispatchers.IO) {
        try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(url)
                .allowHardware(false) // Safe Bitmap extraction
                .build()
            val result = loader.execute(request)
            if (result is SuccessResult) {
                (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("AIRetouchPro", "Error fetching bitmap from url $url", e)
            null
        }
    }
}

fun saveBitmapToGallery(context: Context, bitmap: Bitmap, namePrefix: String = "Edited"): Uri? {
    val filename = "${namePrefix}_${System.currentTimeMillis()}.png"
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AIRetouchPro")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
    }

    val resolver = context.contentResolver
    val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

    if (imageUri != null) {
        try {
            val outputStream: OutputStream? = resolver.openOutputStream(imageUri)
            if (outputStream != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.close()
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(imageUri, contentValues, null, null)
            }
            return imageUri
        } catch (e: Exception) {
            Log.e("AIRetouchPro", "Error saving bitmap to storage", e)
            resolver.delete(imageUri, null, null)
        }
    }
    return null
}
