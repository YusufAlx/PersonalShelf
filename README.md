# Personal Shelf Android application

Personal Shelf is a Kotlin and Jetpack Compose Android Studio application for a private library of books, comics, documents and study material. It follows the supplied planning and research documents: the shelf is designed around easy upload and access like Google Drive, with a focused reading-library experience inspired by WEBTOON.

## What is included

| Assessment criterion | Implementation |
|---|---|
| Register, log in and encrypted passwords | Firebase Authentication email/password functions. Firebase handles password hashing; the app never stores passwords. |
| Single sign-on | Firebase Authentication has a dedicated Google SSO setup point; enable its provider as described below. |
| User settings | English/Afrikaans language toggle and sign-out settings. |
| Cloud API and database | Firebase Authentication, Cloud Firestore and Cloud Storage integration points. Firestore stores each user under `users/{uid}/library/{itemId}`. |
| Offline mode and synchronisation | The local DataStore copy lets users add, edit, favourite, track progress, delete, search and filter while offline. Changes are marked pending and sent to Firestore by Sync on reconnection; WorkManager also schedules cloud sync only when Android reports connectivity. |
| Real-time notifications | `ShelfMessagingService` receives Firebase Cloud Messaging updates and displays an Android notification. |
| Multi-language | English and Afrikaans labels are included for the shelf’s primary interactions. |

## Open in Android Studio

1. Open this **PersonalShelf** folder in Android Studio Ladybug or newer.
2. Let Android Studio download the Gradle dependencies and sync.
3. Run on an Android 8.0+ emulator or device. **Explore offline demo** allows the complete local shelf workflow before cloud setup.

## Connect Firebase

1. Create a Firebase project, add Android app package `za.ac.personalshelf`, and put the downloaded `google-services.json` in `app/`.
2. Enable **Email/Password** and **Google** in Firebase Authentication. Configure the Google OAuth SHA-1 fingerprints from Android Studio for SSO. The Google Services plugin generates the required Web client ID from `google-services.json` automatically.
3. Create a Cloud Firestore database and Cloud Storage bucket. Deploy the example security rules below.
4. Enable Firebase Cloud Messaging. The included service receives messages sent to the device token.

When Firebase is enabled, add the Google Services plugin in the root and application Gradle files according to Firebase’s current Android setup guide. This is intentionally left out of source control because the configuration file belongs to your own Firebase project.

## Firestore security rules

```text
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
      match /library/{itemId} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
    }
  }
}
```

## Storage security rules

```text
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /users/{userId}/{allPaths=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

## Suggested next incremental work

- Connect the selected document URI to Firebase Storage and save the resulting file path with its Firestore metadata.
- Add a PDF/EPUB reader screen and persist page bookmarks.
