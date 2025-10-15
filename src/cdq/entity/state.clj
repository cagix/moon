(ns cdq.entity.state
  (:require
   cdq.entity.state.active-skill
   cdq.entity.state.npc-dead
   cdq.entity.state.npc-moving
   cdq.entity.state.npc-sleeping
   cdq.entity.state.player-dead
   cdq.entity.state.player-item-on-cursor
   cdq.entity.state.player-moving
   cdq.entity.state.stunned
   cdq.entity.state.player-idle.clicked-inventory-cell
   cdq.entity.state.player-item-on-cursor.clicked-inventory-cell
   )
  )

(defprotocol State
  (create       [_ eid world])
  (enter        [_ eid])
  (exit         [_ eid ctx])
  (clicked-inventory-cell [_ eid cell]))

(def ^:private fn->k->var
  {
   :create {:active-skill          cdq.entity.state.active-skill/create
            :npc-moving            cdq.entity.state.npc-moving/create
            :player-item-on-cursor cdq.entity.state.player-item-on-cursor/create
            :player-moving         cdq.entity.state.player-moving/create
            :stunned               cdq.entity.state.stunned/create}

   :enter {:npc-dead              cdq.entity.state.npc-dead/enter
           :npc-moving            cdq.entity.state.npc-moving/enter
           :player-dead           cdq.entity.state.player-dead/enter
           :player-item-on-cursor cdq.entity.state.player-item-on-cursor/enter
           :player-moving         cdq.entity.state.player-moving/enter
           :active-skill          cdq.entity.state.active-skill/enter}

   :exit {:npc-moving            cdq.entity.state.npc-moving/exit
          :npc-sleeping          cdq.entity.state.npc-sleeping/exit
          :player-item-on-cursor cdq.entity.state.player-item-on-cursor/exit
          :player-moving         cdq.entity.state.player-moving/exit}

   :clicked-inventory-cell {:player-idle           cdq.entity.state.player-idle.clicked-inventory-cell/txs
                            :player-item-on-cursor cdq.entity.state.player-item-on-cursor.clicked-inventory-cell/txs}
   })

(extend clojure.lang.APersistentVector
  State
  {:create (fn [[k v] eid ctx]
             (if-let [f (k (:create fn->k->var))]
               (f eid v ctx)
               v))

   :enter (fn [[k v] eid]
            (when-let [f (k (:enter fn->k->var))]
              (f v eid)))

   :exit (fn [[k v] eid ctx]
           (when-let [f (k (:exit fn->k->var))]
             (f v eid ctx)))

   :clicked-inventory-cell (fn [[k v] eid cell]
                             (when-let [f (k (:clicked-inventory-cell fn->k->var))]
                               (f eid cell)))})
