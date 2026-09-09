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
        "elec_inverter" -> R.drawable.clay_inverter
        "elec_geyser" -> R.drawable.clay_geyser_heater
        "elec_ac_point" -> R.drawable.clay_socket
        "elec_wiring" -> R.drawable.clay_wiring

        // Plumber appliances & fixtures
        "plumb_tap" -> R.drawable.clay_water_tap
        "plumb_sink" -> R.drawable.clay_washbasin
        "plumb_toilet" -> R.drawable.clay_toilet
        "plumb_drain" -> R.drawable.clay_drain
        "plumb_pipe" -> R.drawable.clay_water_pipe
        "plumb_geyser" -> R.drawable.clay_geyser_heater
        "plumb_shower" -> R.drawable.clay_shower
        "plumb_motor" -> R.drawable.clay_water_motor

        // Carpenter fixtures & furniture
        "carp_locks" -> R.drawable.clay_door_lock
        "carp_cupboard" -> R.drawable.clay_wardrobe
        "carp_assembly" -> R.drawable.clay_furniture_chair
        "carp_bed" -> R.drawable.clay_bed
        "carp_window" -> R.drawable.clay_window
        "carp_kitchen" -> R.drawable.clay_chimney
        "carp_polish" -> R.drawable.clay_saw
        "carp_mount" -> R.drawable.clay_drill_mount

        // Mechanic / Auto
        "mech_bike" -> R.drawable.clay_motorcycle
        "mech_car_oil" -> R.drawable.clay_engine_oil
        "mech_brake" -> R.drawable.clay_brake_disc
        "mech_battery" -> R.drawable.clay_car_battery
        "mech_tyre" -> R.drawable.clay_car_tyre
        "mech_scan" -> R.drawable.clay_car_scanner
        "mech_ac" -> R.drawable.clay_ac_cooler
        "mech_inspect" -> R.drawable.clay_motorcycle

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
        "paint_room" -> R.drawable.clay_paint_brush
        "paint_waterproof" -> R.drawable.clay_waterproof_bucket
        "paint_enamel" -> R.drawable.clay_spray_gun
        "paint_texture" -> R.drawable.clay_paint_roller
        "paint_exterior" -> R.drawable.clay_paint_brush
        "paint_consult" -> R.drawable.clay_paint_brush
        "paint_wood" -> R.drawable.clay_wood_polish

        // Mason services
        "mason_brickwork" -> R.drawable.clay_mason_bricks
        "mason_tile" -> R.drawable.clay_mason_tiles
        "mason_plaster" -> R.drawable.clay_mason_trowel
        "mason_concrete" -> R.drawable.clay_mason_cement
        "mason_granite" -> R.drawable.clay_mason_granite
        "mason_drill" -> R.drawable.clay_mason_hammer

        // Gardener services
        "garden_lawn" -> R.drawable.clay_lawn_mower
        "garden_hedge" -> R.drawable.clay_hedge_shears
        "garden_plants" -> R.drawable.clay_potted_plant
        "garden_pest" -> R.drawable.clay_garden_sprayer
        "garden_watering" -> R.drawable.clay_watering_can
        "garden_balcony" -> R.drawable.clay_balcony_garden

        // House Cleaning services
        "clean_deep_home" -> R.drawable.clay_vacuum_cleaner
        "clean_kitchen" -> R.drawable.clay_kitchen_clean
        "clean_bathroom" -> R.drawable.clay_bathroom_clean
        "clean_sofa" -> R.drawable.clay_sofa_cleaner
        "clean_floor" -> R.drawable.clay_mop_bucket
        "clean_window" -> R.drawable.clay_spray_bottle

        // Other & Custom unlisted services across all categories
        "elec_other", "plumb_other", "carp_other", "mech_other",
        "app_other", "paint_other", "mason_other", "garden_other",
        "clean_other", "other_general", "other_fabrication", "other_glass",
        "other_solar", "other_locksmith", "other_any" -> R.drawable.clay_other_custom

        else -> R.drawable.clay_other_custom
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
