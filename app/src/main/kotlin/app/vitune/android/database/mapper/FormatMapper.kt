package app.vitune.android.database.mapper

import app.vitune.android.database.entity.FormatEntity
import app.vitune.android.domain.material.Format

class FormatMapper {
    companion object {
        fun map(format: Format): FormatEntity {
            return FormatEntity(
                format.songId,
                format.itag,
                format.mimeType,
                format.bitrate,
                format.contentLength,
                format.lastModified,
                format.loudnessDb
            )
        }

        fun map(formatEntity: FormatEntity): Format {
            return Format(
                formatEntity.songId,
                formatEntity.itag,
                formatEntity.mimeType,
                formatEntity.bitrate,
                formatEntity.contentLength,
                formatEntity.lastModified,
                formatEntity.loudnessDb
            )
        }
    }
}