app/src/main/java/com/geoguessrclone/
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
│   └── adapters/
│       ├── LeaderboardAdapter.kt
│       └── FriendsAdapter.kt
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
└── di/
├── NetworkModule.kt
├── DatabaseModule.kt
└── RepositoryModule.kt