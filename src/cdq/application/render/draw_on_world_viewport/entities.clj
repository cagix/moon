(ns cdq.application.render.draw-on-world-viewport.entities
  (:require [cdq.animation :as animation]
            [cdq.ctx :as ctx]
            [cdq.effect :as effect]
            [cdq.entity :as entity]
            [cdq.entity.faction :as faction]
            [cdq.entity.state.player-item-on-cursor :as player-item-on-cursor]
            [cdq.graphics :as graphics]
            [cdq.stats :as stats]
            [cdq.timer :as timer]
            [cdq.val-max :as val-max]
            [cdq.world :as world]
            [clojure.utils :as utils]
            [gdl.graphics.color :as color]))

(def ^:private outline-alpha 0.4)
(def ^:private enemy-color    [1 0 0 outline-alpha])
(def ^:private friendly-color [0 1 0 outline-alpha])
(def ^:private neutral-color  [1 1 1 outline-alpha])
(def ^:private mouseover-ellipse-width 5)

(defn- draw-mouseover
  [_
   {:keys [entity/body
           entity/faction]}
   {:keys [ctx/world]}]
  (let [player @(:world/player-eid world)]
    [[:draw/with-line-width mouseover-ellipse-width
      [[:draw/ellipse
        (:body/position body)
        (/ (:body/width  body) 2)
        (/ (:body/height body) 2)
        (cond (= faction (faction/enemy (:entity/faction player)))
              enemy-color
              (= faction (:entity/faction player))
              friendly-color
              :else
              neutral-color)]]]]))

(def ^:private stunned-circle-width 0.5)
(def ^:private stunned-circle-color [1 1 1 0.6])

(defn- draw-stunned [_ {:keys [entity/body]} _ctx]
  [[:draw/circle
    (:body/position body)
    stunned-circle-width
    stunned-circle-color]])

(defn- draw-item-on-cursor
  [{:keys [item]}
   entity
   {:keys [ctx/graphics
           ctx/mouseover-actor
           ctx/world-mouse-position]}]
  (when (player-item-on-cursor/world-item? mouseover-actor)
    [[:draw/texture-region
      (graphics/texture-region graphics (:entity/image item))
      (player-item-on-cursor/item-place-position world-mouse-position entity)
      {:center? true}]]))

(defn- draw-clickable
  [{:keys [text]}
   {:keys [entity/body
           entity/mouseover?] }
   _ctx]
  (when (and mouseover? text)
    (let [[x y] (:body/position body)]
      [[:draw/text {:text text
                    :x x
                    :y (+ y (/ (:body/height body) 2))
                    :up? true}]])))

(defn- draw-image
  [image
   {:keys [entity/body]}
   {:keys [ctx/graphics]}]
  [[:draw/texture-region
    (graphics/texture-region graphics image)
    (:body/position body)
    {:center? true
     :rotation (or (:body/rotation-angle body)
                   0)}]])

(defn- draw-animation [animation entity ctx]
  (draw-image (animation/current-frame animation)
              entity
              ctx))

(defn- draw-line [{:keys [thick? end color]} {:keys [entity/body]} _ctx]
  (let [position (:body/position body)]
    (if thick?
      [[:draw/with-line-width 4 [[:draw/line position end color]]]]
      [[:draw/line position end color]])))

(defn- draw-sleeping [_ {:keys [entity/body]} _ctx]
  (let [[x y] (:body/position body)]
    [[:draw/text {:text "zzz"
                  :x x
                  :y (+ y (/ (:body/height body) 2))
                  :up? true}]]))

(defn- draw-temp-modifier [_ entity _ctx]
  [[:draw/filled-circle (entity/position entity) 0.5 [0.5 0.5 0.5 0.4]]])

(defn- draw-string-effect [{:keys [text]} entity {:keys [ctx/graphics]}]
  (let [[x y] (entity/position entity)]
    [[:draw/text {:text text
                  :x x
                  :y (+ y
                        (/ (:body/height (:entity/body entity)) 2)
                        (* 5 (:graphics/world-unit-scale graphics)))
                  :scale 2
                  :up? true}]]))

(def ^:private hpbar-colors
  {:green     [0 0.8 0 1]
   :darkgreen [0 0.5 0 1]
   :yellow    [0.5 0.5 0 1]
   :red       [0.5 0 0 1]})

(defn- hpbar-color [ratio]
  (let [ratio (float ratio)
        color (cond
               (> ratio 0.75) :green
               (> ratio 0.5)  :darkgreen
               (> ratio 0.25) :yellow
               :else          :red)]
    (color hpbar-colors)))

(def ^:private borders-px 1)

(defn- draw-hpbar [world-unit-scale {:keys [body/position body/width body/height]} ratio]
  (let [[x y] position]
    (let [x (- x (/ width  2))
          y (+ y (/ height 2))
          height (* 5          world-unit-scale)
          border (* borders-px world-unit-scale)]
      [[:draw/filled-rectangle x y width height color/black]
       [:draw/filled-rectangle
        (+ x border)
        (+ y border)
        (- (* width ratio) (* 2 border))
        (- height          (* 2 border))
        (hpbar-color ratio)]])))

(defn- draw-hp-bar [_ entity {:keys [ctx/graphics]}]
  (let [ratio (val-max/ratio (stats/get-hitpoints (:creature/stats entity)))]
    (when (or (< ratio 1) (:entity/mouseover? entity))
      (draw-hpbar (:graphics/world-unit-scale graphics)
                  (:entity/body entity)
                  ratio))))

(def ^:private skill-image-radius-world-units
  (let [tile-size 48
        image-width 32]
    (/ (/ image-width tile-size) 2)))

(defn- render-active-effect [ctx effect-ctx effect]
  (mapcat #(effect/render % effect-ctx ctx) effect))

(defn- draw-skill-image
  [texture-region entity [x y] action-counter-ratio]
  (let [radius skill-image-radius-world-units
        y (+ (float y)
             (float (/ (:body/height (:entity/body entity)) 2))
             (float 0.15))
        center [x (+ y radius)]]
    [[:draw/filled-circle center radius [1 1 1 0.125]]
     [:draw/sector
      center
      radius
      90 ; start-angle
      (* (float action-counter-ratio) 360) ; degree
      [1 1 1 0.5]]
     [:draw/texture-region texture-region [(- (float x) radius) y]]]))

(defn- draw-active-skill
  [{:keys [skill effect-ctx counter]}
   entity
   {:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (let [{:keys [entity/image skill/effects]} skill]
    (concat (draw-skill-image (graphics/texture-region graphics image)
                              entity
                              (entity/position entity)
                              (timer/ratio (:world/elapsed-time world) counter))
            (render-active-effect ctx
                                  effect-ctx ; TODO !!!
                                  ; !! FIXME !!
                                  ; (update-effect-ctx effect-ctx)
                                  ; - render does not need to update .. update inside active-skill
                                  effects))))

(def ^:private render-layers
  [{:entity/mouseover?     draw-mouseover
    :stunned               draw-stunned
    :player-item-on-cursor draw-item-on-cursor}
   {:entity/clickable      draw-clickable
    :entity/animation      draw-animation
    :entity/image          draw-image
    :entity/line-render    draw-line}
   {:npc-sleeping          draw-sleeping
    :entity/temp-modifier  draw-temp-modifier
    :entity/string-effect  draw-string-effect}
   {:creature/stats        draw-hp-bar
    :active-skill          draw-active-skill}])

(def ^:dbg-flag show-body-bounds? false)

(defn- draw-body-rect [{:keys [body/position body/width body/height]} color]
  (let [[x y] [(- (position 0) (/ width  2))
               (- (position 1) (/ height 2))]]
    [[:draw/rectangle x y width height color]]))

(defn- draw-entity
  [{:keys [ctx/graphics]
    :as ctx}
   entity render-layer]
  (try (do
        (when show-body-bounds?
          (graphics/handle-draws! graphics (draw-body-rect (:entity/body entity)
                                                           (if (:body/collides? (:entity/body entity))
                                                             color/white
                                                             color/gray))))
        (doseq [[k v] entity
                :let [draw-fn (get render-layer k)]
                :when draw-fn]
          (graphics/handle-draws! graphics (draw-fn v entity ctx))))
       (catch Throwable t
         (graphics/handle-draws! graphics (draw-body-rect (:entity/body entity) color/red))
         (ctx/handle-txs! ctx
                          [[:tx/print-stacktrace t]]))))

(defn do!
  [{:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (let [entities (map deref (world/active-eids world))
        player @(:world/player-eid world)
        should-draw? (fn [entity z-order]
                       (or (= z-order :z-order/effect)
                           (world/line-of-sight? world player entity)))]
    (doseq [[z-order entities] (utils/sort-by-order (group-by (comp :body/z-order :entity/body) entities)
                                                    first
                                                    (:world/render-z-order world))
            render-layer render-layers
            entity entities
            :when (should-draw? entity z-order)]
      (draw-entity ctx entity render-layer))))
