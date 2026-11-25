package com.example.taxipot.feature.pot.room

import android.app.AlertDialog
import androidx.activity.ComponentActivity

/**
 * "이미 참여 중인 택시팟이 있어요" 안내 창 (방 만들기·참여하기에서 ACTIVE_POT_EXISTS일 때)
 * 한 번에 하나의 방만 참여할 수 있어서, 참여 중인 방(대기실)으로 가는 버튼을 보여줌
 */
fun ComponentActivity.showActivePotDialog(activePotId: Long) {
    AlertDialog.Builder(this)
        .setTitle("이미 참여 중인 택시팟이 있어요")
        .setMessage("한 번에 하나의 택시팟만 참여할 수 있어요.\n참여 중인 방을 먼저 마무리하거나 취소해 주세요.")
        .setNegativeButton("닫기", null)
        .setPositiveButton("참여 중인 방으로") { _, _ ->
            startActivity(WaitingRoomActivity.newIntent(this, activePotId))
        }
        .show()
}