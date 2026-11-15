package com.matchly.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchly.app.R
import com.matchly.app.core.MatchLabels
import com.matchly.app.data.model.Match
import com.matchly.app.data.model.MatchStatus
import com.matchly.app.data.model.displayScore
import com.matchly.app.data.model.matchStatus
import com.matchly.app.data.model.penaltyWinner
import com.matchly.app.ui.LocalFormatters
import com.matchly.app.ui.theme.Matchly

@Composable
fun statusColor(status: MatchStatus) = when {
    status.isLive -> Matchly.colors.live
    status.isDisrupted -> Matchly.colors.gold
    else -> Matchly.colors.textSecondary
}

@Composable
fun MatchRow(match: Match, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val f = LocalFormatters.current
    val ctx = LocalContext.current
    val status = match.matchStatus
    val score = match.displayScore()
    val statusText = MatchLabels.statusText(ctx, match, f)
    val penWinner = score.penaltyWinner()
    val homeWon = (score.hasScore && score.home!! > score.away!!) || penWinner == 1
    val awayWon = (score.hasScore && score.away!! > score.home!!) || penWinner == -1
    val scoreText = if (score.hasScore) f.scoreLine(score) else stringResource(R.string.no_score)
    val description = stringResource(R.string.match_cd, match.home.name, match.away.name, scoreText, statusText)

    GlassCard(
        modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = description },
        onClick = onClick,
        corner = 16.dp,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(58.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    statusText,
                    color = statusColor(status),
                    fontWeight = if (status.isLive) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TeamLine(match.home.name, match.home.logo, if (score.hasScore) f.number(score.home) else null,
                    score.penaltyHome?.let { f.digits("($it)") }, homeWon, status.isLive)
                TeamLine(match.away.name, match.away.logo, if (score.hasScore) f.number(score.away) else null,
                    score.penaltyAway?.let { f.digits("($it)") }, awayWon, status.isLive)
            }
        }
    }
}

@Composable
private fun TeamLine(name: String, logo: String?, score: String?, pen: String?, winner: Boolean, live: Boolean) {
    val c = Matchly.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        TeamLogo(logo, name, 24.dp)
        Text(
            name,
            Modifier.weight(1f).padding(horizontal = 8.dp),
            color = c.textPrimary,
            fontWeight = if (winner) FontWeight.Bold else FontWeight.Normal,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (pen != null) {
            Text(pen, color = c.textSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 4.dp))
        }
        if (score != null) {
            Text(
                score,
                color = if (live) c.live else c.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.width(24.dp),
                textAlign = TextAlign.End,
            )
        }
    }
}
