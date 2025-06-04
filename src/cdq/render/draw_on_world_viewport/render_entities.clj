(ns cdq.render.draw-on-world-viewport.render-entities
  (:require [cdq.ctx :as ctx]
            [cdq.effect :as effect]
            [cdq.entity :as entity]
            [cdq.entity.state.player-item-on-cursor]
            [cdq.timer :as timer]
            [cdq.utils :as utils]
            [cdq.val-max :as val-max]))

(defmulti  render-below! (fn [[k] entity ctx] k))
(defmethod render-below! :default [_ _entity ctx])

(defmulti  render-default! (fn [[k] entity ctx] k))
(defmethod render-default! :default [_ _entity ctx])

(defmulti  render-above! (fn [[k] entity ctx] k))
(defmethod render-above! :default [_ _entity ctx])

(defmulti  render-info! (fn [[k] entity ctx] k))
(defmethod render-info! :default [_ _entity ctx])

(defn- draw-skill-image [image entity [x y] action-counter-ratio]
  (let [[width height] (:sprite/world-unit-dimensions image)
        _ (assert (= width height))
        radius (/ (float width) 2)
        y (+ (float y) (float (:half-height entity)) (float 0.15))
        center [x (+ y radius)]]
    [[:draw/filled-circle center radius [1 1 1 0.125]]
     [:draw/sector
      center
      radius
      90 ; start-angle
      (* (float action-counter-ratio) 360) ; degree
      [1 1 1 0.5]]
     [:draw/image image [(- (float x) radius) y]]]))

(def ^:private hpbar-colors
  {:green     [0 0.8 0]
   :darkgreen [0 0.5 0]
   :yellow    [0.5 0.5 0]
   :red       [0.5 0 0]})

(defn- hpbar-color [ratio]
  (let [ratio (float ratio)
        color (cond
               (> ratio 0.75) :green
               (> ratio 0.5)  :darkgreen
               (> ratio 0.25) :yellow
               :else          :red)]
    (color hpbar-colors)))

(def ^:private borders-px 1)

(defn- draw-hpbar [{:keys [ctx/world-unit-scale]}
                   {:keys [width half-width half-height]
                    :as entity}
                   ratio]
  (let [[x y] (entity/position entity)]
    (let [x (- x half-width)
          y (+ y half-height)
          height (* 5          world-unit-scale)
          border (* borders-px world-unit-scale)]
      [[:draw/filled-rectangle x y width height :black]
       [:draw/filled-rectangle
        (+ x border)
        (+ y border)
        (- (* width ratio) (* 2 border))
        (- height          (* 2 border))
        (hpbar-color ratio)]])))

(def ^:private outline-alpha 0.4)
(def ^:private enemy-color    [1 0 0 outline-alpha])
(def ^:private friendly-color [0 1 0 outline-alpha])
(def ^:private neutral-color  [1 1 1 outline-alpha])

(defmethod render-below! :entity/mouseover? [_
                                             entity
                                             {:keys [ctx/player-eid]}]
  (let [player @player-eid
        faction (entity/faction entity)]
    [[:draw/with-line-width 3
      [[:draw/ellipse
        (entity/position entity)
        (:half-width entity)
        (:half-height entity)
        (cond (= faction (entity/enemy player))
              enemy-color
              (= faction (entity/faction player))
              friendly-color
              :else
              neutral-color)]]]]))

(defmethod render-below! :stunned [_ entity _ctx]
  [[:draw/circle (entity/position entity) 0.5 [1 1 1 0.6]]])

(defmethod render-below! :player-item-on-cursor [[_ {:keys [item]}] entity ctx]
  (when (cdq.entity.state.player-item-on-cursor/world-item? ctx)
    [[:draw/centered
      (:entity/image item)
      (cdq.entity.state.player-item-on-cursor/item-place-position ctx entity)]]))

(defmethod render-default! :entity/clickable [[_ {:keys [text]}]
                                              {:keys [entity/mouseover?] :as entity}
                                              _ctx]
  (when (and mouseover? text)
    (let [[x y] (entity/position entity)]
      [[:draw/text {:text text
                    :x x
                    :y (+ y (:half-height entity))
                    :up? true}]])))

(defmethod render-default! :entity/image [[_ image] entity _ctx]
  [[:draw/rotated-centered
    image
    (or (:rotation-angle entity) 0)
    (entity/position entity)]])

(defmethod render-default! :entity/line-render
  [[_ {:keys [thick? end color]}]
   entity
   _ctx]
  (let [position (entity/position entity)]
    (if thick?
      [[:draw/with-line-width 4 [[:draw/line position end color]]]]
      [[:draw/line position end color]])))

(defmethod render-above! :npc-sleeping [_ entity _ctx]
  (let [[x y] (entity/position entity)]
    [[:draw/text {:text "zzz"
                  :x x
                  :y (+ y (:half-height entity))
                  :up? true}]]))

; TODO draw opacity as of counter ratio?
(defmethod render-above! :entity/temp-modifier [_ entity _ctx]
  [[:draw/filled-circle (entity/position entity) 0.5 [0.5 0.5 0.5 0.4]]])

(defmethod render-above! :entity/string-effect [[_ {:keys [text]}]
                                                entity
                                                {:keys [ctx/world-unit-scale]}]
  (let [[x y] (entity/position entity)]
    [[:draw/text {:text text
                  :x x
                  :y (+ y
                        (:half-height entity)
                        (* 5 world-unit-scale))
                  :scale 2
                  :up? true}]]))

(defmethod render-info! :creature/stats [_ entity c]
  (let [ratio (val-max/ratio (entity/hitpoints entity))]
    (when (or (< ratio 1) (:entity/mouseover? entity))
      (draw-hpbar c entity ratio))))

(defn- render-active-effect [ctx effect-ctx effect]
  (mapcat #(effect/render % effect-ctx ctx) effect))

(defmethod render-info! :active-skill [[_ {:keys [skill effect-ctx counter]}]
                                       entity
                                       {:keys [ctx/elapsed-time] :as ctx}]
  (let [{:keys [entity/image skill/effects]} skill]
    (concat (draw-skill-image image
                              entity
                              (entity/position entity)
                              (timer/ratio elapsed-time counter))
            (render-active-effect ctx
                                  effect-ctx ; TODO !!!
                                  ; !! FIXME !!
                                  ; (update-effect-ctx effect-ctx)
                                  ; - render does not need to update .. update inside active-skill
                                  effects))))

(def ^:dbg-flag show-body-bounds? false)

(defn- draw-body-rect [entity color]
  (let [[x y] (:left-bottom entity)]
    [[:draw/rectangle x y (:width entity) (:height entity) color]]))

(defn do! [{:keys [ctx/active-entities
                   ctx/player-eid
                   ctx/render-z-order]
            :as ctx}]
  (let [entities (map deref active-entities)
        player @player-eid]
    (doseq [[z-order entities] (utils/sort-by-order (group-by :z-order entities)
                                                    first
                                                    render-z-order)
            render! [#'render-below!
                     #'render-default!
                     #'render-above!
                     #'render-info!]
            entity entities
            :when (or (= z-order :z-order/effect)
                      (ctx/line-of-sight? ctx player entity))]
      (try
       (when show-body-bounds?
         (ctx/handle-draws! ctx (draw-body-rect entity (if (:collides? entity) :white :gray))))
       (doseq [component entity]
         (ctx/handle-draws! ctx (render! component entity ctx)))
       (catch Throwable t
         (ctx/handle-draws! ctx (draw-body-rect entity :red))
         (utils/pretty-pst t))))))
