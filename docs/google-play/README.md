# Google Play — всё для ручной публикации

Тексты лежат в `listing/<язык>/*.txt` (копируйте как есть), графика — в `graphics/`.
Ниже — что вписать в каждое поле Play Console и в каком порядке.

## 0. Перед стартом

- **Нужен AAB, не APK.** Релизный workflow кладёт в GitHub Release ещё и `Vigil-v<версия>.aab`.
  v1.0.0 вышел раньше без AAB, поэтому в Play идёт **v1.0.1**: Actions → Release → Run workflow → `1.0.1`
  (или `git tag v1.0.1 && git push origin v1.0.1`).
  Убедитесь, что в репозитории заданы секреты `SIGNING_*`: без них сборка подписывается debug-ключом,
  а такой файл Play отклонит.
- **Языки.** AAB собирается без разбиения по языкам (`bundle.language.enableSplit = false`), иначе Play
  ставит только язык телефона и переключение языка в настройках приложения перестаёт работать.
- **Подпись (Play App Signing).** При первой загрузке оставьте вариант «Google создаст ключ подписи».
  Ваш keystore станет ключом загрузки (upload key) — храните его.
  ⚠️ Из-за этого APK из GitHub и версия из Play будут подписаны разными ключами: поверх друг друга они
  не обновятся. Если нужно, чтобы совпадали, выберите «Использовать свой ключ» и загрузите ключ
  из того же keystore через PEPK (инструкция в самом диалоге).
- **Новый личный аккаунт разработчика** (создан после ноября 2023): перед выходом в production нужен
  закрытый тест — минимум 12 тестировщиков, которые не выходят из теста 14 дней подряд.
- **Политика конфиденциальности** — `PRIVACY.md` в корне репозитория. После push ссылка:
  `https://github.com/bigdevwhale/vigil/blob/main/PRIVACY.md`
  (или перенесите текст на сайт Stillcraft).

## 1. Создание приложения

| Поле | Значение |
|---|---|
| App name | `Vigil: Vows Sealed with Wax` |
| Default language | English (United States) – en-US |
| App or game | App |
| Free or paid | Free |
| Declarations | обе галочки (Developer Program Policies, US export laws) |

## 2. Store listing → Main store listing

| Поле | en-US | ru-RU (Add translations → Russian) |
|---|---|---|
| App name (≤30) | `listing/en-US/title.txt` | `listing/ru-RU/title.txt` |
| Short description (≤80) | `listing/en-US/short_description.txt` | `listing/ru-RU/short_description.txt` |
| Full description (≤4000) | `listing/en-US/full_description.txt` | `listing/ru-RU/full_description.txt` |
| App icon 512×512 | `graphics/icon-512.png` | та же |
| Feature graphic 1024×500 | `graphics/feature-graphic-en.png` | `graphics/feature-graphic-ru.png` |
| Phone screenshots (2–8) | `graphics/screenshots-en/1…6.png` | `graphics/screenshots-ru/1…6.png` |
| Tablet screenshots | можно не загружать | — |
| Video | пусто | — |

Скриншоты 1080×1920 собраны из `docs/screenshots/*.png` с подписями. Исходник там 540 px в ширину,
поэтому при увеличении текст слегка размыт. Для чётких — снять заново с телефона
(`tools/capture.py`) и пересобрать.

## 3. Store settings

| Поле | Значение |
|---|---|
| App category | Application → **Health & Fitness** (запасной вариант — Productivity) |
| Tags (до 5) | Habit tracker, Self-help, Health & fitness, Motivation, Productivity |
| Email address | *ваш публичный email* — обязательно, его увидят все |
| Website | `https://bigdevwhale.github.io/stillcraft/` |
| Phone | пусто |
| External marketing | можно оставить включённым |

## 4. App content (Policy → App content)

**Privacy policy** — URL выше.

**App access** — *All functionality is available without any access restrictions.*

**Ads** — *No, my app does not contain ads.*

**Content rating** (анкета IARC)
- Email — ваш; категория — **All Other App Types**.
- **Алкоголь и табак:** в каталоге есть обеты «Без алкоголя», «Трезвый октябрь», «Без курения» —
  это упоминания в контексте отказа. На вопрос об упоминании алкоголя/табака отвечайте честно **Yes**
  (без пропаганды и без продажи). Иначе рейтинг могут пересмотреть после проверки.
- Остальное — **No**: насилие, страх, сексуальный контент, ненормативная лексика, азартные игры,
  грубый юмор, общение между пользователями, обмен контентом, передача местоположения,
  цифровые покупки, браузер или поиск.
- Религиозная образность (обеты, пост, чётки) в анкете не спрашивается.

**Target audience and content**
- Target age: **18 and over** (обеты про алкоголь; заодно не попадаем под политику Families).
- *Could your store listing unintentionally appeal to children?* → **No**.

**News apps** — No.

**COVID-19 contact tracing and status apps** — My app is not a publicly available COVID-19 app.

**Data safety**
- *Does your app collect or share any of the required user data types?* → **No**.
  Приложение без разрешения INTERNET, всё хранится локально; запись голоса остаётся на устройстве,
  а по правилам Play «сбор» — это передача с устройства. `allowBackup="false"`, так что и системный
  бэкап ничего не уносит.
- Итог в карточке: «No data collected · No data shared».

**Government apps** — No.

**Financial features** — *My app doesn't provide any financial features.*
(«Десятина» и «ноябрь без покупок» — только заметки, денег приложение не касается.)

**Health apps** — если форма спросит, отметьте ближайший пункт про привычки и самочувствие
(*Habit tracking / Wellness* или близкий по смыслу). Лечение зависимостей, Health Connect и медицинские
функции — не отмечать: в описании прямо сказано, что это не лечение.

**Advertising ID** — *No* (рекламы и SDK аналитики нет).

**Permissions** — особых деклараций не нужно:
- `RECORD_AUDIO` — обычное runtime-разрешение, запрашивается только при записи причины голосом.
- `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED` не относятся к чувствительным.
- Поручитель открывает чужое приложение SMS/мессенджер через intent, разрешения `SEND_SMS` нет —
  декларация SMS/Call Log не нужна.
- Точных будильников (exact alarm) и foreground-сервисов нет.

## 5. Pricing & distribution

- Countries / regions: все доступные.
- Free, без встроенных покупок.

## 6. Релиз

1. Testing → **Internal testing** (или Closed testing, если аккаунт новый) → Create new release.
2. Загрузить `Vigil-v1.0.1.aab` из GitHub Release.
3. Release name: `1.0.1`. Release notes:

```
<en-US>
(listing/en-US/release_notes.txt)
</en-US>
<ru-RU>
(listing/ru-RU/release_notes.txt)
</ru-RU>
```

4. Review release → Start rollout. Первая проверка Google обычно занимает от нескольких дней до недели.
5. После установки из Play проверьте: Настройки → Язык → Русский/English — экран должен
   переключиться сразу.

## После публикации

Добавьте в README бейдж и ссылку `https://play.google.com/store/apps/details?id=app.vigil`
(как в SwissZen).
