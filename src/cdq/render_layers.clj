(ns cdq.render-layers
  (:require [cdq.animation :as animation]
            [cdq.timer :as timer]
            [cdq.val-max :as val-max]
            [cdq.world.effect :as effect]
            [cdq.world.entity :as entity]
            [cdq.world.entity.stats :as modifiers]
            [cdq.world.entity.faction :as faction]
            cdq.entity.state.player-item-on-cursor))

(def ^:private skill-image-radius-world-units
  (let [tile-size 48
        image-width 32]
    (/ (/ image-width tile-size) 2)))

(def ^:private outline-alpha 0.4)
(def ^:private enemy-color    [1 0 0 outline-alpha])
(def ^:private friendly-color [0 1 0 outline-alpha])
(def ^:private neutral-color  [1 1 1 outline-alpha])

(def ^:private mouseover-ellipse-width 5)

(def ^:private stunned-circle-width 0.5)
(def ^:private stunned-circle-color [1 1 1 0.6])

(defn draw-item-on-cursor-state
  [{:keys [item]}
   entity
   {:keys [ctx/mouseover-actor
           ctx/world-mouse-position]}]
  (when (cdq.entity.state.player-item-on-cursor/world-item? mouseover-actor)
    [[:draw/centered
      (:entity/image item)
      (cdq.entity.state.player-item-on-cursor/item-place-position
       world-mouse-position
       entity)]]))

(defn draw-mouseover-highlighting [_ entity {:keys [ctx/player-eid]}]
  (let [player @player-eid
        faction (:entity/faction entity)]
    [[:draw/with-line-width mouseover-ellipse-width
      [[:draw/ellipse
        (entity/position entity)
        (/ (:body/width  (:entity/body entity)) 2)
        (/ (:body/height (:entity/body entity)) 2)
        (cond (= faction (faction/enemy (:entity/faction player)))
              enemy-color
              (= faction (:entity/faction player))
              friendly-color
              :else
              neutral-color)]]]]))

(defn draw-stunned-state [_ entity _ctx]
  [[:draw/circle
    (entity/position entity)
    stunned-circle-width
    stunned-circle-color]])

(defn draw-clickable-mouseover-text [{:keys [text]} {:keys [entity/mouseover?] :as entity} _ctx]
  (when (and mouseover? text)
    (let [[x y] (entity/position entity)]
      [[:draw/text {:text text
                    :x x
                    :y (+ y (/ (:body/height (:entity/body entity)) 2))
                    :up? true}]])))

(defn draw-skill-image [image entity [x y] action-counter-ratio]
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
     [:draw/image image [(- (float x) radius) y]]]))

(defn render-active-effect [ctx effect-ctx effect]
  (mapcat #(effect/render % effect-ctx ctx) effect))

(defn draw-active-skill [{:keys [skill effect-ctx counter]}
                         entity
                         {:keys [ctx/world] :as ctx}]
  (let [{:keys [entity/image skill/effects]} skill]
    (concat (draw-skill-image image
                              entity
                              (entity/position entity)
                              (timer/ratio (:world/elapsed-time world) counter))
            (render-active-effect ctx
                                  effect-ctx ; TODO !!!
                                  ; !! FIXME !!
                                  ; (update-effect-ctx effect-ctx)
                                  ; - render does not need to update .. update inside active-skill
                                  effects))))

(defn draw-centered-rotated-image [image entity _ctx]
  [[:draw/rotated-centered
    image
    (or (:body/rotation-angle (:entity/body entity)) 0)
    (entity/position entity)]])

(defn call-render-image [animation entity ctx]
  (draw-centered-rotated-image (animation/current-frame animation)
                               entity
                               ctx))

(defn draw-line-entity [{:keys [thick? end color]} entity _ctx]
  (let [position (entity/position entity)]
    (if thick?
      [[:draw/with-line-width 4 [[:draw/line position end color]]]]
      [[:draw/line position end color]])))

(defn draw-sleeping-state [_ entity _ctx]
  (let [[x y] (entity/position entity)]
    [[:draw/text {:text "zzz"
                  :x x
                  :y (+ y (/ (:body/height (:entity/body entity)) 2))
                  :up? true}]]))

; TODO draw opacity as of counter ratio?
(defn draw-temp-modifiers [_ entity _ctx]
  [[:draw/filled-circle (entity/position entity) 0.5 [0.5 0.5 0.5 0.4]]])

(defn draw-text-over-entity [{:keys [text]} entity {:keys [ctx/graphics]}]
  (let [[x y] (entity/position entity)]
    [[:draw/text {:text text
                  :x x
                  :y (+ y
                        (/ (:body/height (:entity/body entity)) 2)
                        (* 5 (:world-unit-scale graphics)))
                  :scale 2
                  :up? true}]]))

(def ^:private hpbar-colors
  {:green     [0 0.8 0 1]
   :darkgreen [0 0.5 0 1]
   :yellow    [0.5 0.5 0 1]
   :red       [0.5 0 0 1]})

(defn hpbar-color [ratio]
  (let [ratio (float ratio)
        color (cond
               (> ratio 0.75) :green
               (> ratio 0.5)  :darkgreen
               (> ratio 0.25) :yellow
               :else          :red)]
    (color hpbar-colors)))

(def ^:private borders-px 1)

(defn draw-hpbar [world-unit-scale {:keys [body/position body/width body/height]} ratio]
  (let [[x y] position]
    (let [x (- x (/ width  2))
          y (+ y (/ height 2))
          height (* 5          world-unit-scale)
          border (* borders-px world-unit-scale)]
      [[:draw/filled-rectangle x y width height :black]
       [:draw/filled-rectangle
        (+ x border)
        (+ y border)
        (- (* width ratio) (* 2 border))
        (- height          (* 2 border))
        (hpbar-color ratio)]])))

(defn draw-stats [_ entity {:keys [ctx/graphics]}]
  (let [ratio (val-max/ratio (modifiers/get-hitpoints (:creature/stats entity)))]
    (when (or (< ratio 1) (:entity/mouseover? entity))
      (draw-hpbar (:world-unit-scale graphics)
                  (:entity/body entity)
                  ratio))))
