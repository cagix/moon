(ns cdq.graphics
  (:require [gdl.context :as c]
            [gdl.context.timer :as timer]
            [gdl.error :refer [pretty-pst]]
            [gdl.utils :refer [defsystem sort-by-order]]
            [gdl.val-max :as val-max]
            [cdq.context :refer [active-entities
                                 render-z-order
                                 line-of-sight?
                                 draw-body-rect
                                 render-before-entities
                                 render-after-entities
                                 creatures-in-los-of-player
                                 world-item?
                                 item-place-position]]
            [cdq.entity :as entity]))

(defn- draw-skill-image [c image entity [x y] action-counter-ratio]
  (let [[width height] (:world-unit-dimensions image)
        _ (assert (= width height))
        radius (/ (float width) 2)
        y (+ (float y) (float (:half-height entity)) (float 0.15))
        center [x (+ y radius)]]
    (c/filled-circle c center radius [1 1 1 0.125])
    (c/sector c
              center
              radius
              90 ; start-angle
              (* (float action-counter-ratio) 360) ; degree
              [1 1 1 0.5])
    (c/draw-image c image [(- (float x) radius) y])))

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

(defn- draw-hpbar [c
                   {:keys [position width half-width half-height]}
                   ratio]
  (let [[x y] position]
    (let [x (- x half-width)
          y (+ y half-height)
          height (c/pixels->world-units c 5)
          border (c/pixels->world-units c borders-px)]
      (c/filled-rectangle c x y width height :black)
      (c/filled-rectangle c
                          (+ x border)
                          (+ y border)
                          (- (* width ratio) (* 2 border))
                          (- height          (* 2 border))
                          (hpbar-color ratio)))))

(defsystem below)
(defmethod below :default [_ entity c])

(defsystem default)
(defmethod default :default [_ entity c])

(defsystem above)
(defmethod above :default [_ entity c])

(defsystem info)
(defmethod info :default [_ entity c])

(defmethod default :entity/clickable
  [[_ {:keys [text]}] {:keys [entity/mouseover?] :as entity} c]
  (when (and mouseover? text)
    (let [[x y] (:position entity)]
      (c/draw-text c
                   {:text text
                    :x x
                    :y (+ y (:half-height entity))
                    :up? true}))))

(defmethod info :entity/hp
  [ _ entity c]
  (let [ratio (val-max/ratio (entity/hitpoints entity))]
    (when (or (< ratio 1) (:entity/mouseover? entity))
      (draw-hpbar c entity ratio))))

(defmethod default :entity/image
  [[_ image] entity c]
  (c/draw-rotated-centered c
                           image
                           (or (:rotation-angle entity) 0)
                           (:position entity)))

(defmethod default :entity/line-render
  [[_ {:keys [thick? end color]}] entity c]
  (let [position (:position entity)]
    (if thick?
      (c/with-line-width c 4
        #(c/line c position end color))
      (c/line c position end color))))

(def ^:private outline-alpha 0.4)
(def ^:private enemy-color    [1 0 0 outline-alpha])
(def ^:private friendly-color [0 1 0 outline-alpha])
(def ^:private neutral-color  [1 1 1 outline-alpha])

(defmethod below :entity/mouseover?
  [_
   {:keys [entity/faction] :as entity}
   {:keys [cdq.context/player-eid] :as c}]
  (let [player @player-eid]
    (c/with-line-width c 3
      #(c/ellipse c
                  (:position entity)
                  (:half-width entity)
                  (:half-height entity)
                  (cond (= faction (entity/enemy player))
                        enemy-color
                        (= faction (:entity/faction player))
                        friendly-color
                        :else
                        neutral-color)))))

(defsystem render-effect)
(defmethod render-effect :default [_ _effect-ctx context])

(defn- render-active-effect [context effect-ctx effect]
  (run! #(render-effect % effect-ctx context) effect))

(defmethod info :active-skill
  [[_ {:keys [skill effect-ctx counter]}] entity c]
  (let [{:keys [entity/image skill/effects]} skill]
    (draw-skill-image c
                      image
                      entity
                      (:position entity)
                      (timer/ratio c counter))
    (render-active-effect c
                          effect-ctx
                          ; !! FIXME !!
                          ; (update-effect-ctx c effect-ctx)
                          ; - render does not need to update .. update inside active-skill
                          effects)))

(defmethod above :npc-sleeping
  [_ entity c]
  (let [[x y] (:position entity)]
    (c/draw-text c
                 {:text "zzz"
                  :x x
                  :y (+ y (:half-height entity))
                  :up? true})))

(defmethod below :player-item-on-cursor
  [[_ {:keys [item]}] entity c]
  (when (world-item? c)
    (c/draw-centered c
                     (:entity/image item)
                     (item-place-position c entity))))


(defmethod entity/draw-gui-view :player-item-on-cursor
  [[_ {:keys [eid]}] c]
  (when (not (world-item? c))
    (c/draw-centered c
                     (:entity/image (:entity/item-on-cursor @eid))
                     (c/mouse-position c))))

(defmethod below :stunned
  [_ entity c]
  (c/circle c (:position entity) 0.5 [1 1 1 0.6]))

(defmethod above :entity/string-effect
  [[_ {:keys [text]}] entity c]
  (let [[x y] (:position entity)]
    (c/draw-text c
                 {:text text
                  :x x
                  :y (+ y
                        (:half-height entity)
                        (c/pixels->world-units c 5))
                  :scale 2
                  :up? true})))

; TODO draw opacity as of counter ratio?
(defmethod above :entity/temp-modifier
  [_ entity c]
  (c/filled-circle c (:position entity) 0.5 [0.5 0.5 0.5 0.4]))

(defmethod render-effect :effects/target-all
  [_ {:keys [effect/source]} c]
  (let [source* @source]
    (doseq [target* (map deref (creatures-in-los-of-player c))]
      (c/line c
              (:position source*) #_(start-point source* target*)
              (:position target*)
              [1 0 0 0.5]))))

(defmethod render-effect :effects/target-entity
  [[_ {:keys [maxrange]}] {:keys [effect/source effect/target]} c]
  (when target
    (let [source* @source
          target* @target]
      (c/line c
              (entity/start-point source* target*)
              (entity/end-point source* target* maxrange)
              (if (entity/in-range? source* target* maxrange)
                [1 0 0 0.5]
                [1 1 0 0.5])))))

(def ^:private ^:dbg-flag show-body-bounds false)

(defn- render-entities
  "Draws all active entities, sorted by the `:z-order` and with the render systems `below`, `default`, `above`, `info` for each z-order if the entity is in line-of-sight? to the player entity or is an `:z-order/effect`.

  Optionally for debug purposes body rectangles can be drown which show white for collidings and gray for non colliding entities.

  If an error is thrown during rendering, the entity body drawn with a red rectangle and the error is pretty printed to the console."
  [{:keys [cdq.context/player-eid] :as c}]
  (let [entities (map deref (active-entities c))
        player @player-eid]
    (doseq [[z-order entities] (sort-by-order (group-by :z-order entities)
                                              first
                                              render-z-order)
            system [below default above info]
            entity entities
            :when (or (= z-order :z-order/effect)
                      (line-of-sight? c player entity))]
      (try
       (when show-body-bounds
         (draw-body-rect c entity (if (:collides? entity) :white :gray)))
       (run! #(system % entity c) entity)
       (catch Throwable t
         (draw-body-rect c entity :red)
         (pretty-pst t))))))

(defn draw-world-view [c]
  (c/draw-on-world-view c
                        (fn [c]
                          (render-before-entities c)
                          ; FIXME position DRY (from player)
                          (render-entities c)
                          (render-after-entities c)))
  c)
