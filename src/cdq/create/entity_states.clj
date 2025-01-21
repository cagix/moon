(ns cdq.create.entity-states)

(defn create [_context]
  {:active-skill          {:cursor :cursors/sandclock}
   :player-dead           {:cursor :cursors/black-x}
   :player-item-on-cursor {:cursor :cursors/hand-grab}
   :player-moving         {:cursor :cursors/walking}
   :stunned               {:cursor :cursors/denied}})
