package com.rankfodinha.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Fotos tiradas para o avatar. Ficam só no aparelho, em filesDir/avatares, com o id do jogador no nome.
 * A câmera grava primeiro em cacheDir/avatares-tmp. [saveCapture] transforma essa foto na foto do avatar.
 */
object AvatarPhotos {

    /** Lado da foto final, em pixels. Basta para o avatar maior do app em telas 3x. */
    private const val SIZE_PX = 256
    private const val JPEG_QUALITY = 85
    private const val CAPTURE_DIR = "avatares-tmp"
    private const val PHOTO_DIR = "avatares"

    /**
     * Prepara o arquivo temporário em que a câmera vai gravar e devolve o endereço dele.
     * Apaga a foto temporária de uma tentativa anterior, para nunca usar uma foto que não é desta vez.
     */
    fun prepareCapture(context: Context, playerId: String): Uri {
        val file = captureFile(context, playerId)
        file.parentFile?.mkdirs()
        file.delete()
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /**
     * Transforma a foto que a câmera gravou na foto do avatar: gira conforme o EXIF, corta um quadrado
     * no centro, reduz para [SIZE_PX] e grava em JPEG. Retorna false se a câmera não gravou uma imagem válida.
     * Chame fora da thread principal. A foto temporária é apagada em qualquer caso.
     */
    fun saveCapture(context: Context, playerId: String): Boolean {
        val capture = captureFile(context, playerId)
        return try {
            val avatar = readAvatar(capture) ?: return false
            writeJpeg(avatar, photoFile(context, playerId))
        } finally {
            capture.delete()
        }
    }

    /** Foto salva do jogador, ou null se ela não existe. Chame fora da thread principal. */
    fun load(context: Context, playerId: String): Bitmap? {
        val file = photoFile(context, playerId)
        return if (file.isFile) BitmapFactory.decodeFile(file.path) else null
    }

    /** Apaga a foto temporária de uma tentativa cancelada. A foto salva não é tocada. */
    fun discardCapture(context: Context, playerId: String) {
        captureFile(context, playerId).delete()
    }

    /** Apaga a foto do jogador. Usado quando ele é removido. */
    fun delete(context: Context, playerId: String) {
        photoFile(context, playerId).delete()
        captureFile(context, playerId).delete()
    }

    private fun photoFile(context: Context, playerId: String): File =
        File(File(context.filesDir, PHOTO_DIR), "$playerId.jpg")

    private fun captureFile(context: Context, playerId: String): File =
        File(File(context.cacheDir, CAPTURE_DIR), "$playerId.jpg")

    /** Lê a foto da câmera já girada e cortada em quadrado. Reduz antes de decodificar, para não estourar a memória. */
    private fun readAvatar(file: File): Bitmap? {
        if (!file.isFile || file.length() == 0L) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
        }
        val decoded = BitmapFactory.decodeFile(file.path, options) ?: return null
        val upright = rotate(decoded, rotationOf(file))
        val side = minOf(upright.width, upright.height)
        val square = Bitmap.createBitmap(
            upright,
            (upright.width - side) / 2,
            (upright.height - side) / 2,
            side,
            side,
        )
        return square.scale(SIZE_PX, SIZE_PX, true)
    }

    /** Maior redução em potência de 2 que ainda deixa o lado menor com pelo menos [SIZE_PX] pixels. */
    private fun sampleSizeFor(width: Int, height: Int): Int {
        var sample = 1
        val shorterSide = minOf(width, height)
        while (shorterSide / (sample * 2) >= SIZE_PX) sample *= 2
        return sample
    }

    /** Graus de giro indicados pelo EXIF da foto. Sem EXIF legível, a foto fica como está. */
    private fun rotationOf(file: File): Int =
        runCatching { ExifInterface(file).rotationDegrees }.getOrDefault(0)

    private fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /** Grava o JPEG em um arquivo temporário e só então troca pelo definitivo. Uma falha no meio não deixa foto pela metade. */
    private fun writeJpeg(avatar: Bitmap, target: File): Boolean {
        target.parentFile?.mkdirs()
        val temp = File(target.path + ".tmp")
        val written = try {
            FileOutputStream(temp).use { out -> avatar.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out) }
        } catch (error: IOException) {
            false
        }
        if (written && temp.renameTo(target)) return true
        temp.delete()
        return false
    }
}
