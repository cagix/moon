(ns cdq.application.create.init-entity-states
  (:require [cdq.entity.state :as state])
  (:import (clojure.lang APersistentVector)))

(defn do! [ctx]
  ctx)

(def ^:private fn->k->var
  '{
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

    :cursor {:active-skill :cursors/sandclock
             :player-dead :cursors/black-x
             :player-idle cdq.entity.state.player-idle/cursor
             :player-item-on-cursor :cursors/hand-grab
             :player-moving :cursors/walking
             :stunned :cursors/denied}

    :handle-input {:player-idle           cdq.entity.state.player-idle/handle-input
                   :player-item-on-cursor cdq.entity.state.player-item-on-cursor/handle-input
                   :player-moving         cdq.entity.state.player-moving/handle-input}

    :clicked-inventory-cell {:player-idle           cdq.entity.state.player-idle/clicked-inventory-cell
                             :player-item-on-cursor cdq.entity.state.player-item-on-cursor/clicked-inventory-cell}

    :draw-gui-view {:player-item-on-cursor cdq.entity.state.player-item-on-cursor/draw-gui-view}
    })

(alter-var-root #'fn->k->var update-vals
                (fn [k->fns]
                  (update-vals k->fns (fn [sym?]
                                        (if (symbol? sym?)
                                          (let [avar (requiring-resolve sym?)]
                                            (assert avar)
                                            avar)
                                          (do
                                           (assert (keyword? sym?))
                                           sym?))))))

(extend APersistentVector
  state/State
  {:create (fn [[k v] eid ctx]
             (if-let [f (k (:create fn->k->var))]
               (f eid v ctx)
               v))

   :handle-input (fn [[k v] eid ctx]
                   (if-let [f (k (:handle-input fn->k->var))]
                     (f eid ctx)
                     nil))

   :cursor (fn [[k v] eid ctx]
             (let [->cursor (k (:cursor fn->k->var))]
               (if (keyword? ->cursor)
                 ->cursor
                 (->cursor eid ctx))))

   :enter (fn [[k v] eid]
            (when-let [f (k (:enter fn->k->var))]
              (f v eid)))

   :exit (fn [[k v] eid ctx]
           (when-let [f (k (:exit fn->k->var))]
             (f v eid ctx)))

   :clicked-inventory-cell (fn [[k v] eid cell]
                             (when-let [f (k (:clicked-inventory-cell fn->k->var))]
                               (f eid cell)))

   :draw-gui-view (fn [[k] eid ctx]
                    (when-let [f (k (:draw-gui-view fn->k->var))]
                      (f eid ctx)))})
