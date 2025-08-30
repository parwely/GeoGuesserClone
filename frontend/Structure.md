app/src/main/java/com.example.geogeusserclone
├── ui/
│   ├── activities/
│   │   ├── MainActivity.kt
│   │   ├── AuthActivity.kt
│   │   ├── GameActivity.kt
│   │   ├── BattleRoyaleActivity.kt
│   │   └── StatsActivity.kt
│   ├── fragments/
│   │   ├── StreetViewFragment.kt
│   │   ├── MapGuessFragment.kt
│   │   ├── ScoreFragment.kt
│   │   └── LeaderboardFragment.kt
├   ├── components/        # Compose Components ✅
│   │   ├── MapGuessComponent.kt
│   │   ├── RoundComponent.kt
│   │   └── StreetViewComponent.kt
│   ├── adapters/
│   │    ├── LeaderboardAdapter.kt
│   │    └── FriendsAdapter.kt
│   │
│   └──   screens/  # Full Compose Screens
│       
│ 
├── data/
│   ├── network/
│   │   ├── ApiService.kt
│   │   ├── SocketClient.kt
│   │   └── AuthInterceptor.kt
│   ├── database/
│   │   ├── entities/
│   │   ├── dao/
│   │   └── AppDatabase.kt
│   ├── repositories/
│   │   ├── GameRepository.kt
│   │   ├── UserRepository.kt
│   │   └── LocationRepository.kt
│   └── models/
│       ├── Game.kt
│       ├── User.kt
│       └── Location.kt
├── viewmodels/
│   ├── GameViewModel.kt
│   ├── AuthViewModel.kt
│   └── StatsViewModel.kt
├── utils/
│   ├── DistanceCalculator.kt
│   ├── ScoreCalculator.kt
│   └── Constants.kt
└
├── NetworkModule.kt
├── DatabaseModule.kt
└── RepositoryModule.kt