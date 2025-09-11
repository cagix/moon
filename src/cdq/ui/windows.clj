(ns cdq.ui.windows
  (:require [cdq.ctx :as ctx]
            [cdq.gdx.graphics :as graphics]
            [cdq.info :as info]
            cdq.ui.windows.entity-info
            cdq.ui.windows.inventory))

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
  (info/generate (:ctx/info ctx) (apply dissoc entity disallowed-keys) ctx))

(defn create-inventory
  [{:keys [ctx/graphics]
    :as ctx}]
  (cdq.ui.windows.inventory/create
   ctx
   (let [slot->y-sprite-idx #:inventory.slot {:weapon   0
                                              :shield   1
                                              :rings    2
                                              :necklace 3
                                              :helm     4
                                              :cloak    5
                                              :chest    6
                                              :leg      7
                                              :glove    8
                                              :boot     9
                                              :bag      10} ; transparent
         slot->texture-region (fn [slot]
                                (let [width  48
                                      height 48
                                      sprite-x 21
                                      sprite-y (+ (slot->y-sprite-idx slot) 2)
                                      bounds [(* sprite-x width)
                                              (* sprite-y height)
                                              width
                                              height]]
                                  (graphics/texture-region graphics
                                                           {:image/file "images/items.png"
                                                            :image/bounds bounds})))
         ]
     {:title "Inventory"
      :id :inventory-window
      :visible? false
      :clicked-cell-fn (fn [cell]
                         (fn [{:keys [ctx/entity-states
                                      ctx/player-eid] :as ctx}]
                           (ctx/handle-txs!
                            ctx
                            (when-let [f ((:clicked-inventory-cell entity-states) (:state (:entity/fsm @player-eid)))]
                              (f player-eid cell)))))
      :slot->texture-region slot->texture-region
      })))

(defn create [ctx _]
  {:actor/type :actor.type/group
   :id :windows
   :actors [(cdq.ui.windows.entity-info/create ctx {:y 0
                                                    :->label-text ->label-text
                                                    }) ; graphics only
            (create-inventory ctx)]})
