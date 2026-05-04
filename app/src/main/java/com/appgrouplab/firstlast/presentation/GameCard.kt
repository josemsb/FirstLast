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
import com.appgrouplab.firstlast.model.Game
import com.appgrouplab.firstlast.ui.theme.GreenFistLast
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun GameCard(game: Game) {
    val context = LocalContext.current

    // Logo de liga: busca drawable por key (ej: "premier_league")
    val leagueResId = context.resources.getIdentifier(
        game.league.key, "drawable", context.packageName
    )
    // Logos de equipos: busca drawable por key (ej: "manchester_city")
    val homeResId = context.resources.getIdentifier(
        game.home.key, "drawable", context.packageName
    )
    val awayResId = context.resources.getIdentifier(
        game.away.key, "drawable", context.packageName
    )

    val formattedDate = try {
        val instant = try {
            Instant.parse(game.dateTimeIso)
        } catch (_: Exception) {
            LocalDateTime.parse(game.dateTimeIso).toInstant(ZoneOffset.UTC)
        }
        instant.atZone(ZoneId.systemDefault())
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
                Text(
                    text = game.league.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.size(8.dp))
                if (leagueResId != 0) {
                    Image(
                        painter = painterResource(id = leagueResId),
                        contentDescription = game.league.name,
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
                teamName = game.home.name,
                position = game.home.pos,
                imageResId = homeResId
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
                teamName = game.away.name,
                position = game.away.pos,
                imageResId = awayResId
            )
        }
    }
    HorizontalDivider()
}
