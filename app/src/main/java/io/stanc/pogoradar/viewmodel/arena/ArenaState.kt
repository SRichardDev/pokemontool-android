package io.stanc.pogoradar.viewmodel.arena

import io.stanc.pogoradar.AppSettings
import io.stanc.pogoradar.firebase.node.FirebaseArena


fun isArenaVisibleOnMap(arena: FirebaseArena): Boolean {

    var visible = AppSettings.enableArenas.get() == true
    if (visible) {
        visible = !(!arena.isEX && AppSettings.justEXArenas.get() == true)
        if (visible) {
            visible = !(currentRaidState(arena.raid) == RaidState.NONE && AppSettings.justRaidArenas.get() == true)
        }
    }

    return visible
}