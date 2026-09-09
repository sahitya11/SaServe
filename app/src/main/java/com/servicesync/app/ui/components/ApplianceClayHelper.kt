package com.servicesync.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.servicesync.app.R
import com.servicesync.app.data.model.ServiceCategory

/**
 * Returns the appropriate high-quality 3D Clay Style drawable resource for any appliance or repair subservice.
 */
@DrawableRes
fun getApplianceClayDrawable(itemId: String): Int {
    return when (itemId) {
        // Electrician appliances
        "elec_fan" -> R.drawable.clay_ceiling_fan
        "elec_switch" -> R.drawable.clay_switchboard
        "elec_light" -> R.drawable.clay_lights_bulb
        "elec_mcb" -> R.drawable.clay_mcb_box
        "elec_inverter" -> R.drawable.clay_mcb_box
        "elec_geyser" -> R.drawable.clay_geyser_heater
        "elec_ac_point" -> R.drawable.clay_switchboard
        "elec_wiring" -> R.drawable.clay_switchboard

        // Plumber appliances & fixtures
        "plumb_tap" -> R.drawable.clay_water_tap
        "plumb_sink" -> R.drawable.clay_washbasin
        "plumb_toilet" -> R.drawable.clay_washbasin
        "plumb_drain" -> R.drawable.clay_water_tap
        "plumb_pipe" -> R.drawable.clay_water_tap
        "plumb_geyser" -> R.drawable.clay_geyser_heater
        "plumb_shower" -> R.drawable.clay_water_tap
        "plumb_motor" -> R.drawable.clay_geyser_heater

        // Carpenter fixtures & furniture
        "carp_locks" -> R.drawable.clay_door_lock
        "carp_cupboard" -> R.drawable.clay_furniture_chair
        "carp_assembly" -> R.drawable.clay_furniture_chair
        "carp_bed" -> R.drawable.clay_furniture_chair
        "carp_window" -> R.drawable.clay_door_lock
        "carp_kitchen" -> R.drawable.clay_chimney
        "carp_polish" -> R.drawable.clay_furniture_chair
        "carp_mount" -> R.drawable.clay_door_lock

        // Mechanic / Auto
        "mech_bike" -> R.drawable.clay_motorcycle
        "mech_car_oil" -> R.drawable.clay_car_tuneup
        "mech_brake" -> R.drawable.clay_car_tuneup
        "mech_battery" -> R.drawable.clay_car_tuneup
        "mech_tyre" -> R.drawable.clay_motorcycle
        "mech_scan" -> R.drawable.clay_car_tuneup
        "mech_ac" -> R.drawable.clay_ac_cooler
        "mech_inspect" -> R.drawable.clay_car_tuneup

        // Appliance Repair appliances
        "app_fridge" -> R.drawable.clay_fridge
        "app_wm" -> R.drawable.clay_washing_machine
        "app_ac" -> R.drawable.clay_ac_cooler
        "app_micro" -> R.drawable.clay_microwave
        "app_ro" -> R.drawable.clay_ro_purifier
        "app_tv" -> R.drawable.clay_smart_tv
        "app_chimney" -> R.drawable.clay_chimney
        "app_heater" -> R.drawable.clay_geyser_heater

        // Painter services
        "paint_patch" -> R.drawable.clay_paint_roller
        "paint_room" -> R.drawable.clay_paint_roller
        "paint_waterproof" -> R.drawable.clay_paint_roller
        "paint_enamel" -> R.drawable.clay_paint_roller
        "paint_texture" -> R.drawable.clay_paint_roller
        "paint_exterior" -> R.drawable.clay_paint_roller
        "paint_consult" -> R.drawable.clay_paint_roller
        "paint_wood" -> R.drawable.clay_furniture_chair

        else -> R.drawable.clay_switchboard
    }
}

/**
 * Reusable Clay Appliance Image Composable with soft 3D clay studio frame and fallback vector icon.
 */
@Composable
fun ApplianceClayImage(
    itemId: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 14.dp
) {
    val drawableRes = getApplianceClayDrawable(itemId)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color(0xFFF1F5F9))
            .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}
