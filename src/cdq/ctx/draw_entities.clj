(ns cdq.ctx.draw-entities
  (:require cdq.entity.state.active-skill.draw
            [cdq.entity.animation :as animation]
            [cdq.entity.faction :as faction]
            [cdq.entity.state.player-item-on-cursor]
            [cdq.entity.stats :as stats]
            [cdq.graphics :as graphics]
            [cdq.graphics.draws :as draws]
            [cdq.input :as input]
            [cdq.stage :as stage]
            [cdq.throwable :as throwable]
            [cdq.world.raycaster :as raycaster]
            [cdq.val-max :as val-max]
            [clojure.graphics.color :as color]
            [gdl.utils :as utils]))

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

(let [outline-alpha 0.4
      enemy-color    [1 0 0 outline-alpha]
      friendly-color [0 1 0 outline-alpha]
      neutral-color  [1 1 1 outline-alpha]
      mouseover-ellipse-width 5
      stunned-circle-width 0.5
      stunned-circle-color [1 1 1 0.6]
      draw-image (fn
                   [image
                    {:keys [entity/body]}
                    {:keys [ctx/graphics]}]
                   [[:draw/texture-region
                     (graphics/texture-region graphics image)
                     (:body/position body)
                     {:center? true
                      :rotation (or (:body/rotation-angle body)
                                    0)}]])
      ]
  (def ^:private render-layers
    [{:entity/mouseover?     (fn
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
      :stunned               (fn [_ {:keys [entity/body]} _ctx]
                               [[:draw/circle
                                 (:body/position body)
                                 stunned-circle-width
                                 stunned-circle-color]])
      :player-item-on-cursor (fn
                               [{:keys [item]}
                                entity
                                {:keys [ctx/graphics
                                        ctx/input
                                        ctx/stage]}]
                               (when (cdq.entity.state.player-item-on-cursor/world-item? (stage/mouseover-actor stage (input/mouse-position input)))
                                 [[:draw/texture-region
                                   (graphics/texture-region graphics (:entity/image item))
                                   (cdq.entity.state.player-item-on-cursor/item-place-position (:graphics/world-mouse-position graphics)
                                                                                               entity)
                                   {:center? true}]]))}
     {:entity/clickable      (fn
                               [{:keys [text]}
                                {:keys [entity/body
                                        entity/mouseover?]}
                                _ctx]
                               (when (and mouseover? text)
                                 (let [[x y] (:body/position body)]
                                   [[:draw/text {:text text
                                                 :x x
                                                 :y (+ y (/ (:body/height body) 2))
                                                 :up? true}]])))
      :entity/animation      (fn [animation entity ctx]
                               (draw-image (animation/current-frame animation) entity ctx))
      :entity/image          draw-image
      :entity/line-render    (fn [{:keys [thick? end color]} {:keys [entity/body]} _ctx]
                               (let [position (:body/position body)]
                                 (if thick?
                                   [[:draw/with-line-width 4 [[:draw/line position end color]]]]
                                   [[:draw/line position end color]])))}
     {:npc-sleeping          (fn [_ {:keys [entity/body]} _ctx]
                               (let [[x y] (:body/position body)]
                                 [[:draw/text {:text "zzz"
                                               :x x
                                               :y (+ y (/ (:body/height body) 2))
                                               :up? true}]]))
      :entity/temp-modifier  (fn [_ entity _ctx]
                               [[:draw/filled-circle (:body/position (:entity/body entity)) 0.5 [0.5 0.5 0.5 0.4]]])
      :entity/string-effect  (fn [{:keys [text]} entity {:keys [ctx/graphics]}]
                               (let [[x y] (:body/position (:entity/body entity))]
                                 [[:draw/text {:text text
                                               :x x
                                               :y (+ y
                                                     (/ (:body/height (:entity/body entity)) 2)
                                                     (* 5 (:graphics/world-unit-scale graphics)))
                                               :scale 2
                                               :up? true}]]))}
     {:entity/stats        (fn [_ entity {:keys [ctx/graphics]}]
                             (let [ratio (val-max/ratio (stats/get-hitpoints (:entity/stats entity)))]
                               (when (or (< ratio 1) (:entity/mouseover? entity))
                                 (draw-hpbar (:graphics/world-unit-scale graphics)
                                             (:entity/body entity)
                                             ratio))))
      :active-skill          cdq.entity.state.active-skill.draw/txs}]))

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
          (draws/handle! graphics (draw-body-rect (:entity/body entity)
                                                  (if (:body/collides? (:entity/body entity))
                                                    color/white
                                                    color/gray))))
        (doseq [[k v] entity
                :let [draw-fn (get render-layer k)]
                :when draw-fn]
          (draws/handle! graphics (draw-fn v entity ctx))))
       (catch Throwable t
         (draws/handle! graphics (draw-body-rect (:entity/body entity) color/red))
         (throwable/pretty-pst t))))

(defn do!
  [{:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (let [entities (map deref (:world/active-entities world))
        player @(:world/player-eid world)
        should-draw? (fn [entity z-order]
                       (or (= z-order :z-order/effect)
                           (raycaster/line-of-sight? world player entity)))]
    (doseq [[z-order entities] (utils/sort-by-order (group-by (comp :body/z-order :entity/body) entities)
                                                    first
                                                    (:world/render-z-order world))
            render-layer render-layers
            entity entities
            :when (should-draw? entity z-order)]
      (draw-entity ctx entity render-layer))))
