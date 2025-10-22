(ns cdq.entity.skills.skill)

; ??
(defprotocol Skill
  (usable-state [_ entity effect-ctx]))
