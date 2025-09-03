(ns cdq.ui.windows
  (:require [cdq.ctx :as ctx]
            [cdq.info :as info]
            cdq.ui.windows.entity-info
            cdq.ui.windows.inventory))

(def state->clicked-inventory-cell)

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

(defn create [ctx]
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
