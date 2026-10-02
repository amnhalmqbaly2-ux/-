package com.example.data.ai

import com.example.BuildConfig
import com.example.data.model.CustomStoryEntity
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class OmaniStoryGeneratorService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateVocalizedOmaniStory(
        topic: String,
        villageSetting: String,
        moralValue: String
    ): Result<CustomStoryEntity> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalStateException("يرجى إضافة مفتاح GEMINI_API_KEY في لوحة Secrets داخل AI Studio لتفعيل توليد القصص بالذكاء الاصطناعي.")
            )
        }

        val prompt = """
            أنت كاتب قصص أطفال تربوي عماني متخصص في الفئة العمرية (6 إلى 10 سنوات).
            اكتب قصة قصيرة وممتعة من القرية العمانية بعنوان مشكول بالكامل حول الموضوع: "$topic"
            في قرية/بيئة: "$villageSetting"
            وتركز على القيمة الأخلاقية: "$moralValue".

            الشروط الإلزامية:
            1. جميع فقرات القصة والعنوان يجب أن تكون باللغة العربية الفصحى المبسطة والمشكولة تشكيلاً كاملاً وواضحاً (الحركات والشدة والتنوين).
            2. تتكون القصة من 4 فقرات مترابطة تحتوي على مفردات تراثية عمانية أصيلة (مثل الفلج، السبلة، القفير، الكمة، الدشداشة، الهبطة، البيدر، الفوالة، الصاروج، البوم، النوخذة).
            3. أضف 3 مفردات عمانية وردت في القصة مع معناها المبسط وجملة توضيحية.
            4. أضف 5 أسئلة فهم مقروء تشخيصية حول القصة تغطي المهارات الخمس بالترتيب:
               - LITERAL (الفهم المباشر)
               - VOCABULARY (فهم المفردات)
               - SEQUENCING (تسلسل الأحداث)
               - INFERENCE (الاستنتاج)
               - MAIN_IDEA (الفكرة الرئيسة)
               لكل سؤال 4 خيارات مشكولة، وحدد رقم الإجابة الصحيحة (0 أو 1 أو 2 أو 3) وشرحاً مبسطاً للطفل.

            أرجع النتيجة بصيغة JSON فقط تطابق الهيكل التالي تماماً:
            {
              "title": "عُنْوَانُ القِصَّةِ المشكول",
              "villageName": "اسم القرية العمانية",
              "moral": "الحكمة المستفادة مشكولة",
              "paragraphs": ["الفقرة ١ مشكولة", "الفقرة ٢ مشكولة", "الفقرة ٣ مشكولة", "الفقرة ٤ مشكولة"],
              "vocabulary": [
                {"word": "الكلمة", "meaning": "المعنى", "example": "مثال من القصة"}
              ],
              "questions": [
                {
                  "questionText": "نص السؤال المشكول؟",
                  "options": ["الخيار الأول", "الخيار الثاني", "الخيار الثالث", "الخيار الرابع"],
                  "correctIndex": 0,
                  "skill": "LITERAL",
                  "explanation": "توضيح الإجابة"
                }
              ]
            }
        """.trimIndent()

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.7)
            })
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        val body = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url(url).post(body).build()

        try {
            client.newCall(request).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException("تعذر الاتصال بخدمة الذكاء الاصطناعي (${response.code})")
                    )
                }

                val root = JSONObject(rawBody)
                val text = root.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text")
                    .orEmpty()

                if (text.isBlank()) {
                    return@withContext Result.failure(IllegalStateException("لم يتم استلام نص القصة من النموذج."))
                }

                val storyJson = JSONObject(text)
                val title = storyJson.optString("title", "حِكَايَةٌ مِنَ القَرْيَةِ العُمَانِيَّةِ")
                val village = storyJson.optString("villageName", villageSetting)
                val moral = storyJson.optString("moral", moralValue)

                val pArray = storyJson.optJSONArray("paragraphs") ?: JSONArray()
                val paragraphs = (0 until pArray.length()).mapNotNull { i ->
                    pArray.optString(i)?.takeIf { it.isNotBlank() }
                }

                val vArray = storyJson.optJSONArray("vocabulary") ?: JSONArray()
                val vocabLines = (0 until vArray.length()).mapNotNull { i ->
                    val obj = vArray.optJSONObject(i) ?: return@mapNotNull null
                    val w = obj.optString("word").replace("|", " ")
                    val m = obj.optString("meaning").replace("|", " ")
                    val ex = obj.optString("example").replace("|", " ")
                    if (w.isNotBlank()) "$w|$m|$ex" else null
                }

                val qArray = storyJson.optJSONArray("questions") ?: JSONArray()
                val questionLines = (0 until qArray.length()).mapNotNull { i ->
                    val obj = qArray.optJSONObject(i) ?: return@mapNotNull null
                    val qText = obj.optString("questionText").replace("|", " ")
                    val optsArr = obj.optJSONArray("options") ?: JSONArray()
                    val opts = (0 until optsArr.length()).map { j ->
                        optsArr.optString(j).replace("~", " ").replace("|", " ")
                    }
                    val correctIdx = obj.optInt("correctIndex", 0)
                    val skill = obj.optString("skill", "LITERAL")
                    val exp = obj.optString("explanation", "إِجَابَةٌ صَحِيحَةٌ!").replace("|", " ")
                    if (qText.isNotBlank() && opts.isNotEmpty()) {
                        "$qText|${opts.joinToString("~")}|$correctIdx|$skill|$exp"
                    } else null
                }

                val entity = CustomStoryEntity(
                    id = "ai_${UUID.randomUUID().toString().replace("-", "").take(12)}",
                    title = title,
                    villageName = village,
                    moral = moral,
                    paragraphsText = paragraphs.joinToString("\n---\n"),
                    vocabularyText = vocabLines.joinToString("\n"),
                    questionsText = questionLines.joinToString("\n")
                )
                Result.success(entity)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
