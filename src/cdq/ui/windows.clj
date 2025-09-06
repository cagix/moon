(ns cdq.ui.windows
  (:require [cdq.ctx :as ctx]
            [cdq.info :as info]
            [cdq.inventory :as inventory]
            cdq.ui.windows.entity-info
            cdq.ui.windows.inventory))

(def state->clicked-inventory-cell
  {:player-idle           (fn [eid cell]
                            ; TODO no else case
                            (when-let [item (get-in (:entity/inventory @eid) cell)]
                              [[:tx/sound "bfxr_takeit"]
                               [:tx/event eid :pickup-item item]
                               [:tx/remove-item eid cell]]))
   :player-item-on-cursor (fn [eid cell]
                            (let [entity @eid
                                  inventory (:entity/inventory entity)
                                  item-in-cell (get-in inventory cell)
                                  item-on-cursor (:entity/item-on-cursor entity)]
                              (cond
                               ; PUT ITEM IN EMPTY CELL
                               (and (not item-in-cell)
                                    (inventory/valid-slot? cell item-on-cursor))
                               [[:tx/sound "bfxr_itemput"]
                                [:tx/dissoc eid :entity/item-on-cursor]
                                [:tx/set-item eid cell item-on-cursor]
                                [:tx/event eid :dropped-item]]

                               ; STACK ITEMS
                               (and item-in-cell
                                    (inventory/stackable? item-in-cell item-on-cursor))
                               [[:tx/sound "bfxr_itemput"]
                                [:tx/dissoc eid :entity/item-on-cursor]
                                [:tx/stack-item eid cell item-on-cursor]
                                [:tx/event eid :dropped-item]]

                               ; SWAP ITEMS
                               (and item-in-cell
                                    (inventory/valid-slot? cell item-on-cursor))
                               [[:tx/sound "bfxr_itemput"]
                                ; need to dissoc and drop otherwise state enter does not trigger picking it up again
                                ; TODO? coud handle pickup-item from item-on-cursor state also
                                [:tx/dissoc eid :entity/item-on-cursor]
                                [:tx/remove-item eid cell]
                                [:tx/set-item eid cell item-on-cursor]
                                [:tx/event eid :dropped-item]
                                [:tx/event eid :pickup-item item-in-cell]])))})

(comment

 ; items then have 2x pretty-name
 #_(.setText (.getTitleLabel window)
             (info-text [:property/pretty-name (:property/pretty-name entity)])
             "Entity Info")
 )

(def disallowed-keys [:entity/skills
                      #_:entity/fsm
                      :entity/faction
                      :active-skill])

; TODO details how the text looks move to info
; only for :
; * skill
; * entity -> all sub-types
; * item
; => can test separately !?

(defn- ->label-text [entity ctx]
  ; don't use select-keys as it loses Entity record type
  (info/info-text ctx (apply dissoc entity disallowed-keys)))

(defn create [ctx _]
  {:actor/type :actor.type/group
   :id :windows
   :actors [(cdq.ui.windows.entity-info/create ctx {:y 0
                                                    :->label-text ->label-text
                                                    }) ; graphics only
            (cdq.ui.windows.inventory/create ctx ; graphics only
                                             {:title "Inventory"
                                              :id :inventory-window
                                              :visible? false
                                              :clicked-cell-fn (fn [cell]
                                                                 (fn [{:keys [ctx/player-eid] :as ctx}]
                                                                   (ctx/handle-txs!
                                                                    ctx
                                                                    (when-let [f (state->clicked-inventory-cell (:state (:entity/fsm @player-eid)))]
                                                                      (f player-eid cell)))))})]})
