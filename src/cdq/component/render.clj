(ns cdq.component.render
  (:require [anvil.effect :as effect]
            [anvil.entity :as entity]
            [cdq.context :as world :refer [finished-ratio]]
            [clojure.component :as component]
            [gdl.context :as c]
            [gdl.val-max :as val-max]))

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
                          (- (* width ratio)
                             (* 2 border))
                          (- height
                             (* 2 border))
                          (hpbar-color ratio)))))

(defmethod component/render-default :entity/line-render
  [[_ {:keys [thick? end color]}] entity c]
  (let [position (:position entity)]
    (if thick?
      (c/with-line-width c 4
        #(c/line c position end color))
      (c/line c position end color))))

(defmethod component/render-default :entity/clickable
  [[_ {:keys [text]}] {:keys [entity/mouseover?] :as entity} c]
  (when (and mouseover? text)
    (let [[x y] (:position entity)]
      (c/draw-text c
                   {:text text
                    :x x
                    :y (+ y (:half-height entity))
                    :up? true}))))

(defmethod component/render-info :entity/hp
  [_ entity c]
  (let [ratio (val-max/ratio (entity/hitpoints entity))]
    (when (or (< ratio 1) (:entity/mouseover? entity))
      (draw-hpbar c entity ratio))))

(defmethod component/render-default :entity/image
  [[_ image] entity c]
  (c/draw-rotated-centered c
                           image
                           (or (:rotation-angle entity) 0)
                           (:position entity)))

(def ^:private outline-alpha 0.4)
(def ^:private enemy-color    [1 0 0 outline-alpha])
(def ^:private friendly-color [0 1 0 outline-alpha])
(def ^:private neutral-color  [1 1 1 outline-alpha])

(defmethod component/render-below :entity/mouseover?
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

(defmethod component/render-above :entity/string-effect
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
(defmethod component/render-above :entity/temp-modifier
  [_ entity c]
  (c/filled-circle c (:position entity) 0.5 [0.5 0.5 0.5 0.4]))

(defmethod component/render-effect :effects/target-all
  [_ {:keys [effect/source]} c]
  (let [source* @source]
    (doseq [target* (map deref (world/creatures-in-los-of-player c))]
      (c/line c
              (:position source*) #_(start-point source* target*)
              (:position target*)
              [1 0 0 0.5]))))

(defmethod component/render-effect :effects/target-entity
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

(defmethod component/render-above :npc-sleeping
  [_ entity c]
  (let [[x y] (:position entity)]
    (c/draw-text c
                 {:text "zzz"
                  :x x
                  :y (+ y (:half-height entity))
                  :up? true})))
