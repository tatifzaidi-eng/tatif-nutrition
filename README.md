# NutriScan 🥗

Application Android de recherche et d'analyse nutritionnelle.
**Kotlin · Jetpack Compose · Material Design 3 · Clean Architecture**

---

## Structure du projet

```
NutriScan/
├── app/src/main/java/com/nutriscan/
│   ├── MainActivity.kt                  ← Entry point + DataStore prefs
│   ├── di/
│   │   └── AppModule.kt                 ← Hilt modules (Network, DB, Repo)
│   ├── data/
│   │   ├── api/
│   │   │   ├── UsdaApi.kt               ← API USDA FoodData Central + DTOs + mappers
│   │   │   └── OpenFoodFactsApi.kt      ← Open Food Facts + scanner code-barres
│   │   ├── db/
│   │   │   └── FoodDatabase.kt          ← Room (Entity, DAO, mappers Gson)
│   │   └── repository/
│   │       ├── FoodRepository.kt        ← Interface domaine
│   │       └── FoodRepositoryImpl.kt    ← Implémentation + fallback offline
│   ├── domain/
│   │   ├── model/
│   │   │   └── Food.kt                  ← Modèles de domaine (Food, UiState, etc.)
│   │   └── usecase/
│   │       └── FoodUseCases.kt          ← 6 use cases métier
│   └── presentation/
│       ├── navigation/
│       │   ├── NavRoutes.kt             ← Routes + BottomNavItems
│       │   └── NavHost.kt               ← NavHost + animations de transition
│       ├── theme/
│       │   └── Theme.kt                 ← Material3 clair/sombre + Material You
│       └── ui/
│           ├── home/
│           │   ├── HomeViewModel.kt     ← Search StateFlow + debounce
│           │   └── HomeScreen.kt        ← Écran accueil + barre de recherche
│           ├── detail/
│           │   ├── FoodDetailViewModel.kt
│           │   └── FoodDetailScreen.kt  ← Fiche complète + anneau macro + onglets
│           ├── compare/
│           │   ├── CompareViewModel.kt
│           │   └── CompareScreen.kt     ← Comparaison + graphique en barres Canvas
│           ├── history/
│           │   └── HistoryAndFavoritesScreens.kt
│           ├── barcode/
│           │   └── BarcodeScannerScreen.kt  ← CameraX + ML Kit
│           ├── settings/
│           │   └── SettingsScreen.kt    ← Mode sombre + langue
│           └── components/
│               └── UiComponents.kt      ← SearchBar, FoodListItem, LoadingView, etc.
```

---

## Architecture

```
UI (Compose) → ViewModel → UseCase → Repository → API / Room
```

- **MVVM** avec StateFlow + collectAsState
- **Clean Architecture** : couches data / domain / presentation bien séparées
- **Repository Pattern** : une seule source de vérité, avec fallback offline transparent
- **Hilt** pour l'injection de dépendances

---

## APIs utilisées

### USDA FoodData Central
- Base : `https://api.nal.usda.gov/fdc/v1/`
- Clé gratuite : https://fdc.nal.usda.gov/api-guide.html
- Endpoints utilisés :
  - `GET /foods/search?query=...` — recherche
  - `GET /food/{fdcId}` — détail complet

### Open Food Facts
- Base : `https://world.openfoodfacts.org/api/v2/`
- **Gratuit, sans clé API**
- Endpoints :
  - `GET /search?search_terms=...` — recherche
  - `GET /product/{barcode}.json` — scan code-barres

---

## Configuration

### 1. Clé API USDA
Créer `local.properties` à la racine :
```properties
USDA_API_KEY=votre_cle_ici
```

### 2. Dépendances CameraX + ML Kit (scanner)
Dans `app/build.gradle.kts`, ajouter :
```kotlin
implementation("androidx.camera:camera-core:1.3.4")
implementation("androidx.camera:camera-camera2:1.3.4")
implementation("androidx.camera:camera-lifecycle:1.3.4")
implementation("androidx.camera:camera-view:1.3.4")
implementation("com.google.mlkit:barcode-scanning:17.3.0")
```

### 3. AndroidManifest.xml
- Permission `CAMERA` déjà déclarée
- Deep link `nutriscan://food/{id}` configuré

---

## Fonctionnalités

| # | Fonctionnalité | Fichier clé |
|---|---|---|
| 1 | Recherche auto-complétion + debounce 300ms | `HomeViewModel.kt` |
| 2 | Fiche nutritionnelle complète (macros + micros) | `FoodDetailScreen.kt` |
| 3 | Anneau de macros animé (Canvas) | `FoodDetailScreen.kt` → `MacroRing` |
| 4 | Sélecteur de portion personnalisé | `FoodDetailScreen.kt` → `PortionSelector` |
| 5 | Comparaison 2-3 aliments + graphique barres | `CompareScreen.kt` |
| 6 | Tableau comparatif avec highlights | `CompareScreen.kt` → `CompareTable` |
| 7 | Bienfaits & points d'attention générés | `UsdaApi.kt` → `generateBenefits()` |
| 8 | Historique avec regroupement par date | `HistoryAndFavoritesScreens.kt` |
| 9 | Favoris persistés en Room | `FoodRepository` + `ToggleFavoriteUseCase` |
| 10 | Scanner code-barres (CameraX + ML Kit) | `BarcodeScannerScreen.kt` |
| 11 | Mode clair/sombre + Material You | `Theme.kt` + `SettingsScreen.kt` |
| 12 | Cache offline (Room) | `FoodRepositoryImpl.kt` |
| 13 | Gestion d'états (Loading/Success/Error) | `UiState.kt` + tous les ViewModels |
| 14 | Multi-langue FR/AR (structure) | `SettingsScreen.kt` + `Food.nameAr` |

---

## Lancer le projet

```bash
# Cloner et ouvrir dans Android Studio Hedgehog+
git clone ...
cd NutriScan

# Ajouter votre clé dans local.properties
echo "USDA_API_KEY=DEMO_KEY" >> local.properties

# Synchroniser Gradle puis lancer sur émulateur API 26+
./gradlew assembleDebug
```

---

## Publication Play Store

- `minSdk = 26` (Android 8.0 — 97% des appareils actifs)
- `targetSdk = 35`
- ProGuard activé en release
- Screenshots conseillés : 1080×1920 pour les 4 écrans principaux
- Catégorie suggérée : **Santé & Forme**

---

*Développé avec Kotlin + Jetpack Compose — Architecture recommandée par Google (2024)*
