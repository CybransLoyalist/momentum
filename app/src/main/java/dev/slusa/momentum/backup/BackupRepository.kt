package dev.slusa.momentum.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.room.withTransaction
import dev.slusa.momentum.data.MomentumDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Kopia zapasowa jako plik, ktory widac i da sie sprawdzic.
 *
 * Android Auto Backup dziala rownolegle i jest bezobslugowy, ale nie da sie go
 * podejrzec ani cofnac do stanu sprzed tygodnia. To nie sa alternatywy - JSON jest
 * jedynym sposobem, zeby w ogole zweryfikowac, ze kopia istnieje.
 *
 * Folder wybiera sie raz przez systemowy wybor katalogu i uprawnienie do niego jest
 * utrwalane. Nie uzywamy wlasnego katalogu aplikacji, bo na Androidzie 11 i nowszym
 * nie da sie do niego zajrzec menedzerem plikow, czyli kopia znowu bylaby niewidzialna.
 */
class BackupRepository(
    private val context: Context,
    private val db: MomentumDatabase,
) {

    suspend fun snapshot(): BackupData = withContext(Dispatchers.IO) {
        BackupData(
            todos = db.todoDao().all(),
            recurrences = db.recurrenceDao().all(),
            habits = db.habitDao().allHabits(),
            completions = db.habitDao().allCompletions(),
        )
    }

    /** Zapisuje kopie w wybranym folderze i zwraca nazwe pliku. */
    suspend fun writeTo(folder: Uri, today: LocalDate = LocalDate.now()): String? =
        withContext(Dispatchers.IO) {
            val dir = DocumentFile.fromTreeUri(context, folder) ?: return@withContext null
            val name = "momentum-$today.json"

            // Druga kopia tego samego dnia nadpisuje pierwsza. Zostawianie obu dalby
            // katalog pelen plikow z jednego popoludnia, a i tak liczy sie najnowsza.
            dir.findFile(name)?.delete()

            val file = dir.createFile("application/json", name) ?: return@withContext null
            context.contentResolver.openOutputStream(file.uri)?.use { out ->
                out.write(BackupFormat.toJson(snapshot()).toByteArray())
            } ?: return@withContext null

            prune(dir)
            name
        }

    suspend fun readFrom(file: Uri): BackupData? = withContext(Dispatchers.IO) {
        runCatching {
            val text = context.contentResolver.openInputStream(file)?.use { it.readBytes() }
                ?.decodeToString() ?: return@runCatching null
            BackupFormat.fromJson(text)
        }.getOrNull()
    }

    /**
     * Podmienia caly stan. Wszystko albo nic - polowicznie wczytana kopia bylaby
     * gorsza niz brak kopii, bo nie wiadomo by bylo, ktore dane sa z ktorego swiata.
     */
    suspend fun restore(data: BackupData) {
        db.withTransaction {
            db.todoDao().deleteAll()
            db.recurrenceDao().deleteAll()
            db.habitDao().deleteAllCompletions()
            db.habitDao().deleteAllHabits()

            // Reguly przed todosami, bo todosy na nie wskazuja.
            db.recurrenceDao().insertAll(data.recurrences)
            db.todoDao().insertAll(data.todos)
            db.habitDao().insertHabits(data.habits)
            db.habitDao().insertCompletions(data.completions)
        }
    }

    /** Zostawiamy dwa tygodnie wstecz - dalej i tak nikt nie siega, a folder rosnie. */
    private fun prune(dir: DocumentFile, keep: Int = 14) {
        val files = dir.listFiles()
            .filter { it.name?.startsWith("momentum-") == true }
            .sortedByDescending { it.name }

        files.drop(keep).forEach { it.delete() }
    }
}
