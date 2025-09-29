(ns cdq.application.create.stage.inventory
  (:require [cdq.ctx :as ctx]
            [cdq.graphics :as graphics]
            [cdq.entity.inventory :as inventory]
            [cdq.entity.state :as state]
            [cdq.ui.inventory]
            [clojure.scene2d :as scene2d]
            [clojure.scene2d.actor :as actor]
            [clojure.scene2d.event :as event]
            [clojure.scene2d.group :as group]
            [com.badlogic.gdx.scenes.scene2d.stage :as stage]
            [clojure.scene2d.ui.image :as image]
            [clojure.scene2d.utils.drawable :as drawable]
            [clojure.scene2d.utils.listener :as listener]))

(defn- create*
  [stage
   {:keys [title
           actor/visible?
           clicked-cell-listener
           slot->texture-region]}]
  (let [cell-size 48
        slot->drawable (fn [slot]
                         (drawable/create (slot->texture-region slot)
                                         :width cell-size
                                         :height cell-size
                                         :tint-color [1 1 1 0.4]))
        droppable-color   [0   0.6 0 0.8 1]
        not-allowed-color [0.6 0   0 0.8 1]
        draw-cell-rect (fn [player-entity x y mouseover? cell]
                         [[:draw/rectangle x y cell-size cell-size [0.5 0.5 0.5 1]]
                          (when (and mouseover?
                                     (= :player-item-on-cursor (:state (:entity/fsm player-entity))))
                            (let [item (:entity/item-on-cursor player-entity)
                                  color (if (inventory/valid-slot? cell item)
                                          droppable-color
                                          not-allowed-color)]
                              [:draw/filled-rectangle (inc x) (inc y) (- cell-size 2) (- cell-size 2) color]))])
        draw-rect-actor (fn []
                          {:actor/type :actor.type/widget
                           :draw (fn [actor {:keys [ctx/graphics
                                                    ctx/world]}]
                                   (draw-cell-rect @(:world/player-eid world)
                                                   (actor/get-x actor)
                                                   (actor/get-y actor)
                                                   (actor/hit actor
                                                              (actor/stage->local-coordinates actor (:graphics/ui-mouse-position graphics)))
                                                   (actor/user-object (actor/parent actor))))})
        ->cell (fn [slot & {:keys [position]}]
                 (let [cell [slot (or position [0 0])]
                       background-drawable (slot->drawable slot)]
                   {:actor {:actor/type :actor.type/stack
                            :actor/name "inventory-cell"
                            :actor/user-object cell
                            :actor/listener (clicked-cell-listener cell)
                            :group/actors [(draw-rect-actor)
                                           {:actor/type :actor.type/image
                                            :image/object background-drawable
                                            :actor/name "image-widget"
                                            :actor/user-object {:background-drawable background-drawable
                                                                :cell-size cell-size}}]}}))]
    (scene2d/build
     {:actor/type :actor.type/window
      :title title
      :actor/name "cdq.ui.windows.inventory"
      :actor/visible? visible?
      :pack? true
      :actor/position [(:viewport/width  (stage/viewport stage))
                       (:viewport/height (stage/viewport stage))]
      :rows [[{:actor {:actor/name "inventory-cell-table"
                       :actor/type :actor.type/table
                       :rows (concat [[nil nil
                                       (->cell :inventory.slot/helm)
                                       (->cell :inventory.slot/necklace)]
                                      [nil
                                       (->cell :inventory.slot/weapon)
                                       (->cell :inventory.slot/chest)
                                       (->cell :inventory.slot/cloak)
                                       (->cell :inventory.slot/shield)]
                                      [nil nil
                                       (->cell :inventory.slot/leg)]
                                      [nil
                                       (->cell :inventory.slot/glove)
                                       (->cell :inventory.slot/rings :position [0 0])
                                       (->cell :inventory.slot/rings :position [1 0])
                                       (->cell :inventory.slot/boot)]]
                                     (for [y (range 4)]
                                       (for [x (range 6)]
                                         (->cell :inventory.slot/bag :position [x y]))))}
               :pad 4}]]})))

(defn- find-cell [group cell]
  (first (filter #(= (actor/user-object % ) cell)
                 (group/children group))))

(defn- window->cell [inventory-window cell]
  (-> inventory-window
      (group/find-actor "inventory-cell-table")
      (find-cell cell)))

(extend-type com.badlogic.gdx.scenes.scene2d.ui.Window
  cdq.ui.inventory/Inventory
  (set-item! [inventory-window cell {:keys [texture-region tooltip-text]}]
    (let [cell-widget (window->cell inventory-window cell)
          image-widget (group/find-actor cell-widget "image-widget")
          cell-size (:cell-size (actor/user-object image-widget))
          drawable (drawable/create texture-region :width cell-size :height cell-size)]
      (image/set-drawable! image-widget drawable)
      (actor/add-tooltip! cell-widget tooltip-text)))

  (remove-item! [inventory-window cell]
    (let [cell-widget (window->cell inventory-window cell)
          image-widget (group/find-actor cell-widget "image-widget")]
      (image/set-drawable! image-widget (:background-drawable (actor/user-object image-widget)))
      (actor/remove-tooltip! cell-widget)))

  (cell-with-item? [_ actor]
    (and (actor/parent actor)
         (= "inventory-cell" (actor/get-name (actor/parent actor)))
         (actor/user-object (actor/parent actor)))))

(defn create
  [stage graphics]
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
                                             :bag      10}
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
                                                           :image/bounds bounds})))]
    (create* stage
             {:title "Inventory"
              :actor/visible? false
              :clicked-cell-listener (fn [cell]
                                       (listener/click
                                        (fn [event _x _y]
                                          (let [{:keys [ctx/world] :as ctx} (stage/get-ctx (event/stage event))
                                                eid (:world/player-eid world)
                                                entity @eid
                                                state-k (:state (:entity/fsm entity))
                                                txs (state/clicked-inventory-cell [state-k (state-k entity)]
                                                                                  eid
                                                                                  cell)]
                                            (ctx/handle-txs! ctx txs)))))
              :slot->texture-region slot->texture-region})))
