(ns cdq.entity.state
  (:require cdq.entity.state.active-skill
            cdq.entity.state.stunned
            cdq.entity.state.player-item-on-cursor
            cdq.entity.state.player-moving
            cdq.entity.state.player-dead
            cdq.entity.state.npc-moving
            cdq.entity.state.npc-dead
            cdq.entity.state.npc-sleeping))

(def ->create {:active-skill          cdq.entity.state.active-skill/create
               :npc-moving            cdq.entity.state.npc-moving/create
               :player-item-on-cursor cdq.entity.state.player-item-on-cursor/create
               :player-moving         cdq.entity.state.player-moving/create
               :stunned               cdq.entity.state.stunned/create})

(def ->enter {:npc-dead              cdq.entity.state.npc-dead/enter
              :npc-moving            cdq.entity.state.npc-moving/enter
              :player-dead           cdq.entity.state.player-dead/enter
              :player-item-on-cursor cdq.entity.state.player-item-on-cursor/enter
              :player-moving         cdq.entity.state.player-moving/enter
              :active-skill          cdq.entity.state.active-skill/enter})

(def ->exit {:npc-moving            cdq.entity.state.npc-moving/exit
             :npc-sleeping          cdq.entity.state.npc-sleeping/exit
             :player-item-on-cursor cdq.entity.state.player-item-on-cursor/exit
             :player-moving         cdq.entity.state.player-moving/exit})
