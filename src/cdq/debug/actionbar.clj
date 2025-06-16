(ns cdq.debug.actionbar)

(comment
 (keys (:entity/skills @(:world/player-eid (:ctx/world @cdq.application/state))))

 (remove-skill! (:action-bar (:ctx/stage @cdq.application/state))
                :skills/meditation))
