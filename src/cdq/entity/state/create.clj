(ns cdq.entity.state.create
  (:require [cdq.effects]))

(def function-map
  (cdq.effects/walk-method-map
   '{:active-skill cdq.entity.state.active-skill/create
     :npc-moving cdq.entity.state.npc-moving/create
     :player-item-on-cursor cdq.entity.state.player-item-on-cursor/create
     :player-moving cdq.entity.state.player-moving/create
     :stunned cdq.entity.state.stunned/create}))
