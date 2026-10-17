package com.raulshma.jellyplay.feature.player.video.components

import androidx.compose.runtime.Composable
import com.raulshma.jellyplay.core.model.DecoderMode
import com.raulshma.jellyplay.core.model.MpvRenderQuality
import com.raulshma.jellyplay.core.model.MpvShaderPack
import com.raulshma.jellyplay.core.model.MpvToneMapping
import com.raulshma.jellyplay.core.model.PlaybackMode
import com.raulshma.jellyplay.feature.player.video.generated.resources.Res
import com.raulshma.jellyplay.feature.player.video.generated.resources.player_option_auto
import com.raulshma.jellyplay.feature.player.video.generated.resources.player_option_decoder_hw_only
import com.raulshma.jellyplay.feature.player.video.generated.resources.player_option_decoder_hw_preferred
import com.raulshma.jellyplay.feature.player.video.generated.resources.player_option_decoder_sw_only
import com.raulshma.jellyplay.feature.player.video.generated.resources.player_option_force_direct
import com.raulshma.jellyplay.feature.player.video.generated.resources.player_option_force_transcode
import com.raulshma.jellyplay.feature.player.video.generated.resources.player_option_quality_balanced
import com.raulshma.jellyplay.feature.player.video.generated.resources.player_option_quality_high
import com.raulshma.jellyplay.feature.player.video.generated.resources.player_option_shader_a
import com.raulshma.jellyplay.feature.player.video.generated.resources.player_option_shader_b
import com.raulshma.jellyplay.feature.player.video.generated.resources.player_option_shader_c
import com.raulshma.jellyplay.feature.player.video.generated.resources.player_option_shader_custom
import com.raulshma.jellyplay.feature.player.video.generated.resources.player_option_tone_auto
import com.raulshma.jellyplay.feature.player.video.generated.resources.player_video_off
import com.raulshma.jellyplay.feature.player.video.generated.resources.player_video_stats_performance
import org.jetbrains.compose.resources.stringResource

// Display labels are localized at composition time. Enum names, persisted
// preferences and the engine's protocol keys remain independent of locale.
@Composable
internal fun DecoderMode.localizedPlayerLabel(): String = stringResource(when (this) {
    DecoderMode.HW_PREFERRED -> Res.string.player_option_decoder_hw_preferred
    DecoderMode.HW_ONLY -> Res.string.player_option_decoder_hw_only
    DecoderMode.SW_ONLY -> Res.string.player_option_decoder_sw_only
})

@Composable
internal fun PlaybackMode.localizedPlayerLabel(): String = stringResource(when (this) {
    PlaybackMode.AUTO -> Res.string.player_option_auto
    PlaybackMode.FORCE_DIRECT_PLAY -> Res.string.player_option_force_direct
    PlaybackMode.FORCE_TRANSCODE -> Res.string.player_option_force_transcode
})

@Composable
internal fun MpvShaderPack.localizedPlayerLabel(): String = stringResource(when (this) {
    MpvShaderPack.OFF -> Res.string.player_video_off
    MpvShaderPack.ANIME4K_A -> Res.string.player_option_shader_a
    MpvShaderPack.ANIME4K_B -> Res.string.player_option_shader_b
    MpvShaderPack.ANIME4K_C -> Res.string.player_option_shader_c
    MpvShaderPack.CUSTOM -> Res.string.player_option_shader_custom
})

@Composable
internal fun MpvToneMapping.localizedPlayerLabel(): String =
    if (this == MpvToneMapping.AUTO) stringResource(Res.string.player_option_tone_auto)
    else displayName // Fixed algorithm names (BT.2390, Hable, Reinhard, etc.).

@Composable
internal fun MpvRenderQuality.localizedPlayerLabel(): String = stringResource(when (this) {
    MpvRenderQuality.PERFORMANCE -> Res.string.player_video_stats_performance
    MpvRenderQuality.BALANCED -> Res.string.player_option_quality_balanced
    MpvRenderQuality.HIGH -> Res.string.player_option_quality_high
})
