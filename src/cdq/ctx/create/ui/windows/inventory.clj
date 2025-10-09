(ns cdq.ctx.create.ui.windows.inventory
  (:require [clojure.txs :as txs]
            [cdq.entity.state :as state]
            [cdq.entity.inventory :as inventory]
            [cdq.graphics :as graphics]
            [cdq.graphics.textures :as textures]
            [cdq.ui :as ui]
            [clojure.scene2d :as scene2d]
            [clojure.gdx.math.vector2 :as vector2])
  (:import (com.badlogic.gdx.graphics Color)
           (com.badlogic.gdx.scenes.scene2d Actor)
           (com.badlogic.gdx.scenes.scene2d.ui Widget)
           (com.badlogic.gdx.scenes.scene2d.utils ClickListener
                                                  TextureRegionDrawable)))

(defn- draw-cell-rect-actor [draw-cell-rect]
  (proxy [Widget] []
    (draw [_batch _parent-alpha]
      (when-let [stage (.getStage this)]
        (let [{:keys [ctx/graphics
                      ctx/world]} (.ctx stage)]
          (graphics/draw! graphics
                          (let [ui-mouse (:graphics/ui-mouse-position graphics)]
                            (draw-cell-rect @(:world/player-eid world)
                                            (Actor/.getX this)
                                            (Actor/.getY this)
                                            (let [[x y] (-> this
                                                            (Actor/.stageToLocalCoordinates (vector2/->java ui-mouse))
                                                            vector2/->clj)]
                                              (Actor/.hit this x y true))
                                            (Actor/.getUserObject (Actor/.getParent this))))))))))

(defn- create-inventory-window*
  [{:keys [position
           title
           actor/visible?
           clicked-cell-listener
           slot->texture-region]}]
  (let [cell-size 48
        slot->drawable (fn [slot]
                         (doto (TextureRegionDrawable. (slot->texture-region slot))
                           (.setMinSize (float cell-size) (float cell-size))
                           (.tint (Color. 1 1 1 0.4))))
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
        ->cell (fn [slot & {:keys [position]}]
                 (let [cell [slot (or position [0 0])]
                       background-drawable (slot->drawable slot)]
                   {:actor {:actor/type :actor.type/stack
                            :actor/name "inventory-cell"
                            :actor/user-object cell
                            :actor/listener (clicked-cell-listener cell)
                            :group/actors [(draw-cell-rect-actor draw-cell-rect)
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
      :actor/position position
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

(defn create
  [{:keys [ctx/graphics
           ctx/stage]}]
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
                                 (textures/texture-region graphics
                                                          {:image/file "images/items.png"
                                                           :image/bounds bounds})))]
    (create-inventory-window*
     {:title "Inventory"
      :actor/visible? false
      :position [(ui/viewport-width  stage)
                 (ui/viewport-height stage)]
      :clicked-cell-listener (fn [cell]
                               (proxy [ClickListener] []
                                 (clicked [event x y]
                                   (let [{:keys [ctx/world] :as ctx} (.ctx (.getStage event))
                                         eid (:world/player-eid world)
                                         entity @eid
                                         state-k (:state (:entity/fsm entity))
                                         txs (state/clicked-inventory-cell [state-k (state-k entity)]
                                                                           eid
                                                                           cell)]
                                     (txs/handle! ctx txs)))))
      :slot->texture-region slot->texture-region})))
