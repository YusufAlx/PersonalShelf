# Personal Shelf Android application

Personal Shelf is a Kotlin and Jetpack Compose Android Studio application for a private library of books, comics, documents and study material. It follows the supplied planning and research documents: the shelf is designed around easy upload and access like Google Drive, with a focused reading-library experience inspired by WEBTOON.
## Link to Youtube Demonstration
[https://youtu.be/tP0NGFqUOfw]

## What is included

|Feature list
| Register, log in and encrypted passwords | Firebase Authentication email/password functions. Firebase handles password hashing; the app never stores passwords. |
| Single sign-on | Firebase Authentication has a dedicated Google SSO setup point; enable its provider as described below. |
| User settings | English/Afrikaans language toggle and sign-out settings. | Push notifications through Firebase
| Cloud API and database | Firebase Authentication, Cloud Firestore and Cloud Storage integration points. Firestore stores each user under `users/{uid}/library/{itemId}`. |
| Offline mode and synchronisation | The local DataStore copy lets users add, edit, favourite, track progress, delete, search and filter while offline. Changes are marked pending and sent to Firestore by Sync on reconnection; WorkManager also schedules cloud sync only when Android reports connectivity. |
| Real-time notifications | `ShelfMessagingService` receives Firebase Cloud Messaging updates and displays an Android notification. |
| Multi-language | English and Afrikaans labels are included for the shelf’s primary interactions. |

## Open in Android Studio

1. Open this **PersonalShelf** folder in Android Studio Ladybug or newer.
2. Let Android Studio download the Gradle dependencies and sync.
3. Run on an Android 8.0+ emulator or device. **Explore offline demo** allows the complete local shelf workflow before cloud setup.

## Image assets

### Logo
<img width="364" height="364" alt="image" src="https://github.com/user-attachments/assets/7258d4af-c22e-4de4-a496-15259a9f01ea" />

### Login Screen
<img width="720" height="1520" alt="signing in P SHelf" src="https://github.com/user-attachments/assets/a7674136-0892-4fa0-b73e-ecb88e142f15" />

### Uploading file
<img width="720" height="1520" alt="categories" src="https://github.com/user-attachments/assets/d5d8af2a-23a5-4099-84d0-387dbadee096" />

