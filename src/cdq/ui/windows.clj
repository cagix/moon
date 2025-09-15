(ns cdq.ui.windows
  (:require [cdq.ctx :as ctx]
            [cdq.ctx.graphics :as graphics]
            [cdq.info :as info]
            cdq.ui.windows.entity-info
            cdq.ui.windows.inventory
            [clojure.gdx.scene2d.ctx-stage :as ctx-stage]
            [clojure.gdx.scene2d.utils.listener :as listener]
            [clojure.scene2d.event :as event]
            ))

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
      :actor/visible? false
      :clicked-cell-listener (fn [cell]
                               (listener/click
                                (fn [event _x _y]
                                  (let [{:keys [ctx/entity-states
                                                ctx/world] :as ctx} (ctx-stage/get-ctx (event/stage event))]
                                    (ctx/handle-txs!
                                     ctx
                                     (when-let [f ((:clicked-inventory-cell entity-states) (:state (:entity/fsm @(:world/player-eid world))))]
                                       (f (:world/player-eid world) cell)))))))
      :slot->texture-region slot->texture-region
      })))

(defn create [ctx _]
  {:actor/type :actor.type/group
   :actor/name "cdq.ui.windows"
   :group/actors [(cdq.ui.windows.entity-info/create ctx {:y 0
                                                          :->label-text ->label-text
                                                          }) ; graphics only
                  (create-inventory ctx)]})
