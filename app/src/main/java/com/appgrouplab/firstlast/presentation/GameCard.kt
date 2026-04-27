package com.appgrouplab.firstlast.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appgrouplab.firstlast.data.TOURNAMENT_DICTIONARY
import com.appgrouplab.firstlast.model.Game
import com.appgrouplab.firstlast.ui.theme.GreenFistLast
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun GameCard(game: Game) {
    val context = LocalContext.current
    val leagueResId = context.resources.getIdentifier(
        game.season.lowercase().replace(" ", "_"),
        "drawable",
        context.packageName
    )
    val formattedDate = try {
        LocalDateTime.parse(game.dateTimeIso)
            .format(DateTimeFormatter.ofPattern("EEEE, dd MMM HH:mm", Locale("es", "ES")))
    } catch (_: Exception) {
        game.dateTimeIso
    }

    Column(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.labelMedium,
                color = GreenFistLast,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                TOURNAMENT_DICTIONARY[game.season]?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.size(8.dp))
                if (leagueResId != 0) {
                    Image(
                        painter = painterResource(id = leagueResId),
                        contentDescription = game.season,
                        modifier = Modifier.size(20.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TeamInfoLocal(
                modifier = Modifier.weight(1f),
                teamName = game.homeTeam,
                position = game.homePosition,
                imageResId = context.resources.getIdentifier(
                    game.homeTeam, "drawable", context.packageName
                )
            )
            Text(
                text = "vs",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            TeamInfoVisitante(
                modifier = Modifier.weight(1f),
                teamName = game.visitingTeam,
                position = game.visitingPosition,
                imageResId = context.resources.getIdentifier(
                    game.visitingTeam, "drawable", context.packageName
                )
            )
        }
    }
    HorizontalDivider()
}
