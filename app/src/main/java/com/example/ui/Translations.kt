package com.example.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

data class TranslationPack(
    val appTitle: String,
    val save: String,
    val processingToast: String,
    val successToast: String,
    val errorToast: String,
    val saveSuccess: String,
    val serverError: String,
    val startEditing: String,
    val uploadDesc: String,
    val chooseImage: String,
    val applyingMagic: String,
    val orUseSample: String,
    val sampleMale: String,
    val sampleFemale: String,
    val sampleNeutral: String
)

object Translations {
    val en = TranslationPack(
        appTitle = "AI Retouch Pro",
        save = "Save",
        processingToast = "Processing image with AI...",
        successToast = "Edit applied successfully!",
        errorToast = "Error processing image. Try again.",
        saveSuccess = "Image saved to gallery!",
        serverError = "No image returned from server.",
        startEditing = "Start Editing",
        uploadDesc = "Upload a face image to apply magical AI effects like age changing, lighting, and filters.",
        chooseImage = "Choose Image",
        applyingMagic = "Applying magic...",
        orUseSample = "Or try one of our beautiful portrait samples:",
        sampleMale = "Elegant Man",
        sampleFemale = "Glow Woman",
        sampleNeutral = "Modern Studio"
    )

    val ar = TranslationPack(
        appTitle = "AI Retouch Pro",
        save = "حفظ",
        processingToast = "جاري معالجة الصورة بالذكاء الاصطناعي...",
        successToast = "تم تطبيق التعديل بنجاح!",
        errorToast = "حدث خطأ أثناء معالجة الصورة. حاول مرة أخرى.",
        saveSuccess = "تم حفظ الصورة في الاستوديو!",
        serverError = "لم يتم إرجاع صورة من الخادم.",
        startEditing = "ابدأ بتعديل صورتك",
        uploadDesc = "قم بتحميل صورة الوجه لتطبيق تأثيرات الذكاء الاصطناعي السحرية مثل تغيير العمر، الإضاءة، والفلاتر.",
        chooseImage = "اختيار صورة",
        applyingMagic = "يتم تطبيق السحر...",
        orUseSample = "أو جرب أحد النماذج المسبقة الجميلة:",
        sampleMale = "نموذج رجالي",
        sampleFemale = "نموذج نسائي",
        sampleNeutral = "نموذج استوديو"
    )

    fun get(lang: String): TranslationPack {
        return if (lang == "ar") ar else en
    }
}

data class FilterOption(
    val id: String,
    val labelEn: String,
    val labelAr: String,
    val prompt: String
)

data class ToolCategory(
    val id: String,
    val titleEn: String,
    val titleAr: String,
    val icon: ImageVector,
    val options: List<FilterOption>
)

val categories = listOf(
    ToolCategory(
        id = "enhance",
        titleEn = "Enhancement",
        titleAr = "تحسين الجودة",
        icon = Icons.Default.AutoFixHigh,
        options = listOf(
            FilterOption(
                id = "enhance_quality",
                labelEn = "Enhance & Sharpen",
                labelAr = "تحسين الدقة وتصفية الصورة",
                prompt = "Dramatically enhance the image quality, upscale, sharpen details, improve resolution, make it high definition and crystal clear, photorealistic."
            )
        )
    ),
    ToolCategory(
        id = "face",
        titleEn = "Face & Age",
        titleAr = "الوجه والعمر",
        icon = Icons.Default.Face,
        options = listOf(
            FilterOption("age_young", "Make Younger", "تصغير العمر", "Make the person in the image look significantly younger, smooth skin, keep identity."),
            FilterOption("age_old", "Make Older", "تكبير العمر", "Make the person in the image look older, add natural aging features, keep identity."),
            FilterOption("skin_smooth", "Smooth Skin", "تجميل البشرة", "Apply high-end beauty retouching, smooth skin, remove blemishes, enhance facial features."),
            FilterOption("jawline", "Enhance Jawline", "تحسين الفكين", "Enhance and define the jawline of the person, make it sharper and more attractive."),
            FilterOption("blemish", "Remove Blemish", "إزالة الشوائب", "Remove all skin blemishes, acne, and spots. Make the skin perfectly clear.")
        )
    ),
    ToolCategory(
        id = "expressions",
        titleEn = "Expressions",
        titleAr = "التعابير والشفاه",
        icon = Icons.Default.SentimentSatisfiedAlt,
        options = listOf(
            FilterOption("smile", "Smile", "ابتسامة", "Make the person smile naturally showing teeth, adjust cheeks to match the smile."),
            FilterOption("lips_big", "Bigger Lips", "تكبير الشفتين", "Make the lips fuller and slightly larger naturally."),
            FilterOption("lips_small", "Smaller Lips", "تصغير الشفتين", "Make the lips slightly thinner and smaller."),
            FilterOption("angry", "Angry", "غضب", "Change the facial expression to look angry, furrowed brows.")
        )
    ),
    ToolCategory(
        id = "hair",
        titleEn = "Hair & Beard",
        titleAr = "الشعر والذقن",
        icon = Icons.Default.ContentCut,
        options = listOf(
            FilterOption("hair_long", "Long Hair", "تطويل الشعر", "Make the person's hair longer naturally."),
            FilterOption("hair_short", "Short Hair", "تقصير الشعر", "Give the person a shorter haircut."),
            FilterOption("hair_blonde", "Blonde Hair", "شعر أشقر", "Change the hair color to natural blonde."),
            FilterOption("beard", "Add Beard", "إضافة لحية", "Add a well-groomed full beard and mustache to the face."),
            FilterOption("mustache", "Mustache Only", "شارب فقط", "Add a thick mustache to the face without a beard.")
        )
    ),
    ToolCategory(
        id = "eyes",
        titleEn = "Eyes & Gaze",
        titleAr = "العيون والنظرة",
        icon = Icons.Default.RemoveRedEye,
        options = listOf(
            FilterOption("eye_blue", "Blue Eyes", "عيون زرقاء", "Change the eye color to deep blue."),
            FilterOption("eye_green", "Green Eyes", "عيون خضراء", "Change the eye color to emerald green."),
            FilterOption("gaze_center", "Look Forward", "تعديل النظرة للأمام", "Adjust the eyes and face angle so the person is looking directly straight at the camera."),
            FilterOption("gaze_side", "Look Sideways", "نظرة جانبية", "Adjust the eyes so the person is looking to the side, cinematic profile feel.")
        )
    ),
    ToolCategory(
        id = "lighting",
        titleEn = "Lighting",
        titleAr = "الإضاءة",
        icon = Icons.Default.LightMode,
        options = listOf(
            FilterOption("light_studio", "Studio Light", "إضاءة استوديو", "Apply professional studio portrait lighting, softboxes, perfectly lit face."),
            FilterOption("light_outdoor", "Outdoor Light", "إضاءة خارجية", "Apply natural sunny outdoor lighting with soft shadows."),
            FilterOption("light_rim", "Rim Light", "إضاءة ريم (خلفية)", "Add dramatic cinematic rim lighting from the back edges."),
            FilterOption("light_top", "Top Light", "إضاءة علوية", "Apply overhead top lighting, dramatic shadows under nose and chin."),
            FilterOption("light_bottom", "Bottom Light", "إضاءة سفلية", "Apply bottom lighting (underlighting) for a dramatic or mysterious look.")
        )
    ),
    ToolCategory(
        id = "filters",
        titleEn = "Filters",
        titleAr = "فلاتر سينمائية",
        icon = Icons.Default.PhotoCamera,
        options = listOf(
            FilterOption("fuji", "Fujifilm", "فوجي فيلم", "Apply a Fujifilm classic negative film simulation, vintage green and red tones, film grain."),
            FilterOption("cinematic", "Cinematic", "سينمائي", "Apply a Hollywood cinematic color grade, teal and orange look, high contrast."),
            FilterOption("bw", "Black & White", "أبيض وأسود", "Convert the image to high-contrast artistic black and white photography."),
            FilterOption("vintage", "Vintage", "عتيق (Vintage)", "Apply a vintage 80s film camera effect, faded colors, light leaks."),
            FilterOption("uniform", "Uniform Light", "إضاءة موحدة", "Apply a flat, uniform, high-key lighting effect with soft pastel tones.")
        )
    )
)
