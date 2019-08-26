package io.stanc.pogoradar.viewmodel.arena

import io.stanc.pogoradar.MapFilterSettings
import io.stanc.pogoradar.firebase.node.FirebaseArena


fun isArenaVisibleOnMap(arena: FirebaseArena): Boolean {

    var visible = MapFilterSettings.enableArenas.get() == true
    if (visible) {
        visible = !(!arena.isEX && MapFilterSettings.justEXArenas.get() == true)
        if (visible) {
            visible = !(currentRaidState(arena.raid) == RaidState.NONE && MapFilterSettings.justRaidArenas.get() == true)
        }
    }

    return visible
}