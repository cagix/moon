(ns cdq.dev.actionbar)

(comment
 (keys (:entity/skills @(:ctx/player-eid @cdq.application/state)))

 (remove-skill! (:action-bar (:ctx/stage @cdq.application/state))
                :skills/meditation))
